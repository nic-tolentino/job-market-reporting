package com.techmarket.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LocationFormatterTest {

    @Test
    fun `should format various messy location strings correctly`() {
        assertEquals("Auckland, New Zealand", LocationFormatter.format("Auckland, New Zealand"))
        assertEquals("Auckland", LocationFormatter.format("Auckland, "))
        assertEquals("Auckland", LocationFormatter.format(" Auckland , "))
        assertEquals("Auckland", LocationFormatter.format("Auckland, Auckland"))
        assertEquals("Sydney, NSW, Australia", LocationFormatter.format("Sydney, NSW, Australia"))
        assertEquals("Wellington, New Zealand", LocationFormatter.format("Wellington, , New Zealand"))
        assertEquals("", LocationFormatter.format(null))
        assertEquals("", LocationFormatter.format(""))
        assertEquals("", LocationFormatter.format(" , "))
    }
}
