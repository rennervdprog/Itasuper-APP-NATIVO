package com.example.data.model

data class Store(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val deliveryTime: String,
    val deliveryFee: String,
    val isFreeDelivery: Boolean = false,
    val isOpen: Boolean = true,
    val distanceKm: Double = 1.2,
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val minOrder: Double = 10.0
)

data class CategoryItem(
    val id: String,
    val name: String,
    val iconName: String
)

data class LastOrder(
    val id: String,
    val storeId: String,
    val storeName: String,
    val storeLogoUrl: String,
    val dateText: String,
    val itemsSummary: String,
    val totalPrice: Double
)
