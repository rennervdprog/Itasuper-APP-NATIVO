package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _lastOrder = MutableStateFlow<LastOrder?>(null)
    val lastOrder: StateFlow<LastOrder?> = _lastOrder.asStateFlow()

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }
}
