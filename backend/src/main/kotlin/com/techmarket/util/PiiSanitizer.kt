package com.techmarket.util

/**
 * Utility for redacting Personally Identifiable Information (PII) from free-text fields.
 *
 * Use this on any user-facing or stored text that may inadvertently contain contact details (e.g.,
 * job descriptions, company descriptions) to ensure compliance with data privacy requirements.
 */
object PiiSanitizer {

    private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val PHONE_REGEX =
            Regex("(?:\\+?6[14][\\s-]?)?\\(?0?[\\d]{1,4}\\)?[\\s-]?[\\d]{3,4}[\\s-]?[\\d]{3,4}")

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
