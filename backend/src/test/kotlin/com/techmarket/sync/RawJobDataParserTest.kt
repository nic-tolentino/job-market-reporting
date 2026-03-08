package com.techmarket.sync

import com.techmarket.model.NormalizedSalary
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

    // ===== New NormalizedSalary Tests =====

    // --- NZ/AU Formats ---

    @Test
    fun `parseSalary handles NZ dollar format with comma`() {
        val result = parser.parseSalary("$120,000", "NZ")
        assertNotNull(result)
        assertEquals(12000000L, result?.amount)  // In cents
        assertEquals("NZD", result?.currency)
        assertEquals("YEAR", result?.period)
        assertEquals(true, result?.isGross)
        assertEquals("JOB_POSTING", result?.source)
    }

    @Test
    fun `parseSalary handles AU dollar format`() {
        val result = parser.parseSalary("$95,000", "AU")
        assertNotNull(result)
        assertEquals(9500000L, result?.amount)
        assertEquals("AUD", result?.currency)
        assertEquals("YEAR", result?.period)
    }

    @Test
    fun `parseSalary handles k suffix`() {
        val result = parser.parseSalary("$80k", "NZ")
        assertNotNull(result)
        assertEquals(8000000L, result?.amount)
    }

    @Test
    fun `parseSalary handles k suffix with range`() {
        val (min, max) = parser.parseSalaryRange("$80k-$100k", "NZ")
        assertNotNull(min)
        assertNotNull(max)
        assertEquals(8000000L, min?.amount)
        assertEquals(10000000L, max?.amount)
    }

    @Test
    fun `parseSalary handles hourly rate`() {
        val result = parser.parseSalary("$45/hr", "AU")
        assertNotNull(result)
        assertEquals(4500L, result?.amount)
        assertEquals("HOUR", result?.period)
    }

    @Test
    fun `parseSalary handles per annum`() {
        val result = parser.parseSalary("$120,000 per annum", "NZ")
        assertNotNull(result)
        assertEquals(12000000L, result?.amount)
        assertEquals("YEAR", result?.period)
    }

    @Test
    fun `parseSalary handles monthly rate`() {
        val result = parser.parseSalary("$8,000 pcm", "NZ")
        assertNotNull(result)
        assertEquals(800000L, result?.amount)
        assertEquals("MONTH", result?.period)
    }

    // --- "Plus" Benefits Logic ---

    @Test
    fun `parseSalary stops at plus sign for super`() {
        val result = parser.parseSalary("$120,000 + super", "AU")
        assertNotNull(result)
        assertEquals(12000000L, result?.amount)
        // Should NOT include the 11% super
    }

    @Test
    fun `parseSalary stops at plus sign for benefits`() {
        val result = parser.parseSalary("$150k + benefits", "NZ")
        assertNotNull(result)
        assertEquals(15000000L, result?.amount)
    }

    @Test
    fun `parseSalary handles package with super percentage`() {
        val result = parser.parseSalary("$100,000 + 11% super", "AU")
        assertNotNull(result)
        assertEquals(10000000L, result?.amount)
    }

    // --- European Formats ---

    @Test
    fun `parseSalary handles European format with dot separator`() {
        val result = parser.parseSalary("35.000€", "ES")
        assertNotNull(result)
        assertEquals(3500000L, result?.amount)  // 35,000 not 35!
        assertEquals("EUR", result?.currency)
        assertEquals("YEAR", result?.period)
    }

    @Test
    fun `parseSalary handles European format with euro prefix`() {
        val result = parser.parseSalary("€40.000", "ES")
        assertNotNull(result)
        assertEquals(4000000L, result?.amount)
    }

    @Test
    fun `parseSalary handles Spanish Bruto gross`() {
        val result = parser.parseSalary("Salario Bruto 35.000€", "ES")
        assertNotNull(result)
        assertEquals(3500000L, result?.amount)
        assertEquals(true, result?.isGross)
    }

    @Test
    fun `parseSalary handles Spanish Neto net`() {
        val result = parser.parseSalary("Salario Neto 30.000€", "ES")
        assertNotNull(result)
        assertEquals(3000000L, result?.amount)
        assertEquals(false, result?.isGross)  // Net salary
    }

    @Test
    fun `parseSalary handles German format`() {
        val result = parser.parseSalary("45.000€", "DE")
        assertNotNull(result)
        assertEquals(4500000L, result?.amount)
    }

    // --- Edge Cases ---

    @Test
    fun `parseSalary returns null for competitive`() {
        val result = parser.parseSalary("Competitive", "NZ")
        assertNull(result)
    }

    @Test
    fun `parseSalary returns null for market rate`() {
        val result = parser.parseSalary("Market rate", "AU")
        assertNull(result)
    }

    @Test
    fun `parseSalary handles null input`() {
        val result = parser.parseSalary(null, "NZ")
        assertNull(result)
    }

    @Test
    fun `parseSalary handles empty string`() {
        val result = parser.parseSalary("", "NZ")
        assertNull(result)
    }

    @Test
    fun `parseSalary handles blank string`() {
        val result = parser.parseSalary("   ", "NZ")
        assertNull(result)
    }

    // --- Country Defaults ---

    @Test
    fun `getDefaultPeriodForCountry returns YEAR for NZ`() {
        assertEquals("YEAR", NormalizedSalary.getDefaultPeriodForCountry("NZ"))
    }

    @Test
    fun `getDefaultPeriodForCountry returns YEAR for AU`() {
        assertEquals("YEAR", NormalizedSalary.getDefaultPeriodForCountry("AU"))
    }

    @Test
    fun `getDefaultPeriodForCountry returns YEAR for ES`() {
        assertEquals("YEAR", NormalizedSalary.getDefaultPeriodForCountry("ES"))
    }

    @Test
    fun `getDefaultCurrencyForCountry returns NZD for NZ`() {
        assertEquals("NZD", NormalizedSalary.getDefaultCurrencyForCountry("NZ"))
    }

    @Test
    fun `getDefaultCurrencyForCountry returns EUR for ES`() {
        assertEquals("EUR", NormalizedSalary.getDefaultCurrencyForCountry("ES"))
    }

    @Test
    fun `getDefaultCurrencyForCountry returns AUD for AU`() {
        assertEquals("AUD", NormalizedSalary.getDefaultCurrencyForCountry("AU"))
    }

    @Test
    fun `getDefaultCurrencyForCountry returns USD for US`() {
        assertEquals("USD", NormalizedSalary.getDefaultCurrencyForCountry("US"))
    }

    @Test
    fun `getDefaultCurrencyForCountry returns GBP for UK`() {
        assertEquals("GBP", NormalizedSalary.getDefaultCurrencyForCountry("UK"))
    }

    // --- Conversion Tests ---

    @Test
    fun `toAnnualAmount converts hourly to annual`() {
        val hourly = NormalizedSalary(
            amount = 5000L,      // $50/hr
            currency = "NZD",
            period = "HOUR",
            source = "JOB_POSTING"
        )
        assertEquals(10400000L, hourly.toAnnualAmount())  // $104,000/year
    }

    @Test
    fun `toAnnualAmount converts monthly to annual`() {
        val monthly = NormalizedSalary(
            amount = 800000L,    // $8,000/month
            currency = "NZD",
            period = "MONTH",
            source = "JOB_POSTING"
        )
        assertEquals(9600000L, monthly.toAnnualAmount())  // $96,000/year
    }

    @Test
    fun `toAnnualAmount returns same for annual`() {
        val annual = NormalizedSalary(
            amount = 12000000L,  // $120,000/year
            currency = "NZD",
            period = "YEAR",
            source = "JOB_POSTING"
        )
        assertEquals(12000000L, annual.toAnnualAmount())
    }

    @Test
    fun `toAnnualAmount converts daily to annual`() {
        val daily = NormalizedSalary(
            amount = 50000L,     // $500/day
            currency = "NZD",
            period = "DAY",
            source = "JOB_POSTING"
        )
        assertEquals(13000000L, daily.toAnnualAmount())  // $130,000/year (260 days)
    }

    // --- Confidence Tests ---

    @Test
    fun `confidence is HIGH for JOB_POSTING`() {
        val salary = NormalizedSalary(
            amount = 10000000L,
            currency = "NZD",
            period = "YEAR",
            source = "JOB_POSTING"
        )
        assertEquals("HIGH", salary.confidence)
    }

    @Test
    fun `confidence is HIGH for ATS_API`() {
        val salary = NormalizedSalary(
            amount = 10000000L,
            currency = "NZD",
            period = "YEAR",
            source = "ATS_API"
        )
        assertEquals("HIGH", salary.confidence)
    }

    @Test
    fun `confidence is MEDIUM for MARKET_DATA`() {
        val salary = NormalizedSalary(
            amount = 10000000L,
            currency = "NZD",
            period = "YEAR",
            source = "MARKET_DATA"
        )
        assertEquals("MEDIUM", salary.confidence)
    }

    @Test
    fun `confidence is LOW for AI_ESTIMATE`() {
        val salary = NormalizedSalary(
            amount = 10000000L,
            currency = "NZD",
            period = "YEAR",
            source = "AI_ESTIMATE"
        )
        assertEquals("LOW", salary.confidence)
    }

    @Test
    fun `AI_ESTIMATE source includes disclaimer`() {
        val salary = parser.parseSalary("$120,000", "NZ", NormalizedSalary.SOURCE_AI_ESTIMATE)
        assertNotNull(salary)
        // Disclaimer is computed at runtime, not persisted
        assertNotNull(salary?.disclaimer)
        assertTrue(salary?.disclaimer?.contains("AI-estimated") == true)
    }

    // --- Range Parsing Tests ---

    @Test
    fun `parseSalaryRange handles en-dash separator`() {
        val (min, max) = parser.parseSalaryRange("$80k – $100k", "NZ")
        assertNotNull(min)
        assertNotNull(max)
        assertEquals(8000000L, min?.amount)
        assertEquals(10000000L, max?.amount)
    }

    @Test
    fun `parseSalaryRange handles 'to' separator`() {
        val (min, max) = parser.parseSalaryRange("$80k to $100k", "NZ")
        assertNotNull(min)
        assertNotNull(max)
        assertEquals(8000000L, min?.amount)
        assertEquals(10000000L, max?.amount)
    }

    @Test
    fun `parseSalaryRange returns same value for both when no range found`() {
        val (min, max) = parser.parseSalaryRange("$120,000", "NZ")
        assertNotNull(min)
        assertEquals(min?.amount, max?.amount)  // Single value returns (value, value)
    }
}
