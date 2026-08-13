package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.SavedAddress
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
    val showAddressEditor: Boolean = true,
    val showGpsAddressConfirmation: Boolean = false,
    val usingGpsAddress: Boolean = false,
    val savedAddresses: List<SavedAddress> = emptyList(),
    val selectedSavedAddressId: String? = null,
    val isLoadingSavedAddresses: Boolean = false,
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
    val pixDirectPayment: PixDirectPaymentUiState? = null,
    val confirmingDeliveryOrderId: String? = null,
    val cancellingOrderId: String? = null
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

    private var addressEditedByCustomer = false

    init {
        applySavedAddress(UserSessionRepository.userSession.value)
        viewModelScope.launch {
            UserSessionRepository.userSession.collect { session ->
                if (!addressEditedByCustomer) applySavedAddress(session)
                loadSavedAddresses(session)
            }
        }
        refreshOrders()
        validateRestoredCart()
    }

    private fun validateRestoredCart() {
        val cart = CartRepository.cartState.value
        val storeId = cart.storeId ?: return
        if (cart.items.isEmpty()) return
        viewModelScope.launch {
            val products = SupabaseClient.fetchProductsForStore(storeId)
            if (products.isEmpty()) return@launch
            val before = CartRepository.cartState.value.totalItemCount
            CartRepository.validateAgainstCatalog(products)
            val after = CartRepository.cartState.value.totalItemCount
            if (after < before) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Alguns itens da sua sacola não estão mais disponíveis e foram removidos."
                )
            }
        }
    }

    private fun applySavedAddress(session: com.example.data.model.UserSession) {
        val selected = _uiState.value.savedAddresses.firstOrNull { it.id == _uiState.value.selectedSavedAddressId }
            ?: _uiState.value.savedAddresses.firstOrNull { it.isDefault }
            ?: _uiState.value.savedAddresses.firstOrNull()
        if (selected != null) {
            applyAddress(selected, session)
            return
        }
        val hasProfileAddress = session.addressStreet.isNotBlank() &&
            session.addressNumber.isNotBlank() &&
            session.addressNeighborhood.isNotBlank() &&
            session.addressCity.isNotBlank()
        CartRepository.setDeliveryCoordinates(null, null)
        _uiState.value = _uiState.value.copy(
            cep = session.addressCep,
            street = session.addressStreet,
            number = session.addressNumber,
            neighborhood = session.addressNeighborhood,
            city = session.addressCity,
            complement = session.addressComplement,
            showAddressEditor = !hasProfileAddress,
            showGpsAddressConfirmation = hasProfileAddress && activeLocationDiffersFromAddress(session, null),
            usingGpsAddress = false,
            selectedSavedAddressId = null
        )
    }

    private fun applyAddress(address: SavedAddress, session: com.example.data.model.UserSession) {
        CartRepository.setDeliveryCoordinates(address.latitude, address.longitude)
        _uiState.value = _uiState.value.copy(
            cep = address.cep,
            street = address.street,
            number = address.number,
            neighborhood = address.neighborhood,
            city = session.addressCity.ifBlank { session.activeLocationCity },
            complement = listOf(address.complement, address.referencePoint).filter { it.isNotBlank() }.joinToString(" · "),
            showAddressEditor = false,
            showGpsAddressConfirmation = activeLocationDiffersFromAddress(session, address),
            usingGpsAddress = false,
            selectedSavedAddressId = address.id
        )
    }

    private fun activeLocationDiffersFromAddress(session: com.example.data.model.UserSession, address: SavedAddress?): Boolean {
        val latitude = session.activeLocationLatitude
        val longitude = session.activeLocationLongitude
        if (address?.latitude != null && address.longitude != null && latitude != null && longitude != null) {
            return haversineMeters(latitude, longitude, address.latitude, address.longitude) > 300.0
        }
        if (session.activeLocationCity.isBlank() || session.activeLocationStreet.isBlank()) return false
        fun normalized(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
        val street = address?.street ?: session.addressStreet
        val number = address?.number ?: session.addressNumber
        val sameCity = normalized(session.activeLocationCity) == normalized(session.addressCity)
        val sameStreet = normalized(session.activeLocationStreet) == normalized(street)
        val activeNumber = normalized(session.activeLocationNumber)
        val sameNumber = activeNumber.isBlank() || number.isBlank() || activeNumber == normalized(number)
        return !(sameCity && sameStreet && sameNumber)
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        return earthRadius * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    private fun loadSavedAddresses(session: com.example.data.model.UserSession) {
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSavedAddresses = true)
            val addresses = SupabaseClient.fetchSavedAddresses(session.userId, session.accessToken)
            val selectedId = _uiState.value.selectedSavedAddressId
                ?: addresses.firstOrNull { it.isDefault }?.id
                ?: addresses.firstOrNull()?.id
            _uiState.value = _uiState.value.copy(
                savedAddresses = addresses,
                selectedSavedAddressId = selectedId,
                isLoadingSavedAddresses = false
            )
            if (!addressEditedByCustomer) applySavedAddress(session)
        }
    }

    fun selectSavedAddress(address: SavedAddress) {
        addressEditedByCustomer = false
        applyAddress(address, UserSessionRepository.userSession.value)
    }

    fun makeSavedAddressDefault(address: SavedAddress) {
        val session = UserSessionRepository.userSession.value
        viewModelScope.launch {
            if (SupabaseClient.setDefaultSavedAddress(session.userId, address.id, session.accessToken)) {
                loadSavedAddresses(session)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível definir o endereço padrão.")
            }
        }
    }

    fun deleteSavedAddress(address: SavedAddress) {
        val session = UserSessionRepository.userSession.value
        viewModelScope.launch {
            if (SupabaseClient.deleteSavedAddress(session.userId, address.id, session.accessToken)) {
                loadSavedAddresses(session)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível excluir o endereço.")
            }
        }
    }

    fun useSavedAddressForCheckout() {
        addressEditedByCustomer = false
        applySavedAddress(UserSessionRepository.userSession.value)
        _uiState.value = _uiState.value.copy(showGpsAddressConfirmation = false)
    }

    fun saveCurrentAddress(label: String = "Novo endereço") {
        val session = UserSessionRepository.userSession.value
        val state = _uiState.value
        if (session.userId.isBlank() || session.accessToken.isBlank()) return
        if (state.street.isBlank() || state.number.isBlank() || state.neighborhood.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Preencha rua, número e bairro antes de salvar o endereço.")
            return
        }
        viewModelScope.launch {
            val useGpsCoordinates = state.usingGpsAddress
            val saved = SupabaseClient.createSavedAddress(
                userId = session.userId,
                accessToken = session.accessToken,
                address = SavedAddress(
                    label = label,
                    street = state.street.trim(),
                    number = state.number.trim(),
                    complement = state.complement.trim(),
                    neighborhood = state.neighborhood.trim(),
                    cep = state.cep.filter(Char::isDigit),
                    latitude = if (useGpsCoordinates) session.activeLocationLatitude else null,
                    longitude = if (useGpsCoordinates) session.activeLocationLongitude else null,
                    pinConfirmed = useGpsCoordinates && session.activeLocationLatitude != null && session.activeLocationLongitude != null,
                    isDefault = state.savedAddresses.isEmpty()
                )
            )
            if (saved == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível salvar este endereço.")
            } else {
                addressEditedByCustomer = false
                loadSavedAddresses(session)
            }
        }
    }

    fun useGpsAddressForCheckout() {
        val session = UserSessionRepository.userSession.value
        addressEditedByCustomer = true
        CartRepository.setDeliveryCoordinates(session.activeLocationLatitude, session.activeLocationLongitude)
        _uiState.value = _uiState.value.copy(
            street = session.activeLocationStreet.ifBlank { _uiState.value.street },
            number = session.activeLocationNumber,
            neighborhood = session.activeLocationNeighborhood.ifBlank { _uiState.value.neighborhood },
            city = session.activeLocationCity.ifBlank { _uiState.value.city },
            showAddressEditor = true,
            showGpsAddressConfirmation = false,
            usingGpsAddress = true,
            cepError = null
        )
    }

    fun openAddressEditor() {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(showAddressEditor = true, showGpsAddressConfirmation = false)
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

    fun confirmDelivery(order: Order) {
        val session = UserSessionRepository.userSession.value
        if (session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sessão expirou. Entre novamente para confirmar a entrega.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(confirmingDeliveryOrderId = order.id, errorMessage = null)
            val result = SupabaseClient.confirmDeliveryByClient(order.id, session.accessToken)
            _uiState.value = _uiState.value.copy(
                confirmingDeliveryOrderId = null,
                errorMessage = result.exceptionOrNull()?.message
            )
            if (result.isSuccess) refreshOrders()
        }
    }

    fun cancelOrder(order: Order) {
        val session = UserSessionRepository.userSession.value
        if (session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sessão expirou. Entre novamente para cancelar o pedido.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cancellingOrderId = order.id, errorMessage = null)
            val result = SupabaseClient.cancelOrderByClient(order.id, session.accessToken)
            _uiState.value = _uiState.value.copy(
                cancellingOrderId = null,
                errorMessage = result.exceptionOrNull()?.message
            )
            if (result.isSuccess) refreshOrders()
        }
    }

    fun submitOrderRating(order: Order, rating: Int, comment: String, onComplete: (Boolean) -> Unit) {
        val session = UserSessionRepository.userSession.value
        if (session.accessToken.isBlank() || rating !in 1..5) {
            _uiState.value = _uiState.value.copy(errorMessage = "Informe uma avaliação de 1 a 5 estrelas.")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val success = SupabaseClient.submitOrderRating(
                orderId = order.id,
                storeId = order.storeId,
                userId = session.userId,
                rating = rating,
                comment = comment,
                accessToken = session.accessToken
            )
            if (!success) _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível enviar sua avaliação.")
            onComplete(success)
        }
    }

    fun requestRefund(order: Order, reason: String, description: String, onComplete: (Boolean) -> Unit) {
        val session = UserSessionRepository.userSession.value
        val status = order.status.lowercase()
        val refundable = status in setOf("entregue", "finalizado") && order.paymentMethod !in setOf("dinheiro", "pix_machine", "cartao")
        if (!refundable) {
            _uiState.value = _uiState.value.copy(errorMessage = "Este pedido não é elegível para reembolso pela plataforma.")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val success = SupabaseClient.requestRefund(order, session.userId, reason, description, session.accessToken)
            if (!success) _uiState.value = _uiState.value.copy(errorMessage = "Não foi possível enviar a solicitação de reembolso.")
            onComplete(success)
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
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(cep = value, cepError = null, showAddressEditor = true)
    }

    fun updateStreet(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(street = value, showAddressEditor = true)
    }

    fun updateNumber(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(number = value, showAddressEditor = true)
    }

    fun updateNeighborhood(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(neighborhood = value, showAddressEditor = true)
    }

    fun updateCity(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(city = value, showAddressEditor = true)
    }

    fun updateComplement(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(complement = value, showAddressEditor = true)
    }

    fun searchAddressByCep() {
        addressEditedByCustomer = true
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
                changeFor = if (normalizedPaymentMethod == "dinheiro") changeDouble else null,
                clientLatitude = cart.deliveryLatitude,
                clientLongitude = cart.deliveryLongitude,
                coupon = cart.appliedCoupon
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
