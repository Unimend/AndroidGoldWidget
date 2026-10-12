package com.unimend.goldwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class GoldPriceDirectionTest {
    @Test fun risingQuoteIsRedDirection() {
        assertEquals(GoldPriceDirection.RISING, goldPriceDirection(4184.50, 4184.66))
    }

    @Test fun fallingQuoteIsGreenDirection() {
        assertEquals(GoldPriceDirection.FALLING, goldPriceDirection(4184.66, 4184.50))
    }

    @Test fun noiseWithinOneCentIsFlat() {
        assertEquals(GoldPriceDirection.FLAT, goldPriceDirection(4184.500, 4184.505))
    }
}
