package com.techmarket.persistence

import com.techmarket.api.model.JobLocationDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.model.NormalizedSalary
import com.techmarket.models.JobRow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tests for JobRowMapper shared mapper functions.
 * 
 * These functions are used by both JobMapper and TechMapper,
 * so they need dedicated test coverage.
 */
class JobRowMapperTest {

    @Test
    fun `mapToJobRole maps all fields correctly`() {
        val job = createTestJobRow(
            jobId = "test-job",
            title = "Software Engineer",
            companyId = "test-company",
            companyName = "Test Corp",
            city = "Auckland",
            stateRegion = "Auckland",
            technologies = listOf("kotlin", "spring"),
            postedDate = "2026-03-05"
        )

        val role = JobRowMapper.mapToJobRole(job)

        assertEquals("test-job", role.id)
        assertEquals("Software Engineer", role.title)
        assertEquals("test-company", role.companyId)
        assertEquals("Test Corp", role.companyName)
        assertEquals(listOf("Auckland"), role.locations)
        assertEquals(listOf("test-job"), role.jobIds)
        assertEquals(listOf("Kotlin", "Spring"), role.technologies)
        assertEquals("2026-03-05", role.postedDate)
    }

    @Test
    fun `mapToJobRole filters nulls from applyUrls and platformLinks`() {
        val job = createTestJobRow(
            jobId = "test-job",
            applyUrls = listOf("https://apply.com", null, "https://apply2.com"),
            platformLinks = listOf(null, "https://link.com")
        )

        val role = JobRowMapper.mapToJobRole(job)

        assertEquals(listOf("https://apply.com", "https://apply2.com"), role.applyUrls)
        assertEquals(listOf("https://link.com"), role.platformLinks)
    }

    @Test
    fun `mapToJobLocations handles equal length arrays`() {
        val job = createTestJobRow(
            locations = listOf("Auckland", "Wellington"),
            jobIds = listOf("job-1", "job-2"),
            applyUrls = listOf("https://apply1.com", "https://apply2.com"),
            platformLinks = listOf("https://link1.com", "https://link2.com")
        )

        val locations = JobRowMapper.mapToJobLocations(job)

        assertEquals(2, locations.size)
        assertEquals("Auckland", locations[0].location)
        assertEquals("job-1", locations[0].jobId)
        assertEquals("https://apply1.com", locations[0].applyUrl)
        assertEquals("https://link1.com", locations[0].link)
        assertEquals("Wellington", locations[1].location)
        assertEquals("job-2", locations[1].jobId)
    }

    @Test
    fun `mapToJobLocations handles shorter locations array`() {
        val job = createTestJobRow(
            locations = listOf("Auckland"),
            jobIds = listOf("job-1", "job-2", "job-3"),
            applyUrls = listOf("https://apply1.com", "https://apply2.com", "https://apply3.com"),
            platformLinks = listOf("https://link1.com", "https://link2.com", "https://link3.com")
        )

        val locations = JobRowMapper.mapToJobLocations(job)

        assertEquals(1, locations.size)
        assertEquals("Auckland", locations[0].location)
        assertEquals("job-1", locations[0].jobId)
    }

    @Test
    fun `mapToJobLocations handles shorter jobIds array`() {
        val job = createTestJobRow(
            locations = listOf("Auckland", "Wellington", "Christchurch"),
            jobIds = listOf("job-1"),
            applyUrls = listOf("https://apply1.com", "https://apply2.com", "https://apply3.com"),
            platformLinks = listOf("https://link1.com", "https://link2.com", "https://link3.com")
        )

        val locations = JobRowMapper.mapToJobLocations(job)

        assertEquals(1, locations.size)
        assertEquals("Auckland", locations[0].location)
        assertEquals("job-1", locations[0].jobId)
    }

