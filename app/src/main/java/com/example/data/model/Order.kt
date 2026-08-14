package com.example.data.model

data class Order(
    val id: String,
    val storeId: String,
    val storeName: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryFee: Double,
    val discount: Double = 0.0,
    val walletDiscount: Double = 0.0,
    val loyaltyPointsUsed: Int = 0,
    val loyaltyDiscount: Double = 0.0,
    val total: Double,
    val paymentMethod: String,
    val deliveryAddress: String,
    val status: String = "Em preparação",
    val createdAt: String,
    val confirmedAt: String = "",
    val deliveryPin: String = "",
    val neighborhood: String = "",
    val driverId: String = "",
    val deliveryConfirmedByClient: Boolean = false
)
