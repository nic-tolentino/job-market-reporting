package com.techmarket.sync

import com.techmarket.model.NormalizedSalary
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

        @Test
        fun `mergeCompanies preserves curated fields from existing if new is default`() {
                val existing = createCompanyRecord("c1").copy(isAgency = true, hqCountry = "NZ")
                val new = createCompanyRecord("c1").copy(isAgency = false, hqCountry = null)

                val result = merger.mergeCompanies(listOf(new), listOf(existing))
                
                assertEquals(true, result[0].isAgency)
                assertEquals("NZ", result[0].hqCountry)
        }

        @Test
        fun `mergeJobs handles null salary fields gracefully`() {
                val t = Instant.parse("2023-01-01T00:00:00Z")

                val existing =
                        createJobRecord(jobId = "j1", lastSeenAt = t)
                                .copy(salaryMin = null, salaryMax = null)
                val new =
                        createJobRecord(jobId = "j1", lastSeenAt = t.plusSeconds(1))
                                .copy(salaryMin = NormalizedSalary(8000000L, "NZD", "YEAR", "JOB_POSTING"), salaryMax = NormalizedSalary(12000000L, "NZD", "YEAR", "JOB_POSTING"))

                val result = merger.mergeJobs(listOf(new), listOf(existing))
                assertEquals(8000000L, result[0].salaryMin?.amount)
                assertEquals(12000000L, result[0].salaryMax?.amount)
        }

        @Test
        fun `mergeJobs picks earliest salary min and latest salary max`() {
                val t = Instant.parse("2023-01-01T00:00:00Z")

                val existing =
                        createJobRecord(jobId = "j1", lastSeenAt = t)
                                .copy(salaryMin = NormalizedSalary(9000000L, "NZD", "YEAR", "JOB_POSTING"), salaryMax = NormalizedSalary(13000000L, "NZD", "YEAR", "JOB_POSTING"))
                val new =
                        createJobRecord(jobId = "j1", lastSeenAt = t.plusSeconds(1))
                                .copy(salaryMin = NormalizedSalary(8000000L, "NZD", "YEAR", "JOB_POSTING"), salaryMax = NormalizedSalary(12000000L, "NZD", "YEAR", "JOB_POSTING"))

                val result = merger.mergeJobs(listOf(new), listOf(existing))
                assertEquals(8000000L, result[0].salaryMin?.amount) // min of (9000000, 8000000)
                assertEquals(13000000L, result[0].salaryMax?.amount) // max of (13000000, 12000000)
        }

        @Test
        fun `mergeJobs handles identical timestamps without error`() {
                val t = Instant.parse("2023-01-01T00:00:00Z")

                val existing = createJobRecord(jobId = "j1", lastSeenAt = t, description = "Old")
                val new = createJobRecord(jobId = "j1", lastSeenAt = t, description = "New")

                val result = merger.mergeJobs(listOf(new), listOf(existing))
                assertEquals(1, result.size)
                assertEquals(t, result[0].lastSeenAt)
        }

        @Test
        fun `mergeJobs returns new record unchanged when no existing match`() {
                val t = Instant.parse("2023-01-01T00:00:00Z")
                val new =
                        createJobRecord(jobId = "brand-new", lastSeenAt = t, description = "Fresh")

                val result = merger.mergeJobs(listOf(new), emptyList())
                assertEquals(1, result.size)
                assertEquals("brand-new", result[0].jobId)
                assertEquals("Fresh", result[0].description)
        }

        private fun createJobRecord(
                jobId: String,
                platformJobIds: List<String> = emptyList(),
                lastSeenAt: Instant = Instant.now(),
                postedDate: LocalDate? = null,
                technologies: List<String> = emptyList(),
                locations: List<String> = emptyList(),
                description: String? = null,
                salaryMin: NormalizedSalary? = NormalizedSalary(10000L, "NZD", "YEAR", "JOB_POSTING"),
                salaryMax: NormalizedSalary? = NormalizedSalary(20000L, "NZD", "YEAR", "JOB_POSTING")
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
                        salaryMin = salaryMin,
                        salaryMax = salaryMax,
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
