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
}
