package com.example.data.model

data class Product(
    val id: String,
    val storeId: String,
    val name: String,
    val description: String,
    val price: Double,
    val originalPrice: Double? = null,
    val category: String,
    val imageUrl: String = "",
    val isAvailable: Boolean = true
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String = ""
) {
    val totalPrice: Double
        get() = product.price * quantity
}
