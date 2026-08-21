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
        SearchCategory("pastel", "Pastel & Salgados", "bakery_dining", listOf("pastel", "pasteis", "salgados", "salgado")),
        SearchCategory("churrasco", "Churrasco", "kebab_dining", listOf("churrascaria", "carnes", "churrasco", "carne"))
    )

    val categories = searchCategories.map { CategoryItem(it.id, it.name, it.iconName) }

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()
    private val availabilityRefreshMutex = Mutex()

    /**
     * Atualiza o catálogo sem apagar o último snapshot válido em caso de queda ou timeout.
     * Uma resposta remota vazia e bem-sucedida é aplicada, pois pode indicar que não há
     * entregadores disponíveis para as lojas próprias naquele instante.
     */
    suspend fun refreshStoresFromSupabase(): StoreRefreshResult {
        val catalog = SupabaseClient.fetchActiveStores()
        if (catalog.isSuccess) {
            _stores.value = catalog.stores
            return StoreRefreshResult(isSuccess = true, stores = catalog.stores)
        }
        return StoreRefreshResult(isSuccess = false, stores = _stores.value)
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
