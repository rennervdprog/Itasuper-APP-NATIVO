package com.example

import com.example.ui.search.resolveInitialCategoryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchInitialCategoryTest {

    @Test
    fun `categoria valida vinda da rota abre a busca filtrada`() {
        assertEquals("pizzaria", resolveInitialCategoryId("pizzaria"))
        assertEquals("bebidas", resolveInitialCategoryId("BEBIDAS"))
        assertEquals("marmita", resolveInitialCategoryId(" marmita "))
    }

    @Test
    fun `categoria desconhecida abre a busca completa em vez de vazia`() {
        assertNull(resolveInitialCategoryId("adega"))
        assertNull(resolveInitialCategoryId("sushi"))
    }

    @Test
    fun `rota sem categoria mantem a busca completa`() {
        assertNull(resolveInitialCategoryId(null))
        assertNull(resolveInitialCategoryId(""))
        assertNull(resolveInitialCategoryId("   "))
    }
}
