package com.techmarket.util

/**
 * Centralized utility for consistent ID generation across the system. Ensures that slugs and
 * compound keys are generated identically in both the ingestion/sync pipeline and any manual data
 * management tools.
 *
 * Segments are separated by '.' to make it easy to visually parse the different data components.
 * Within each segment, words are separated by '-'.
 *
 * Examples:
 * - Company ID: `asb-bank`
 * - Job ID: `asb-bank.nz.software-engineer.2023-01-15`
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
     * {companyId}.{country}.{titleSlug}.{datePart}
     */
    fun buildJobId(companyId: String, country: String, title: String, datePart: String): String {
        val titleSlug = slugify(title)
        return "$companyId.${country.lowercase()}.$titleSlug.$datePart"
    }

    /** Creates URL-safe, lowercase slugs. Words within a segment are hyphen-separated. */
    fun slugify(text: String): String {
        return text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "unknown" }
    }
}
