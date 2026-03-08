package com.techmarket.util

/**
 * Utility for redacting Personally Identifiable Information (PII) from free-text fields.
 *
 * Use this on any user-facing or stored text that may inadvertently contain contact details (e.g.,
 * job descriptions, company descriptions) to ensure compliance with data privacy requirements.
 *
 * Supports:
 * - Email addresses (standard formats including .co.nz, .com.au TLDs)
 * - Australian phone numbers (+61, 04xx, landlines)
 * - New Zealand phone numbers (02x mobiles, 03-09 landlines)
 * - Spanish phone numbers (+34, mobiles starting 6/7, landlines starting 8/9)
 */
object PiiSanitizer {

    /**
     * Matches email addresses with common TLDs including regional variants (.co.nz, .com.au, etc.)
     */
    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

    /**
     * Matches Australian and New Zealand phone numbers in various formats:
     *
     * Australian formats:
     * - +61 4 1234 5678, +61412345678
     * - 0412 345 678, 0412-345-678, 0412345678
     * - (02) 1234 5678, 02 1234 5678 (landlines)
     *
     * New Zealand formats:
     * - 021 123 4567, 021-123-4567, 0211234567
     * - 09 123 4567, 03-123-4567 (landlines)
     * - +64 21 123 4567, +64211234567
     *
     * Spanish formats:
     * - +34 91 123 45 67, +34911234567
     * - 91 123 45 67, 911234567 (landlines)
     * - 612 34 56 78, 612345678 (mobiles starting with 6 or 7)
     *
     * Pattern breakdown:
     * - Optional international prefix: (?:\+?6[14][\s-]?)? for +61/+64 or 61/64
     * - Optional area code in parens: \(?0?[\d]{1,4}\)?
     * - Flexible spacing: [\s-]?
     * - Phone number digits in groups
     * - Spanish format: (?:\+?34[\s-]?)?[6-9][\d]{1,2}[\s-]?[\d]{2,3}[\s-]?[\d]{2}[\s-]?[\d]{2}
     */
    private val PHONE_REGEX =
            Regex(
                """(?:\+?6[14][\s-]?)?\(?0?[\d]{1,4}\)?[\s-]?[\d]{3,4}[\s-]?[\d]{3,4}|(?:\+?34[\s-]?)?[6-9][\d]{1,2}[\s-]?[\d]{2,3}[\s-]?[\d]{2}[\s-]?[\d]{2}"""
            )

    /**
     * Redacts email addresses and phone numbers from the given text. Returns null if the input is
     * null.
     */
    fun sanitize(text: String?): String? {
        if (text == null) return null
        return text.replace(EMAIL_REGEX, "[REDACTED EMAIL]")
                .replace(PHONE_REGEX, "[REDACTED PHONE]")
    }
}
