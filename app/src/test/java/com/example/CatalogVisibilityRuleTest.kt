package com.example

import com.example.data.model.Store
import com.example.data.remote.SupabaseClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogVisibilityRuleTest {

    private fun store(
        id: String,
        deliveryMode: String = "own",
        hasAvailableDriver: Boolean? = false,
        planType: String = ""
    ) = Store(
        id = id,
        name = "Loja $id",
        category = "Lanches",
        rating = 5.0,
        deliveryMode = deliveryMode,
        hasAvailableDriver = hasAvailableDriver,
        planType = planType
    )

    @Test
    fun `loja propria sem motoboy permanece no catalogo do cliente`() {
        val offlineOwnDelivery = store(id = "sem-motoboy", hasAvailableDriver = false)
        val onlineOwnDelivery = store(id = "com-motoboy", hasAvailableDriver = true)

        val catalog = SupabaseClient.keepClientCatalogStores(
            listOf(offlineOwnDelivery, onlineOwnDelivery)
        )

        assertEquals(listOf("sem-motoboy", "com-motoboy"), catalog.map { it.id })
        assertFalse(catalog.first().hasAvailableDriver!!)
        assertTrue(catalog.last().hasAvailableDriver!!)
    }

    @Test
    fun `somente loja exclusiva de PDV fica fora do catalogo do cliente`() {
        val clientStore = store(id = "cliente")
        val pdvOnlyStore = store(id = "pdv", planType = "pdv_only")

        val catalog = SupabaseClient.keepClientCatalogStores(
            listOf(clientStore, pdvOnlyStore)
        )

        assertEquals(listOf("cliente"), catalog.map { it.id })
    }
}
