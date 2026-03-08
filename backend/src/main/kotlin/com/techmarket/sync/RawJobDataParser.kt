package com.techmarket.sync

import com.techmarket.model.NormalizedSalary
import com.techmarket.model.NormalizedSalary.Companion.CURRENCY_AUD
import com.techmarket.model.NormalizedSalary.Companion.CURRENCY_EUR
import com.techmarket.model.NormalizedSalary.Companion.CURRENCY_GBP
import com.techmarket.model.NormalizedSalary.Companion.CURRENCY_NZD
import com.techmarket.model.NormalizedSalary.Companion.CURRENCY_USD
import com.techmarket.model.NormalizedSalary.Companion.PERIOD_DAY
import com.techmarket.model.NormalizedSalary.Companion.PERIOD_HOUR
import com.techmarket.model.NormalizedSalary.Companion.PERIOD_MONTH
import com.techmarket.model.NormalizedSalary.Companion.PERIOD_YEAR
import com.techmarket.model.NormalizedSalary.Companion.SOURCE_AI_ESTIMATE
import com.techmarket.model.NormalizedSalary.Companion.SOURCE_ATS_API
import com.techmarket.model.NormalizedSalary.Companion.SOURCE_JOB_POSTING
import com.techmarket.model.NormalizedSalary.Companion.SOURCE_MARKET_DATA
import com.techmarket.model.NormalizedSalary.Companion.getDefaultCurrencyForCountry
import com.techmarket.model.NormalizedSalary.Companion.getDefaultPeriodForCountry
import com.techmarket.util.Constants.UNKNOWN_COUNTRY
import com.techmarket.util.Constants.UNKNOWN_LOCATION
import com.techmarket.util.TechFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import org.springframework.stereotype.Component

/**
 * Contains core parsing logic to normalize messy, unstructured data from job postings into standard
 * values.
 */
@Component
class RawJobDataParser {

    private val techKeywords = TechFormatter.getAllOfficialNames()

    /**
     * Pre-defined mapping of common cities to their full (City, State, Country) tuples. This avoids
     * brittle comma-splitting for well-known locations.
     */
    private val knownLocations =
            mapOf(
                    // Australia
                    "sydney" to Triple("Sydney", "New South Wales", "AU"),
                    "melbourne" to Triple("Melbourne", "Victoria", "AU"),
                    "brisbane" to Triple("Brisbane", "Queensland", "AU"),
                    "perth" to Triple("Perth", "Western Australia", "AU"),
                    "adelaide" to Triple("Adelaide", "South Australia", "AU"),
                    "canberra" to Triple("Canberra", "Australian Capital Territory", "AU"),
                    "hobart" to Triple("Hobart", "Tasmania", "AU"),
                    "darwin" to Triple("Darwin", "Northern Territory", "AU"),
                    "gold coast" to Triple("Gold Coast", "Queensland", "AU"),
                    "newcastle" to Triple("Newcastle", "New South Wales", "AU"),
                    "geelong" to Triple("Geelong", "Victoria", "AU"),

                    // New Zealand
                    "auckland" to Triple("Auckland", "Auckland", "NZ"),
                    "wellington" to Triple("Wellington", "Wellington", "NZ"),
                    "christchurch" to Triple("Christchurch", "Canterbury", "NZ"),
                    "hamilton" to Triple("Hamilton", "Waikato", "NZ"),
                    "tauranga" to Triple("Tauranga", "Bay of Plenty", "NZ"),
                    "dunedin" to Triple("Dunedin", "Otago", "NZ"),

                    // Spain
                    "las palmas" to Triple("Las Palmas", "Canary Islands", "ES"),
                    "santa cruz" to Triple("Santa Cruz", "Canary Islands", "ES"),
                    "madrid" to Triple("Madrid", "Community of Madrid", "ES"),
                    "barcelona" to Triple("Barcelona", "Catalonia", "ES"),
                    "valencia" to Triple("Valencia", "Valencian Community", "ES"),
                    "seville" to Triple("Seville", "Andalusia", "ES"),
                    "sevilla" to Triple("Seville", "Andalusia", "ES"),
                    "zaragoza" to Triple("Zaragoza", "Aragon", "ES"),
                    "malaga" to Triple("Málaga", "Andalusia", "ES"),
                    "málaga" to Triple("Málaga", "Andalusia", "ES"),
                    "bilbao" to Triple("Bilbao", "Basque Country", "ES"),
                    "murcia" to Triple("Murcia", "Region of Murcia", "ES"),
                    "palma" to Triple("Palma", "Balearic Islands", "ES"),
                    "alicante" to Triple("Alicante", "Valencian Community", "ES"),
                    "granada" to Triple("Granada", "Andalusia", "ES")
            )

