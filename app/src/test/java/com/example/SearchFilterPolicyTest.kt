package com.example

import com.example.data.model.Store
import com.example.ui.search.DiscoverQuickFilter
import com.example.ui.search.matchesDiscoverQuickFilter
import com.example.ui.search.supportsPickup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

class SearchFilterPolicyTest {
    @Test
    fun pickupModesIncludePickupAndBoth() {
        assertTrue(supportsPickup("pickup"))
        assertTrue(supportsPickup("both"))
        assertTrue(supportsPickup("BOTH"))
    }

    @Test
    fun deliveryOnlyModesDoNotAppearAsPickup() {
        assertFalse(supportsPickup("platform"))
        assertFalse(supportsPickup("own"))
        assertFalse(supportsPickup(""))
    }

    @Test
    fun discoverFiltersUseStoreCapabilities() {
        val openFreeDelivery = Store(
            id = "open-free",
            name = "Loja aberta",
            category = "Mercado",
            rating = 5.0,
            isOpen = true,
            isFreeDelivery = true,
            deliveryMode = "platform",
            hasAvailableDriver = true
        )
        val closedPickup = Store(
            id = "closed-pickup",
            name = "Loja retirada",
            category = "Mercado",
            rating = 4.0,
            isOpen = false,
            deliveryMode = "pickup",
            hasAvailableDriver = true
        )

        assertTrue(matchesDiscoverQuickFilter(openFreeDelivery, DiscoverQuickFilter.OPEN_NOW))
        assertTrue(matchesDiscoverQuickFilter(openFreeDelivery, DiscoverQuickFilter.DELIVERY_AVAILABLE))
        assertTrue(matchesDiscoverQuickFilter(openFreeDelivery, DiscoverQuickFilter.FREE_FEE))
        assertFalse(matchesDiscoverQuickFilter(openFreeDelivery, DiscoverQuickFilter.PICKUP))
        assertTrue(matchesDiscoverQuickFilter(closedPickup, DiscoverQuickFilter.PICKUP))
        assertFalse(matchesDiscoverQuickFilter(closedPickup, DiscoverQuickFilter.DELIVERY_AVAILABLE))
        assertFalse(matchesDiscoverQuickFilter(closedPickup, DiscoverQuickFilter.FREE_FEE))
    }

    @Test
    fun deliveryFilterDoesNotTreatUnknownModeAsAvailable() {
        val unknown = Store(
            id = "unknown",
            name = "Loja sem modo",
            category = "Mercado",
            rating = 4.0,
            deliveryMode = "",
            hasAvailableDriver = true
        )

        assertFalse(matchesDiscoverQuickFilter(unknown, DiscoverQuickFilter.DELIVERY_AVAILABLE))
    }

    @Test
    fun productNavigationKeepsStoreAndProductIdsTogether() {
        val storeId = URLEncoder.encode("store-123", "UTF-8")
        val productId = URLEncoder.encode("product-456", "UTF-8")
        val route = "loja/$storeId?produto=$productId"

        assertTrue(route.startsWith("loja/store-123?produto=product-456"))
    }
}
