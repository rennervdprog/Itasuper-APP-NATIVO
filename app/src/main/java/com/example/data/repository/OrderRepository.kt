package com.example.data.repository

import com.example.data.model.CartItem
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
        changeFor: Double? = null
    ): Result<Order> {
        if (storeId.isBlank() || clientId.isBlank() || accessToken.isBlank()) {
            return Result.failure(IllegalStateException("Sua sessão expirou. Entre novamente para finalizar o pedido."))
        }
        if (items.isEmpty()) {
            return Result.failure(IllegalStateException("Sua sacola está vazia."))
        }

        val total = (subtotal + deliveryFee - discount).coerceAtLeast(0.0)
        val initialStatus = when (paymentMethod) {
            "pix" -> "aguardando_pagamento"
            "pix_direto" -> "aguardando_comprovante"
            else -> "pendente"
        }
        val draft = Order(
            id = "",
            storeId = storeId,
            storeName = storeName,
            items = items,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discount = discount,
            total = total,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddress,
            status = initialStatus,
            createdAt = dateFormat.format(Date())
        )

        val response = SupabaseClient.submitOrder(
            order = draft,
            clientId = clientId,
            accessToken = accessToken,
            neighborhood = neighborhood,
            needsChange = needsChange,
            changeFor = changeFor
        )
        if (!response.isSuccess || response.orderId.isNullOrBlank()) {
            return Result.failure(IllegalStateException(response.errorMessage ?: "Não foi possível enviar o pedido. Tente novamente."))
        }

        val confirmedOrder = draft.copy(
            id = response.orderId,
            createdAt = response.createdAt ?: draft.createdAt
        )
        _orders.value = listOf(confirmedOrder) + _orders.value
        CartRepository.clearCart()
        return Result.success(confirmedOrder)
    }

    fun replaceOrders(orders: List<Order>) {
        _orders.value = orders
    }
}