    /** Determines the ISO Country Code (AU, NZ, ES) from a location string. */
    fun determineCountry(location: String?): String {
        val (_, _, country) = parseLocation(location)
        if (country != UNKNOWN_COUNTRY) return country

        if (location == null) return UNKNOWN_LOCATION
        val locUpper = location.uppercase()
        return when {
            locUpper.contains("AUSTRALIA") -> "AU"
            locUpper.contains("NEW ZEALAND") -> "NZ"
            locUpper.contains("SPAIN") || locUpper.contains("ESPAÑA") -> "ES"
            else -> UNKNOWN_LOCATION
        }
    }

    private fun normalizeCountry(country: String): String {
        return when (country.uppercase()) {
            "NEW ZEALAND", "NZ" -> "NZ"
            "AUSTRALIA", "AU" -> "AU"
            "SPAIN", "ES" -> "ES"
            else -> country
        }
    }

    /**
     * Normalizes a raw location string (e.g. "Sydney, NSW, Australia") into a Triple of (City,
     * State/Region, Country).
     */
    fun parseLocation(location: String?): Triple<String, String, String> {
        if (location == null) return Triple(UNKNOWN_LOCATION, UNKNOWN_LOCATION, UNKNOWN_LOCATION)

        val locLower = location.lowercase()

        // 1. Known Constants Fast-Path
        for ((key, triple) in knownLocations) {
            if (locLower.contains(key)) {
                return triple
            }
        }

        // 2. Comma-Splitting Fallback for less common locations
        val parts = location.split(",").map { it.trim() }

        val result = when (parts.size) {
            0 -> Triple(UNKNOWN_LOCATION, UNKNOWN_LOCATION, UNKNOWN_LOCATION)
            1 -> Triple(parts[0], UNKNOWN_LOCATION, UNKNOWN_LOCATION)
            2 -> Triple(parts[0], UNKNOWN_LOCATION, parts[1])
            else -> Triple(parts[0], parts[1], parts.last())
        }
        return Triple(result.first, result.second, normalizeCountry(result.third))
    }

    /** Detects "Remote", "Hybrid" based on location or title keywords. Returns "On-site" if no explicit keyword is found. */
    fun extractWorkModel(location: String?, title: String?): String {
        val locLower = location?.lowercase() ?: ""
        val titleLower = title?.lowercase() ?: ""
        val combined = "$locLower $titleLower"
        return when {
            combined.contains("remote") || combined.contains("teletrabajo") -> "Remote"
            combined.contains("hybrid") || combined.contains("híbrido") -> "Hybrid"
            combined.contains("presencial") -> "On-site"
            else -> "On-site"
        }
    }

    /** Heuristically determines seniority level from the job title. */
    fun extractSeniority(title: String, existingLevel: String?): String {
        val t = title.lowercase()
        return when {
            t.contains("senior") || t.contains(" sr.") || t.contains(" sr ") -> "Senior"
            t.contains("lead") || t.contains("principal") || t.contains("staff") -> "Lead/Principal"
            t.contains("junior") || t.contains(" jr.") || t.contains(" jr ") -> "Junior"
            !existingLevel.isNullOrBlank() -> existingLevel
            else -> "Mid-Level"
        }
    }

    /**
     * Scans the job description for 100+ known technology keywords. Uses word-boundary regex to
     * prevent false positives (e.g. "Go" vs "Google").
     */
    fun extractTechnologies(description: String): List<String> {
        val descLower = description.lowercase()
        return techKeywords
                .filter { tech ->
                    val keyword = if (tech.startsWith(".")) tech else tech.lowercase()
                    // Match the keyword as a whole word only
                    descLower.contains(Regex("\\b${Regex.escape(keyword)}\\b"))
                }
                .sorted()
    }

