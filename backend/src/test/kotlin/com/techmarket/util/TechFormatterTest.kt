package com.techmarket.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TechFormatterTest {

    @Test
    fun `format returns official name for known keywords`() {
        assertEquals("Kotlin", TechFormatter.format("kotlin"))
        assertEquals("Kotlin", TechFormatter.format("Kotlin"))
        assertEquals("AWS", TechFormatter.format("aws"))
        assertEquals("AWS", TechFormatter.format("AWS"))
        assertEquals(".NET", TechFormatter.format("dotnet"))
        assertEquals(".NET", TechFormatter.format(".net"))
        assertEquals("Next.js", TechFormatter.format("nextjs"))
        assertEquals("Next.js", TechFormatter.format("next.js"))
    }

    @Test
    fun `format returns capitalized name for unknown keywords`() {
        assertEquals("C++", TechFormatter.format("c++"))
        assertEquals("My-custom-tech", TechFormatter.format("my-custom-tech"))
    }
}
