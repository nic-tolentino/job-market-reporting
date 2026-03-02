package com.techmarket.sync

import com.techmarket.sync.model.ApifyJobDto
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RawJobDataMapperTest {

        private val parser = mockk<RawJobDataParser>()
        private val mapper = RawJobDataMapper(parser)

        @Test
        fun `map groups by company and title and aggregates metadata`() {
                val now = Instant.now()

                // Two postings for the same role in different locations
                val dto1 = createApifyDto("id1", "2023-01-01").copy(location = "Sydney")
                val dto2 = createApifyDto("id2", "2023-01-01").copy(location = "Melbourne")

                // A different role at the same company
                val dto3 =
                        createApifyDto("id3", "2023-01-01")
                                .copy(title = "Manager", location = "Brisbane")

                // MockK: Define general any() first, then specific overrides
                every { parser.parseLocation(any()) } returns
                        Triple("DefaultCity", "DefaultState", "DefaultCountry")
                every { parser.parseLocation("Sydney") } returns Triple("Sydney", "NSW", "AU")
                every { parser.parseLocation("Melbourne") } returns Triple("Melbourne", "VIC", "AU")
                every { parser.parseLocation("Brisbane") } returns Triple("Brisbane", "QLD", "AU")

                every { parser.determineCountry(any()) } returns "AU"
                every { parser.extractSeniority(any(), any()) } returns "Senior"
                every { parser.extractTechnologies(any()) } returns listOf("Kotlin")
                every { parser.extractWorkModel(any(), any()) } returns "Remote"
                every { parser.parseSalary(any()) } returns null
                every { parser.parseDate(any()) } returns LocalDate.parse("2023-01-01")

                val syncedJobs = listOf(RawJob(dto1, now), RawJob(dto2, now), RawJob(dto3, now))

                val result = mapper.map(syncedJobs)

                // Should result in 2 unique roles (Engineer and Manager)
                assertEquals(2, result.jobs.size)

                val engineerJob = result.jobs.find { it.title == "Title" }!!
                assertEquals(listOf("id1", "id2"), engineerJob.jobIds.sorted())
                assertEquals(
                        listOf("Melbourne, VIC", "Sydney, NSW"),
                        engineerJob.locations.sorted()
                )

                val managerJob = result.jobs.find { it.title == "Manager" }!!
                assertEquals(listOf("id3"), managerJob.jobIds)
                assertEquals(listOf("Brisbane, QLD"), managerJob.locations.sorted())

                // Company should aggregate all locations and techs from its roles
                assertEquals(1, result.companies.size)
                val company = result.companies[0]
                assertEquals(
                        listOf("Brisbane, QLD", "Melbourne, VIC", "Sydney, NSW"),
                        company.hiringLocations.sorted()
                )
        }

        @Test
        fun `map filters out jobs with null or blank IDs`() {
                val now = Instant.now()
                val validDto = createApifyDto("valid-id", "2023-01-01")
                val nullIdDto = createApifyDto("id-ignored", "2023-01-01").copy(id = null)
                val blankIdDto = createApifyDto("id-ignored", "2023-01-01").copy(id = "  ")

                every { parser.parseLocation(any()) } returns Triple("Unknown", "Unknown", "AU")
                every { parser.determineCountry(any()) } returns "AU"
                every { parser.extractSeniority(any(), any()) } returns "Senior"
                every { parser.extractTechnologies(any()) } returns emptyList()
                every { parser.extractWorkModel(any(), any()) } returns "Remote"
                every { parser.parseSalary(any()) } returns null
                every { parser.parseDate(any()) } returns null

                val syncedJobs =
                        listOf(
                                RawJob(validDto, now),
                                RawJob(nullIdDto, now),
                                RawJob(blankIdDto, now)
                        )

                val result = mapper.map(syncedJobs)

                assertEquals(1, result.jobs.size)
                assertEquals("valid-id", result.jobs[0].jobIds[0])
        }

        private fun createApifyDto(id: String?, postedAt: String): ApifyJobDto {
                return ApifyJobDto(
                        id = id,
                        title = "Title",
                        companyName = "Company",
                        companyLogo = "http://logo.com",
                        location = "Location",
                        salaryInfo = null,
                        postedAt = postedAt,
                        benefits = listOf("Free coffee"),
                        applicantsCount = null,
                        applyUrl = "http://apply.com/$id",
                        descriptionHtml = null,
                        descriptionText = "Description",
                        link = "http://link.com/$id",
                        seniorityLevel = null,
                        employmentType = "Full-time",
                        jobFunction = "Engineering",
                        industries = "Software",
                        companyDescription = "A great company",
                        companyWebsite = "http://company.com",
                        companyEmployeesCount = 100
                )
        }
}