    /**
     * Parses a salary string into a NormalizedSalary object with currency, period, and source info.
     *
     * Examples:
     * - "$120,000" → NormalizedSalary(12000000, "NZD", "YEAR", "JOB_POSTING")
     * - "€35.000" → NormalizedSalary(3500000, "EUR", "YEAR", "JOB_POSTING")
     * - "$45/hr" → NormalizedSalary(4500, "NZD", "HOUR", "JOB_POSTING")
     * - "$80k-$100k" → Use parseSalaryRange instead
     *
     * @param salaryStr The salary string to parse
     * @param country The country code (NZ, AU, ES, etc.) for defaults
     * @param source The source of the salary data (default: JOB_POSTING)
     */
    fun parseSalary(salaryStr: String?, country: String, source: String = SOURCE_JOB_POSTING): NormalizedSalary? {
        if (salaryStr == null || salaryStr.isBlank()) return null

        // Check for non-salary indicators
        if (isNonSalaryIndicator(salaryStr)) return null

        // 1. Detect currency from symbols and country context
        val currency = detectCurrency(salaryStr, country)

        // 2. Detect period from keywords (default to country-specific)
        val period = detectPeriod(salaryStr)
            ?: getDefaultPeriodForCountry(country)

        // 3. Check for gross/net indicators (default to gross)
        val isGross = !containsNetIndicator(salaryStr)

        // 4. Extract amount (handle European format, stop at '+' for benefits)
        val amount = extractAmount(salaryStr, currency)

        if (amount <= 0) return null

        // Note: disclaimer is computed at BFF level via NormalizedSalary.getDisclaimerForSource()
        return NormalizedSalary(amount, currency, period, source, isGross)
    }

    /**
     * Parses a salary range string into min/max NormalizedSalary objects.
     *
     * Examples:
     * - "$80k-$100k" → (min: 8000000, max: 10000000)
     * - "$120,000 - $150,000" → (min: 12000000, max: 15000000)
     */
    fun parseSalaryRange(salaryStr: String?, country: String, source: String = SOURCE_JOB_POSTING): Pair<NormalizedSalary?, NormalizedSalary?> {
        if (salaryStr == null || salaryStr.isBlank()) return null to null

        // Try to split on common range separators
        val rangePatterns = listOf(
            Regex("\\s*[-–—]\\s*"),  // hyphen, en-dash, em-dash with optional spaces
            Regex("\\s+to\\s+"),     // "80k to 100k"
            Regex("\\s*&\\s*")       // "80k & 100k"
        )

        var parts: List<String>? = null
        for (pattern in rangePatterns) {
            parts = salaryStr.split(pattern)
            if (parts.size == 2) break
        }

        if (parts == null || parts.size != 2) {
            // Not a range, try parsing as single value
            val single = parseSalary(salaryStr, country, source)
            return single to single
        }

        val min = parseSalary(parts[0].trim(), country, source)
        val max = parseSalary(parts[1].trim(), country, source)

        return min to max
    }

    /**
     * Checks if the string indicates no specific salary (e.g., "competitive", "market rate").
     */
    private fun isNonSalaryIndicator(salaryStr: String): Boolean {
        val lower = salaryStr.lowercase().trim()
        return lower in listOf(
            "competitive",
            "competitive salary",
            "market rate",
            "market related",
            "negotiable",
            "doe",  // depending on experience
            "depending on experience",
            "to be discussed",
            "tbd",
            "confidential"
        ) || lower.contains("competitive") || lower.contains("market rate")
    }

    /**
     * Detects currency from symbols and country context.
     */
    private fun detectCurrency(salaryStr: String, country: String): String {
        // Check for explicit currency symbols/codes
        return when {
            salaryStr.contains("€") || salaryStr.contains("EUR") -> CURRENCY_EUR
            salaryStr.contains("£") || salaryStr.contains("GBP") -> CURRENCY_GBP
            salaryStr.contains("$") -> {
                // Dollar sign - use country to determine which dollar
                when (country.uppercase()) {
                    "AU" -> CURRENCY_AUD
                    "NZ" -> CURRENCY_NZD
                    "US" -> CURRENCY_USD
                    "CA" -> "CAD"
                    else -> getDefaultCurrencyForCountry(country)
                }
            }
            else -> getDefaultCurrencyForCountry(country)
        }
    }

