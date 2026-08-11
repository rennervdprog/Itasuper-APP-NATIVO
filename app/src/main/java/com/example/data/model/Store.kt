package com.example.data.model

data class StoreSettings(
    val pizzaHalfEnabled: Boolean = true,
    val pastelHalfEnabled: Boolean = true,
    val pizzaMaxFlavors: Int = 4,
    val pastelMaxFlavors: Int = 4,
    val pastelMaxComplements: Int = 3,
    val pizzaPriceMode: String = "maior",
    val pastelPriceMode: String = "maior",
    val pizzaSingleSize: Boolean = false,
    val pastelSingleSize: Boolean = false
)

data class Store(
    val id: String,
    val name: String,
    val category: String,
    val secondaryCategories: List<String> = emptyList(),
    val rating: Double,
    val deliveryTime: String,
    val deliveryFee: String,
    val isFreeDelivery: Boolean = false,
    val isOpen: Boolean = true,
    val distanceKm: Double = 1.2,
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val minOrder: Double = 10.0,
    val createdAt: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val settings: StoreSettings = StoreSettings(),
    val deliveryMode: String = "direto",
    val ownDeliveryFee: Double = 0.0,
    val address: String = "Av. 22 de Maio, Centro, Itaboraí - RJ",
    val whatsapp: String = "5521999999999",
    val openingHours: List<StoreOpeningHour> = emptyList()
)

data class StoreOpeningHour(
    val storeId: String,
    val dayOfWeek: Int,
    val dayOfWeekStr: String = "",
    val openTime: String,
    val closeTime: String,
    val isClosedAllDay: Boolean = false
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
