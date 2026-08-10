package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.Product
import com.example.data.model.SelectedAddonItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CartState(
    val storeId: String? = null,
    val storeName: String = "",
    val items: List<CartItem> = emptyList(),
    val deliveryType: String = "DELIVERY", // "DELIVERY" or "RETIRADA"
    val appliedCoupon: Coupon? = null,
    val discountAmount: Double = 0.0
) {
    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    val deliveryFee: Double
        get() {
            if (deliveryType == "RETIRADA" || items.isEmpty() || storeId == null) return 0.0
            val store = StoreRepository.getStoreById(storeId)
            if (store?.isFreeDelivery == true) return 0.0
            val dist = store?.distanceKm ?: 1.5
            val calculated = 2.50 + (dist * 1.20)
            return (Math.round(calculated * 100.0) / 100.0).coerceAtLeast(3.0)
        }

    val total: Double
        get() = (subtotal + deliveryFee - discountAmount).coerceAtLeast(0.0)
}

object CartRepository {

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    fun setDeliveryType(type: String) {
        _cartState.value = _cartState.value.copy(deliveryType = type)
    }

    fun applyCoupon(coupon: Coupon, discount: Double) {
        _cartState.value = _cartState.value.copy(
            appliedCoupon = coupon,
            discountAmount = discount
        )
    }

    fun removeCoupon() {
        _cartState.value = _cartState.value.copy(
            appliedCoupon = null,
            discountAmount = 0.0
        )
    }

    fun addProduct(
        product: Product,
        storeName: String,
        quantity: Int = 1,
        notes: String = "",
        selectedAddons: List<SelectedAddonItem> = emptyList()
    ) {
        val current = _cartState.value
        
        // If adding from a different store, reset cart to new store
        if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
            _cartState.value = CartState(
                storeId = product.storeId,
                storeName = storeName,
                items = listOf(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons))
            )
            return
        }

        val existingIndex = current.items.indexOfFirst { 
            it.product.id == product.id && it.selectedAddons == selectedAddons && it.notes == notes
        }
        val updatedItems = current.items.toMutableList()

        if (existingIndex >= 0) {
            val existing = updatedItems[existingIndex]
            val newQty = existing.quantity + quantity
            updatedItems[existingIndex] = existing.copy(quantity = newQty)
        } else {
            updatedItems.add(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons))
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
            val newCoupon = if (updatedItems.isEmpty()) null else current.appliedCoupon
            val newDiscount = if (updatedItems.isEmpty()) 0.0 else current.discountAmount
            _cartState.value = current.copy(
                storeId = newStoreId,
                items = updatedItems,
                appliedCoupon = newCoupon,
                discountAmount = newDiscount
            )
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
