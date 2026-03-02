package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SilverDataMergerTest {

    private val merger = SilverDataMerger()

    @Test
    fun `mergeJobs aggregates list fields and updates lifecycle`() {
        val baseTime = Instant.parse("2023-01-01T00:00:00Z")
        val laterTime = Instant.parse("2023-01-15T00:00:00Z")

        val existing =
                createJobRecord(
                        jobId = "job1",
                        platformJobIds = listOf("p1"),
                        lastSeenAt = baseTime,
                        postedDate = LocalDate.parse("2022-12-25"),
                        technologies = listOf("Kotlin"),
                        locations = listOf("Sydney")
                )

        val new =
                createJobRecord(
                        jobId = "job1",
                        platformJobIds = listOf("p2"),
                        lastSeenAt = laterTime,
                        postedDate = LocalDate.parse("2022-12-26"),
                        technologies = listOf("Java"),
                        locations = listOf("Melbourne"),
                        description = "New Description"
                )

        val result = merger.mergeJobs(listOf(new), listOf(existing))

        assertEquals(1, result.size)
        val merged = result[0]
        assertEquals("job1", merged.jobId)
        assertEquals(listOf("p1", "p2"), merged.platformJobIds) // Union + Sorted
        assertEquals(listOf("Java", "Kotlin"), merged.technologies)
        assertEquals(listOf("Melbourne", "Sydney"), merged.locations)
        assertEquals(LocalDate.parse("2022-12-25"), merged.postedDate) // Earliest
        assertEquals(laterTime, merged.lastSeenAt) // Latest
        assertEquals("New Description", merged.description) // Latest wins for text
    }

    @Test
    fun `mergeCompanies updates metadata with latest non-blank values`() {
        val baseTime = Instant.parse("2023-01-01T00:00:00Z")
        val laterTime = Instant.parse("2023-01-15T00:00:00Z")

        val existing =
                createCompanyRecord(
                        companyId = "comp1",
                        name = "Old Name",
                        logoUrl = "http://old.logo",
                        description = "Old Description",
                        lastUpdatedAt = baseTime,
                        technologies = listOf("React")
                )

        val new =
                createCompanyRecord(
                        companyId = "comp1",
                        name = "New Name",
                        logoUrl = " ", // Blank should be ignored
                        description = "Updated Description",
                        lastUpdatedAt = laterTime,
                        technologies = listOf("Angular")
                )

        val result = merger.mergeCompanies(listOf(new), listOf(existing))

        assertEquals(1, result.size)
        val merged = result[0]
        assertEquals("New Name", merged.name)
        assertEquals("http://old.logo", merged.logoUrl) // Preserved because new was blank
        assertEquals("Updated Description", merged.description)
        assertEquals(listOf("Angular", "React"), merged.technologies) // Aggregated
        assertEquals(laterTime, merged.lastUpdatedAt)
    }

    private fun createJobRecord(
            jobId: String,
            platformJobIds: List<String> = emptyList(),
            lastSeenAt: Instant = Instant.now(),
            postedDate: LocalDate? = null,
            technologies: List<String> = emptyList(),
            locations: List<String> = emptyList(),
            description: String? = null
    ) =
            JobRecord(
                    jobId = jobId,
                    platformJobIds = platformJobIds,
                    applyUrls = emptyList(),
                    platformLinks = emptyList(),
                    locations = locations,
                    companyId = "comp1",
                    companyName = "Comp",
                    source = "LinkedIn",
                    country = "AU",
                    city = "Sydney",
                    stateRegion = "NSW",
                    title = "Dev",
                    seniorityLevel = "Senior",
                    technologies = technologies,
                    salaryMin = 100,
                    salaryMax = 200,
                    postedDate = postedDate,
                    benefits = emptyList(),
                    employmentType = "Full-time",
                    workModel = "Remote",
                    jobFunction = "Eng",
                    description = description,
                    lastSeenAt = lastSeenAt
            )

    private fun createCompanyRecord(
            companyId: String,
            name: String = "Company",
            logoUrl: String? = null,
            description: String? = null,
            lastUpdatedAt: Instant = Instant.now(),
            technologies: List<String> = emptyList()
    ) =
            CompanyRecord(
                    companyId = companyId,
                    name = name,
                    alternateNames = emptyList(),
                    logoUrl = logoUrl,
                    description = description,
                    website = "http://comp.com",
                    employeesCount = 100,
                    industries = "Tech",
                    technologies = technologies,
                    hiringLocations = emptyList(),
                    lastUpdatedAt = lastUpdatedAt
            )
}
