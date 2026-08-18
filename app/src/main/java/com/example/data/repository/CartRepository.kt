package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.DeliveryQuote
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
    val deliveryLatitude: Double? = null,
    val deliveryLongitude: Double? = null,
    /** Perfil configurado pela loja quando o item foi adicionado à sacola. */
    val storeDeliveryFeeType: String = "",
    val storeOfficialDeliveryFee: Double? = null,
    /** Valor e coordenadas liberados exclusivamente pela cotação central. */
    val officialDeliveryFee: Double? = null,
    val officialDeliveryDistanceKm: Double? = null,
    val officialDeliveryQuoteKey: String? = null,
    val appliedCoupon: Coupon? = null,
    val discountAmount: Double = 0.0
) {
    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    /** A taxa financeira vem exclusivamente da cotação central, já com regras VIP. */
    val deliveryFee: Double
        get() = if (deliveryType == "RETIRADA" || items.isEmpty()) 0.0 else officialDeliveryFee ?: 0.0

    val hasOfficialDeliveryQuote: Boolean
        get() = deliveryType == "RETIRADA" || (officialDeliveryFee != null && officialDeliveryQuoteKey != null)

    val deliveryDistanceKm: Double?
        get() = officialDeliveryDistanceKm

    /** Frete grátis acompanha a taxa central — inclusive após uma nova cotação por km. */
    val effectiveCouponDiscount: Double
        get() = if (appliedCoupon?.discountType.equals("free_shipping", ignoreCase = true) && deliveryType == "DELIVERY") {
            deliveryFee
        } else {
            discountAmount
        }

    val total: Double
        get() = (subtotal + deliveryFee - effectiveCouponDiscount).coerceAtLeast(0.0)
}

object CartRepository {

    private data class StoreDeliveryProfile(val feeType: String, val officialFee: Double?)
    private val deliveryProfiles = mutableMapOf<String, StoreDeliveryProfile>()

    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    private var storage: CartStorage? = null
    private var activeUserId: String = ""

    /** Restaura a sacola da conta atual; ao sair, limpa apenas a memória e preserva a sacola da conta. */
    fun initialize(context: android.content.Context, userId: String) {
        storage = storage ?: CartStorage(context)
        if (userId == activeUserId) return
        activeUserId = userId
        _cartState.value = if (userId.isBlank()) CartState() else storage?.read(userId) ?: CartState()
    }

    /** Registra o perfil real carregado na página da loja antes da adição de itens. */
    fun rememberStoreDeliveryProfile(storeId: String, feeType: String, officialFee: Double?) {
        if (storeId.isBlank()) return
        deliveryProfiles[storeId] = StoreDeliveryProfile(feeType, officialFee)
        val current = _cartState.value
        if (current.storeId == storeId && current.items.isNotEmpty()) {
            val isFixed = !feeType.equals("km", ignoreCase = true) && officialFee != null
            publish(current.copy(
                storeDeliveryFeeType = feeType,
                storeOfficialDeliveryFee = officialFee,
                officialDeliveryFee = if (isFixed && current.deliveryType == "DELIVERY") officialFee else current.officialDeliveryFee,
                officialDeliveryQuoteKey = if (isFixed && current.deliveryType == "DELIVERY") "fixed:$storeId" else current.officialDeliveryQuoteKey
            ))
        }
    }

    fun setDeliveryType(type: String) {
        val current = _cartState.value
        val restoreFixed = type == "DELIVERY" &&
            current.storeDeliveryFeeType.equals("fixed", ignoreCase = true) &&
            current.storeOfficialDeliveryFee != null
        publish(current.copy(
            deliveryType = type,
            officialDeliveryFee = if (restoreFixed) current.storeOfficialDeliveryFee else null,
            officialDeliveryDistanceKm = null,
            officialDeliveryQuoteKey = if (restoreFixed) "fixed:${current.storeId}" else null
        ))
    }

    fun setDeliveryCoordinates(latitude: Double?, longitude: Double?) {
        publish(_cartState.value.copy(deliveryLatitude = latitude, deliveryLongitude = longitude))
    }

    /** Aplica a taxa VIP já configurada pela loja fixa; não depende de endereço ou distância. */
    fun setOfficialFixedDeliveryFee(storeId: String, fee: Double) {
        val current = _cartState.value
        if (current.storeId != storeId || current.deliveryType != "DELIVERY") return
        val requestKey = "fixed:$storeId"
        if (current.officialDeliveryFee == fee && current.officialDeliveryQuoteKey == requestKey) return
        publish(current.copy(
            officialDeliveryFee = fee.coerceAtLeast(0.0),
            officialDeliveryDistanceKm = null,
            officialDeliveryQuoteKey = requestKey
        ))
    }

    fun setOfficialDeliveryQuote(quote: DeliveryQuote, requestKey: String) {
        val destination = quote.destination ?: return
        if (!quote.isSuccessfulDelivery) return
        publish(_cartState.value.copy(
            deliveryLatitude = destination.latitude,
            deliveryLongitude = destination.longitude,
            officialDeliveryFee = quote.pricing.deliveryFee.coerceAtLeast(0.0),
            officialDeliveryDistanceKm = quote.distance.km.takeIf { it.isFinite() },
            officialDeliveryQuoteKey = requestKey
        ))
    }

    fun clearOfficialDeliveryQuote(clearCoordinates: Boolean = false) {
        val current = _cartState.value
        publish(current.copy(
            deliveryLatitude = if (clearCoordinates) null else current.deliveryLatitude,
            deliveryLongitude = if (clearCoordinates) null else current.deliveryLongitude,
            officialDeliveryFee = null,
            officialDeliveryDistanceKm = null,
            officialDeliveryQuoteKey = null
        ))
    }

