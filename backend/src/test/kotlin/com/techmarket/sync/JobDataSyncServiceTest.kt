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
import io.mockk.confirmVerified
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
    private val companySyncService = mockk<CompanySyncService>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private val service =
            JobDataSyncService(
                    apifyClient,
                    jobDataMapper,
                    jobRepository,
                    companyRepository,
                    ingestionRepository,
                    silverDataMerger,
                    companySyncService,
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
        every { jobDataMapper.map(any(), any()) } returns
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

    @Test
    fun `runDataSync aborts gracefully when Apify returns empty dataset`() {
        every { ingestionRepository.isDatasetIngested("empty-ds") } returns false
        every { apifyClient.fetchRecentJobs("empty-ds") } returns emptyList()

        service.runDataSync("empty-ds")

        // Should not attempt any persistence
        verify(exactly = 0) { ingestionRepository.saveRawIngestions(any()) }
        verify(exactly = 0) { jobRepository.saveJobs(any()) }
        verify(exactly = 0) { companyRepository.saveCompanies(any()) }
    }

    @Test
    fun `runDataSync skips when dataset already ingested`() {
        every { ingestionRepository.isDatasetIngested("duplicate-ds") } returns true

        service.runDataSync("duplicate-ds")

        // Should abort before fetching or saving
        verify { ingestionRepository.isDatasetIngested("duplicate-ds") }
        verify(exactly = 0) { apifyClient.fetchRecentJobs(any()) }
        verify(exactly = 0) { ingestionRepository.saveRawIngestions(any()) }
        confirmVerified(apifyClient, ingestionRepository)
    }

    @Test
    fun `runDataSync proceeds and saves datasetId when not already ingested`() {
        val syncTime = Instant.now()
        val apifyJob = createApifyDto("p1")
        
        every { ingestionRepository.isDatasetIngested("new-ds") } returns false
        every { apifyClient.fetchRecentJobs("new-ds") } returns listOf(ApifyJobResult(apifyJob, "{}"))
        every { jobDataMapper.map(any(), any()) } returns MappedSyncData(emptyList(), emptyList())

        service.runDataSync("new-ds")

        verify { ingestionRepository.isDatasetIngested("new-ds") }
        verify { apifyClient.fetchRecentJobs("new-ds") }
        verify { 
            ingestionRepository.saveRawIngestions(match { 
                it.all { record -> record.datasetId == "new-ds" } 
            }) 
        }
    }

    @Test
    fun `reprocessHistoricalData wipes Silver and re-inserts from Bronze`() {
        val syncTime = Instant.parse("2023-02-01T00:00:00Z")
        val apifyDto = createApifyDto("p1")
        val jsonPayload = objectMapper.writeValueAsString(apifyDto)

        val bronzeRecord =
                com.techmarket.persistence.model.RawIngestionRecord(
                        id = "bronze-1",
                        source = "LinkedIn-Apify",
                        ingestedAt = syncTime,
                        rawPayload = jsonPayload
                )

        // 1. Mock Bronze fetch
        every { ingestionRepository.getRawIngestions() } returns listOf(bronzeRecord)

        // 2. Mock Mapper (re-mapping from Bronze)
        val mappedJob = createJobRecord("job1", lastSeenAt = syncTime)
        val mappedCompany = createCompanyRecord("comp1", lastUpdatedAt = syncTime)
        every { jobDataMapper.map(any(), any()) } returns
                MappedSyncData(jobs = listOf(mappedJob), companies = listOf(mappedCompany))

        // Execute
        service.reprocessHistoricalData()

        // Verify: Silver tables wiped then re-populated
        verify(exactly = 1) { jobRepository.deleteAllJobs() }
        verify(exactly = 1) { companyRepository.deleteAllCompanies() }
        verify(exactly = 1) { companyRepository.saveCompanies(any()) }
        verify(exactly = 1) { jobRepository.saveJobs(any()) }
    }

    @Test
    fun `reprocessHistoricalData skips malformed Bronze records gracefully`() {
        val syncTime = Instant.parse("2023-02-01T00:00:00Z")
        val validDto = createApifyDto("p1")
        val validPayload = objectMapper.writeValueAsString(validDto)

        val validRecord =
                com.techmarket.persistence.model.RawIngestionRecord(
                        id = "valid-1",
                        source = "LinkedIn-Apify",
                        ingestedAt = syncTime,
                        rawPayload = validPayload
                )
        val malformedRecord =
                com.techmarket.persistence.model.RawIngestionRecord(
                        id = "bad-1",
                        source = "LinkedIn-Apify",
                        ingestedAt = syncTime,
                        rawPayload = "THIS IS NOT JSON"
                )

        every { ingestionRepository.getRawIngestions() } returns
                listOf(validRecord, malformedRecord)

        val mappedJob = createJobRecord("job1", lastSeenAt = syncTime)
        val mappedCompany = createCompanyRecord("comp1", lastUpdatedAt = syncTime)
        every { jobDataMapper.map(any(), any()) } returns
                MappedSyncData(jobs = listOf(mappedJob), companies = listOf(mappedCompany))

        // Should NOT throw — malformed records are skipped
        service.reprocessHistoricalData()

        verify(exactly = 1) { jobRepository.saveJobs(any()) }
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
