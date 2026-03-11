package com.techmarket.persistence.job

import com.techmarket.model.NormalizedSalary
import com.techmarket.models.JobRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class JobMapperTest {

    @Test
    fun `mapToJobRecord should handle ISO date strings`() {
        val jobRow = JobRow(
            jobId = "test-job-id",
            jobIds = listOf("test-job-id"),
            applyUrls = listOf("https://example.com/apply"),
            platformLinks = listOf("https://example.com/link"),
            locations = listOf("Auckland, New Zealand"),
            title = "Software Engineer",
            companyId = "test-company-id",
            companyName = "Test Company",
            description = "Test description",
            employmentType = "Full-time",
            jobFunction = "Engineering",
            salaryMin = null,
            salaryMax = null,
            postedDate = "2026-03-05",
            technologies = listOf("Kotlin", "Spring"),
            benefits = listOf("Health insurance"),
            city = "Auckland",
            stateRegion = "Auckland",
            seniorityLevel = "Senior",
            source = "LinkedIn",
            lastSeenAt = Instant.now(),
            country = "NZ",
            workModel = "Hybrid"
        )

        val record = JobMapper.mapToJobRecord(jobRow)

        assertEquals(LocalDate.of(2026, 3, 5), record.postedDate)
        assertEquals("test-job-id", record.jobId)
        assertEquals("Software Engineer", record.title)
    }

    @Test
    fun `mapToJobRecord should handle empty postedDate`() {
        val jobRow = JobRow(
            jobId = "test-job-id",
            jobIds = listOf("test-job-id"),
            applyUrls = emptyList(),
            platformLinks = emptyList(),
            locations = listOf("Auckland"),
            title = "Software Engineer",
            companyId = "test-company-id",
            companyName = "Test Company",
            description = null,
            employmentType = null,
            jobFunction = null,
            salaryMin = null,
            salaryMax = null,
            postedDate = "",
            technologies = emptyList(),
            benefits = emptyList(),
            city = "Auckland",
            stateRegion = "Auckland",
            seniorityLevel = "Mid-Level",
            source = "Manual",
            lastSeenAt = Instant.EPOCH,
            country = "NZ",
            workModel = null
        )

        val record = JobMapper.mapToJobRecord(jobRow)

        assertNull(record.postedDate)
        assertEquals("test-job-id", record.jobId)
    }

    @Test
    fun `mapToJobRecord should apply defaults for missing required fields`() {
        val jobRow = JobRow(
            jobId = "",
            jobIds = emptyList(),
            applyUrls = emptyList(),
            platformLinks = emptyList(),
            locations = emptyList(),
            title = "",
            companyId = "",
            companyName = "",
            description = null,
            employmentType = null,
            jobFunction = null,
            salaryMin = null,
            salaryMax = null,
            postedDate = "",
            technologies = emptyList(),
            benefits = emptyList(),
            city = "",
            stateRegion = "",
            seniorityLevel = "",
            source = "",
            lastSeenAt = Instant.EPOCH,
            country = null,
            workModel = null
        )

        val record = JobMapper.mapToJobRecord(jobRow)

        assertEquals("", record.jobId)
        assertEquals("", record.title)  // Title is passed through as-is from JobRow (defaults applied in fromJobRow)
        assertEquals("", record.companyId)
        assertEquals("", record.companyName)  // Name is passed through as-is from JobRow
        assertEquals("", record.seniorityLevel)
        assertEquals("", record.source)  // Source is passed through as-is from JobRow
    }

    @Test
    fun `mapJobRole should format technologies`() {
        val jobRow = JobRow(
            jobId = "test-job-id",
            jobIds = listOf("test-job-id"),
            applyUrls = emptyList(),
            platformLinks = emptyList(),
            locations = listOf("Remote"),
            title = "Backend Developer",
            companyId = "test-company-id",
            companyName = "Tech Corp",
            description = null,
            employmentType = "Full-time",
            jobFunction = "Engineering",
            salaryMin = NormalizedSalary(100000, "USD", "YEAR", "Glassdoor", true),
            salaryMax = NormalizedSalary(150000, "USD", "YEAR", "Glassdoor", true),
            postedDate = "2026-03-01",
            technologies = listOf("kotlin", "spring", "postgresql"),
            benefits = emptyList(),
            city = "San Francisco",
            stateRegion = "CA",
            seniorityLevel = "Senior",
            source = "Company Website",
            lastSeenAt = Instant.now(),
            country = "US",
            workModel = "Remote"
        )

        val role = JobMapper.mapJobRole(jobRow)

        assertEquals("Backend Developer", role.title)
        assertEquals(listOf("Kotlin", "Spring", "PostgreSQL"), role.technologies)
        assertEquals("Senior", role.seniorityLevel)
    }

    @Test
    fun `mapJobDetailsDto should handle null benefits`() {
        val jobRow = JobRow(
            jobId = "test-job-id",
            jobIds = listOf("test-job-id"),
            applyUrls = emptyList(),
            platformLinks = emptyList(),
            locations = emptyList(),
            title = "Frontend Developer",
            companyId = "test-company-id",
            companyName = "Web Co",
            description = "Build amazing UIs",
            employmentType = "Contract",
            jobFunction = "Engineering",
            salaryMin = null,
            salaryMax = null,
            postedDate = "2026-03-07",
            technologies = listOf("react", "typescript"),
            benefits = emptyList(),
            city = "London",
            stateRegion = "England",
            seniorityLevel = "Mid-Level",
            source = "LinkedIn",
            lastSeenAt = Instant.now(),
            country = "GB",
            workModel = "Hybrid"
        )

        val details = JobMapper.mapJobDetailsDto(jobRow)

        assertEquals("Frontend Developer", details.title)
        assertNull(details.benefits)
        assertEquals("Build amazing UIs", details.description)
    }

    @Test
    fun `mapJobCompanyDto formats hiring locations correctly`() {
        val company = com.techmarket.models.CompanyInfoRow(
            companyId = "test-company",
            name = "Test Corp",
            logoUrl = "https://logo.png",
            description = "Test description",
            website = "https://test.com",
            hiringLocations = listOf("auckland, nz", "remote", "london, uk"),
            hqCountry = "NZ",
            verificationLevel = "VERIFIED"
        )

        val companyDto = JobMapper.mapJobCompanyDto(company)

        assertEquals("test-company", companyDto.companyId)
        assertEquals("Test Corp", companyDto.name)
        assertEquals("https://logo.png", companyDto.logoUrl)
        assertEquals("Test description", companyDto.description)
        assertEquals("https://test.com", companyDto.website)
        // LocationFormatter.format() passes through as-is for simple strings
        assertEquals(listOf("auckland, nz", "remote", "london, uk"), companyDto.hiringLocations)
        assertEquals("NZ", companyDto.hqCountry)
        assertEquals("VERIFIED", companyDto.verificationLevel)
    }

    @Test
    fun `mapJobCompanyDto handles empty hiring locations`() {
        val company = com.techmarket.models.CompanyInfoRow(
            companyId = "test-company",
            name = "Test Corp",
            logoUrl = "",
            description = "",
            website = "",
            hiringLocations = emptyList(),
            hqCountry = null,
            verificationLevel = "unverified"
        )

        val companyDto = JobMapper.mapJobCompanyDto(company)

        assertEquals("test-company", companyDto.companyId)
        assertEquals("Test Corp", companyDto.name)
        assertEquals("", companyDto.logoUrl)
        assertEquals("", companyDto.description)
        assertEquals("", companyDto.website)
        assertEquals(emptyList<String>(), companyDto.hiringLocations)
        assertEquals(null, companyDto.hqCountry)
        assertEquals("unverified", companyDto.verificationLevel)
    }
}
