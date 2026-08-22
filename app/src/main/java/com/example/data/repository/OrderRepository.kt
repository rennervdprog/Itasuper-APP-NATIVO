package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.DeliveryQuoteSnapshot
import com.example.data.model.Order
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OrderRepository {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    /**
     * Persiste o pedido e todos os seus itens no Supabase. O carrinho só é limpo
     * depois que a API devolve o identificador oficial do pedido.
     */
    suspend fun placeOrder(
        storeId: String,
        storeName: String,
        items: List<CartItem>,
        subtotal: Double,
        deliveryFee: Double,
        discount: Double,
        paymentMethod: String,
        deliveryAddress: String,
        neighborhood: String,
        clientId: String,
        accessToken: String,
        needsChange: Boolean = false,
        changeFor: Double? = null,
        clientLatitude: Double? = null,
        clientLongitude: Double? = null,
        deliveryCep: String = "",
        deliveryCity: String = "",
        deliveryState: String = "",
        deliveryFeeAbsorbedByStore: Double = 0.0,
        deliveryQuoteSnapshot: DeliveryQuoteSnapshot? = null,
        coupon: Coupon? = null,
        walletDiscount: Double = 0.0,
        loyaltyPointsUsed: Int = 0,
        loyaltyDiscount: Double = 0.0,
        initialStatusOverride: String = "",
        scheduledFor: String = "",
        releaseAt: String = ""
    ): Result<Order> {
        if (storeId.isBlank() || clientId.isBlank() || accessToken.isBlank()) {
            return Result.failure(IllegalStateException("Sua sessão expirou. Entre novamente para finalizar o pedido."))
        }
        if (items.isEmpty()) {
            return Result.failure(IllegalStateException("Sua sacola está vazia."))
        }
        if (items.any { item ->
                item.product.requiresPrescription ||
                    item.product.isControlled ||
                    item.product.pharmacySaleMode != "platform_checkout"
            }) {
            return Result.failure(
                IllegalStateException(
                    "Há um produto que exige validação da farmácia. Remova o item para finalizar pelo checkout comum do ItaSuper."
                )
            )
        }

        // A carteira é debitada depois do insert com lock no banco; o total inicial não pode antecipá-la.
        val totalBeforeWallet = (subtotal + deliveryFee - discount - loyaltyDiscount).coerceAtLeast(0.0)
        val initialStatus = initialStatusOverride.ifBlank {
            when (paymentMethod) {
                "pix" -> "aguardando_pagamento"
                "pix_direto" -> "aguardando_comprovante"
                else -> "pendente"
            }
        }
        val draft = Order(
            id = "",
            storeId = storeId,
            storeName = storeName,
            items = items,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discount = discount,
            walletDiscount = 0.0,
            loyaltyPointsUsed = loyaltyPointsUsed,
            loyaltyDiscount = loyaltyDiscount,
            total = totalBeforeWallet,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddress,
            status = initialStatus,
            createdAt = dateFormat.format(Date()),
            neighborhood = neighborhood,
            deliveryCep = deliveryCep,
            deliveryCity = deliveryCity,
            deliveryState = deliveryState,
            clientLatitude = clientLatitude,
            clientLongitude = clientLongitude,
            deliveryFeeAbsorbedByStore = deliveryFeeAbsorbedByStore,
            deliveryQuoteSnapshot = deliveryQuoteSnapshot,
            scheduledFor = scheduledFor,
            releaseAt = releaseAt
        )

        val response = SupabaseClient.submitOrder(
            order = draft,
            clientId = clientId,
            accessToken = accessToken,
            needsChange = needsChange,
            changeFor = changeFor
        )
        if (!response.isSuccess || response.orderId.isNullOrBlank()) {
            return Result.failure(IllegalStateException(response.errorMessage ?: "Não foi possível enviar o pedido. Tente novamente."))
        }

        // Benefícios e cupom são processados após o pedido, como no checkout Capacitor.
        if (loyaltyPointsUsed > 0 && loyaltyDiscount > 0) {
            SupabaseClient.redeemLoyaltyPoints(response.orderId, storeId, loyaltyPointsUsed, accessToken)
        }
        val confirmedWalletDiscount = if (walletDiscount > 0) {
            SupabaseClient.applyWalletDiscount(response.orderId, clientId, walletDiscount, accessToken)
                .getOrNull()
                ?.let { walletDiscount }
                ?: 0.0
        } else {
            0.0
        }
        val confirmedOrder = draft.copy(
            id = response.orderId,
            createdAt = response.createdAt ?: draft.createdAt,
            walletDiscount = confirmedWalletDiscount,
            total = (totalBeforeWallet - confirmedWalletDiscount).coerceAtLeast(0.0)
        )
        coupon?.id?.takeIf { it.isNotBlank() }?.let { couponId ->
            SupabaseClient.registerCouponUse(couponId, clientId, response.orderId, accessToken)
        }
        _orders.value = listOf(confirmedOrder) + _orders.value
        CartRepository.clearCart()
        return Result.success(confirmedOrder)
    }

    fun replaceOrders(orders: List<Order>) {
        _orders.value = orders
    }
}
