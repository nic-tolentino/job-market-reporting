package com.techmarket.sync

import com.techmarket.sync.ats.model.NormalizedJob
import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AtsJobDataMapperTest {

        private val parser = RawJobDataParser()
        private val mapper = AtsJobDataMapper(parser)

        @Test
        fun `should map normalized jobs to records successfully`() {
                val syncTime = Instant.parse("2023-11-01T12:00:00Z")
                val companyId = "test-company"

                val normalizedJobs =
                        listOf(
                                NormalizedJob(
                                        platformId = "gh-1",
                                        source = "Greenhouse",
                                        title = "Senior Kotlin Engineer",
                                        companyName = "Test Company",
                                        location = "Sydney, Australia, ",
                                        descriptionHtml = "We use Kotlin and Spring.",
                                        descriptionText = "We use Kotlin and Spring.",
                                        salaryMin = 120000,
                                        salaryMax = 180000,
                                        salaryCurrency = "AUD",
                                        employmentType = "Full-time",
                                        seniorityLevel = null,
                                        workModel = "Hybrid",
                                        department = "Engineering",
                                        postedAt = "2023-10-31T10:00:00Z",
                                        firstPublishedAt = null,
                                        applyUrl = "https://apply.com",
                                        platformUrl = "https://gh.com",
                                        rawPayload = "{}"
                                )
                        )

                val mappedData = mapper.map(normalizedJobs, companyId, syncTime)

                assertEquals(1, mappedData.jobs.size)
                assertEquals(1, mappedData.companies.size)

                val job = mappedData.jobs[0]
                assertEquals("test-company.au.senior-kotlin-engineer.2023-10-31", job.jobId)
                assertEquals("Senior", job.seniorityLevel)
                assertTrue(job.technologies.contains("Kotlin"))
                assertTrue(job.technologies.contains("Spring"))
                assertEquals("AU", job.country)
                assertEquals("Sydney", job.city)
                assertEquals("Full-time", job.employmentType)
                assertEquals("Hybrid", job.workModel)
                assertEquals("Engineering", job.jobFunction)

                val company = mappedData.companies[0]
                assertEquals("test-company", company.companyId)
                assertTrue(company.technologies.contains("Kotlin"))
        }

        @Test
        fun `should use firstPublishedAt for stable ID generation if available`() {
                val syncTime = Instant.parse("2023-11-01T12:00:00Z")
                val companyId = "test-company"

                val normalizedJobs =
                        listOf(
                                NormalizedJob(
                                        platformId = "gh-1",
                                        source = "Greenhouse",
                                        title = "Engineer",
                                        companyName = "Test Comp",
                                        location = "Auckland",
                                        descriptionHtml = "...",
                                        descriptionText = "...",
                                        salaryMin = null,
                                        salaryMax = null,
                                        salaryCurrency = null,
                                        employmentType = null,
                                        seniorityLevel = null,
                                        workModel = null,
                                        department = null,
                                        postedAt = "2023-10-31T10:00:00Z",
                                        firstPublishedAt = "2023-09-15T09:00:00Z",
                                        applyUrl = null,
                                        platformUrl = null,
                                        rawPayload = "{}"
                                )
                        )

                val job = mapper.map(normalizedJobs, companyId, syncTime).jobs[0]

                // Should use 2023-09-15 NOT 2023-10-31
                assertEquals("test-company.nz.engineer.2023-09-15", job.jobId)
        }
}
