package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.repository.CartRepository
import com.example.data.repository.CartState
import com.example.data.repository.OrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val selectedTab: Int = 0, // 0 = Sacola, 1 = Histórico
    val couponCode: String = "",
    val couponApplied: String? = null,
    val discountAmount: Double = 0.0,
    val paymentMethod: String = "PIX",
    val changeForAmount: String = "",
    val deliveryAddress: String = "Av. 22 de Maio, 1500, Centro - Itaboraí",
    val isPlacingOrder: Boolean = false,
    val placedOrderSuccess: Order? = null,
    val errorMessage: String? = null
)

class OrdersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    val cartState: StateFlow<CartState> = CartRepository.cartState

    val ordersList: StateFlow<List<Order>> = OrderRepository.orders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun updateQuantity(productId: String, newQty: Int) {
        CartRepository.updateQuantity(productId, newQty)
    }

    fun clearCart() {
        CartRepository.clearCart()
        _uiState.value = _uiState.value.copy(
            couponApplied = null,
            discountAmount = 0.0,
            couponCode = ""
        )
    }

    fun onCouponCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(couponCode = code.uppercase())
    }

    fun applyCoupon() {
        val code = _uiState.value.couponCode.trim().uppercase()
        if (code == "ITASUPER10" || code == "BEMVINDO") {
            _uiState.value = _uiState.value.copy(
                couponApplied = code,
                discountAmount = 10.0,
                errorMessage = null
            )
        } else if (code == "PRIMEIRACOMPRA") {
            val sub = cartState.value.subtotal
            val disc = sub * 0.15 // 15% OFF
            _uiState.value = _uiState.value.copy(
                couponApplied = code,
                discountAmount = disc,
                errorMessage = null
            )
        } else if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Digite o código do cupom.")
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Cupom inválido. Tente ITASUPER10 ou BEMVINDO."
            )
        }
    }

    fun removeCoupon() {
        _uiState.value = _uiState.value.copy(
            couponApplied = null,
            discountAmount = 0.0,
            couponCode = ""
        )
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun updateChangeForAmount(amount: String) {
        _uiState.value = _uiState.value.copy(changeForAmount = amount)
    }

    fun updateDeliveryAddress(address: String) {
        _uiState.value = _uiState.value.copy(deliveryAddress = address)
    }

    fun checkoutOrder() {
        val cart = cartState.value
        if (cart.items.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sacola está vazia.")
            return
        }

        _uiState.value = _uiState.value.copy(isPlacingOrder = true, errorMessage = null)

        viewModelScope.launch {
            delay(1200) // Realistic network delay simulation

            val storeId = cart.storeId ?: "s1"
            val storeName = cart.storeName.ifBlank { "Loja ItaSuper" }
            val deliveryFee = 5.0 // Fixed ItaSuper delivery rate
            val subtotal = cart.subtotal
            val discount = _uiState.value.discountAmount

            val newOrder = OrderRepository.placeOrder(
                storeId = storeId,
                storeName = storeName,
                items = cart.items,
                subtotal = subtotal,
                deliveryFee = deliveryFee,
                discount = discount,
                paymentMethod = _uiState.value.paymentMethod,
                deliveryAddress = _uiState.value.deliveryAddress
            )

            _uiState.value = _uiState.value.copy(
                isPlacingOrder = false,
                placedOrderSuccess = newOrder,
                couponApplied = null,
                discountAmount = 0.0,
                couponCode = ""
            )
        }
    }

    fun dismissSuccessModal() {
        _uiState.value = _uiState.value.copy(placedOrderSuccess = null)
    }

    fun repeatOrder(order: Order) {
        // Add items back to cart
        for (item in order.items) {
            CartRepository.addProduct(
                product = item.product,
                storeName = order.storeName,
                quantity = item.quantity,
                notes = item.notes
            )
        }
        _uiState.value = _uiState.value.copy(selectedTab = 0)
    }
}
