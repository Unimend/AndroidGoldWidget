package com.unimend.goldwidget

import org.junit.Assert.*
import org.junit.Test

class GoldPriceRepositoryTest {
    @Test fun usesPreviousCloseInsteadOfBidOrZero() {
        val body = "var hq_str_hf_XAU=\"4184.40,4194.390,4184.40,4184.75,4217.06,4184.29,13:28:00,4194.39\";" +
            "var hq_str_gds_AU9999=\"903.60,0,903.60,903.70,911.90,900.00,13:27:46,904.48\";"
        val price = GoldPriceRepository().parse(body)!!
        assertEquals(-9.99, price.internationalChange, 0.001)
        assertEquals(-0.88, price.domesticChange, 0.001)
        assertTrue(price.domesticPercent < 0)
    }

    @Test fun rejectsInvalidQuotes() {
        assertNull(GoldPriceRepository().parse(""))
        assertNull(GoldPriceRepository().parse("var hq_str_hf_XAU=\"NaN,1,1,1,1,1,00:00,1\";"))
    }
}
