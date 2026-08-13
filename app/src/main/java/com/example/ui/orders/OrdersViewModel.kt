package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.remote.SupabaseClient
import com.example.data.repository.CartRepository
import com.example.data.repository.CartState
import com.example.data.repository.OrderRepository
import com.example.data.repository.StoreRepository
import com.example.data.repository.UserSessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PixPaymentUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val pixCode: String? = null,
    val qrCodeBase64: String? = null,
    val errorMessage: String? = null
)

data class PixDirectPaymentUiState(
    val order: Order? = null,
    val storeName: String = "",
    val pixKey: String = "",
    val pixKeyType: String = "",
    val beneficiary: String = "",
    val instructions: String = "",
    val isUploading: Boolean = false,
    val proofSent: Boolean = false,
    val errorMessage: String? = null
)

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
    val paymentMethod: String = "",
    val changeForAmount: String = "",

    // Flow status
    val isPlacingOrder: Boolean = false,
    val placedOrderSuccess: Order? = null,
    val errorMessage: String? = null,
    val pixPayment: PixPaymentUiState? = null,
    val pixDirectPayment: PixDirectPaymentUiState? = null
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
            cep = session.addressCep,
            street = session.addressStreet,
            number = session.addressNumber,
            neighborhood = session.addressNeighborhood,
            city = session.addressCity
        )
        refreshOrders()
    }

    fun refreshOrders() {
        val session = UserSessionRepository.userSession.value
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            OrderRepository.replaceOrders(emptyList())
            return
        }
        viewModelScope.launch {
            val remoteOrders = SupabaseClient.fetchOrdersForClient(session.userId, session.accessToken)
            OrderRepository.replaceOrders(remoteOrders)
        }
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

        val storeId = cart.storeId
        val store = storeId?.let { StoreRepository.getStoreById(it) }
        val session = UserSessionRepository.userSession.value
        if (storeId.isNullOrBlank() || store == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível identificar a loja do pedido. Volte à sacola e tente novamente.")
            return
        }
        if (!store.isOpen) {
            _uiState.value = _uiState.value.copy(errorMessage = "Esta loja está fechada no momento.")
            return
        }
        if (store.minOrder > 0.0 && cart.subtotal < store.minOrder) {
            val missing = store.minOrder - cart.subtotal
            _uiState.value = _uiState.value.copy(
                errorMessage = "Pedido mínimo: R$ ${String.format("%.2f", store.minOrder).replace(".", ",")}. Faltam R$ ${String.format("%.2f", missing).replace(".", ",")}.")
            return
        }
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sessão expirou. Entre novamente para finalizar o pedido.")
            return
        }
        if (_uiState.value.paymentMethod.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Selecione a forma de pagamento.")
            return
        }

        val normalizedPaymentMethod = when (_uiState.value.paymentMethod.lowercase()) {
            "pix", "pix online" -> "pix"
            "pix na maquininha" -> "pix_machine"
            "pix direto" -> "pix_direto"
            "cartão", "cartão na entrega" -> "cartao"
            "dinheiro", "dinheiro na entrega" -> "dinheiro"
            else -> _uiState.value.paymentMethod.lowercase()
        }
        val changeRaw = _uiState.value.changeForAmount.replace(",", ".").trim()
        val changeDouble = changeRaw.toDoubleOrNull()
        if (normalizedPaymentMethod == "dinheiro") {
            if (changeRaw.isBlank() || changeDouble == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Informe o valor do troco para pagamento em dinheiro.")
                return
            }
            if (changeDouble < cart.total) {
                val formattedTotal = String.format("R$ %.2f", cart.total).replace(".", ",")
                _uiState.value = _uiState.value.copy(errorMessage = "O valor para troco deve ser igual ou maior que o total do pedido ($formattedTotal).")
                return
            }
        }

        // Validate Address if Delivery
        if (cart.deliveryType == "DELIVERY") {
            if (_uiState.value.street.isBlank() || _uiState.value.number.isBlank() || _uiState.value.neighborhood.isBlank() || _uiState.value.city.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Preencha rua, número, bairro e cidade para a entrega.")
                return
            }
        }

        _uiState.value = _uiState.value.copy(isPlacingOrder = true, errorMessage = null)

        viewModelScope.launch {
            val storeName = store.name
            val deliveryNeighborhood = if (cart.deliveryType == "RETIRADA") "RETIRADA" else _uiState.value.neighborhood.trim()
            val formattedAddress = if (cart.deliveryType == "RETIRADA") {
                "Retirada na loja"
            } else {
                val st = _uiState.value.street.trim()
                val num = _uiState.value.number.trim()
                val neigh = _uiState.value.neighborhood.trim()
                val cty = _uiState.value.city.trim()
                val comp = if (_uiState.value.complement.isNotBlank()) ", ${_uiState.value.complement.trim()}" else ""
                "$st, $num - $neigh, $cty$comp"
            }

            val result = OrderRepository.placeOrder(
                storeId = storeId,
                storeName = storeName,
                items = cart.items,
                subtotal = cart.subtotal,
                deliveryFee = cart.deliveryFee,
                discount = cart.discountAmount,
                paymentMethod = normalizedPaymentMethod,
                deliveryAddress = formattedAddress,
                neighborhood = deliveryNeighborhood,
                clientId = session.userId,
                accessToken = session.accessToken,
                needsChange = normalizedPaymentMethod == "dinheiro",
                changeFor = if (normalizedPaymentMethod == "dinheiro") changeDouble else null
            )

            result.onSuccess { newOrder ->
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    placedOrderSuccess = newOrder,
                    couponCode = "",
                    changeForAmount = ""
                )
                refreshOrders()
                onSuccess(newOrder)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    errorMessage = error.message ?: "Não foi possível enviar o pedido. Tente novamente."
                )
            }
        }
    }

    fun generatePixPayment(order: Order) {
        val session = UserSessionRepository.userSession.value
        if (!session.isLoggedIn || session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sessão expirou. Entre novamente para pagar com PIX.")
            return
        }
        _uiState.value = _uiState.value.copy(
            pixPayment = PixPaymentUiState(order = order, isLoading = true),
            errorMessage = null
        )
        viewModelScope.launch {
            val response = SupabaseClient.generatePixForOrder(
                order = order,
                payerFullName = session.name,
                payerDocument = session.cpfCnpj,
                accessToken = session.accessToken
            )
            _uiState.value = _uiState.value.copy(
                pixPayment = if (response.isSuccess) {
                    PixPaymentUiState(
                        order = order,
                        pixCode = response.pixCode,
                        qrCodeBase64 = response.qrCodeBase64
                    )
                } else {
                    PixPaymentUiState(order = order, errorMessage = response.errorMessage ?: "Não foi possível gerar o PIX.")
                }
            )
        }
    }

    fun dismissPixPayment() {
        _uiState.value = _uiState.value.copy(pixPayment = null)
    }

    fun openPixDirectPayment(order: Order) {
        viewModelScope.launch {
            val store = StoreRepository.getStoreById(order.storeId) ?: SupabaseClient.fetchStoreById(order.storeId)
            if (store == null || !store.pixDirectEnabled || store.pixDirectKey.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "A chave PIX direto desta loja não está disponível.")
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                pixDirectPayment = PixDirectPaymentUiState(
                    order = order,
                    storeName = store.name,
                    pixKey = store.pixDirectKey,
                    pixKeyType = store.pixDirectKeyType,
                    beneficiary = store.pixDirectBeneficiary,
                    instructions = store.pixDirectInstructions
                ),
                errorMessage = null
            )
        }
    }

    fun uploadPixDirectProof(bytes: ByteArray, mimeType: String, extension: String) {
        val state = _uiState.value.pixDirectPayment ?: return
        val order = state.order ?: return
        val session = UserSessionRepository.userSession.value
        if (session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                pixDirectPayment = state.copy(errorMessage = "Sua sessão expirou. Entre novamente para enviar o comprovante.")
            )
            return
        }
        _uiState.value = _uiState.value.copy(pixDirectPayment = state.copy(isUploading = true, errorMessage = null))
        viewModelScope.launch {
            val result = SupabaseClient.uploadPixDirectProof(order, bytes, mimeType, extension, session.accessToken)
            val current = _uiState.value.pixDirectPayment ?: state
            result.onSuccess {
                _uiState.value = _uiState.value.copy(pixDirectPayment = current.copy(isUploading = false, proofSent = true))
                refreshOrders()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    pixDirectPayment = current.copy(isUploading = false, errorMessage = error.message ?: "Não foi possível enviar o comprovante.")
                )
            }
        }
    }

    fun dismissPixDirectPayment() {
        _uiState.value = _uiState.value.copy(pixDirectPayment = null)
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