    @Test
    fun `mapToJobLocations handles null applyUrls and platformLinks`() {
        val job = createTestJobRow(
            locations = listOf("Auckland"),
            jobIds = listOf("job-1"),
            applyUrls = listOf(null),
            platformLinks = listOf(null)
        )

        val locations = JobRowMapper.mapToJobLocations(job)

        assertEquals(1, locations.size)
        assertEquals(null, locations[0].applyUrl)
        assertEquals(null, locations[0].link)
    }

    @Test
    fun `mapToJobLocations returns empty list when no locations`() {
        val job = createTestJobRow(
            locations = emptyList(),
            jobIds = listOf("job-1")
        )

        val locations = JobRowMapper.mapToJobLocations(job)

        assertTrue(locations.isEmpty())
    }

    @Test
    fun `mapToJobLocations returns empty list when no jobIds`() {
        val job = createTestJobRow(
            locations = listOf("Auckland"),
            jobIds = emptyList()
        )

        val locations = JobRowMapper.mapToJobLocations(job)

        assertTrue(locations.isEmpty())
    }

    @Test
    fun `buildLocationList handles city and stateRegion`() {
        val result = JobRowMapper.buildLocationList("Auckland", "Auckland")

        assertEquals(listOf("Auckland"), result)
    }

    @Test
    fun `buildLocationList handles Unknown stateRegion`() {
        val result = JobRowMapper.buildLocationList("Auckland", "Unknown")

        assertEquals(listOf("Auckland"), result)
    }

    @Test
    fun `buildLocationList handles different city and stateRegion`() {
        val result = JobRowMapper.buildLocationList("Auckland", "Auckland Region")

        assertEquals(listOf("Auckland, Auckland Region"), result)
    }

    @Test
    fun `buildLocationList handles empty city`() {
        // Edge case: empty city with non-empty stateRegion produces ", Auckland"
        // This shouldn't occur in production as BigQuery always has non-empty city
        val result = JobRowMapper.buildLocationList("", "Auckland")

        assertEquals(listOf(", Auckland"), result)
    }

    @Test
    fun `buildLocationList handles both empty strings`() {
        // Edge case: both empty produces [""]
        // This shouldn't occur in production as BigQuery always has non-empty city
        val result = JobRowMapper.buildLocationList("", "")

        assertEquals(listOf(""), result)
    }

    private fun createTestJobRow(
        jobId: String = "test-job",
        jobIds: List<String> = listOf(jobId),
        applyUrls: List<String?> = emptyList(),
        platformLinks: List<String?> = emptyList(),
        locations: List<String> = listOf("Auckland"),
        title: String = "Developer",
        companyId: String = "test-company",
        companyName: String = "Test Corp",
        description: String? = null,
        employmentType: String? = null,
        jobFunction: String? = null,
        salaryMin: NormalizedSalary? = null,
        salaryMax: NormalizedSalary? = null,
        postedDate: String = "2026-03-05",
        technologies: List<String> = emptyList(),
        benefits: List<String> = emptyList(),
        city: String = "Auckland",
        stateRegion: String = "Auckland",
        seniorityLevel: String = "Mid-Level",
        source: String = "LinkedIn",
        lastSeenAt: Instant = Instant.now(),
        country: String? = "NZ",
        workModel: String? = null
    ): JobRow {
        return JobRow(
            jobId = jobId,
            jobIds = jobIds,
            applyUrls = applyUrls,
            platformLinks = platformLinks,
            locations = locations,
            title = title,
            companyId = companyId,
            companyName = companyName,
            description = description,
            employmentType = employmentType,
            jobFunction = jobFunction,
            salaryMin = salaryMin,
            salaryMax = salaryMax,
            postedDate = postedDate,
            technologies = technologies,
            benefits = benefits,
            city = city,
            stateRegion = stateRegion,
            seniorityLevel = seniorityLevel,
            source = source,
            lastSeenAt = lastSeenAt,
            country = country,
            workModel = workModel
        )
    }
}
