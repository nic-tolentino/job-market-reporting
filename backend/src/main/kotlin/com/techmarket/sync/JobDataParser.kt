package com.techmarket.sync

import com.techmarket.util.TechFormatter
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.springframework.stereotype.Component

@Component
class JobDataParser {

    private val techKeywords = TechFormatter.getAllOfficialNames()

    // Map of known location substring -> Triple(City, State/Region, CountryCode)
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
                    "madrid" to Triple("Madrid", "Community of Madrid", "ES"),
                    "barcelona" to Triple("Barcelona", "Catalonia", "ES"),
                    "valencia" to Triple("Valencia", "Valencian Community", "ES"),
                    "seville" to Triple("Seville", "Andalusia", "ES"),
                    "sevilla" to Triple("Seville", "Andalusia", "ES"),
                    "zaragoza" to Triple("Zaragoza", "Aragon", "ES"),
                    "malaga" to Triple("Málaga", "Andalusia", "ES"),
                    "málaga" to Triple("Málaga", "Andalusia", "ES"),
                    "bilbao" to Triple("Bilbao", "Basque Country", "ES")
            )

    fun determineCountry(location: String?): String {
        val (c, s, country) = parseLocation(location)
        if (country != "Unknown") return country

        if (location == null) return "Unknown"
        val locUpper = location.uppercase()
        return when {
            locUpper.contains("AUSTRALIA") -> "AU"
            locUpper.contains("NEW ZEALAND") -> "NZ"
            locUpper.contains("SPAIN") -> "ES"
            else -> "Unknown"
        }
    }

    fun parseLocation(location: String?): Triple<String, String, String> {
        if (location == null) return Triple("Unknown", "Unknown", "Unknown")

        val locLower = location.lowercase()

        // 1. Known Constants Fast-Path
        for ((key, triple) in knownLocations) {
            if (locLower.contains(key)) {
                return triple
            }
        }

        // 2. Comma-Splitting Fallback
        val parts = location.split(",").map { it.trim() }

        // TODO: Future enhancement - log when we fallback to comma splitting so we can build up the
        // knownLocations map.

        return when (parts.size) {
            0 ->
                    Triple(
                            "Unknown",
                            "Unknown",
                            "Unknown"
                    ) // Should theoretically not happen with split
            1 -> Triple(parts[0], "Unknown", "Unknown")
            2 -> Triple(parts[0], "Unknown", parts[1]) // Usually City, Country
            else -> Triple(parts[0], parts[1], parts.last()) // Usually City, State, ..., Country
        }
    }

    fun extractWorkModel(location: String?, title: String?): String {
        val locLower = location?.lowercase() ?: ""
        val titleLower = title?.lowercase() ?: ""
        val combined = "$locLower $titleLower"
        return when {
            combined.contains("remote") -> "Remote"
            combined.contains("hybrid") -> "Hybrid"
            else -> "On-site"
        }
    }

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

    fun extractTechnologies(description: String): List<String> {
        val descLower = description.lowercase()
        return techKeywords
                .filter { tech ->
                    val keyword = if (tech.startsWith(".")) tech else tech.lowercase()
                    descLower.contains(Regex("\\b${Regex.escape(keyword)}\\b"))
                }
                .sorted()
    }

    fun parseSalary(salaryStr: String?): Int? {
        if (salaryStr == null) return null
        return try {
            val digits = salaryStr.replace(Regex("[^0-9]"), "")
            if (digits.isNotBlank()) digits.toInt() else null
        } catch (e: Exception) {
            null
        }
    }

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
