package com.techmarket.util

/**
 * Centralized utility for consistent ID generation across the system. Ensures that slugs and
 * compound keys are generated identically in both the ingestion/sync pipeline and any manual data
 * management tools.
 */
object IdGenerator {

    /**
     * Generates a unique identifier for a company based on its name. Guaranteed to be URI-safe and
     * lowercased.
     */
    fun buildCompanyId(companyName: String?): String {
        return slugify(companyName ?: "Unknown Company")
    }

    /**
     * Generates a stable unique identifier for a job posting. Structure:
     * {companyId}-{country}-{titleSlug}-{datePart}
     */
    fun buildJobId(companyId: String, country: String, title: String, datePart: String): String {
        val titleSlug = slugify(title)
        return "$companyId-${country.lowercase()}-$titleSlug-$datePart"
    }

    /** Internal helper to create URL-safe, lowercase slugs. */
    fun slugify(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "unknown" }
    }
}
