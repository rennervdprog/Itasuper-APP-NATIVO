package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StoreRepository {

    val categories = listOf(
        CategoryItem("todas", "Todas", "apps"),
        CategoryItem("lanches", "Lanches", "fastfood"),
        CategoryItem("pizza", "Pizza", "local_pizza"),
        CategoryItem("mercado", "Mercado", "shopping_cart"),
        CategoryItem("farmacia", "Farmácia", "medical_services"),
        CategoryItem("bebidas", "Bebidas", "local_bar")
    )

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    suspend fun refreshStoresFromSupabase(): Boolean {
        val activeStores = SupabaseClient.fetchActiveStores()
        _stores.value = activeStores
        return activeStores.isNotEmpty()
    }

    private val _lastOrder = MutableStateFlow<LastOrder?>(null)
    val lastOrder: StateFlow<LastOrder?> = _lastOrder.asStateFlow()

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }
}
