package com.techmarket.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PiiSanitizerTest {

    // --- Null handling ---

    @Test
    fun `sanitize returns null for null input`() {
        assertNull(PiiSanitizer.sanitize(null))
    }

    // --- Email redaction ---

    @Test
    fun `sanitize redacts a standard email address`() {
        val input = "Contact me at john.doe@example.com for details"
        val result = PiiSanitizer.sanitize(input)
        assertEquals("Contact me at [REDACTED EMAIL] for details", result)
    }

    @Test
    fun `sanitize redacts multiple email addresses`() {
        val input = "Reach alice@foo.co.nz or bob@bar.com"
        val result = PiiSanitizer.sanitize(input)
        assertFalse(result!!.contains("@"))
        assertTrue(result.contains("[REDACTED EMAIL]"))
    }

    // --- Phone redaction ---

    @Test
    fun `sanitize redacts AU phone number`() {
        val input = "Call us at +61 4 1234 5678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
        assertFalse(result.contains("1234"))
    }

    @Test
    fun `sanitize redacts NZ phone number`() {
        val input = "Phone: 021 456 7890"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
        assertFalse(result.contains("456"))
    }

    // --- Mixed content ---

    @Test
    fun `sanitize redacts both email and phone in same text`() {
        val input = "Apply to jobs@company.com or call 0412 345 678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED EMAIL]"))
        assertTrue(result.contains("[REDACTED PHONE]"))
    }

    // --- Clean text ---

    @Test
    fun `sanitize leaves clean text unchanged`() {
        val input = "We are hiring a Senior Kotlin Engineer in Sydney"
        assertEquals(input, PiiSanitizer.sanitize(input))
    }
}
