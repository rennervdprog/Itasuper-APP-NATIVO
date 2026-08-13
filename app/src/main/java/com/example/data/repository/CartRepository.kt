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
    val deliveryLatitude: Double? = null,
    val deliveryLongitude: Double? = null,
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
            val store = StoreRepository.getStoreById(storeId) ?: return 0.0
            val mode = store.deliveryMode.lowercase()
            if (mode == "pickup") return 0.0

            val isAutonomy = store.planType.equals("autonomy", true) || store.autonomyLifetimeFree
            val splitFull = if (isAutonomy) 0.0 else (store.platformDeliverySplitOverride ?: 0.99)
            val platformAddToCustomer = when (store.platformFeeSplit.lowercase()) {
                "meio_a_meio" -> Math.round((splitFull / 2.0) * 100.0) / 100.0
                "lojista" -> 0.0
                else -> splitFull
            }
            val storeDeliveryFee = when (mode) {
                "platform" -> store.platformDeliveryFee
                "own", "direto" -> {
                    if (store.deliveryFeeType.equals("km", true) &&
                        store.latitude != null && store.longitude != null &&
                        deliveryLatitude != null && deliveryLongitude != null
                    ) {
                        val distanceKm = haversineKm(store.latitude, store.longitude, deliveryLatitude, deliveryLongitude)
                        val pricedKm = kotlin.math.max(1, kotlin.math.ceil(distanceKm).toInt())
                        val baseKm = store.deliveryBaseKm.coerceAtLeast(0.0)
                        val baseFee = store.deliveryFeeBase.coerceAtLeast(0.0)
                        val extraKm = (pricedKm - baseKm).coerceAtLeast(0.0)
                        baseFee + extraKm * store.deliveryFeePerKm.coerceAtLeast(0.0)
                    } else {
                        store.ownDeliveryFee
                    }
                }
                else -> store.platformDeliveryFee
            }
            val total = storeDeliveryFee + if (mode in setOf("own", "direto")) platformAddToCustomer else 0.0
            return (Math.round(total * 100.0) / 100.0).coerceAtLeast(0.0)
        }

    val deliveryDistanceKm: Double?
        get() {
            val store = storeId?.let { StoreRepository.getStoreById(it) } ?: return null
            if (store.latitude == null || store.longitude == null || deliveryLatitude == null || deliveryLongitude == null) return null
            return Math.round(haversineKm(store.latitude, store.longitude, deliveryLatitude, deliveryLongitude) * 10.0) / 10.0
        }

    val total: Double
        get() = (subtotal + deliveryFee - discountAmount).coerceAtLeast(0.0)

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val radius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        return radius * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }
}

object CartRepository {

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

    fun setDeliveryType(type: String) {
        publish(_cartState.value.copy(deliveryType = type))
    }

    fun setDeliveryCoordinates(latitude: Double?, longitude: Double?) {
        publish(_cartState.value.copy(deliveryLatitude = latitude, deliveryLongitude = longitude))
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
        selectedAddons: List<SelectedAddonItem> = emptyList()
    ) {
        val current = _cartState.value
        if (current.storeId != null && current.storeId != product.storeId && current.items.isNotEmpty()) {
            publish(
                CartState(
                    storeId = product.storeId,
                    storeName = storeName,
                    items = listOf(CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = selectedAddons))
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
        publish(current.copy(storeId = product.storeId, storeName = storeName, items = updatedItems))
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
                    discountAmount = if (updatedItems.isEmpty()) 0.0 else current.discountAmount
                )
            )
        } else {
            publish(current.copy(items = current.items.map {
                if (it.product.id == productId) it.copy(quantity = newQuantity) else it
            }))
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
                discountAmount = if (validatedItems.isEmpty()) 0.0 else current.discountAmount
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
