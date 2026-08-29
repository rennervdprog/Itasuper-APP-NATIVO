package com.example

import com.example.ui.search.supportsPickup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
