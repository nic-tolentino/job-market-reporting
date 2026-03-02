package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.IngestionRepository
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.ApifyJobDto
import com.techmarket.sync.model.ApifyJobResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Test

class JobDataSyncServiceTest {

    private val apifyClient = mockk<ApifyClient>()
    private val jobDataMapper = mockk<RawJobDataMapper>()
    private val jobRepository = mockk<JobRepository>(relaxed = true)
    private val companyRepository = mockk<CompanyRepository>(relaxed = true)
    private val ingestionRepository = mockk<IngestionRepository>(relaxed = true)
    private val silverDataMerger = SilverDataMerger() // Unit test state: use real merger
    private val objectMapper = ObjectMapper()

    private val service =
            JobDataSyncService(
                    apifyClient,
                    jobDataMapper,
                    jobRepository,
                    companyRepository,
                    ingestionRepository,
                    silverDataMerger,
                    objectMapper
            )

    @Test
    fun `runDataSync performs Fetch-Merge-Delete-Save cycle`() {
        val syncTime = Instant.now()
        val apifyJob = createApifyDto("p1")
        val mappedJob = createJobRecord("job1", lastSeenAt = syncTime)
        val mappedCompany = createCompanyRecord("comp1", lastUpdatedAt = syncTime)

        // 1. Mock Fetch
        every { apifyClient.fetchRecentJobs("dataset1") } returns
                listOf(ApifyJobResult(apifyJob, "{}"))

        // 2. Mock Mapper
        every { jobDataMapper.map(any()) } returns
                MappedSyncData(jobs = listOf(mappedJob), companies = listOf(mappedCompany))

        // 3. Mock Repository Fetch (Returning existing data for comp1 only, none for job1)
        every { jobRepository.getJobsByIds(listOf("job1")) } returns emptyList()
        val existingCompany =
                createCompanyRecord(
                        "comp1",
                        name = "Old Name",
                        lastUpdatedAt = syncTime.minusSeconds(100)
                )
        every { companyRepository.getCompaniesByIds(listOf("comp1")) } returns
                listOf(existingCompany)

        // Execute
        service.runDataSync("dataset1")

        // 4. Verify Merge and Save logic
        verify { jobRepository.getJobsByIds(listOf("job1")) }
        verify { companyRepository.getCompaniesByIds(listOf("comp1")) }

        // Targeted deletion
        verify { jobRepository.deleteJobsByIds(listOf("job1")) }
        verify { companyRepository.deleteCompaniesByIds(listOf("comp1")) }

        // Final saves
        verify { jobRepository.saveJobs(any()) }
        verify {
            companyRepository.saveCompanies(match { it.first().name == "Comp" })
        } // "Comp" is from createJobRecord default
    }

    private fun createApifyDto(id: String) =
            ApifyJobDto(
                    id = id,
                    title = "Dev",
                    companyName = "Comp",
                    companyLogo = null,
                    location = "Sydney",
                    salaryInfo = null,
                    postedAt = "2023-01-01",
                    benefits = emptyList(),
                    applicantsCount = null,
                    applyUrl = "http://apply",
                    descriptionHtml = null,
                    descriptionText = "Desc",
                    link = "http://link",
                    seniorityLevel = null,
                    employmentType = null,
                    jobFunction = null,
                    industries = null,
                    companyDescription = null,
                    companyWebsite = null,
                    companyEmployeesCount = null
            )

    private fun createJobRecord(jobId: String, lastSeenAt: Instant) =
            JobRecord(
                    jobId = jobId,
                    platformJobIds = listOf("p1"),
                    applyUrls = emptyList(),
                    platformLinks = emptyList(),
                    locations = emptyList(),
                    companyId = "comp1",
                    companyName = "Comp",
                    source = "LinkedIn",
                    country = "AU",
                    city = "Sydney",
                    stateRegion = "NSW",
                    title = "Dev",
                    seniorityLevel = "Senior",
                    technologies = emptyList(),
                    salaryMin = null,
                    salaryMax = null,
                    postedDate = LocalDate.now(),
                    benefits = emptyList(),
                    employmentType = null,
                    workModel = null,
                    jobFunction = null,
                    description = "Desc",
                    lastSeenAt = lastSeenAt
            )

    private fun createCompanyRecord(
            companyId: String,
            name: String = "Comp",
            lastUpdatedAt: Instant
    ) =
            CompanyRecord(
                    companyId = companyId,
                    name = name,
                    alternateNames = emptyList(),
                    logoUrl = null,
                    description = null,
                    website = null,
                    employeesCount = null,
                    industries = null,
                    technologies = emptyList(),
                    hiringLocations = emptyList(),
                    lastUpdatedAt = lastUpdatedAt
            )
}
