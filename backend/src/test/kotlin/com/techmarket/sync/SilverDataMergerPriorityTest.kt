package com.techmarket.sync

import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.model.JobRecord
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SilverDataMergerPriorityTest {

        private val merger = SilverDataMerger()

        @Test
        fun `ATS source should outrank LinkedIn-Apify source even if older`() {
                val jobId = "test-job-id"
                val syncTime = Instant.parse("2023-11-01T12:00:00Z")
                val olderSyncTime = Instant.parse("2023-11-01T10:00:00Z")

                val existingApifyJob =
                        JobRecord(
                                jobId = jobId,
                                companyId = "comp",
                                companyName = "Comp",
                                source = "LinkedIn-Apify",
                                title = "Apify Title",
                                description = "Apify Description",
                                lastSeenAt = syncTime, // Sync time is LATER but priority is LOWER
                                country = "AU",
                                city = "Sydney",
                                stateRegion = "",
                                locations = listOf("Sydney"),
                                platformJobIds = listOf("apify-1"),
                                applyUrls = listOf("apify-url"),
                                platformLinks = listOf("apify-link"),
                                technologies = listOf("Java"),
                                seniorityLevel = "Senior",
                                salaryMin = null,
                                salaryMax = null,
                                postedDate = null,
                                benefits = emptyList(),
                                employmentType = null,
                                workModel = "On-site",
                                jobFunction = null
                        )

                val newAtsJob =
                        JobRecord(
                                jobId = jobId,
                                companyId = "comp",
                                companyName = "Comp",
                                source = "Greenhouse",
                                title = "ATS Title",
                                description = "ATS Description",
                                lastSeenAt = olderSyncTime, // Sync time is EARLIER but priority is
                                // HIGHER
                                country = "AU",
                                city = "Sydney",
                                stateRegion = "",
                                locations = listOf("Sydney"),
                                platformJobIds = listOf("gh-1"),
                                applyUrls = listOf("gh-url"),
                                platformLinks = listOf("gh-link"),
                                technologies = listOf("Kotlin"),
                                seniorityLevel = "Senior",
                                salaryMin = NormalizedSalary(15000000L, "NZD", "YEAR", "JOB_POSTING"),
                                salaryMax = NormalizedSalary(20000000L, "NZD", "YEAR", "JOB_POSTING"),
                                postedDate = null,
                                benefits = emptyList(),
                                employmentType = null,
                                workModel = "Remote",
                                jobFunction = null
                        )

                val merged = merger.mergeJobs(listOf(newAtsJob), listOf(existingApifyJob))[0]

                // ATS data should win for metadata fields
                assertEquals("ATS Title", merged.title)
                assertEquals("ATS Description", merged.description)
                assertEquals("Remote", merged.workModel)
                assertEquals(15000000L, merged.salaryMin?.amount)

                // List aggregators should still be unions
                assertTrue(merged.technologies.contains("Java"))
                assertTrue(merged.technologies.contains("Kotlin"))
                assertTrue(merged.platformJobIds.contains("apify-1"))
                assertTrue(merged.platformJobIds.contains("gh-1"))

                // lastSeenAt should be the max
                assertEquals(syncTime, merged.lastSeenAt)
        }
}
