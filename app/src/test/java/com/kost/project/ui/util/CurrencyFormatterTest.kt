package com.kost.project.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {
    @Test
    fun testFormatRupiah() {
        val amount = 3820919377.0
        val formatted = formatRupiah(amount)
        assertEquals("Rp 3.820.919.377", formatted)
    }

    @Test
    fun testFormatRupiahZero() {
        val amount = 0.0
        val formatted = formatRupiah(amount)
        assertEquals("Rp 0", formatted)
    }
}
