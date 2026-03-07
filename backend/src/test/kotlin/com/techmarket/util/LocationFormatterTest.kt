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

    // ===== Spain Support Tests =====

    @Test
    fun `should format Spanish street address correctly`() {
        assertEquals("Calle de Alcalá, 1, Madrid", LocationFormatter.format("Calle de Alcalá, 1, Madrid"))
    }

    @Test
    fun `should deduplicate Spanish city repetition`() {
        assertEquals("Madrid", LocationFormatter.format("Madrid, Madrid"))
        assertEquals("Barcelona, Catalonia", LocationFormatter.format("Barcelona, Barcelona, Catalonia"))
    }

    @Test
    fun `should handle Spanish accented characters`() {
        assertEquals("Málaga, Andalucía", LocationFormatter.format("Málaga, Málaga, Andalucía"))
        assertEquals("Sevilla, Andalucía", LocationFormatter.format("Sevilla, Sevilla, Andalucía"))
    }

    @Test
    fun `should format Spanish location with empty parts`() {
        assertEquals("Valencia, Spain", LocationFormatter.format("Valencia, , Spain"))
        assertEquals("Bilbao, Basque Country", LocationFormatter.format("Bilbao, , Basque Country"))
    }

    @Test
    fun `should handle Spanish location with extra whitespace`() {
        assertEquals("Madrid, España", LocationFormatter.format(" Madrid , España "))
        assertEquals("Barcelona, Catalonia, Spain", LocationFormatter.format("Barcelona,  Catalonia,  Spain"))
    }
}
