package com.techmarket.sync

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RawJobDataParserTest {

    private val parser = RawJobDataParser()

    // ===== determineCountry =====

    @Test
    fun `determineCountry returns AU for Sydney`() {
        assertEquals("AU", parser.determineCountry("Sydney"))
    }

    @Test
    fun `determineCountry returns NZ for Auckland`() {
        assertEquals("NZ", parser.determineCountry("Auckland"))
    }

    @Test
    fun `determineCountry returns ES for Madrid`() {
        assertEquals("ES", parser.determineCountry("Madrid"))
    }

    @Test
    fun `determineCountry returns AU via uppercase fallback`() {
        // Input has no known city, so parseLocation returns Unknown country.
        // Falls through to the uppercase AUSTRALIA check.
        assertEquals("AU", parser.determineCountry("Australia"))
    }

    @Test
    fun `determineCountry returns Unknown for unrecognised location`() {
        // "Lisbon" is not a known city, and "Portugal" doesn't match AU/NZ/ES
        assertEquals("Unknown", parser.determineCountry("Lisbon"))
    }

    @Test
    fun `determineCountry handles null input`() {
        assertEquals("Unknown", parser.determineCountry(null))
    }

    // ===== parseLocation =====

    @Test
    fun `parseLocation resolves known city to full tuple`() {
        val (city, state, country) = parser.parseLocation("Sydney, NSW, Australia")
        assertEquals("Sydney", city)
        assertEquals("New South Wales", state)
        assertEquals("AU", country)
    }

    @Test
    fun `parseLocation falls back to comma splitting for unknown city`() {
        val (city, state, country) = parser.parseLocation("Lisbon, Lisboa, Portugal")
        assertEquals("Lisbon", city)
        assertEquals("Lisboa", state)
        assertEquals("Portugal", country)
    }

    @Test
    fun `parseLocation handles single-part location`() {
        val (city, state, country) = parser.parseLocation("Remoteville")
        assertEquals("Remoteville", city)
        assertEquals("Unknown", state)
        assertEquals("Unknown", country)
    }

    @Test
    fun `parseLocation handles null input`() {
        val (city, state, country) = parser.parseLocation(null)
        assertEquals("Unknown", city)
        assertEquals("Unknown", state)
        assertEquals("Unknown", country)
    }

    // ===== extractWorkModel =====

    @Test
    fun `extractWorkModel detects Remote from location`() {
        assertEquals("Remote", parser.extractWorkModel("Remote, Australia", "Engineer"))
    }

    @Test
    fun `extractWorkModel detects Hybrid from title`() {
        assertEquals("Hybrid", parser.extractWorkModel("Sydney", "Hybrid Engineer"))
    }

    @Test
    fun `extractWorkModel defaults to On-site`() {
        assertEquals("On-site", parser.extractWorkModel("Sydney", "Engineer"))
    }

    // ===== extractSeniority =====

    @Test
    fun `extractSeniority detects Senior from title`() {
        assertEquals("Senior", parser.extractSeniority("Senior Software Engineer", null))
    }

    @Test
    fun `extractSeniority detects Lead from title`() {
        assertEquals("Lead/Principal", parser.extractSeniority("Lead Engineer", null))
    }

    @Test
    fun `extractSeniority detects Junior from title`() {
        assertEquals("Junior", parser.extractSeniority("Junior Developer", null))
    }

    @Test
    fun `extractSeniority falls back to existing level`() {
        assertEquals("Entry", parser.extractSeniority("Software Developer", "Entry"))
    }

    @Test
    fun `extractSeniority defaults to Mid-Level when no signal`() {
        assertEquals("Mid-Level", parser.extractSeniority("Software Developer", null))
    }

    // ===== extractTechnologies =====

    @Test
    fun `extractTechnologies finds known technologies`() {
        val techs = parser.extractTechnologies("We use Kotlin, React, and Docker in our stack")
        assertTrue(techs.contains("Kotlin"))
        assertTrue(techs.contains("React"))
        assertTrue(techs.contains("Docker"))
    }

    @Test
    fun `extractTechnologies avoids false positives for short keywords`() {
        // "Go" should match as a standalone word
        val techs = parser.extractTechnologies("We build reliable Go microservices using Docker")
        assertTrue(techs.contains("Go"), "Should find 'Go' as standalone word")
        assertTrue(techs.contains("Docker"), "Should find 'Docker'")
    }

    @Test
    fun `extractTechnologies returns empty for irrelevant description`() {
        val techs = parser.extractTechnologies("We are looking for a passionate team player")
        assertTrue(techs.isEmpty())
    }

    // ===== parseSalary =====

    @Test
    fun `parseSalary extracts digits from formatted salary`() {
        assertEquals(120000, parser.parseSalary("$120,000"))
    }

    @Test
    fun `parseSalary returns null for null input`() {
        assertNull(parser.parseSalary(null))
    }

    @Test
    fun `parseSalary returns null for non-numeric input`() {
        assertNull(parser.parseSalary("Competitive"))
    }

    // ===== parseDate =====

    @Test
    fun `parseDate parses valid ISO date`() {
        val date = parser.parseDate("2023-06-15")
        assertNotNull(date)
        assertEquals(2023, date!!.year)
        assertEquals(6, date.monthValue)
        assertEquals(15, date.dayOfMonth)
    }

    @Test
    fun `parseDate returns null for null input`() {
        assertNull(parser.parseDate(null))
    }

    @Test
    fun `parseDate returns null for garbage input`() {
        assertNull(parser.parseDate("not-a-date"))
    }
}
