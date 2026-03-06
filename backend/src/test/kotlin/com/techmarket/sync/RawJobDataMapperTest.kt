package com.techmarket.sync

import com.techmarket.persistence.model.VerificationLevel
import com.techmarket.sync.model.ApifyJobDto
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RawJobDataMapperTest {

        private val parser = mockk<RawJobDataParser>()
        private val classifier = mockk<TechRoleClassifier>()
        private val mapper = RawJobDataMapper(parser, classifier)
        private val now = Instant.now()

        init {
                every { classifier.isTechRole(any<ApifyJobDto>()) } returns true
        }

        @Test
        fun `filterValidJobs removes null or blank platform IDs`() {
                val valid = createApifyDto("valid", "2023-01-01")
                val nullId = createApifyDto(null, "2023-01-01")
                val blankId = createApifyDto(" ", "2023-01-01")

                val result =
                        mapper.filterValidJobs(
                                listOf(
                                        RawJob(valid, now),
                                        RawJob(nullId, now),
                                        RawJob(blankId, now)
                                )
                        )

                assertEquals(1, result.size)
                assertEquals("valid", result[0].dto.id)
        }

        @Test
        fun `groupByLogicalRole separates by company country and title`() {
                val role1 =
                        createApifyDto("1", "2023-01-01")
                                .copy(
                                        companyName = "Google",
                                        location = "Sydney",
                                        title = "Engineer"
                                )
                val role1Twin =
                        createApifyDto("2", "2023-01-01")
                                .copy(
                                        companyName = "Google",
                                        location = "Melbourne",
                                        title = "Engineer"
                                )
                val role2 =
                        createApifyDto("3", "2023-01-01")
                                .copy(
                                        companyName = "Atlassian",
                                        location = "Sydney",
                                        title = "Engineer"
                                )

                every { parser.determineCountry("Sydney") } returns "AU"
                every { parser.determineCountry("Melbourne") } returns "AU"

                val result =
                        mapper.groupByLogicalRole(
                                listOf(
                                        RawJob(role1, now),
                                        RawJob(role1Twin, now),
                                        RawJob(role2, now)
                                )
                        )

                assertEquals(2, result.size)
                assertEquals(2, result[Triple("ghost-google", "au", "engineer")]?.size)
        }

        @Test
        fun `groupByOpening handles lifecycle duration and 14-day buffer`() {
                // Mock sync time to fixed date: 2023-03-02
                val syncTime = Instant.parse("2023-03-02T00:00:00Z")

                // Group A:
                val job1OldStart = createApifyDto("1", "2023-01-01")
                val job2Middle = createApifyDto("2", "2023-02-01")
                val job3Boundary =
                        createApifyDto("3", "2023-03-16") // EXACTLY 14 days after March 2

                // Group B:
                val job4NewOpening =
                        createApifyDto("4", "2023-03-17") // 15 days after March 2 -> new opening

                every { parser.parseDate(any()) } returns LocalDate.MIN
                every { parser.parseDate("2023-01-01") } returns LocalDate.parse("2023-01-01")
                every { parser.parseDate("2023-02-01") } returns LocalDate.parse("2023-02-01")
                every { parser.parseDate("2023-03-16") } returns LocalDate.parse("2023-03-16")
                every { parser.parseDate("2023-03-17") } returns LocalDate.parse("2023-03-17")

                val result =
                        mapper.groupByOpening(
                                listOf(
                                        RawJob(job1OldStart, syncTime),
                                        RawJob(job2Middle, syncTime),
                                        RawJob(job3Boundary, syncTime),
                                        RawJob(job4NewOpening, syncTime)
                                )
                        )

                assertEquals(2, result.size)
                assertEquals(3, result[0].size) // 1, 2, 3 joined by duration and then buffer
                assertEquals(1, result[1].size) // 4 is too far (15 days)
        }

        @Test
        fun `assembleMappedData aggregates company locations and tech`() {
                val job1 =
                        createApifyDto("1", "2023-01-01")
                                .copy(companyName = "Google", location = "Sydney")
                val job2 =
                        createApifyDto("2", "2023-01-01")
                                .copy(companyName = "Google", location = "Melbourne")

                every { parser.parseLocation("Sydney") } returns Triple("Sydney", "NSW", "AU")
                every { parser.parseLocation("Melbourne") } returns Triple("Melbourne", "VIC", "AU")
                every { parser.determineCountry(any()) } returns "AU"
                every { parser.extractTechnologies(any()) } returns listOf("Kotlin", "Java")
                every { parser.extractSeniority(any(), any()) } returns "Senior"
                every { parser.parseDate(any()) } returns LocalDate.parse("2023-01-01")
                every { parser.parseSalary(any()) } returns null
                every { parser.extractWorkModel(any(), any()) } returns "Remote"

                val result =
                        mapper.assembleMappedData(
                                listOf(listOf(RawJob(job1, now), RawJob(job2, now)))
                        )

                assertEquals(1, result.companies.size)
                val google = result.companies[0]
                assertEquals(
                        listOf("Melbourne, VIC", "Sydney, NSW"),
                        google.hiringLocations.sorted()
                )
                assertEquals(listOf("Java", "Kotlin"), google.technologies.sorted())
        }

        @Test
        fun `findCompanyId correctly matches by name or alias and falls back to ghost`() {
            val manifest = mapOf(
                "google" to createCompanyRecord("google", "Google", listOf("Google Inc", "Alphabet")),
                "asb" to createCompanyRecord("asb-bank", "ASB Bank", listOf("ASB"))
            )
            
            // Exact ID/Name match
            assertEquals("google", mapper.findCompanyId("Google", manifest))
            
            // Alias match
            assertEquals("google", mapper.findCompanyId("Alphabet", manifest))
            assertEquals("asb-bank", mapper.findCompanyId("ASB", manifest))
            
            // Case insensitive
            assertEquals("google", mapper.findCompanyId("google inc", manifest))
            
            // Ghost fallback
            assertEquals("ghost-microsoft", mapper.findCompanyId("Microsoft", manifest))
        }

        @Test
        fun `map pipeline handles the full lifecycle flow correctly`() {
                val syncTime = Instant.parse("2023-01-10T00:00:00Z")
                val dto1 = createApifyDto("id1", "2023-01-01").copy(location = "Sydney")
                val dto2 = createApifyDto("id2", "2023-01-05").copy(location = "Melbourne")
                val dto3 = createApifyDto("id3", "2023-01-30") // 20 days later -> new

                every { parser.parseLocation(any()) } returns
                        Triple("DefaultCity", "DefaultState", "AU")
                every { parser.determineCountry(any()) } returns "AU"
                every { parser.extractSeniority(any(), any()) } returns "Senior"
                every { parser.extractTechnologies(any()) } returns listOf("Kotlin")
                every { parser.extractWorkModel(any(), any()) } returns "Remote"
                every { parser.parseSalary(any()) } returns null

                every { parser.parseDate(any()) } returns LocalDate.parse("2023-01-01")
                every { parser.parseDate("2023-01-01") } returns LocalDate.parse("2023-01-01")
                every { parser.parseDate("2023-01-05") } returns LocalDate.parse("2023-01-05")
                every { parser.parseDate("2023-01-30") } returns LocalDate.parse("2023-01-30")

                val result =
                        mapper.map(
                                listOf(
                                        RawJob(dto1, syncTime),
                                        RawJob(dto2, syncTime),
                                        RawJob(dto3, syncTime)
                                )
                        )

                assertEquals(2, result.jobs.size)
                assertEquals(1, result.companies.size)
                val google = result.companies[0]

                // CRITICAL: Ensure companyId consistency for BigQuery JOINs
                assertTrue(
                        result.jobs.all { it.companyId == google.companyId },
                        "Job companyIds must match CompanyRecord companyId"
                )
                assertFalse(
                        google.companyId.contains("company/"),
                        "CompanyId should not have redundant 'company/' prefix"
                )

                assertTrue(result.jobs.any { it.jobId.contains("2023-01-01") })
                assertTrue(result.jobs.any { it.jobId.contains("2023-01-30") })
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

        private fun createCompanyRecord(id: String, name: String, aliases: List<String>) =
                com.techmarket.persistence.model.CompanyRecord(
                        companyId = id,
                        name = name,
                        alternateNames = aliases,
                        logoUrl = null,
                        description = null,
                        website = null,
                        employeesCount = null,
                        industries = null,
                        technologies = emptyList(),
                        hiringLocations = emptyList(),
                        verificationLevel = VerificationLevel.VERIFIED,
                        lastUpdatedAt = Instant.now()
                )

        @Test
        fun `assembleMappedData filters out jobs from blocked companies`() {
            val blockedCompanyId = "spammer-corp"
            val manifestCompanies = mapOf(
                blockedCompanyId to createCompanyRecord(blockedCompanyId, "Spammer Corp", emptyList()).copy(
                    verificationLevel = VerificationLevel.BLOCKED
                )
            )

            val dto = createApifyDto("id1", "2023-01-01").copy(companyName = "Spammer Corp")
            val group = listOf(RawJob(dto, Instant.now()))

            every { parser.parseLocation(any()) } returns Triple("City", "State", "Country")
            every { parser.determineCountry(any()) } returns "US"
            every { parser.extractSeniority(any(), any()) } returns "Entry"
            every { parser.extractTechnologies(any()) } returns emptyList()
            every { parser.extractWorkModel(any(), any()) } returns "On-site"
            every { parser.parseSalary(any()) } returns null
            every { parser.parseDate(any()) } returns LocalDate.now()

            val result = mapper.assembleMappedData(listOf(group), manifestCompanies)

                assertEquals(0, result.jobs.size, "Blocked company jobs should be filtered out")
                assertEquals(0, result.companies.size, "Blocked company should not be returned in MappedSyncData")
        }

        // Helper to invoke private methods for testing matching logic
        private fun Any.invokePrivate(methodName: String, vararg args: Any?): Any? {
            val method = this.javaClass.getDeclaredMethod(methodName, *args.map { it?.javaClass ?: String::class.java }.toTypedArray())
            method.isAccessible = true
            return method.invoke(this, *args)
        }
}
