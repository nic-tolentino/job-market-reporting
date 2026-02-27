package com.techmarket.sync

import com.techmarket.sync.model.ApifyJobDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JobDataMapperTest {

    private val parser = JobDataParser()
    private val mapper = JobDataMapper(parser)

    @Test
    fun `mapSyncData deduplicates jobs with same title, company and seniority`() {
        val rawData =
                listOf(
                        ApifyJobDto(
                                id = "job-1",
                                title = "Senior Backend Engineer",
                                companyName = "Tech Corp",
                                location = "Sydney, NSW",
                                applyUrl = "url-1",
                                descriptionText = "We use Java and Spring Boot.",
                                postedAt = "2024-01-01",
                                seniorityLevel = "Senior",
                                employmentType = "Full-time",
                                benefits = emptyList(),
                                jobFunction = "Engineering",
                                companyLogo = null,
                                companyDescription = null,
                                companyWebsite = null,
                                companyEmployeesCount = null,
                                industries = null,
                                salaryInfo = null,
                                descriptionHtml = null,
                                applicantsCount = null,
                                jobPosterName = null
                        ),
                        ApifyJobDto(
                                id = "job-2",
                                title = "Senior Backend Engineer",
                                companyName = "Tech Corp",
                                location = "Melbourne, VIC",
                                applyUrl = "url-2",
                                descriptionText = "We use Kotlin and AWS.",
                                postedAt = "2024-01-02",
                                seniorityLevel = "Senior",
                                employmentType = "Full-time",
                                benefits = emptyList(),
                                jobFunction = "Engineering",
                                companyLogo = null,
                                companyDescription = null,
                                companyWebsite = null,
                                companyEmployeesCount = null,
                                industries = null,
                                salaryInfo = null,
                                descriptionHtml = null,
                                applicantsCount = null,
                                jobPosterName = null
                        ),
                        ApifyJobDto(
                                id = "job-3",
                                title = "Junior Dev",
                                companyName = "Tech Corp",
                                location = "Sydney, NSW",
                                applyUrl = "url-3",
                                descriptionText = "Welcome to Tech Corp",
                                postedAt = "2024-01-03",
                                seniorityLevel = "Junior",
                                employmentType = "Full-time",
                                benefits = emptyList(),
                                jobFunction = "Engineering",
                                companyLogo = null,
                                companyDescription = null,
                                companyWebsite = null,
                                companyEmployeesCount = null,
                                industries = null,
                                salaryInfo = null,
                                descriptionHtml = null,
                                applicantsCount = null,
                                jobPosterName = null
                        )
                )

        val result = mapper.mapSyncData(rawData)

        // Only 1 company should be created
        assertEquals(1, result.companies.size)
        // Two unique roles: the duplicate 'Senior Backend Engineer' is merged, and the 'Junior Dev'
        // is new.
        assertEquals(2, result.jobs.size)

        val seniorJob = result.jobs.find { it.title == "Senior Backend Engineer" }!!
        assertEquals(2, seniorJob.locations.size) // Sydney and Melbourne
        assertEquals(listOf("job-1", "job-2"), seniorJob.jobIds)
        assertEquals(listOf("url-1", "url-2"), seniorJob.applyUrls)

        // Technologies should be unified across duplicates!
        assertEquals(
                listOf("aws", "java", "kotlin", "spring", "spring boot").sorted(),
                seniorJob.technologies.sorted()
        )
    }
}