    /**
     * Detects salary period from keywords.
     * Returns null if no explicit period found (will use country default).
     */
    private fun detectPeriod(salaryStr: String): String? {
        val lower = salaryStr.lowercase()
        return when {
            // Hourly indicators
            lower.contains("/hr") || lower.contains("/hour") || lower.contains(" per hour") ||
            lower.contains(" hourly") || lower.contains("p/h") || lower.contains("ph") ->
                PERIOD_HOUR

            // Daily indicators
            lower.contains("/day") || lower.contains(" per day") || lower.contains(" daily") ||
            lower.contains("per diem") ->
                PERIOD_DAY

            // Monthly indicators
            lower.contains("/month") || lower.contains(" per month") || lower.contains(" monthly") ||
            lower.contains(" pcm") || lower.contains("per month") ->
                PERIOD_MONTH

            // Yearly indicators
            lower.contains("/year") || lower.contains(" per year") || lower.contains(" yearly") ||
            lower.contains(" annual") || lower.contains("/annum") || lower.contains(" p.a.") ||
            lower.contains("per annum") ->
                PERIOD_YEAR

            else -> null  // Use country default
        }
    }

    /**
     * Checks if the salary string indicates net (after-tax) salary.
     * Spanish: "Neto", English: "Net", "after tax"
     */
    private fun containsNetIndicator(salaryStr: String): Boolean {
        val lower = salaryStr.lowercase()
        return lower.contains("neto") ||
               lower.contains(" net ") ||
               lower.contains("after tax") ||
               lower.contains("take home")
    }

    /**
     * Extracts the numeric amount from a salary string.
     * Handles European format (dot = thousands separator for EUR).
     * Stops at '+' sign to avoid including benefits/super.
     * Handles decimal points correctly (e.g., "$120,000.00" → 12000000 cents).
     */
    private fun extractAmount(salaryStr: String, currency: String): Long {
        // Determine thousands separator based on currency
        val thousandsSeparator = when (currency) {
            CURRENCY_EUR -> "."  // Most EU uses dot for thousands
            else -> ","  // AU/NZ/US use comma
        }

        var cleaned = salaryStr
            .replace(Regex("[€\\$£]"), "")
            .replace("EUR", "")
            .replace("USD", "")
            .replace("NZD", "")
            .replace("AUD", "")
            .replace("GBP", "")
            .trim()

        // Stop at "+" sign FIRST (e.g., "$120k + super" → "$120k")
        cleaned = cleaned.substringBefore("+").trim()

        // Handle "k" suffix (e.g., 80k → 80000)
        if (cleaned.endsWith("k", ignoreCase = true)) {
            cleaned = cleaned.dropLast(1) + "000"
        }

        // Remove common words that might interfere
        cleaned = cleaned.replace(Regex("(?i)salario|bruto|neto|about|approximately|circa"), "")

        // Replace thousands separator with nothing
        cleaned = cleaned.replace(thousandsSeparator, "")

        // Handle decimal point: split into whole and fractional parts
        val decimalIndex = cleaned.indexOf('.')
        val (wholePart, fractionalPart) = if (decimalIndex >= 0) {
            val whole = cleaned.substring(0, decimalIndex).replace(Regex("[^0-9]"), "")
            // Take up to 2 decimal digits (cents)
            val frac = cleaned.substring(decimalIndex + 1).replace(Regex("[^0-9]"), "").take(2)
            whole to frac
        } else {
            // No decimal point - extract digits only
            cleaned.replace(Regex("[^0-9]"), "") to ""
        }

        // Combine whole and fractional parts, then convert to cents
        val digits = wholePart + fractionalPart.padEnd(2, '0')

        return if (digits.isNotBlank()) {
            digits.toLong()
        } else {
            0L
        }
    }

    /** Parses ISO-8601 date strings or Unix timestamps. */
    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null || dateStr.isBlank()) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            // Fallback: Handle numeric or scientific notation timestamps (seconds)
            try {
                val timestampSeconds = dateStr.toDouble().toLong()
                Instant.ofEpochSecond(timestampSeconds)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
            } catch (ex: Exception) {
                null
            }
        }
    }
}
