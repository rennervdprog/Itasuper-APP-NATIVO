package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.withLock

data class SearchCategory(
    val id: String,
    val name: String,
    val iconName: String,
    val matchingTerms: List<String>
)

/** Separa falha de comunicação de uma resposta válida sem lojas elegíveis. */
data class StoreRefreshResult(
    val isSuccess: Boolean,
    val stores: List<Store>
)

object StoreRepository {

    val searchCategories = listOf(
        SearchCategory("lanches", "Lanches", "fastfood", listOf("lanchonete", "hamburgueria", "lanches", "lanche", "burger")),
        SearchCategory("pizzaria", "Pizzaria", "local_pizza", listOf("pizzaria", "pizza")),
        SearchCategory("marmita", "Marmita", "restaurant", listOf("marmitaria", "restaurante", "comida caseira", "marmita")),
        SearchCategory("acai", "Açaí & Sobremesas", "icecream", listOf("acai", "açaí", "sorveteria", "doceria", "confeitaria", "sobremesa")),
        SearchCategory("bebidas", "Bebidas", "local_bar", listOf("adega", "bebidas", "bebida", "cerveja")),
        SearchCategory("mercado", "Mercado", "shopping_cart", listOf("mercado", "supermercado", "hortifruti", "conveniencia")),
        SearchCategory("farmacias", "Farmácias", "pharmacy", listOf("farmacias", "farmácia", "farmacia", "drogaria", "drogarias")),
        SearchCategory("pastel", "Pastel & Salgados", "bakery_dining", listOf("pastel", "pasteis", "salgados", "salgado")),
        SearchCategory("churrasco", "Churrasco", "kebab_dining", listOf("churrascaria", "carnes", "churrasco", "carne"))
    )

    val categories = searchCategories.map { CategoryItem(it.id, it.name, it.iconName) }

    /**
     * Mantém Home e Descobrir alinhados ao filtrar categorias.
     * Além do ID/nome exibido, considera aliases cadastrados para a categoria;
     * por isso uma loja classificada como "Adega" também aparece em Bebidas.
     */
    fun matchesCategory(store: Store, categoryId: String): Boolean {
        if (categoryId.equals("todas", ignoreCase = true)) return true
        val selected = searchCategories.firstOrNull { it.id.equals(categoryId, ignoreCase = true) }
            ?: return false
        val acceptedTerms = (listOf(selected.id, selected.name) + selected.matchingTerms)
            .map(::normalizeCategoryText)
            .filter { it.isNotBlank() }
        val storeCategories = (listOf(store.category) + store.secondaryCategories)
            .map(::normalizeCategoryText)
            .filter { it.isNotBlank() }
        return storeCategories.any { storeCategory ->
            acceptedTerms.any { term ->
                storeCategory == term || storeCategory.contains(term) || term.contains(storeCategory)
            }
        }
    }

    private fun normalizeCategoryText(value: String): String =
        java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .trim()

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()
    private val availabilityRefreshMutex = Mutex()
    private val catalogRefreshMutex = Mutex()
    private const val CATALOG_CACHE_TTL_MS = 2 * 60 * 1000L
    private var lastCatalogRefreshAt = 0L

    /**
     * Atualiza o catálogo sem apagar o último snapshot válido em caso de queda ou timeout.
     * Uma resposta remota vazia e bem-sucedida é aplicada, pois pode indicar que não há
     * entregadores disponíveis para as lojas próprias naquele instante.
     */
    suspend fun refreshStoresFromSupabase(force: Boolean = false): StoreRefreshResult = catalogRefreshMutex.withLock {
        val now = System.currentTimeMillis()
        val hasFreshCatalog = _stores.value.isNotEmpty() &&
            now - lastCatalogRefreshAt in 0..CATALOG_CACHE_TTL_MS
        if (!force && hasFreshCatalog) {
            return@withLock StoreRefreshResult(isSuccess = true, stores = _stores.value)
        }

        val catalog = SupabaseClient.fetchActiveStores()
        if (catalog.isSuccess) {
            _stores.value = catalog.stores
            lastCatalogRefreshAt = System.currentTimeMillis()
            return@withLock StoreRefreshResult(isSuccess = true, stores = catalog.stores)
        }
        StoreRefreshResult(isSuccess = false, stores = _stores.value)
    }

    /**
     * Atualiza somente a disponibilidade dos motoboys das lojas próprias.
     * A fonte é a mesma RPC canônica usada pelo checkout: vínculo aceito,
     * entregador ativo/online e último heartbeat dentro de 13 minutos.
     * Falhas transitórias preservam o último estado conhecido para não piscar
     * nem esconder lojas enquanto a rede está instável.
     */
    suspend fun refreshDriverAvailability(): Boolean = availabilityRefreshMutex.withLock {
        val currentStores = _stores.value
        if (currentStores.none { it.deliveryMode.equals("own", ignoreCase = true) }) return@withLock true

        // A RPC é consultada fora da Main thread e tem limite curto: uma degradação
        // transitória do banco não pode congelar a Home nem substituir um estado válido.
        val onlineDriverStoreIds = withTimeoutOrNull(8_000L) {
            SupabaseClient.fetchStoreIdsWithOnlineDrivers()
        } ?: return@withLock false
        val updatedStores = applyDriverAvailability(currentStores, onlineDriverStoreIds)
        if (updatedStores != currentStores) _stores.value = updatedStores
        true
    }

    internal fun applyDriverAvailability(stores: List<Store>, onlineDriverStoreIds: Set<String>): List<Store> =
        stores.map { store ->
            if (!store.deliveryMode.equals("own", ignoreCase = true)) {
                store
            } else {
                val available = onlineDriverStoreIds.contains(store.id)
                store.copy(
                    hasAvailableDriver = available,
                    deliveryAvailabilityMessage = if (available) {
                        ""
                    } else {
                        "Esta loja está sem entregador disponível no momento."
                    }
                )
            }
        }

    private val _lastOrder = MutableStateFlow<LastOrder?>(null)
    val lastOrder: StateFlow<LastOrder?> = _lastOrder.asStateFlow()

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }
}
