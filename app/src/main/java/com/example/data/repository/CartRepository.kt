package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CartState(
    val storeId: String? = null,
    val storeName: String = "",
    val items: List<CartItem> = emptyList()
) {
    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    val subtotal: Double
        get() = items.sumOf { it.totalPrice }
}

object CartRepository {

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    fun addProduct(product: Product, storeName: String, quantity: Int = 1, notes: String = "") {
        val current = _cartState.value
        
        // If adding from a different store, reset cart to new store
        if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
            _cartState.value = CartState(
                storeId = product.storeId,
                storeName = storeName,
                items = listOf(CartItem(product, quantity, notes))
            )
            return
        }

        val existingIndex = current.items.indexOfFirst { it.product.id == product.id }
        val updatedItems = current.items.toMutableList()

        if (existingIndex >= 0) {
            val existing = updatedItems[existingIndex]
            val newQty = existing.quantity + quantity
            updatedItems[existingIndex] = existing.copy(quantity = newQty, notes = notes)
        } else {
            updatedItems.add(CartItem(product, quantity, notes))
        }

        _cartState.value = current.copy(
            storeId = product.storeId,
            storeName = storeName,
            items = updatedItems
        )
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        val current = _cartState.value
        if (newQuantity <= 0) {
            val updatedItems = current.items.filterNot { it.product.id == productId }
            val newStoreId = if (updatedItems.isEmpty()) null else current.storeId
            _cartState.value = current.copy(storeId = newStoreId, items = updatedItems)
        } else {
            val updatedItems = current.items.map {
                if (it.product.id == productId) it.copy(quantity = newQuantity) else it
            }
            _cartState.value = current.copy(items = updatedItems)
        }
    }

    fun clearCart() {
        _cartState.value = CartState()
    }
}
