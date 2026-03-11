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

    @Test
    fun `getTechKeysForCategory returns non-empty list for valid categories`() {
        val frontendKeys = TechFormatter.getTechKeysForCategory(com.techmarket.model.TechCategory.FRONTEND)
        org.junit.jupiter.api.Assertions.assertTrue(frontendKeys.isNotEmpty())
        org.junit.jupiter.api.Assertions.assertTrue(frontendKeys.contains("react"))
    }

    @Test
    fun `countDistinctTechsByCategory counts unique display names`() {
        // "vue" and "vue.js" both format to "Vue.js", so they should count as 1
        val frontendCount = TechFormatter.countDistinctTechsByCategory(com.techmarket.model.TechCategory.FRONTEND)
        val frontendKeys = TechFormatter.getTechKeysForCategory(com.techmarket.model.TechCategory.FRONTEND)
        
        // The count should be strictly less than keys size because of aliases (vue/vue.js, nextjs/next.js)
        org.junit.jupiter.api.Assertions.assertTrue(
            frontendCount < frontendKeys.size,
            "Expected aliases to reduce count: $frontendCount distinct vs ${frontendKeys.size} keys"
        )
    }

    @Test
    fun `getCategory returns correct category for known tech`() {
        assertEquals(com.techmarket.model.TechCategory.FRONTEND, TechFormatter.getCategory("react"))
        assertEquals(com.techmarket.model.TechCategory.MOBILE, TechFormatter.getCategory("flutter"))
        assertEquals(null, TechFormatter.getCategory("nonexistent-tech"))
    }
}
