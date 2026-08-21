package com.example

import com.example.data.model.Store
import com.example.data.repository.StoreRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryAvailabilityTest {

    private fun store(id: String, deliveryMode: String = "own") = Store(
        id = id,
        name = "Loja $id",
        category = "Lanches",
        rating = 5.0,
        deliveryMode = deliveryMode,
        hasAvailableDriver = true
    )

    @Test
    fun `loja propria fica indisponivel quando nao ha entregador presente`() {
        val updated = StoreRepository.applyDriverAvailability(
            stores = listOf(store("own-store")),
            onlineDriverStoreIds = emptySet()
        )

        assertFalse(updated.single().hasAvailableDriver!!)
        assertEquals(
            "Esta loja está sem entregador disponível no momento.",
            updated.single().deliveryAvailabilityMessage
        )
    }

    @Test
    fun `loja propria volta a ficar disponivel quando entregador retorna`() {
        val unavailable = store("own-store").copy(
            hasAvailableDriver = false,
            deliveryAvailabilityMessage = "Esta loja está sem entregador disponível no momento."
        )

        val updated = StoreRepository.applyDriverAvailability(
            stores = listOf(unavailable),
            onlineDriverStoreIds = setOf("own-store")
        )

        assertTrue(updated.single().hasAvailableDriver!!)
        assertEquals("", updated.single().deliveryAvailabilityMessage)
    }

    @Test
    fun `loja sem entrega propria nao recebe bloqueio de motoboy`() {
        val marketplaceStore = store("marketplace-store", deliveryMode = "direto").copy(
            hasAvailableDriver = null
        )

        val updated = StoreRepository.applyDriverAvailability(
            stores = listOf(marketplaceStore),
            onlineDriverStoreIds = emptySet()
        )

        assertNull(updated.single().hasAvailableDriver)
    }
}
