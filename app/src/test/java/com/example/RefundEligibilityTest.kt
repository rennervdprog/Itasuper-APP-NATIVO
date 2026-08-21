package com.example

import com.example.data.model.RefundEligibility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefundEligibilityTest {
    @Test
    fun `permite PIX Direto em pedido entregue ou finalizado`() {
        assertTrue(RefundEligibility.canOpenPixDiretoCase("pix_direto", "entregue"))
        assertTrue(RefundEligibility.canOpenPixDiretoCase("pix_direto", "finalizado"))
    }

    @Test
    fun `bloqueia PIX Direto antes da conclusao`() {
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "preparando"))
        assertFalse(RefundEligibility.canOpenPixDiretoCase("pix_direto", "cancelado"))
    }

    @Test
    fun `bloqueia todos os pagamentos fisicos e PIX legado`() {
        listOf(
            "dinheiro", "cartao", "cartao_credito", "cartao_debito",
            "pix_machine", "maquininha_credito", "maquininha_debito",
            "maquininha_pix", "cash", "pix"
        ).forEach { method ->
            assertFalse(RefundEligibility.canOpenPixDiretoCase(method, "entregue"))
        }
    }
}
