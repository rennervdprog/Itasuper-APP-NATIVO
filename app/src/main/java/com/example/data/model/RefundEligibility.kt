package com.example.data.model

/**
 * Regra visual compartilhada. O Supabase continua sendo a autoridade final:
 * além deste critério, exige PIX Direto confirmado e ownership do pedido.
 */
object RefundEligibility {
    fun canOpenPixDiretoCase(paymentMethod: String, status: String): Boolean =
        paymentMethod == "pix_direto" && status.lowercase() in setOf("entregue", "finalizado")

    const val INELIGIBLE_MESSAGE =
        "Somente pedidos pagos por PIX Direto confirmado podem abrir um caso de reembolso pela plataforma."
}
