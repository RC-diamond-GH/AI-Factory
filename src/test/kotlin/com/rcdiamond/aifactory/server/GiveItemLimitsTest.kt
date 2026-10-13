package com.rcdiamond.aifactory.server

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GiveItemLimitsTest {
    @Test
    fun `count bounds are inclusive and reject everything outside the allowed range`() {
        assertTrue(GiveItemLimits.isCountInRange(1))
        assertTrue(GiveItemLimits.isCountInRange(64))
        assertTrue(GiveItemLimits.isCountInRange(2304))
        assertFalse(GiveItemLimits.isCountInRange(0))
        assertFalse(GiveItemLimits.isCountInRange(-1))
        assertFalse(GiveItemLimits.isCountInRange(2305))
        assertFalse(GiveItemLimits.isCountInRange(Int.MAX_VALUE))
    }

}
