package com.techmarket.model

import com.techmarket.persistence.SalaryFields.AMOUNT
import com.techmarket.persistence.SalaryFields.CURRENCY
import com.techmarket.persistence.SalaryFields.IS_GROSS
import com.techmarket.persistence.SalaryFields.PERIOD
import com.techmarket.persistence.SalaryFields.SOURCE

/**
 * Represents a normalized salary value with currency, period, and source information.
 *
 * @property amount The salary amount in the smallest currency unit (e.g., cents)
 * @property currency ISO 4217 currency code (NZD, AUD, USD, EUR)
 * @property period The time period for the salary (HOUR, DAY, MONTH, YEAR)
 * @property source The source of the salary data (determines confidence level)
 * @property isGross Whether this is gross salary (true) or net (false). Defaults to true.
 *                   Persisted to BigQuery as this is meaningful metadata (especially for EU markets).
 */
data class NormalizedSalary(
    val amount: Long,
    val currency: String,      // NZD, AUD, USD, EUR
    val period: String,        // HOUR, DAY, MONTH, YEAR
    val source: String,        // JOB_POSTING, ATS_API, MARKET_DATA, AI_ESTIMATE
    val isGross: Boolean = true
) {
    companion object {
        // Source constants
        const val SOURCE_JOB_POSTING = "JOB_POSTING"    // HIGH confidence
        const val SOURCE_ATS_API = "ATS_API"            // HIGH confidence
        const val SOURCE_MARKET_DATA = "MARKET_DATA"    // MEDIUM confidence
        const val SOURCE_AI_ESTIMATE = "AI_ESTIMATE"    // LOW confidence

        // Period constants
        const val PERIOD_HOUR = "HOUR"
        const val PERIOD_DAY = "DAY"
        const val PERIOD_MONTH = "MONTH"
        const val PERIOD_YEAR = "YEAR"

        // Currency constants
        const val CURRENCY_NZD = "NZD"
        const val CURRENCY_AUD = "AUD"
        const val CURRENCY_USD = "USD"
        const val CURRENCY_EUR = "EUR"
        const val CURRENCY_GBP = "GBP"

        /**
         * Returns the confidence level based on the source.
         */
        fun getConfidenceForSource(source: String): String = when (source) {
            SOURCE_JOB_POSTING -> "HIGH"
            SOURCE_ATS_API -> "HIGH"
            SOURCE_MARKET_DATA -> "MEDIUM"
            SOURCE_AI_ESTIMATE -> "LOW"
            else -> "UNKNOWN"
        }

        /**
         * Country-specific default salary period.
         * Most countries default to annual gross salary.
         */
        fun getDefaultPeriodForCountry(country: String): String = when (country.uppercase()) {
            "NZ", "AU", "US", "UK", "ES", "CA", "IE" -> PERIOD_YEAR
            else -> PERIOD_YEAR  // Safe fallback
        }

        /**
         * Country-specific default currency.
         */
        fun getDefaultCurrencyForCountry(country: String): String = when (country.uppercase()) {
            "NZ" -> CURRENCY_NZD
            "AU" -> CURRENCY_AUD
            "US" -> CURRENCY_USD
            "ES", "DE", "FR", "IT", "IE", "NL", "BE" -> CURRENCY_EUR
            "UK" -> "GBP"
            "CA" -> "CAD"
            else -> CURRENCY_NZD  // Safe fallback
        }

        /**
         * Creates a NormalizedSalary from a BigQuery STRUCT map.
         */
        fun fromMap(map: Map<String, Any?>): NormalizedSalary? {
            val amount = (map[AMOUNT] as? Number)?.toLong() ?: return null
            val currency = map[CURRENCY] as? String ?: return null
            val period = map[PERIOD] as? String ?: return null
            val source = map[SOURCE] as? String ?: return null
            val isGross = map[IS_GROSS] as? Boolean ?: true
            return NormalizedSalary(amount, currency, period, source, isGross)
        }

        /**
         * Returns a disclaimer text based on the salary source.
         * This is computed at runtime, not persisted to BigQuery.
         */
        fun getDisclaimerForSource(source: String): String? = when (source) {
            SOURCE_AI_ESTIMATE -> "AI-estimated salary based on market data for similar roles"
            SOURCE_MARKET_DATA -> "Estimated from market data - not confirmed by employer"
            else -> null
        }
    }

    /**
     * Converts this salary to an annual amount in the same currency.
     * Uses standard assumptions:
     * - Hourly: 2080 hours/year (40 hrs/week × 52 weeks)
     * - Daily: 260 days/year (5 days/week × 52 weeks)
     * - Monthly: 12 months/year
     * - Yearly: already annual
     */
    fun toAnnualAmount(): Long = when (period) {
        PERIOD_HOUR -> amount * 2080
        PERIOD_DAY -> amount * 260
        PERIOD_MONTH -> amount * 12
        PERIOD_YEAR -> amount
        else -> amount
    }

    /**
     * Returns the confidence level for this salary.
     */
    val confidence: String
        get() = getConfidenceForSource(source)

    /**
     * Returns a disclaimer text for this salary based on its source.
     * Computed at runtime, not persisted.
     */
    val disclaimer: String?
        get() = getDisclaimerForSource(source)

    /**
     * Converts this salary to a map for BigQuery STRUCT serialization.
     * Note: disclaimer is NOT persisted - it's computed at BFF level from source.
     */
    fun toMap(): Map<String, Any?> = mapOf(
        AMOUNT to amount,
        CURRENCY to currency,
        PERIOD to period,
        SOURCE to source,
        IS_GROSS to isGross
    )
}
