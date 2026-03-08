package com.techmarket.sync

import com.techmarket.util.Constants
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
        if (country != "Unknown") return country

        if (location == null) return Constants.UNKNOWN_LOCATION
        val locUpper = location.uppercase()
        return when {
            locUpper.contains("AUSTRALIA") -> "AU"
            locUpper.contains("NEW ZEALAND") -> "NZ"
            locUpper.contains("SPAIN") || locUpper.contains("ESPAÑA") -> "ES"
            else -> Constants.UNKNOWN_LOCATION
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
        if (location == null) return Triple(Constants.UNKNOWN_LOCATION, Constants.UNKNOWN_LOCATION, Constants.UNKNOWN_LOCATION)

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
            0 -> Triple(Constants.UNKNOWN_LOCATION, Constants.UNKNOWN_LOCATION, Constants.UNKNOWN_LOCATION)
            1 -> Triple(parts[0], Constants.UNKNOWN_LOCATION, Constants.UNKNOWN_LOCATION)
            2 -> Triple(parts[0], Constants.UNKNOWN_LOCATION, parts[1])
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

    /** Extracts numerical digits from a salary string (e.g. "$120,000" -> 120000). */
    fun parseSalary(salaryStr: String?): Int? {
        if (salaryStr == null) return null
        return try {
            val digits = salaryStr.replace(Regex("[^0-9]"), "")
            if (digits.isNotBlank()) digits.toInt() else null
        } catch (e: Exception) {
            null
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
