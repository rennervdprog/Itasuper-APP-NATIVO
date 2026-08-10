package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.remote.SupabaseClient
import com.example.data.repository.CartRepository
import com.example.data.repository.CartState
import com.example.data.repository.OrderRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val couponCode: String = "",
    val couponLoading: Boolean = false,
    val couponError: String? = null,
    
    // Address state
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val complement: String = "",
    val isSearchingCep: Boolean = false,
    val cepError: String? = null,

    // Payment state
    val paymentMethod: String = "PIX",
    val changeForAmount: String = "",

    // Flow status
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

    init {
        // Load default address from UserSession
        val session = UserSessionRepository.userSession.value
        _uiState.value = _uiState.value.copy(
            cep = session.addressCep.ifBlank { "23810-000" },
            street = session.addressStreet.ifBlank { "Rua Central" },
            number = session.addressNumber.ifBlank { "100" },
            neighborhood = session.addressNeighborhood.ifBlank { "Centro" },
            city = session.addressCity.ifBlank { "Itaguaí" }
        )
    }

    fun updateQuantity(productId: String, newQty: Int) {
        CartRepository.updateQuantity(productId, newQty)
    }

    fun clearCart() {
        CartRepository.clearCart()
        _uiState.value = _uiState.value.copy(
            couponCode = "",
            couponError = null,
            errorMessage = null
        )
    }

    fun setDeliveryType(type: String) {
        CartRepository.setDeliveryType(type)
    }

    fun onCouponCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(
            couponCode = code.uppercase(),
            couponError = null
        )
    }

    fun applyCoupon() {
        val code = _uiState.value.couponCode.trim().uppercase()
        val cart = cartState.value

        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(couponError = "Digite o código do cupom.")
            return
        }

        _uiState.value = _uiState.value.copy(couponLoading = true, couponError = null)

        viewModelScope.launch {
            val coupon = SupabaseClient.fetchCoupon(code, cart.storeId)
            
            if (coupon == null) {
                _uiState.value = _uiState.value.copy(
                    couponLoading = false,
                    couponError = "Cupom '$code' não encontrado ou não aplicável a esta loja."
                )
                return@launch
            }

            if (cart.subtotal < coupon.minOrderValue) {
                val minStr = String.format("R$ %.2f", coupon.minOrderValue).replace(".", ",")
                _uiState.value = _uiState.value.copy(
                    couponLoading = false,
                    couponError = "Valor mínimo do pedido para este cupom é $minStr."
                )
                return@launch
            }

            val discount = if (coupon.discountType.lowercase().contains("percent")) {
                cart.subtotal * (coupon.discountValue / 100.0)
            } else {
                coupon.discountValue
            }

            CartRepository.applyCoupon(coupon, discount)
            _uiState.value = _uiState.value.copy(
                couponLoading = false,
                couponError = null
            )
        }
    }

    fun removeCoupon() {
        CartRepository.removeCoupon()
        _uiState.value = _uiState.value.copy(
            couponCode = "",
            couponError = null
        )
    }

    // Address & ViaCEP handlers
    fun updateCep(value: String) {
        _uiState.value = _uiState.value.copy(cep = value, cepError = null)
    }

    fun updateStreet(value: String) {
        _uiState.value = _uiState.value.copy(street = value)
    }

    fun updateNumber(value: String) {
        _uiState.value = _uiState.value.copy(number = value)
    }

    fun updateNeighborhood(value: String) {
        _uiState.value = _uiState.value.copy(neighborhood = value)
    }

    fun updateCity(value: String) {
        _uiState.value = _uiState.value.copy(city = value)
    }

    fun updateComplement(value: String) {
        _uiState.value = _uiState.value.copy(complement = value)
    }

    fun searchAddressByCep() {
        val cepInput = _uiState.value.cep.trim()
        if (cepInput.isBlank()) {
            _uiState.value = _uiState.value.copy(cepError = "Informe o CEP.")
            return
        }

        _uiState.value = _uiState.value.copy(isSearchingCep = true, cepError = null)

        viewModelScope.launch {
            val result = SupabaseClient.fetchAddressByCep(cepInput)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    isSearchingCep = false,
                    street = result.street.ifBlank { _uiState.value.street },
                    neighborhood = result.neighborhood.ifBlank { _uiState.value.neighborhood },
                    city = result.city.ifBlank { _uiState.value.city },
                    cepError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSearchingCep = false,
                    cepError = "CEP não encontrado. Preencha o endereço manualmente."
                )
            }
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(
            paymentMethod = method,
            errorMessage = null
        )
    }

    fun updateChangeForAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            changeForAmount = amount,
            errorMessage = null
        )
    }

    fun checkoutOrder(onSuccess: (Order) -> Unit) {
        val cart = cartState.value
        if (cart.items.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sacola está vazia.")
            return
        }

        // Validate Cash payment & Change amount
        if (_uiState.value.paymentMethod.lowercase().contains("dinheiro")) {
            val changeRaw = _uiState.value.changeForAmount.replace(",", ".").trim()
            val changeDouble = changeRaw.toDoubleOrNull()
            if (changeRaw.isBlank() || changeDouble == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Informe o valor do troco para pagamento em dinheiro."
                )
                return
            }
            if (changeDouble < cart.total) {
                val formattedTotal = String.format("R$ %.2f", cart.total).replace(".", ",")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "O valor para troco deve ser igual ou maior que o total do pedido ($formattedTotal)."
                )
                return
            }
        }

        // Validate Address if Delivery
        if (cart.deliveryType == "DELIVERY") {
            if (_uiState.value.street.isBlank() || _uiState.value.number.isBlank() || _uiState.value.neighborhood.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Preencha rua, número e bairro para a entrega."
                )
                return
            }
        }

        _uiState.value = _uiState.value.copy(isPlacingOrder = true, errorMessage = null)

        viewModelScope.launch {
            val storeId = cart.storeId ?: "s1"
            val storeName = cart.storeName.ifBlank { "Loja ItaSuper" }
            
            val formattedAddress = if (cart.deliveryType == "RETIRADA") {
                "Retirada na loja: $storeName"
            } else {
                val st = _uiState.value.street.trim()
                val num = _uiState.value.number.trim()
                val neigh = _uiState.value.neighborhood.trim()
                val cty = _uiState.value.city.trim()
                val comp = if (_uiState.value.complement.isNotBlank()) " (${_uiState.value.complement.trim()})" else ""
                "$st, $num - $neigh, $cty$comp"
            }

            val payMethodText = if (_uiState.value.paymentMethod.lowercase().contains("dinheiro")) {
                "Dinheiro (Troco para R$ ${_uiState.value.changeForAmount})"
            } else {
                _uiState.value.paymentMethod
            }

            val newOrder = OrderRepository.placeOrder(
                storeId = storeId,
                storeName = storeName,
                items = cart.items,
                subtotal = cart.subtotal,
                deliveryFee = cart.deliveryFee,
                discount = cart.discountAmount,
                paymentMethod = payMethodText,
                deliveryAddress = formattedAddress
            )

            _uiState.value = _uiState.value.copy(
                isPlacingOrder = false,
                placedOrderSuccess = newOrder,
                couponCode = "",
                changeForAmount = ""
            )

            onSuccess(newOrder)
        }
    }

    fun dismissSuccessModal() {
        _uiState.value = _uiState.value.copy(placedOrderSuccess = null)
    }

    fun repeatOrder(order: Order) {
        for (item in order.items) {
            CartRepository.addProduct(
                product = item.product,
                storeName = order.storeName,
                quantity = item.quantity,
                notes = item.notes
            )
        }
    }
}
