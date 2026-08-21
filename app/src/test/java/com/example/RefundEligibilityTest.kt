package com.example

import com.example.data.model.RefundEligibility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefundEligibilityTest {
    private val now = 1_750_000_000_000L
    private val openDeadline = "2025-06-15T16:06:40Z"
    private val expiredDeadline = "2025-06-15T14:06:40Z"

    @Test
    fun `permite PIX Direto concluido dentro da janela de 24 horas`() {
        assertTrue(RefundEligibility.canOpenPixDiretoCase("pix_direto", "entregue", openDeadline, now))
        assertTrue(RefundEligibility.canOpenPixDiretoCase("pix_direto", "finalizado", openDeadline, now))
    }

    @Test
    fun `bloqueia PIX Direto ao expirar a janela de 24 horas`() {
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "entregue", expiredDeadline, now))
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "entregue", "2025-06-15T13:06:40Z", now))
    }

    @Test
    fun `bloqueia prazo ausente ou invalido`() {
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "entregue", "", now))
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "entregue", "invalido", now))
    }

    @Test
    fun `bloqueia PIX Direto antes da conclusao`() {
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "preparando", openDeadline, now))
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "cancelado", openDeadline, now))
    }

    @Test
    fun `bloqueia todos os pagamentos fisicos e PIX legado`() {
        listOf(
            "dinheiro", "cartao", "cartao_credito", "cartao_debito",
            "pix_machine", "maquininha_credito", "maquininha_debito",
            "maquininha_pix", "cash", "pix"
        ).forEach { method ->
            assertFalse(RefundEligibility.canOpenPixDiretoCase(method, "entregue", openDeadline, now))
        }
    }
}
