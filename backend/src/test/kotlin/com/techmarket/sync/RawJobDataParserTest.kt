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

    // ===== Spain Support Tests =====

    // --- determineCountry ---

    @Test
    fun `determineCountry returns ES for Barcelona`() {
        assertEquals("ES", parser.determineCountry("Barcelona"))
    }

    @Test
    fun `determineCountry returns ES for Valencia`() {
        assertEquals("ES", parser.determineCountry("Valencia"))
    }

    @Test
    fun `determineCountry returns ES for Sevilla`() {
        assertEquals("ES", parser.determineCountry("Sevilla"))
    }

    @Test
    fun `determineCountry returns ES for España`() {
        assertEquals("ES", parser.determineCountry("España"))
    }

    @Test
    fun `determineCountry returns ES for location with España`() {
        assertEquals("ES", parser.determineCountry("Madrid, España"))
    }

    // --- parseLocation ---

    @Test
    fun `parseLocation resolves Madrid to full tuple`() {
        val (city, state, country) = parser.parseLocation("Madrid, Spain")
        assertEquals("Madrid", city)
        assertEquals("Community of Madrid", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Barcelona to full tuple`() {
        val (city, state, country) = parser.parseLocation("Barcelona, Spain")
        assertEquals("Barcelona", city)
        assertEquals("Catalonia", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Valencia to full tuple`() {
        val (city, state, country) = parser.parseLocation("Valencia, Spain")
        assertEquals("Valencia", city)
        assertEquals("Valencian Community", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Murcia to full tuple`() {
        val (city, state, country) = parser.parseLocation("Murcia, Spain")
        assertEquals("Murcia", city)
        assertEquals("Region of Murcia", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Palma to full tuple`() {
        val (city, state, country) = parser.parseLocation("Palma, Spain")
        assertEquals("Palma", city)
        assertEquals("Balearic Islands", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Las Palmas to full tuple`() {
        val (city, state, country) = parser.parseLocation("Las Palmas, Spain")
        assertEquals("Las Palmas", city)
        assertEquals("Canary Islands", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Santa Cruz to full tuple`() {
        val (city, state, country) = parser.parseLocation("Santa Cruz, Spain")
        assertEquals("Santa Cruz", city)
        assertEquals("Canary Islands", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Alicante to full tuple`() {
        val (city, state, country) = parser.parseLocation("Alicante, Spain")
        assertEquals("Alicante", city)
        assertEquals("Valencian Community", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Granada to full tuple`() {
        val (city, state, country) = parser.parseLocation("Granada, Spain")
        assertEquals("Granada", city)
        assertEquals("Andalusia", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Málaga to full tuple`() {
        val (city, state, country) = parser.parseLocation("Málaga, Spain")
        assertEquals("Málaga", city)
        assertEquals("Andalusia", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Bilbao to full tuple`() {
        val (city, state, country) = parser.parseLocation("Bilbao, Spain")
        assertEquals("Bilbao", city)
        assertEquals("Basque Country", state)
        assertEquals("ES", country)
    }

    @Test
    fun `parseLocation resolves Zaragoza to full tuple`() {
        val (city, state, country) = parser.parseLocation("Zaragoza, Spain")
        assertEquals("Zaragoza", city)
        assertEquals("Aragon", state)
        assertEquals("ES", country)
    }

    // --- extractWorkModel ---

    @Test
    fun `extractWorkModel detects Teletrabajo as Remote`() {
        assertEquals("Remote", parser.extractWorkModel("Madrid, España", "Desarrollador - Teletrabajo"))
    }

    @Test
    fun `extractWorkModel detects Híbrido as Hybrid`() {
        assertEquals("Hybrid", parser.extractWorkModel("Barcelona", "Ingeniero Híbrido"))
    }

    @Test
    fun `extractWorkModel detects Presencial as On-site`() {
        assertEquals("On-site", parser.extractWorkModel("Valencia", "Desarrollador Presencial"))
    }

    @Test
    fun `extractWorkModel detects remote in lowercase Spanish`() {
        assertEquals("Remote", parser.extractWorkModel("teletrabajo", "Desarrollador"))
    }

    // --- parseSalary ---

    @Test
    fun `parseSalary handles European format with euro symbol`() {
        assertEquals(35000, parser.parseSalary("35.000€"))
    }

    @Test
    fun `parseSalary handles European format with euro prefix`() {
        assertEquals(40000, parser.parseSalary("€40.000"))
    }

    @Test
    fun `parseSalary handles Spanish salary format with text`() {
        assertEquals(30000, parser.parseSalary("Salario Bruto Anual 30.000€"))
    }
}
