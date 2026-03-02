package com.techmarket.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IdGeneratorTest {

    // --- buildCompanyId ---

    @Test
    fun `buildCompanyId produces lowercase slug from normal name`() {
        assertEquals("google", IdGenerator.buildCompanyId("Google"))
    }

    @Test
    fun `buildCompanyId handles multi-word names with hyphens`() {
        assertEquals("asb-bank", IdGenerator.buildCompanyId("ASB Bank"))
    }

    @Test
    fun `buildCompanyId handles null input`() {
        assertEquals("unknown-company", IdGenerator.buildCompanyId(null))
    }

    @Test
    fun `buildCompanyId handles blank input`() {
        assertEquals("unknown", IdGenerator.buildCompanyId("   "))
    }

    @Test
    fun `buildCompanyId strips special characters`() {
        assertEquals("caf-co", IdGenerator.buildCompanyId("Café & Co."))
    }

    @Test
    fun `buildCompanyId condenses consecutive special chars`() {
        assertEquals("my-company", IdGenerator.buildCompanyId("My---Company"))
    }

    @Test
    fun `buildCompanyId does not produce leading or trailing hyphens`() {
        val result = IdGenerator.buildCompanyId("--Leading Trailing--")
        assertFalse(result.startsWith("-"))
        assertFalse(result.endsWith("-"))
        assertEquals("leading-trailing", result)
    }

    // --- buildJobId ---

    @Test
    fun `buildJobId produces hyphen-separated URL-safe ID`() {
        val result = IdGenerator.buildJobId("google", "AU", "Software Engineer", "2023-01-15")
        assertEquals("google-au-software-engineer-2023-01-15", result)
    }

    @Test
    fun `buildJobId handles special characters in title`() {
        val result = IdGenerator.buildJobId("acme", "NZ", "Sr. Engineer (Backend)", "2023-06-01")
        assertEquals("acme-nz-sr-engineer-backend-2023-06-01", result)
    }

    // --- slugify ---

    @Test
    fun `slugify produces consistent URL-safe output`() {
        assertEquals("hello-world", IdGenerator.slugify("Hello World!"))
        assertEquals("test-123", IdGenerator.slugify("  Test  123  "))
        assertEquals("unknown", IdGenerator.slugify(""))
    }
}
