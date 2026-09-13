package com.example

import com.example.ui.home.DaypartRule
import org.junit.Assert.assertEquals
import org.junit.Test

class DaypartRuleTest {

    private fun faixaEm(hora: Int, minuto: Int = 0) =
        DaypartRule.forMinuteOfDay(hora * 60 + minuto).id

    @Test
    fun `cada faixa cobre o seu intervalo`() {
        assertEquals("madrugada", faixaEm(0))
        assertEquals("madrugada", faixaEm(3))
        assertEquals("manha", faixaEm(7))
        assertEquals("almoco", faixaEm(12))
        assertEquals("tarde", faixaEm(16))
        assertEquals("noite", faixaEm(20))
        assertEquals("noite", faixaEm(23, 59))
    }

    @Test
    fun `as bordas pertencem a faixa que comeca`() {
        assertEquals("madrugada", faixaEm(5, 59))
        assertEquals("manha", faixaEm(6, 0))
        assertEquals("manha", faixaEm(10, 29))
        assertEquals("almoco", faixaEm(10, 30))
        assertEquals("almoco", faixaEm(14, 59))
        assertEquals("tarde", faixaEm(15, 0))
        assertEquals("tarde", faixaEm(17, 59))
        assertEquals("noite", faixaEm(18, 0))
    }

    @Test
    fun `minuto invalido cai na noite em vez de quebrar a home`() {
        assertEquals("noite", DaypartRule.forMinuteOfDay(-1).id)
        assertEquals("noite", DaypartRule.forMinuteOfDay(24 * 60).id)
        assertEquals("noite", DaypartRule.forMinuteOfDay(99_999).id)
    }

    @Test
    fun `a categoria primaria e a primeira da faixa`() {
        assertEquals("marmita", DaypartRule.forMinuteOfDay(12 * 60).categoriaPrimaria)
        assertEquals("pizzaria", DaypartRule.forMinuteOfDay(20 * 60).categoriaPrimaria)
    }
}
