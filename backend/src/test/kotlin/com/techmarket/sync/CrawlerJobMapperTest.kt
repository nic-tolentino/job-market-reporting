package com.techmarket.sync

import com.techmarket.sync.model.NormalizedJobDto
import com.techmarket.util.Constants.UNKNOWN_COUNTRY
import com.techmarket.util.Constants.UNKNOWN_LOCATION
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CrawlerJobMapperTest {

    private val parser = mockk<RawJobDataParser>()
    private val mapper = CrawlerJobMapper(parser)

    init {
        // Default parser behaviour — override per test as needed
        every { parser.parseLocation(any()) } returns Triple("Auckland", UNKNOWN_LOCATION, "NZ")
        every { parser.determineCountry(any()) } returns "NZ"
        every { parser.extractTechnologies(any()) } returns emptyList()
        every { parser.extractSeniority(any(), any()) } returns "Mid-Level"
        every { parser.extractWorkModel(any(), any(), any()) } returns "On-site"
    }

    // -------------------------------------------------------------------------
    // jobId
    // -------------------------------------------------------------------------

    @Test
    fun `jobId is built from companyId, country, title and postedAt date`() {
        val job = dto(title = "Senior Angular Developer", postedAt = "2026-03-16")
        val records = mapper.toJobRecords(listOf(job), "trademe")
        assertEquals("trademe.nz.senior-angular-developer.2026-03-16", records[0].jobId)
    }

    @Test
    fun `jobId falls back to today when postedAt is null`() {
        val job = dto(title = "Developer", postedAt = null)
        val records = mapper.toJobRecords(listOf(job), "trademe")
        val today = LocalDate.now().toString()
        assertTrue(records[0].jobId.endsWith(today), "Expected jobId to end with $today but was ${records[0].jobId}")
    }

    @Test
    fun `jobId falls back to today when postedAt is malformed`() {
        val job = dto(title = "Developer", postedAt = "not-a-date")
        val records = mapper.toJobRecords(listOf(job), "acme")
        val today = LocalDate.now().toString()
        assertTrue(records[0].jobId.endsWith(today))
    }

    // -------------------------------------------------------------------------
    // Location & country
    // -------------------------------------------------------------------------

    @Test
    fun `country is lowercased from parseLocation result`() {
        every { parser.parseLocation("Wellington, NZ") } returns Triple("Wellington", UNKNOWN_LOCATION, "NZ")
        val job = dto(location = "Wellington, NZ")
        val records = mapper.toJobRecords(listOf(job), "xero")
        assertEquals("nz", records[0].country)
        assertEquals("Wellington", records[0].city)
    }

    @Test
    fun `falls back to determineCountry when parseLocation returns unknown`() {
        every { parser.parseLocation("New Zealand") } returns Triple(UNKNOWN_LOCATION, UNKNOWN_LOCATION, UNKNOWN_COUNTRY)
        every { parser.determineCountry("New Zealand") } returns "NZ"
        val job = dto(location = "New Zealand")
        val records = mapper.toJobRecords(listOf(job), "xero")
        assertEquals("nz", records[0].country)
    }

    @Test
    fun `unknown city is excluded from locations list`() {
        every { parser.parseLocation(any()) } returns Triple(UNKNOWN_LOCATION, UNKNOWN_LOCATION, "NZ")
        val job = dto(location = "New Zealand")
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertTrue(records[0].locations.isEmpty(), "Unknown city should not be in locations list")
    }

    @Test
    fun `known city is included in locations list`() {
        every { parser.parseLocation(any()) } returns Triple("Auckland", UNKNOWN_LOCATION, "NZ")
        val job = dto(location = "Auckland, NZ")
        val records = mapper.toJobRecords(listOf(job), "trademe")
        assertEquals(listOf("Auckland"), records[0].locations)
    }

    // -------------------------------------------------------------------------
    // Technologies
    // -------------------------------------------------------------------------

    @Test
    fun `technologies are extracted from title and description combined`() {
        every { parser.extractTechnologies("Angular Developer Angular is great") } returns listOf("Angular")
        val job = dto(title = "Angular Developer", descriptionText = "Angular is great")
        val records = mapper.toJobRecords(listOf(job), "trademe")
        assertEquals(listOf("Angular"), records[0].technologies)
    }

    @Test
    fun `empty technologies when parser returns nothing`() {
        every { parser.extractTechnologies(any()) } returns emptyList()
        val records = mapper.toJobRecords(listOf(dto()), "acme")
        assertTrue(records[0].technologies.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Seniority
    // -------------------------------------------------------------------------

    @Test
    fun `seniority delegates to parser with title and existing level`() {
        every { parser.extractSeniority("Senior Dev", "Senior") } returns "Senior"
        val job = dto(title = "Senior Dev", seniorityLevel = "Senior")
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("Senior", records[0].seniorityLevel)
    }

    // -------------------------------------------------------------------------
    // Work model
    // -------------------------------------------------------------------------

    @Test
    fun `workModel uses job field when present`() {
        val job = dto(workModel = "Remote")
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("Remote", records[0].workModel)
    }

    @Test
    fun `workModel falls back to parser extraction when job field is blank`() {
        every { parser.extractWorkModel(any(), any(), any()) } returns "Hybrid"
        val job = dto(workModel = null)
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("Hybrid", records[0].workModel)
    }

    @Test
    fun `workModel defaults to On-site when both job field and parser return nothing`() {
        every { parser.extractWorkModel(any(), any(), any()) } returns "On-site"
        val job = dto(workModel = null)
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("On-site", records[0].workModel)
    }

    // -------------------------------------------------------------------------
    // Salary
    // -------------------------------------------------------------------------

    @Test
    fun `salary amounts are converted to cents`() {
        val job = dto(salaryMin = 80000.0, salaryMax = 100000.0, salaryCurrency = "NZD")
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals(8_000_000L, records[0].salaryMin?.amount)
        assertEquals(10_000_000L, records[0].salaryMax?.amount)
    }

    @Test
    fun `salary currency defaults to country default when null`() {
        // NZ country → NZD
        val job = dto(salaryMin = 80000.0, salaryCurrency = null)
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("NZD", records[0].salaryMin?.currency)
    }

    @Test
    fun `null salary produces null salaryMin and salaryMax`() {
        val job = dto(salaryMin = null, salaryMax = null)
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertNull(records[0].salaryMin)
        assertNull(records[0].salaryMax)
    }

    // -------------------------------------------------------------------------
    // Source & static fields
    // -------------------------------------------------------------------------

    @Test
    fun `source is always Crawler`() {
        val records = mapper.toJobRecords(listOf(dto()), "acme")
        assertEquals("Crawler", records[0].source)
    }

    @Test
    fun `urlStatus is always UNKNOWN`() {
        val records = mapper.toJobRecords(listOf(dto()), "acme")
        assertEquals("UNKNOWN", records[0].urlStatus)
    }

    @Test
    fun `applyUrl and platformUrl are wrapped in single-element lists`() {
        val job = dto(applyUrl = "https://apply.example.com", platformUrl = "https://jobs.example.com/1")
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals(listOf("https://apply.example.com"), records[0].applyUrls)
        assertEquals(listOf("https://jobs.example.com/1"), records[0].platformLinks)
    }

    @Test
    fun `null applyUrl and platformUrl produce empty lists`() {
        val job = dto(applyUrl = null, platformUrl = null)
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertTrue(records[0].applyUrls.isEmpty())
        assertTrue(records[0].platformLinks.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Null-safety & error handling
    // -------------------------------------------------------------------------

    @Test
    fun `null title falls back to Unknown Title`() {
        val job = dto(title = null)
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("Unknown Title", records[0].title)
    }

    @Test
    fun `blank title falls back to Unknown Title`() {
        val job = dto(title = "   ")
        val records = mapper.toJobRecords(listOf(job), "acme")
        assertEquals("Unknown Title", records[0].title)
    }

    @Test
    fun `failed mapping for one job does not prevent others`() {
        // Force parser to throw on the first job's title
        every { parser.extractSeniority("CRASH", any()) } throws RuntimeException("boom")
        every { parser.extractSeniority("Safe Job", any()) } returns "Mid-Level"

        val crash = dto(title = "CRASH")
        val safe = dto(title = "Safe Job")
        val records = mapper.toJobRecords(listOf(crash, safe), "acme")

        assertEquals(1, records.size)
        assertEquals("Safe Job", records[0].title)
    }

    @Test
    fun `empty input returns empty list`() {
        val records = mapper.toJobRecords(emptyList(), "acme")
        assertTrue(records.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private fun dto(
        platformId: String? = "pid-1",
        title: String? = "Software Engineer",
        companyName: String? = "Acme Corp",
        location: String? = "Auckland, NZ",
        descriptionText: String? = "We are looking for a developer.",
        descriptionHtml: String? = null,
        salaryMin: Double? = null,
        salaryMax: Double? = null,
        salaryCurrency: String? = null,
        employmentType: String? = "Full-time",
        seniorityLevel: String? = null,
        workModel: String? = null,
        department: String? = null,
        postedAt: String? = "2026-03-16",
        applyUrl: String? = null,
        platformUrl: String? = null,
    ) = NormalizedJobDto(
        platformId = platformId,
        source = "Crawler",
        title = title,
        companyName = companyName,
        location = location,
        descriptionHtml = descriptionHtml,
        descriptionText = descriptionText,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        salaryCurrency = salaryCurrency,
        employmentType = employmentType,
        seniorityLevel = seniorityLevel,
        workModel = workModel,
        department = department,
        postedAt = postedAt,
        applyUrl = applyUrl,
        platformUrl = platformUrl,
    )
}
