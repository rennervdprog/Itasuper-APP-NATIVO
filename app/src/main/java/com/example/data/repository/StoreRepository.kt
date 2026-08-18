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
     * Atualiza o catálogo sem apagar o último snapshot válido em caso de queda,
     * timeout ou resposta temporariamente vazia do backend.
     */
    suspend fun refreshStoresFromSupabase(): Boolean {
        val activeStores = SupabaseClient.fetchActiveStores()
        if (activeStores.isNotEmpty()) {
            _stores.value = activeStores
            return true
        }
        return _stores.value.isNotEmpty()
    }

    private val _lastOrder = MutableStateFlow<LastOrder?>(null)
    val lastOrder: StateFlow<LastOrder?> = _lastOrder.asStateFlow()

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }
}
