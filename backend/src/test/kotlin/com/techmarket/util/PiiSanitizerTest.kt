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

    @Test
    fun `sanitize redacts email with regional TLD`() {
        val input = "Send your CV to careers@company.co.nz or jobs@business.com.au"
        val result = PiiSanitizer.sanitize(input)
        assertFalse(result!!.contains("@"))
        assertEquals(2, result.split("[REDACTED EMAIL]").size - 1)
    }

    // --- Australian phone number redaction ---

    @Test
    fun `sanitize redacts AU mobile with plus code and spaces`() {
        val input = "Call us at +61 4 1234 5678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
        assertFalse(result.contains("1234"))
    }

    @Test
    fun `sanitize redacts AU mobile without spaces`() {
        val input = "Contact 0412345678 for more info"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts AU mobile with dashes`() {
        val input = "Phone: 0412-345-678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts AU mobile with standard spacing`() {
        val input = "Call 0412 345 678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts AU landline with area code in parens`() {
        val input = "Our Sydney office: (02) 1234 5678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts AU landline without parens`() {
        val input = "Melbourne number: 03 1234 5678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts AU number with international format no space`() {
        val input = "Reach us on +61412345678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    // --- New Zealand phone number redaction ---

    @Test
    fun `sanitize redacts NZ mobile standard format`() {
        val input = "Phone: 021 456 7890"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
        assertFalse(result.contains("456"))
    }

    @Test
    fun `sanitize redacts NZ mobile without spaces`() {
        val input = "Call Bob on 0214567890"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts NZ mobile with dashes`() {
        val input = "Contact 021-456-7890"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts NZ landline`() {
        val input = "Auckland office: 09 123 4567"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts NZ landline with different area code`() {
        val input = "Wellington: 03-123-4567"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts NZ number with international prefix`() {
        val input = "International: +64 21 123 4567"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts NZ number with international prefix no spaces`() {
        val input = "Call +64211234567"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
    }

    // --- Real-world job description scenarios ---

    @Test
    fun `sanitize handles recruiter contact info scenario`() {
        val input = "If you would like to find out more, call Bob Blob on 021 999 111"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
        assertFalse(result.contains("999"))
    }

    @Test
    fun `sanitize handles multiple contact methods in job post`() {
        val input = "Apply now! Email: hr@techcorp.co.nz, Call: 0412 345 678, or visit our website"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED EMAIL]"))
        assertTrue(result.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize handles AU recruiter scenario`() {
        val input = "Interested? Contact Sarah on 0412 999 888 or sarah@recruit.com.au"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED PHONE]"))
        assertTrue(result.contains("[REDACTED EMAIL]"))
    }

    // --- Mixed content ---

    @Test
    fun `sanitize redacts both email and phone in same text`() {
        val input = "Apply to jobs@company.com or call 0412 345 678"
        val result = PiiSanitizer.sanitize(input)
        assertTrue(result!!.contains("[REDACTED EMAIL]"))
        assertTrue(result.contains("[REDACTED PHONE]"))
    }

    @Test
    fun `sanitize redacts multiple phone numbers`() {
        val input = "Call our AU office on 0412 345 678 or NZ office on 021 987 654"
        val result = PiiSanitizer.sanitize(input)
        assertEquals(2, result!!.split("[REDACTED PHONE]").size - 1)
    }

    // --- Clean text ---

    @Test
    fun `sanitize leaves clean text unchanged`() {
        val input = "We are hiring a Senior Kotlin Engineer in Sydney"
        assertEquals(input, PiiSanitizer.sanitize(input))
    }

    @Test
    fun `sanitize leaves text with numbers but not phone patterns unchanged`() {
        val input = "We have 5 offices and 100+ employees"
        assertEquals(input, PiiSanitizer.sanitize(input))
    }

    @Test
    fun `sanitize handles empty string`() {
        val input = ""
        assertEquals("", PiiSanitizer.sanitize(input))
    }
}
