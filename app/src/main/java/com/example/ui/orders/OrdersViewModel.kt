package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.DeliveryAddressInput
import com.example.data.model.DeliveryQuote
import com.example.data.model.DeliveryQuoteFailure
import com.example.data.model.Order
import com.example.data.model.RefundEligibility
import com.example.data.model.LoyaltyConfig
import com.example.data.model.SavedAddress
import com.example.data.model.normalizeBrazilianUf
import com.example.data.model.preorderReleaseAtMillis
import com.example.data.model.toSnapshot
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

    // Benefícios financeiros do cliente
    val benefitsStoreId: String? = null,
    val isLoadingBenefits: Boolean = false,
    val walletBalance: Double = 0.0,
    val useWallet: Boolean = false,
    val walletDiscount: Double = 0.0,
    val loyaltyConfig: LoyaltyConfig? = null,
    val loyaltyPointsAvailable: Int = 0,
    val loyaltyPointsToUse: Int = 0,
    val loyaltyMaxPointsUsable: Int = 0,
    val loyaltyDiscount: Double = 0.0,
    val finalTotal: Double = 0.0,
    
    // Address state
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val complement: String = "",
    val deliveryQuote: DeliveryQuote? = null,
    val deliveryQuoteFailure: DeliveryQuoteFailure? = null,
    val isQuotingDelivery: Boolean = false,
    val deliveryQuoteRequestKey: String? = null,
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
    val needsChange: Boolean = false,
    val changeForAmount: String = "",

    // Agendamento opcional. Em pré-pedido sem data escolhida, o pedido é liberado na abertura.
    val scheduledForMillis: Long? = null,

    // Flow status
    val isPlacingOrder: Boolean = false,
    val placedOrderSuccess: Order? = null,
    val errorMessage: String? = null,
    val pixPayment: PixPaymentUiState? = null,
    val pixDirectPayment: PixDirectPaymentUiState? = null,
    val isRefreshingOrders: Boolean = false,
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
    private var benefitsLoadingStoreId: String? = null
    private val profileAddressId = "__profile_address__"

    private fun profileSavedAddress(session: com.example.data.model.UserSession): SavedAddress? {
        val hasAnyAddressData = listOf(
            session.addressStreet,
            session.addressNumber,
            session.addressNeighborhood,
            session.addressCity,
            session.addressCep
        ).any { it.isNotBlank() }
        if (!hasAnyAddressData) return null
        return SavedAddress(
            id = profileAddressId,
            label = "Endereço do perfil",
            street = session.addressStreet,
            number = session.addressNumber,
            complement = session.addressComplement,
            neighborhood = session.addressNeighborhood,
            city = session.addressCity,
            state = normalizeBrazilianUf(session.addressState),
            referencePoint = session.addressReferencePoint,
            cep = session.addressCep.filter(Char::isDigit).take(8),
            isDefault = true
        )
    }

    private fun mergeProfileAddress(
        session: com.example.data.model.UserSession,
        remoteAddresses: List<SavedAddress>
    ): List<SavedAddress> {
        val profile = profileSavedAddress(session) ?: return remoteAddresses
        fun normalized(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
        val withoutDuplicate = remoteAddresses.filterNot { address ->
            normalized(address.street) == normalized(profile.street) &&
                normalized(address.number) == normalized(profile.number) &&
                address.cep.filter(Char::isDigit) == profile.cep
        }
        return listOf(profile) + withoutDuplicate
    }
    private var inFlightDeliveryQuoteKey: String? = null
    private var ordersRefreshInFlight = false

    init {
        applySavedAddress(UserSessionRepository.userSession.value)
        viewModelScope.launch {
            UserSessionRepository.userSession.collect { session ->
                if (!addressEditedByCustomer) applySavedAddress(session)
                loadSavedAddresses(session)
                loadCheckoutBenefits(force = true)
            }
        }
        refreshOrders()
        validateRestoredCart()
        viewModelScope.launch {
            cartState.collect { cart ->
                if (cart.storeId != _uiState.value.benefitsStoreId && cart.storeId != benefitsLoadingStoreId) {
                    loadCheckoutBenefits(force = true)
                } else {
                    recalculateBenefits(cart)
                }
                synchronizeDeliveryQuote()
            }
        }
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

    private fun loadCheckoutBenefits(force: Boolean = false) {
        val session = UserSessionRepository.userSession.value
        val cart = cartState.value
        val storeId = cart.storeId
        if (storeId.isNullOrBlank() || !session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            benefitsLoadingStoreId = null
            _uiState.value = _uiState.value.copy(
                benefitsStoreId = storeId,
                isLoadingBenefits = false,
                walletBalance = 0.0,
                useWallet = false,
                walletDiscount = 0.0,
                loyaltyConfig = null,
                loyaltyPointsAvailable = 0,
                loyaltyPointsToUse = 0,
                loyaltyMaxPointsUsable = 0,
                loyaltyDiscount = 0.0,
                finalTotal = cart.total
            )
            return
        }
        val current = _uiState.value
        if (!force && current.benefitsStoreId == storeId && !current.isLoadingBenefits) {
            recalculateBenefits(cart)
            return
        }
        val wasSameStore = current.benefitsStoreId == storeId
        benefitsLoadingStoreId = storeId
        _uiState.value = current.copy(isLoadingBenefits = true, benefitsStoreId = storeId)
        viewModelScope.launch {
            val wallet = SupabaseClient.fetchWalletBalance(session.userId, session.accessToken)
            val config = SupabaseClient.fetchLoyaltyConfig(storeId, session.accessToken)
            val loyalty = config?.let {
                SupabaseClient.fetchLoyaltyBalance(session.userId, storeId, session.accessToken)
            }
            val previous = _uiState.value
            _uiState.value = previous.copy(
                benefitsStoreId = storeId,
                isLoadingBenefits = false,
                walletBalance = wallet.balance.coerceAtLeast(0.0),
                useWallet = if (wasSameStore) current.useWallet else false,
                loyaltyConfig = config,
                loyaltyPointsAvailable = loyalty?.points ?: 0,
                loyaltyPointsToUse = if (wasSameStore) current.loyaltyPointsToUse else 0,
                loyaltyMaxPointsUsable = 0,
                loyaltyDiscount = 0.0
            )
            benefitsLoadingStoreId = null
            recalculateBenefits(cartState.value)
        }
    }

    private fun recalculateBenefits(cart: CartState = cartState.value) {
        val state = _uiState.value
        val config = state.loyaltyConfig?.takeIf { it.isEnabled && state.benefitsStoreId == cart.storeId }
        val maxPoints = if (config == null || config.discountPerPoint <= 0.0) {
            0
        } else {
            val maxByPercent = cart.subtotal * (config.maxDiscountPercent / 100.0)
            val maxByBalance = state.loyaltyPointsAvailable * config.discountPerPoint
            kotlin.math.floor(minOf(maxByPercent, maxByBalance, cart.subtotal) / config.discountPerPoint)
                .toInt()
                .coerceAtLeast(0)
        }
        val pointsToUse = state.loyaltyPointsToUse.coerceAtMost(maxPoints).takeIf {
            config != null && it >= config.minPointsRedeem
        } ?: 0
        val loyaltyDiscount = if (config != null) pointsToUse * config.discountPerPoint else 0.0
        val afterLoyalty = (cart.total - loyaltyDiscount).coerceAtLeast(0.0)
        val walletDiscount = if (state.useWallet) minOf(state.walletBalance, afterLoyalty).coerceAtLeast(0.0) else 0.0
        _uiState.value = state.copy(
            loyaltyPointsToUse = pointsToUse,
            loyaltyMaxPointsUsable = maxPoints,
            loyaltyDiscount = loyaltyDiscount,
            walletDiscount = walletDiscount,
            finalTotal = (afterLoyalty - walletDiscount).coerceAtLeast(0.0)
        )
    }

    private fun currentDeliveryAddress(): DeliveryAddressInput = DeliveryAddressInput(
        street = _uiState.value.street,
        number = _uiState.value.number,
        complement = _uiState.value.complement,
        neighborhood = _uiState.value.neighborhood,
        city = _uiState.value.city,
        state = _uiState.value.state,
        cep = _uiState.value.cep
    )

    private fun fixedDeliveryFeeFor(cart: CartState): Double? {
        if (cart.deliveryType != "DELIVERY") return null
        cart.storeOfficialDeliveryFee?.takeIf {
            !cart.storeDeliveryFeeType.equals("km", ignoreCase = true)
        }?.let { return it }

        // Compatibilidade com carrinhos anteriores à persistência do perfil da loja.
        val store = cart.storeId?.let(StoreRepository::getStoreById) ?: return null
        return store.officialCustomerDeliveryFee?.takeIf {
            !store.deliveryFeeType.equals("km", ignoreCase = true)
        }
    }

    /**
     * Lojas de taxa fixa recebem o preço VIP de imediato. Apenas lojas por
     * quilometragem dependem de endereço, distância e cotação assíncrona.
     */
    private fun synchronizeDeliveryQuote() {
        val cart = cartState.value
        val state = _uiState.value
        if (cart.items.isEmpty() || cart.storeId.isNullOrBlank()) return

        if (cart.deliveryType == "RETIRADA") {
            if (cart.officialDeliveryQuoteKey != null) CartRepository.clearOfficialDeliveryQuote(clearCoordinates = true)
            if (state.deliveryQuote?.fulfillment != "pickup" || state.deliveryQuoteFailure != null || state.isQuotingDelivery) {
                _uiState.value = state.copy(
                    deliveryQuote = DeliveryQuote.pickup(),
                    deliveryQuoteFailure = null,
                    isQuotingDelivery = false,
                    deliveryQuoteRequestKey = "pickup"
                )
            }
            inFlightDeliveryQuoteKey = null
            return
        }

        val fixedFee = fixedDeliveryFeeFor(cart)
        if (fixedFee != null) {
            val fixedKey = "fixed:${cart.storeId}"
            CartRepository.setOfficialFixedDeliveryFee(cart.storeId, fixedFee)
            if (state.deliveryQuote != null || state.deliveryQuoteFailure != null || state.isQuotingDelivery || state.deliveryQuoteRequestKey != fixedKey) {
                _uiState.value = state.copy(
                    deliveryQuote = null,
                    deliveryQuoteFailure = null,
                    isQuotingDelivery = false,
                    deliveryQuoteRequestKey = fixedKey
                )
            }
            inFlightDeliveryQuoteKey = null
            return
        }

        val address = currentDeliveryAddress()
        if (!address.isComplete()) {
            if (cart.officialDeliveryQuoteKey != null) CartRepository.clearOfficialDeliveryQuote()
            if (state.deliveryQuote != null || state.deliveryQuoteFailure != null || state.isQuotingDelivery) {
                _uiState.value = state.copy(
                    deliveryQuote = null,
                    deliveryQuoteFailure = null,
                    isQuotingDelivery = false,
                    deliveryQuoteRequestKey = null
                )
            }
            inFlightDeliveryQuoteKey = null
            return
        }

        val requestKey = address.requestKey(cart.storeId, cart.subtotal)
        val hasCurrentQuote = state.deliveryQuote?.isSuccessfulDelivery == true &&
            state.deliveryQuoteRequestKey == requestKey &&
            cart.officialDeliveryQuoteKey == requestKey
        if (hasCurrentQuote) return
        if (state.deliveryQuoteFailure != null && state.deliveryQuoteRequestKey == requestKey) return
        if (inFlightDeliveryQuoteKey == requestKey) return

        if (cart.officialDeliveryQuoteKey != null) CartRepository.clearOfficialDeliveryQuote()
        inFlightDeliveryQuoteKey = requestKey
        _uiState.value = state.copy(
            deliveryQuote = null,
            deliveryQuoteFailure = null,
            isQuotingDelivery = true,
            deliveryQuoteRequestKey = requestKey
        )

        val session = UserSessionRepository.userSession.value
        viewModelScope.launch {
            val result = SupabaseClient.quoteDelivery(
                storeId = cart.storeId,
                subtotal = cart.subtotal,
                address = address,
                accessToken = session.accessToken
            )
            if (inFlightDeliveryQuoteKey != requestKey) return@launch
            inFlightDeliveryQuoteKey = null
            val stillCurrent = cartState.value.storeId == cart.storeId &&
                cartState.value.subtotal == cart.subtotal &&
                currentDeliveryAddress().requestKey(cart.storeId, cart.subtotal) == requestKey &&
                cartState.value.deliveryType == "DELIVERY"
            if (!stillCurrent) return@launch

            if (result.isSuccess && result.quote != null) {
                _uiState.value = _uiState.value.copy(
                    cep = result.quote.destination?.cep ?: _uiState.value.cep,
                    neighborhood = result.quote.destination?.neighborhood ?: _uiState.value.neighborhood,
                    city = result.quote.destination?.city ?: _uiState.value.city,
                    state = result.quote.destination?.state ?: _uiState.value.state,
                    deliveryQuote = result.quote,
                    deliveryQuoteFailure = null,
                    isQuotingDelivery = false,
                    deliveryQuoteRequestKey = requestKey
                )
                CartRepository.setOfficialDeliveryQuote(result.quote, requestKey)
            } else {
                _uiState.value = _uiState.value.copy(
                    deliveryQuote = null,
                    deliveryQuoteFailure = result.failure ?: DeliveryQuoteFailure(),
                    isQuotingDelivery = false,
                    deliveryQuoteRequestKey = requestKey
                )
                CartRepository.clearOfficialDeliveryQuote()
            }
        }
    }

    private fun invalidateDeliveryQuote() {
        inFlightDeliveryQuoteKey = null
        CartRepository.clearOfficialDeliveryQuote()
        _uiState.value = _uiState.value.copy(
            deliveryQuote = null,
            deliveryQuoteFailure = null,
            isQuotingDelivery = false,
            deliveryQuoteRequestKey = null
        )
        synchronizeDeliveryQuote()
    }

    fun setUseWallet(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(useWallet = enabled, errorMessage = null)
        recalculateBenefits()
    }

    fun applyLoyaltyPoints(points: Int) {
        val state = _uiState.value
        val config = state.loyaltyConfig
        if (config == null || state.loyaltyMaxPointsUsable < config.minPointsRedeem) {
            _uiState.value = state.copy(errorMessage = "Você ainda não possui pontos suficientes para resgatar nesta loja.")
            return
        }
        val normalized = points.coerceIn(config.minPointsRedeem, state.loyaltyMaxPointsUsable)
        _uiState.value = state.copy(loyaltyPointsToUse = normalized, errorMessage = null)
        recalculateBenefits()
    }

    fun removeLoyaltyPoints() {
        _uiState.value = _uiState.value.copy(loyaltyPointsToUse = 0, errorMessage = null)
        recalculateBenefits()
    }

    private fun applySavedAddress(session: com.example.data.model.UserSession) {
        val selected = _uiState.value.savedAddresses.firstOrNull { it.id == _uiState.value.selectedSavedAddressId }
            ?: _uiState.value.savedAddresses.firstOrNull { it.isDefault }
            ?: _uiState.value.savedAddresses.firstOrNull()
        if (selected != null) {
            applyAddress(selected, session)
            return
        }
        val hasProfileAddress = DeliveryAddressInput(
            street = session.addressStreet,
            number = session.addressNumber,
            neighborhood = session.addressNeighborhood,
            city = session.addressCity,
            state = session.addressState,
            cep = session.addressCep
        ).isComplete()
        CartRepository.setDeliveryCoordinates(null, null)
        _uiState.value = _uiState.value.copy(
            cep = session.addressCep,
            street = session.addressStreet,
            number = session.addressNumber,
            neighborhood = session.addressNeighborhood,
            city = session.addressCity,
            state = normalizeBrazilianUf(session.addressState),
            complement = session.addressComplement,
            showAddressEditor = !hasProfileAddress,
            showGpsAddressConfirmation = hasProfileAddress && activeLocationDiffersFromAddress(session, null),
            usingGpsAddress = false,
            selectedSavedAddressId = null
        )
    }

    private fun applyAddress(address: SavedAddress, session: com.example.data.model.UserSession) {
        val resolvedState = normalizeBrazilianUf(
            address.state.ifBlank { session.addressState }
        )
        val resolvedCity = address.city.ifBlank { session.addressCity }
        val resolvedCep = address.cep.filter(Char::isDigit).take(8)
        val completeAddress = DeliveryAddressInput(
            street = address.street,
            number = address.number,
            neighborhood = address.neighborhood,
            city = resolvedCity,
            state = resolvedState,
            cep = resolvedCep
        ).isComplete()
        CartRepository.setDeliveryCoordinates(address.latitude, address.longitude)
        _uiState.value = _uiState.value.copy(
            cep = resolvedCep,
            street = address.street,
            number = address.number,
            neighborhood = address.neighborhood,
            city = resolvedCity,
            state = resolvedState,
            complement = listOf(address.complement, address.referencePoint).filter { it.isNotBlank() }.joinToString(" · "),
            showAddressEditor = !completeAddress,
            showGpsAddressConfirmation = activeLocationDiffersFromAddress(session, address),
            usingGpsAddress = false,
            selectedSavedAddressId = address.id
        )
    }

    private fun activeLocationDiffersFromAddress(session: com.example.data.model.UserSession, address: SavedAddress?): Boolean {
        if (!isActiveLocationFresh(session)) return false
        val latitude = session.activeLocationLatitude
        val longitude = session.activeLocationLongitude
        if (address?.latitude != null && address.longitude != null && latitude != null && longitude != null) {
            return haversineMeters(latitude, longitude, address.latitude, address.longitude) > 300.0
        }
        if (session.activeLocationCity.isBlank() || session.activeLocationStreet.isBlank()) return false
        fun normalized(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
        val street = address?.street ?: session.addressStreet
        val number = address?.number ?: session.addressNumber
        val targetCity = address?.city?.ifBlank { session.addressCity } ?: session.addressCity
        val sameCity = normalized(session.activeLocationCity) == normalized(targetCity)
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
            val remoteAddresses = SupabaseClient.fetchSavedAddresses(session.userId, session.accessToken)
            val addresses = mergeProfileAddress(session, remoteAddresses)
            val selectedId = _uiState.value.selectedSavedAddressId
                ?.takeIf { id -> addresses.any { it.id == id } }
                ?: profileSavedAddress(session)?.id
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
        invalidateDeliveryQuote()
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
        invalidateDeliveryQuote()
    }

    fun saveCurrentAddress(label: String = "Novo endereço") {
        val session = UserSessionRepository.userSession.value
        val state = _uiState.value
        if (session.userId.isBlank() || session.accessToken.isBlank()) return
        if (state.street.isBlank() || state.number.isBlank() || state.neighborhood.isBlank() ||
            state.city.isBlank() || normalizeBrazilianUf(state.state).isBlank() || state.cep.filter(Char::isDigit).length != 8
        ) {
            _uiState.value = state.copy(errorMessage = "Complete rua, número, bairro, cidade, UF e CEP válido antes de salvar o endereço.")
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
                    city = state.city.trim(),
                    state = normalizeBrazilianUf(state.state),
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
        if (!isActiveLocationFresh(session)) {
            _uiState.value = _uiState.value.copy(
                showGpsAddressConfirmation = false,
                errorMessage = "Atualize sua localização atual antes de usá-la no checkout."
            )
            return
        }

        val gpsCep = session.activeLocationCep.filter(Char::isDigit).take(8)
        addressEditedByCustomer = true
        CartRepository.setDeliveryCoordinates(session.activeLocationLatitude, session.activeLocationLongitude)
        _uiState.value = _uiState.value.copy(
            // Nunca mistura o CEP/complemento do endereço salvo com a posição atual.
            cep = gpsCep,
            street = session.activeLocationStreet,
            number = session.activeLocationNumber,
            neighborhood = session.activeLocationNeighborhood,
            city = session.activeLocationCity,
            state = normalizeBrazilianUf(session.activeLocationState),
            complement = "",
            showAddressEditor = true,
            showGpsAddressConfirmation = false,
            usingGpsAddress = true,
            selectedSavedAddressId = null,
            cepError = null,
            deliveryQuoteFailure = null,
            errorMessage = if (gpsCep.length == 8) null else {
                "Não identificamos o CEP da localização atual. Preencha o CEP para calcular a entrega."
            }
        )
        invalidateDeliveryQuote()
    }

    fun confirmGpsAddressForCheckout() {
        val state = _uiState.value
        if (!state.usingGpsAddress) return
        val address = DeliveryAddressInput(
            street = state.street,
            number = state.number,
            neighborhood = state.neighborhood,
            city = state.city,
            state = state.state,
            cep = state.cep
        )
        if (!address.isComplete()) {
            _uiState.value = state.copy(
                errorMessage = if (state.number.isBlank()) {
                    "Informe o número do imóvel para confirmar a localização."
                } else {
                    "Aguarde o endereço ser preenchido para confirmar a localização."
                }
            )
            return
        }
        val cart = cartState.value
        val quoteReady = cart.hasOfficialDeliveryQuote || state.deliveryQuote?.isSuccessfulDelivery == true
        if (!quoteReady) {
            _uiState.value = state.copy(
                errorMessage = if (state.isQuotingDelivery) {
                    "Aguarde a confirmação da entrega antes de prosseguir."
                } else {
                    "Não foi possível confirmar a entrega para este endereço. Revise os dados e tente novamente."
                }
            )
            return
        }
        addressEditedByCustomer = false
        _uiState.value = state.copy(
            showAddressEditor = false,
            showGpsAddressConfirmation = false,
            errorMessage = null,
            selectedSavedAddressId = null
        )
    }

    fun openAddressEditor() {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(showAddressEditor = true, showGpsAddressConfirmation = false)
    }

    fun refreshOrders() {
        val session = UserSessionRepository.userSession.value
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            ordersRefreshInFlight = false
            OrderRepository.replaceOrders(emptyList())
            _uiState.value = _uiState.value.copy(isRefreshingOrders = false)
            return
        }
        if (ordersRefreshInFlight) return
        ordersRefreshInFlight = true
        _uiState.value = _uiState.value.copy(isRefreshingOrders = true)
        viewModelScope.launch {
            try {
                val remoteOrders = SupabaseClient.fetchOrdersForClient(session.userId, session.accessToken)
                // A camada remota retorna lista vazia também em falhas de rede.
                // Não apagamos o histórico já exibido por uma indisponibilidade transitória.
                if (remoteOrders.isNotEmpty() || OrderRepository.orders.value.isEmpty()) {
                    OrderRepository.replaceOrders(remoteOrders)
                }
            } finally {
                ordersRefreshInFlight = false
                _uiState.value = _uiState.value.copy(isRefreshingOrders = false)
            }
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
        val refundable = RefundEligibility.canOpenPixDiretoCase(
            order.paymentMethod,
            order.status,
            order.refundRequestExpiresAt
        )
        if (!refundable) {
            val message = if (order.paymentMethod == "pix_direto" && order.status.lowercase() in setOf("entregue", "finalizado")) {
                RefundEligibility.EXPIRED_MESSAGE
            } else {
                RefundEligibility.INELIGIBLE_MESSAGE
            }
            _uiState.value = _uiState.value.copy(
                errorMessage = message
            )
            onComplete(false)
            return
        }
        if (session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Sua sessão expirou. Entre novamente para solicitar a análise.")
            onComplete(false)
            return
        }
        viewModelScope.launch {
            val result = SupabaseClient.requestPixDiretoRefund(order.id, reason, description, session.accessToken)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Não foi possível abrir a solicitação de PIX Direto."
                )
            }
            onComplete(result.isSuccess)
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
        val normalizedType = if (type.equals("RETIRADA", ignoreCase = true)) "RETIRADA" else "DELIVERY"
        val cart = cartState.value
        val store = cart.storeId?.let { StoreRepository.getStoreById(it) }

        if (normalizedType == "DELIVERY" && store?.deliveryMode?.equals("own", ignoreCase = true) == true) {
            if (store?.hasAvailableDriver == false) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = store.deliveryAvailabilityMessage.ifBlank {
                        "Esta loja está sem entregador disponível no momento. Escolha retirada para continuar."
                    }
                )
                return
            }
            viewModelScope.launch {
                val availability = SupabaseClient.fetchStoreDeliveryAvailability(store?.id.orEmpty())
                if (availability?.canAcceptDeliveryOrders == false) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = availability.reasonMessage.ifBlank {
                            "Esta loja está sem entregador disponível no momento. Escolha retirada para continuar."
                        }
                    )
                    return@launch
                }
                CartRepository.setDeliveryType(normalizedType)
                synchronizeDeliveryQuote()
            }
            return
        }

        CartRepository.setDeliveryType(normalizedType)
        synchronizeDeliveryQuote()
    }

    fun onCouponCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(
            couponCode = code.uppercase(),
            couponError = null
        )
    }

    private fun isCouponExpired(expiresAt: String?): Boolean {
        val raw = expiresAt?.trim().orEmpty()
        if (raw.isBlank()) return false
        val normalized = if (raw.matches(Regex(".*[+-]\\d{2}:\\d{2}$"))) {
            raw.dropLast(3) + raw.takeLast(2)
        } else {
            raw
        }
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ"
        )
        val expiration = patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(normalized)
            }.getOrNull()
        } ?: return false
        return !expiration.after(Date())
    }

    fun applyCoupon() {
        val code = _uiState.value.couponCode.trim().uppercase()
        val cart = cartState.value
        val session = UserSessionRepository.userSession.value

        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(couponError = "Digite o código do cupom.")
            return
        }
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            _uiState.value = _uiState.value.copy(couponError = "Entre novamente para validar o cupom.")
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
            if (isCouponExpired(coupon.expiresAt)) {
                _uiState.value = _uiState.value.copy(couponLoading = false, couponError = "Este cupom expirou.")
                return@launch
            }
            if (coupon.maxUses != null && coupon.usedCount >= coupon.maxUses) {
                _uiState.value = _uiState.value.copy(couponLoading = false, couponError = "Este cupom atingiu o limite de usos.")
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
            val alreadyUsed = SupabaseClient.hasCouponUsage(coupon.id, session.userId, session.accessToken)
            if (alreadyUsed == null) {
                _uiState.value = _uiState.value.copy(couponLoading = false, couponError = "Não foi possível validar o cupom agora. Tente novamente.")
                return@launch
            }
            if (alreadyUsed) {
                _uiState.value = _uiState.value.copy(couponLoading = false, couponError = "Você já utilizou este cupom.")
                return@launch
            }
            if (coupon.firstOrderOnly) {
                val hasPreviousOrder = SupabaseClient.hasNonCancelledOrders(session.userId, session.accessToken)
                if (hasPreviousOrder == null) {
                    _uiState.value = _uiState.value.copy(couponLoading = false, couponError = "Não foi possível validar seu primeiro pedido agora. Tente novamente.")
                    return@launch
                }
                if (hasPreviousOrder) {
                    _uiState.value = _uiState.value.copy(couponLoading = false, couponError = "Este cupom é válido apenas para o primeiro pedido.")
                    return@launch
                }
            }

            val discount = when (coupon.discountType.lowercase()) {
                "percentage" -> (cart.subtotal * (coupon.discountValue / 100.0)).coerceAtMost(cart.subtotal)
                "free_shipping" -> 0.0
                else -> coupon.discountValue.coerceAtMost(cart.subtotal)
            }.coerceAtLeast(0.0)

            CartRepository.applyCoupon(coupon, discount)
            _uiState.value = _uiState.value.copy(couponLoading = false, couponError = null)
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
        _uiState.value = _uiState.value.copy(cep = value.filter(Char::isDigit).take(8), cepError = null, showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
    }

    fun updateStreet(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(street = value, showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
    }

    fun updateNumber(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(number = value, showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
    }

    fun updateNeighborhood(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(neighborhood = value, showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
    }

    fun updateCity(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(city = value, showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
    }

    fun updateState(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(state = normalizeBrazilianUf(value), showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
    }

    fun updateComplement(value: String) {
        addressEditedByCustomer = true
        _uiState.value = _uiState.value.copy(complement = value, showAddressEditor = true, selectedSavedAddressId = null)
        invalidateDeliveryQuote()
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
                val normalizedCep = result.cep.filter(Char::isDigit).take(8).ifBlank { cepInput.filter(Char::isDigit).take(8) }
                _uiState.value = _uiState.value.copy(
                    isSearchingCep = false,
                    cep = normalizedCep,
                    street = result.street.ifBlank { _uiState.value.street },
                    neighborhood = result.neighborhood.ifBlank { _uiState.value.neighborhood },
                    city = result.city.ifBlank { _uiState.value.city },
                    state = result.state.ifBlank { _uiState.value.state }.uppercase(),
                    cepError = null
                )
                invalidateDeliveryQuote()
            } else {
                _uiState.value = _uiState.value.copy(
                    isSearchingCep = false,
                    cepError = "CEP não encontrado. Preencha o endereço manualmente."
                )
            }
        }
    }

    fun setPaymentMethod(method: String) {
        val isCash = method.equals("Dinheiro", ignoreCase = true) || method.equals("Dinheiro na entrega", ignoreCase = true)
        _uiState.value = _uiState.value.copy(
            paymentMethod = method,
            needsChange = if (isCash) _uiState.value.needsChange else false,
            changeForAmount = if (isCash) _uiState.value.changeForAmount else "",
            errorMessage = null
        )
    }

    fun setNeedsChange(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            needsChange = enabled,
            changeForAmount = if (enabled) _uiState.value.changeForAmount else "",
            errorMessage = null
        )
    }

    fun updateChangeForAmount(amount: String) {
        _uiState.value = _uiState.value.copy(
            changeForAmount = amount,
            errorMessage = null
        )
    }

    fun setScheduledForMillis(value: Long?) {
        if (value != null && value < System.currentTimeMillis() + 30 * 60 * 1000L) {
            _uiState.value = _uiState.value.copy(errorMessage = "Escolha um horário com pelo menos 30 minutos de antecedência.")
            return
        }
        _uiState.value = _uiState.value.copy(scheduledForMillis = value, errorMessage = null)
    }

    private fun toIsoUtc(millis: Long?): String = millis?.let {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(it))
    }.orEmpty()

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
        val preorderReleaseAtMillis = store.preorderReleaseAtMillis()
        val isPreorder = preorderReleaseAtMillis != null
        if (!store.isOpen && !isPreorder) {
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
        val checkoutTotal = if (_uiState.value.benefitsStoreId == storeId) _uiState.value.finalTotal else cart.total
        val scheduledForIso = toIsoUtc(_uiState.value.scheduledForMillis)
        val releaseAtIso = if (isPreorder && normalizedPaymentMethod != "pix") toIsoUtc(preorderReleaseAtMillis) else ""
        val initialStatusOverride = when {
            normalizedPaymentMethod == "pix" -> "aguardando_pagamento"
            normalizedPaymentMethod == "pix_direto" -> "aguardando_comprovante"
            isPreorder -> "scheduled"
            else -> "pendente"
        }
        val changeRaw = _uiState.value.changeForAmount.replace(",", ".").trim()
        val changeDouble = changeRaw.toDoubleOrNull()
        if (normalizedPaymentMethod == "dinheiro" && _uiState.value.needsChange) {
            if (changeRaw.isBlank() || changeDouble == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Informe o valor do troco para pagamento em dinheiro.")
                return
            }
            if (changeDouble < checkoutTotal) {
                val formattedTotal = String.format("R$ %.2f", checkoutTotal).replace(".", ",")
                _uiState.value = _uiState.value.copy(errorMessage = "O valor para troco deve ser igual ou maior que o total do pedido ($formattedTotal).")
                return
            }
        }

        val currentAddress = currentDeliveryAddress()
        val deliveryQuote = _uiState.value.deliveryQuote
        val fixedDeliveryFee = fixedDeliveryFeeFor(cart)
        val quoteRequestKey = currentAddress.requestKey(storeId, cart.subtotal)
        if (cart.deliveryType == "DELIVERY" && !currentAddress.isComplete()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Preencha rua, número, bairro e CEP válido para a entrega.")
            return
        }
        if (cart.deliveryType == "DELIVERY" && fixedDeliveryFee == null) {
            when {
                _uiState.value.isQuotingDelivery -> {
                    _uiState.value = _uiState.value.copy(errorMessage = "Estamos calculando a entrega. Aguarde um instante para confirmar o pedido.")
                    return
                }
                deliveryQuote == null || !deliveryQuote.isSuccessfulDelivery ||
                    _uiState.value.deliveryQuoteRequestKey != quoteRequestKey ||
                    cart.officialDeliveryQuoteKey != quoteRequestKey -> {
                    val failure = _uiState.value.deliveryQuoteFailure
                    _uiState.value = _uiState.value.copy(
                        errorMessage = failure?.userMessage() ?: "Revise o endereço para calcular a taxa oficial de entrega."
                    )
                    return
                }
            }
        }

        _uiState.value = _uiState.value.copy(isPlacingOrder = true, errorMessage = null)

        viewModelScope.launch {
            val storeName = store.name
            val effectiveDeliveryQuote = when {
                cart.deliveryType == "RETIRADA" -> DeliveryQuote.pickup()
                fixedDeliveryFee != null -> {
                    // A taxa VIP permanece imediata na interface. Antes do primeiro insert,
                    // a central confirma o endereço, coordenadas e componentes financeiros.
                    val confirmation = SupabaseClient.quoteDelivery(
                        storeId = storeId,
                        subtotal = cart.subtotal,
                        address = currentAddress,
                        accessToken = session.accessToken
                    )
                    val confirmedQuote = confirmation.quote
                    if (!confirmation.isSuccess || confirmedQuote == null) {
                        _uiState.value = _uiState.value.copy(
                            isPlacingOrder = false,
                            errorMessage = confirmation.failure?.userMessage()
                                ?: "Não foi possível confirmar o endereço de entrega. Revise os dados e tente novamente."
                        )
                        return@launch
                    }
                    val confirmedFee = confirmedQuote.pricing.deliveryFee
                    if (kotlin.math.abs(confirmedFee - fixedDeliveryFee) > 0.009) {
                        CartRepository.setOfficialFixedDeliveryFee(storeId, confirmedFee)
                        _uiState.value = _uiState.value.copy(
                            isPlacingOrder = false,
                            cep = confirmedQuote.destination?.cep ?: _uiState.value.cep,
                            neighborhood = confirmedQuote.destination?.neighborhood ?: _uiState.value.neighborhood,
                            city = confirmedQuote.destination?.city ?: _uiState.value.city,
                            state = confirmedQuote.destination?.state ?: _uiState.value.state,
                            errorMessage = "A taxa de entrega foi atualizada. Revise o total e confirme o pedido novamente."
                        )
                        return@launch
                    }
                    _uiState.value = _uiState.value.copy(
                        cep = confirmedQuote.destination?.cep ?: _uiState.value.cep,
                        neighborhood = confirmedQuote.destination?.neighborhood ?: _uiState.value.neighborhood,
                        city = confirmedQuote.destination?.city ?: _uiState.value.city,
                        state = confirmedQuote.destination?.state ?: _uiState.value.state
                    )
                    confirmedQuote
                }
                else -> deliveryQuote ?: DeliveryQuote.pickup()
            }
            val quoteDestination = effectiveDeliveryQuote.destination
            val deliveryNeighborhood = when {
                cart.deliveryType == "RETIRADA" -> "RETIRADA"
                else -> quoteDestination?.neighborhood.orEmpty()
            }
            val formattedAddress = when {
                cart.deliveryType == "RETIRADA" -> "Retirada na loja"
                else -> quoteDestination?.normalizedAddress.orEmpty()
            }
            val finalDeliveryFee = if (cart.deliveryType == "RETIRADA") {
                0.0
            } else {
                effectiveDeliveryQuote.pricing.deliveryFee
            }

            val result = OrderRepository.placeOrder(
                storeId = storeId,
                storeName = storeName,
                items = cart.items,
                subtotal = cart.subtotal,
                deliveryFee = finalDeliveryFee,
                discount = cart.effectiveCouponDiscount,
                paymentMethod = normalizedPaymentMethod,
                deliveryAddress = formattedAddress,
                neighborhood = deliveryNeighborhood,
                clientId = session.userId,
                accessToken = session.accessToken,
                needsChange = normalizedPaymentMethod == "dinheiro" && _uiState.value.needsChange,
                changeFor = if (normalizedPaymentMethod == "dinheiro" && _uiState.value.needsChange) changeDouble else null,
                clientLatitude = quoteDestination?.latitude,
                clientLongitude = quoteDestination?.longitude,
                deliveryCep = quoteDestination?.cep.orEmpty(),
                deliveryCity = quoteDestination?.city.orEmpty(),
                deliveryState = quoteDestination?.state.orEmpty(),
                deliveryFeeAbsorbedByStore = if (cart.deliveryType == "RETIRADA") {
                    0.0
                } else {
                    effectiveDeliveryQuote.pricing.platformFeeStoreAbsorbed
                },
                deliveryQuoteSnapshot = if (cart.deliveryType == "RETIRADA") {
                    null
                } else {
                    effectiveDeliveryQuote.toSnapshot()
                },
                coupon = cart.appliedCoupon,
                walletDiscount = _uiState.value.walletDiscount,
                loyaltyPointsUsed = _uiState.value.loyaltyPointsToUse,
                loyaltyDiscount = _uiState.value.loyaltyDiscount,
                initialStatusOverride = initialStatusOverride,
                scheduledFor = scheduledForIso,
                releaseAt = releaseAtIso
            )

            result.onSuccess { newOrder ->
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    placedOrderSuccess = newOrder,
                    couponCode = "",
                    needsChange = false,
                    changeForAmount = "",
                    scheduledForMillis = null
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
