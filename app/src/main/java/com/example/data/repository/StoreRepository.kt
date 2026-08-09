package com.example.data.repository

import com.example.data.model.CategoryItem
import com.example.data.model.LastOrder
import com.example.data.model.Store
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

    private val _stores = MutableStateFlow(
        listOf(
            Store(
                id = "pastelao_carioca",
                name = "Pastelao Carioca",
                category = "Lanches",
                rating = 4.8,
                deliveryTime = "30-40 min",
                deliveryFee = "R$ 4,90",
                isFreeDelivery = false,
                isOpen = true,
                distanceKm = 1.1,
                minOrder = 15.0
            ),
            Store(
                id = "aguia_pizzaria",
                name = "Águia Pizzaria",
                category = "Pizza",
                rating = 4.9,
                deliveryTime = "40-50 min",
                deliveryFee = "Grátis",
                isFreeDelivery = true,
                isOpen = true,
                distanceKm = 2.4,
                minOrder = 30.0
            ),
            Store(
                id = "itasuper_mercado",
                name = "ItaSuper Mercado",
                category = "Mercado",
                rating = 4.7,
                deliveryTime = "25-35 min",
                deliveryFee = "R$ 6,00",
                isFreeDelivery = false,
                isOpen = true,
                distanceKm = 0.8,
                minOrder = 40.0
            ),
            Store(
                id = "farmacia_popular",
                name = "Farmácia Popular Ita",
                category = "Farmácia",
                rating = 4.9,
                deliveryTime = "20-30 min",
                deliveryFee = "Grátis",
                isFreeDelivery = true,
                isOpen = true,
                distanceKm = 1.5,
                minOrder = 20.0
            ),
            Store(
                id = "adega_itasuper",
                name = "Adega ItaSuper Express",
                category = "Bebidas",
                rating = 4.6,
                deliveryTime = "15-25 min",
                deliveryFee = "R$ 3,50",
                isFreeDelivery = false,
                isOpen = true,
                distanceKm = 0.5,
                minOrder = 25.0
            )
        )
    )
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _lastOrder = MutableStateFlow<LastOrder?>(
        LastOrder(
            id = "ord_102938",
            storeId = "pastelao_carioca",
            storeName = "Pastelao Carioca",
            storeLogoUrl = "",
            dateText = "26 de julho · 1 itens",
            itemsSummary = "1x Pastel Especial",
            totalPrice = 20.75
        )
    )
    val lastOrder: StateFlow<LastOrder?> = _lastOrder.asStateFlow()

    fun getStoreById(id: String): Store? {
        return _stores.value.find { it.id == id }
    }
}
