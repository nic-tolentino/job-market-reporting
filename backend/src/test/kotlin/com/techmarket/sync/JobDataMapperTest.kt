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
                                        link = "link-1"
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
                                        link = "link-2"
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
                                        link = "link-3"
                                )
                        )

                val result = mapper.mapSyncData(rawData)

                // Only 1 company should be created
                assertEquals(1, result.companies.size)
                // Two unique roles: the duplicate 'Senior Backend Engineer' is merged, and the
                // 'Junior Dev'
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

        @Test
        fun `sanitize redacts emails and phone numbers`() {
                val rawData =
                        listOf(
                                ApifyJobDto(
                                        id = "job-sanitized",
                                        title = "Engineer",
                                        companyName = "Privacy Corp",
                                        location = "Sydney",
                                        descriptionText =
                                                "Contact me at recruiter@example.com or call +61 2 9234 5678 or 0412 345 678",
                                        companyDescription = "Email us at info@privacy.com",
                                        postedAt = "2024-01-01",
                                        seniorityLevel = "Mid-Level",
                                        salaryInfo = null,
                                        descriptionHtml = null,
                                        companyLogo = null,
                                        benefits = null,
                                        applicantsCount = null,
                                        applyUrl = null,
                                        link = null,
                                        employmentType = null,
                                        jobFunction = null,
                                        industries = null,
                                        companyWebsite = null,
                                        companyEmployeesCount = null
                                )
                        )

                val result = mapper.mapSyncData(rawData)
                val job = result.jobs.first()
                val company = result.companies.first()

                assertEquals(
                        "Contact me at [REDACTED EMAIL] or call [REDACTED PHONE] or [REDACTED PHONE]",
                        job.description
                )
                assertEquals("Email us at [REDACTED EMAIL]", company.description)
        }
}
