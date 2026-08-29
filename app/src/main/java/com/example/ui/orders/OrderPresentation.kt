package com.example.ui.orders

import com.example.data.model.Order

/**
 * Código curto para comunicação com o cliente. O UUID completo continua no modelo
 * e é usado normalmente nas operações internas, mas nunca é exibido na UI.
 */
fun Order.customerOrderCode(): String {
    val compactId = id.filter(Char::isLetterOrDigit).uppercase()
    return compactId.take(6).ifBlank { "PENDENTE" }
}

fun Order.customerOrderLabel(): String = "Pedido #${customerOrderCode()}"