    fun applyCoupon(coupon: Coupon, discount: Double) {
        publish(
            _cartState.value.copy(
                appliedCoupon = coupon,
                discountAmount = discount
            )
        )
    }

    fun removeCoupon() {
        publish(
            _cartState.value.copy(
                appliedCoupon = null,
                discountAmount = 0.0
            )
        )
    }

    fun addProduct(
        product: Product,
        storeName: String,
        quantity: Int = 1,
        notes: String = "",
        selectedAddons: List<SelectedAddonItem> = emptyList(),
        storeDeliveryFeeType: String = "",
        storeOfficialDeliveryFee: Double? = null
    ) {
        val current = _cartState.value
        val rememberedProfile = deliveryProfiles[product.storeId]
        val effectiveFeeType = storeDeliveryFeeType.ifBlank { rememberedProfile?.feeType.orEmpty() }
        val effectiveOfficialFee = storeOfficialDeliveryFee ?: rememberedProfile?.officialFee
        if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
            publish(
                CartState(
                    storeId = product.storeId,
                    storeName = storeName,
                    items = listOf(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons)),
                    storeDeliveryFeeType = effectiveFeeType,
                    storeOfficialDeliveryFee = effectiveOfficialFee,
                    officialDeliveryFee = effectiveOfficialFee?.takeIf { effectiveFeeType.equals("fixed", ignoreCase = true) },
                    officialDeliveryQuoteKey = effectiveOfficialFee?.takeIf { effectiveFeeType.equals("fixed", ignoreCase = true) }?.let { "fixed:${product.storeId}" }
                )
            )
            return
        }

        val existingIndex = current.items.indexOfFirst {
            it.product.id == product.id && it.selectedAddons == selectedAddons && it.notes == notes
        }
        val updatedItems = current.items.toMutableList()
        if (existingIndex >= 0) {
            val existing = updatedItems[existingIndex]
            updatedItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            updatedItems.add(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons))
        }
        publish(current.copy(
            storeId = product.storeId,
            storeName = storeName,
            items = updatedItems,
            storeDeliveryFeeType = effectiveFeeType.ifBlank { current.storeDeliveryFeeType },
            storeOfficialDeliveryFee = effectiveOfficialFee ?: current.storeOfficialDeliveryFee,
            officialDeliveryFee = if (effectiveFeeType.ifBlank { current.storeDeliveryFeeType }.equals("fixed", ignoreCase = true)) {
                effectiveOfficialFee ?: current.storeOfficialDeliveryFee
            } else null,
            officialDeliveryDistanceKm = null,
            officialDeliveryQuoteKey = if (effectiveFeeType.ifBlank { current.storeDeliveryFeeType }.equals("fixed", ignoreCase = true)) {
                (effectiveOfficialFee ?: current.storeOfficialDeliveryFee)?.let { "fixed:${product.storeId}" }
            } else null
        ))
    }

    /**
     * Substitui a sacola atual pelos itens validados de um pedido anterior.
     * A operação preserva a regra de uma loja por sacola e remove cupom/desconto
     * anterior, que precisa ser recalculado no novo checkout.
     */
    fun replaceWithOrder(storeId: String, storeName: String, items: List<CartItem>) {
        if (storeId.isBlank() || items.isEmpty()) return
        publish(
            CartState(
                storeId = storeId,
                storeName = storeName,
                items = items,
                deliveryType = _cartState.value.deliveryType,
                deliveryLatitude = _cartState.value.deliveryLatitude,
                deliveryLongitude = _cartState.value.deliveryLongitude
            )
        )
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        val current = _cartState.value
        if (newQuantity <= 0) {
            val updatedItems = current.items.filterNot { it.product.id == productId }
            publish(
                current.copy(
                    storeId = if (updatedItems.isEmpty()) null else current.storeId,
                    items = updatedItems,
                    appliedCoupon = if (updatedItems.isEmpty()) null else current.appliedCoupon,
                    discountAmount = if (updatedItems.isEmpty()) 0.0 else current.discountAmount,
                    officialDeliveryFee = null,
                    officialDeliveryDistanceKm = null,
                    officialDeliveryQuoteKey = null
                )
            )
        } else {
            publish(current.copy(
                items = current.items.map { if (it.product.id == productId) it.copy(quantity = newQuantity) else it },
                officialDeliveryFee = null,
                officialDeliveryDistanceKm = null,
                officialDeliveryQuoteKey = null
            ))
        }
    }

    /** Remove itens indisponíveis ou desatualizados depois de consultar o cardápio real da loja. */
    fun validateAgainstCatalog(products: List<Product>) {
        val current = _cartState.value
        if (current.items.isEmpty()) return
        val available = products.filter { it.isAvailable }.associateBy { it.id }
        val validatedItems = current.items.mapNotNull { item ->
            val currentProduct = available[item.product.id] ?: return@mapNotNull null
            item.copy(product = currentProduct)
        }
        publish(
            current.copy(
                items = validatedItems,
                storeId = if (validatedItems.isEmpty()) null else current.storeId,
                appliedCoupon = if (validatedItems.isEmpty()) null else current.appliedCoupon,
                discountAmount = if (validatedItems.isEmpty()) 0.0 else current.discountAmount,
                officialDeliveryFee = null,
                officialDeliveryDistanceKm = null,
                officialDeliveryQuoteKey = null
            )
        )
    }

    fun clearCart() {
        publish(CartState())
    }

    private fun publish(state: CartState) {
        _cartState.value = state
        if (activeUserId.isNotBlank()) storage?.save(activeUserId, state)
    }
}
