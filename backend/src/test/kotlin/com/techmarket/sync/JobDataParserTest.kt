package com.techmarket.sync

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JobDataParserTest {

    private val parser = JobDataParser()

    @Test
    fun `extractSeniority parses titles correctly`() {
        assertEquals("Senior", parser.extractSeniority("Senior Software Engineer", null))
        assertEquals("Lead/Principal", parser.extractSeniority("Principal Engineer", null))
        assertEquals("Junior", parser.extractSeniority("Junior Dev", null))
        assertEquals("Mid-Level", parser.extractSeniority("Software Engineer", null))
        assertEquals(
                "not-senior",
                parser.extractSeniority("Software Engineer", "not-senior")
        ) // existing level fallback isn't perfect, but let's test current behaviour.
        // Oh wait, the original logic had `!existingLevel.isNullOrBlank() -> existingLevel`.
        assertEquals("Entry", parser.extractSeniority("Software Engineer", "Entry"))
    }

    @Test
    fun `extractWorkModel detects keywords correctly`() {
        assertEquals("Remote", parser.extractWorkModel("Anywhere", "Remote Developer"))
        assertEquals("Remote", parser.extractWorkModel("Remote", "Developer"))
        assertEquals("Hybrid", parser.extractWorkModel("Hybrid Location", "Developer"))
        assertEquals("On-site", parser.extractWorkModel("New York", "Developer"))
    }

    @Test
    fun `extractTechnologies finds matching keywords`() {
        val technologies = parser.extractTechnologies("We use Java, Spring Boot, and PostgreSQL.")
        assertEquals(listOf("java", "postgresql", "spring", "spring boot").sorted(), technologies)

        // Edge cases - partial words should not match
        val technologies2 = parser.extractTechnologies("Working with javascripted tools")
        assertEquals(emptyList<String>(), technologies2)
    }

    @Test
    fun `parseSalary converts string to int`() {
        assertEquals(150000, parser.parseSalary("$150,000"))
        assertEquals(150000, parser.parseSalary("150000"))
        assertEquals(null, parser.parseSalary("competitive salary"))
    }

    /**
     * This test runs the parser against a real snapshot of the locations from BigQuery. Since
     * `rawLocation` is no longer stored in the `raw_jobs` table, we must extract it from the Bronze
     * layer (`raw_ingestions`) which preserves the full original JSON.
     *
     * To update the `raw_locations.csv` file with the latest production data, run:
     *
     * `bq query --use_legacy_sql=false --format=csv --max_rows=100000 "SELECT DISTINCT
     * JSON_EXTRACT_SCALAR(rawPayload, '$.location') as location FROM techmarket.raw_ingestions
     * WHERE JSON_EXTRACT_SCALAR(rawPayload, '$.location') IS NOT NULL" >
     * backend/src/test/resources/raw_locations.csv`
     */
    @Test
    fun testProdLocations() {
        val file = java.io.File("src/test/resources/raw_locations.csv")
        if (!file.exists()) return

        var hits = 0
        var misses = 0
        var fallbacks = 0
        val missExamples = mutableListOf<String>()

        file.readLines().drop(1).forEach { line ->
            val location = line.trim('"')
            if (location.isNotBlank()) {
                val (city, state, country) = parser.parseLocation(location)
                if (country != "Unknown" && state != "Unknown") {
                    hits++
                } else if (country != "Unknown" && state == "Unknown") {
                    fallbacks++
                } else {
                    misses++
                    if (missExamples.size < 20) {
                        missExamples.add("$location -> $city, $state, $country")
                    }
                }
            }
        }

        println("=== LOCATION PARSING RESULTS ===")
        println("Hits (Full match): $hits")
        println("Fallbacks (Country matched, State unknown): $fallbacks")
        println("Misses (Completely unknown): $misses")
        println("Sample misses:")
        missExamples.forEach { println(" - $it") }
        println("================================")
    }
}
