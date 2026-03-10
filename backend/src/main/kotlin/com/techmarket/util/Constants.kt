package com.techmarket.util

/**
 * Shared constants used across the application to prevent magic strings and ensure consistency.
 */
object Constants {
    /**
     * Standard placeholder for unknown or missing location data.
     * Used when city, state, or country cannot be determined.
     */
    const val UNKNOWN_LOCATION = "Unknown"

    /**
     * Default company name when the company cannot be identified.
     */
    const val UNKNOWN_COMPANY = "Unknown Company"

    /**
     * Default value for empty or missing technology lists.
     */
    val EMPTY_TECH_LIST: List<String> = emptyList()

    /**
     * Default value for empty or missing location lists.
     */
    val EMPTY_LOCATION_LIST: List<String> = emptyList()

    /**
     * Default value for empty or missing string lists.
     */
    val EMPTY_STRING_LIST: List<String> = emptyList()

    /**
     * Standard placeholder for unknown data sources.
     */
    const val UNKNOWN_SOURCE = "Unknown"

    /**
     * Standard placeholder for unknown country codes.
     * Used when country cannot be determined from location data.
     */
    const val UNKNOWN_COUNTRY = "Unknown"

    /**
     * Default work model when not specified.
     */
    const val DEFAULT_WORK_MODEL = "On-site"

    /**
     * Default seniority level when not specified.
     */
    const val DEFAULT_SENIORITY = "Mid-Level"

    /**
     * Data sources and provider identifiers.
     */
    const val SOURCE_APIFY = "LinkedIn-Apify"
    const val SOURCE_ATS = "ATS"
    const val SOURCE_GREENHOUSE = "Greenhouse"
    const val SOURCE_LEVER = "Lever"

    /**
     * Webhook event types.
     */
    const val WEBHOOK_EVENT_TEST = "TEST"
}
