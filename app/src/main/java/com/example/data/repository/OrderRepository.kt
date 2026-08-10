package com.example.data.repository

import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object OrderRepository {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    private val initialPastOrders = listOf(
        Order(
            id = "#ITA-8821",
            storeId = "s1",
            storeName = "Pizzaria Bella Ita",
            items = listOf(
                CartItem(
                    product = Product(
                        id = "p1",
                        storeId = "s1",
                        name = "Pizza Calabresa Especial",
                        description = "Calabresa, queijo mussarela e azeitonas",
                        price = 44.90,
                        category = "Pizzas"
                    ),
                    quantity = 1
                ),
                CartItem(
                    product = Product(
                        id = "p5",
                        storeId = "s1",
                        name = "Guaraná Antarctica 2L",
                        description = "2 Litros",
                        price = 11.90,
                        category = "Bebidas"
                    ),
                    quantity = 1
                )
            ),
            subtotal = 56.80,
            deliveryFee = 5.00,
            discount = 0.0,
            total = 61.80,
            paymentMethod = "PIX",
            deliveryAddress = "Av. 22 de Maio, 1500, Centro - Itaboraí",
            status = "Entregue",
            createdAt = "Ontem às 20:15"
        )
    )

    private val _orders = MutableStateFlow<List<Order>>(initialPastOrders)
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    suspend fun placeOrder(
        storeId: String,
        storeName: String,
        items: List<CartItem>,
        subtotal: Double,
        deliveryFee: Double,
        discount: Double,
        paymentMethod: String,
        deliveryAddress: String
    ): Order {
        val orderNum = (1000..9999).random()
        val orderId = "#ITA-$orderNum"
        val nowStr = dateFormat.format(Date())
        val total = (subtotal + deliveryFee - discount).coerceAtLeast(0.0)

        val tempOrder = Order(
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
            status = "Em preparação",
            createdAt = nowStr
        )

        // Submit to Supabase and retrieve the official ID assigned by the database
        val supabaseId = SupabaseClient.submitOrder(tempOrder)
        val officialId = if (!supabaseId.isNullOrBlank()) {
            if (supabaseId.startsWith("#")) supabaseId else "#ITA-${supabaseId.takeLast(6)}"
        } else {
            "#ITA-${(1000..9999).random()}"
        }

        val finalOrder = tempOrder.copy(id = officialId)

        val currentList = _orders.value.toMutableList()
        currentList.add(0, finalOrder)
        _orders.value = currentList

        // Clear Cart after successful order placement
        CartRepository.clearCart()

        return finalOrder
    }
}
