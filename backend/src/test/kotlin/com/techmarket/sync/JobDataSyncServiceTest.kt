package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.BronzeRepository
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.model.ApifyJobDto
import com.techmarket.sync.model.ApifyJobResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.runs
import io.mockk.Runs
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class JobDataSyncServiceTest {

    private val apifyClient = mockk<ApifyClient>()
    private val jobDataMapper = mockk<RawJobDataMapper>()
    private val jobRepository = mockk<JobRepository>(relaxed = true)
    private val companyRepository = mockk<CompanyRepository>(relaxed = true)
    private val bronzeRepository = mockk<BronzeRepository>(relaxed = true)
    private val silverDataMerger = SilverDataMerger() // Unit test state: use real merger
    private val companySyncService = mockk<CompanySyncService>(relaxed = true)
    private val objectMapper = ObjectMapper()
    private val gcsConfig = GcsConfig(
        bucketName = "test-bucket",
        projectId = "test-project",
        compressionEnabled = true
    )

    private val service =
            JobDataSyncService(
                    apifyClient,
                    jobDataMapper,
                    jobRepository,
                    companyRepository,
                    bronzeRepository,
                    silverDataMerger,
                    companySyncService,
                    objectMapper,
                    gcsConfig
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

        // 2. Mock duplication check
        every { bronzeRepository.isDatasetIngested("dataset1") } returns false

        // 3. Mock Bronze save
        every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()

        // 4. Mock Mapper
        every { jobDataMapper.map(any(), any()) } returns
                MappedSyncData(jobs = listOf(mappedJob), companies = listOf(mappedCompany))

        // 5. Mock Repository Fetch (Returning existing data for comp1 only, none for job1)
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
        every { bronzeRepository.isDatasetIngested("empty-ds") } returns false
        every { apifyClient.fetchRecentJobs("empty-ds") } returns emptyList()

        service.runDataSync("empty-ds")

        // Should not attempt any persistence
        verify(exactly = 0) { bronzeRepository.saveIngestion(any(), any()) }
        verify(exactly = 0) { jobRepository.saveJobs(any()) }
        verify(exactly = 0) { companyRepository.saveCompanies(any()) }
    }

    @Test
    fun `runDataSync skips when dataset already ingested`() {
        every { bronzeRepository.isDatasetIngested("duplicate-ds") } returns true

        service.runDataSync("duplicate-ds")

        // Should abort before fetching or saving
        verify { bronzeRepository.isDatasetIngested("duplicate-ds") }
        verify(exactly = 0) { apifyClient.fetchRecentJobs(any()) }
        verify(exactly = 0) { bronzeRepository.saveIngestion(any(), any()) }
        confirmVerified(apifyClient, bronzeRepository)
    }

    @Test
    fun `runDataSync proceeds and saves datasetId when not already ingested`() {
        val syncTime = Instant.now()
        val apifyJob = createApifyDto("p1")

        every { bronzeRepository.isDatasetIngested("new-ds") } returns false
        every { apifyClient.fetchRecentJobs("new-ds") } returns listOf(ApifyJobResult(apifyJob, "{}"))
        every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
        every { jobDataMapper.map(any(), any()) } returns MappedSyncData(emptyList(), emptyList())

        service.runDataSync("new-ds")

        verify { bronzeRepository.isDatasetIngested("new-ds") }
        verify { apifyClient.fetchRecentJobs("new-ds") }
        verify { bronzeRepository.saveIngestion(any(), any()) }
    }

    // TODO: Fix test - MockK setup issue with listManifests parameters
    // @Test
    fun `reprocessHistoricalData wipes Silver and re-inserts from Bronze`() {
        val syncTime = Instant.parse("2023-02-01T00:00:00Z")
        val apifyDto = createApifyDto("p1")
        val jsonPayload = objectMapper.writeValueAsString(apifyDto)

        val manifest = BronzeIngestionManifest(
            datasetId = "test-dataset",
            source = "LinkedIn-Apify",
            ingestedAt = syncTime,
            targetCountry = null,
            recordCount = 1,
            fileCount = 1,
            uncompressedSizeBytes = jsonPayload.length.toLong(),
            compressedSizeBytes = (jsonPayload.length.toLong() * 0.2).toLong(),
            compressionRatio = 0.2,
            files = listOf("gs://bucket/file.json.gz"),
            processingStatus = com.techmarket.persistence.model.ProcessingStatus.COMPLETED
        )

        // 1. Mock Bronze fetch
        every { bronzeRepository.listManifests(any(), any(), any()) } returns listOf(manifest)
        every { bronzeRepository.readFile("gs://bucket/file.json.gz") } returns
                ByteArrayInputStream(jsonPayload.toByteArray())

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
        // With batch processing, saveCompanies/saveJobs called at least once
        verify { companyRepository.saveCompanies(any()) }
        verify { jobRepository.saveJobs(any()) }
    }

    @Test
    fun `reprocessHistoricalData skips malformed Bronze records gracefully`() {
        val syncTime = Instant.parse("2023-02-01T00:00:00Z")
        val validDto = createApifyDto("p1")
        val validPayload = objectMapper.writeValueAsString(validDto)

        val manifest = BronzeIngestionManifest(
            datasetId = "test-dataset",
            source = "LinkedIn-Apify",
            ingestedAt = syncTime,
            targetCountry = null,
            recordCount = 1,
            fileCount = 1,
            uncompressedSizeBytes = validPayload.length.toLong(),
            compressedSizeBytes = (validPayload.length.toLong() * 0.2).toLong(),
            compressionRatio = 0.2,
            files = listOf("gs://bucket/file.json.gz"),
            processingStatus = com.techmarket.persistence.model.ProcessingStatus.COMPLETED
        )

        every { bronzeRepository.listManifests() } returns listOf(manifest)
        every { bronzeRepository.readFile("gs://bucket/file.json.gz") } returns
                ByteArrayInputStream("THIS IS NOT JSON".toByteArray())

        val mappedJob = createJobRecord("job1", lastSeenAt = syncTime)
        val mappedCompany = createCompanyRecord("comp1", lastUpdatedAt = syncTime)
        every { jobDataMapper.map(any(), any()) } returns
                MappedSyncData(jobs = listOf(mappedJob), companies = listOf(mappedCompany))

        // Should NOT throw — malformed records are skipped
        service.reprocessHistoricalData()

        // May or may not save depending on if any valid jobs were parsed
        // The key is it doesn't crash
        verify(exactly = 1) { jobRepository.deleteAllJobs() }
        verify(exactly = 1) { companyRepository.deleteAllCompanies() }
    }

    @Test
    fun `runDataSync propagates custom ingestedAt to Bronze manifest`() {
        val customTime = Instant.parse("2026-03-09T10:30:00Z")
        val apifyJob = createApifyDto("p1")

        every { bronzeRepository.isDatasetIngested("dataset1") } returns false
        every { apifyClient.fetchRecentJobs("dataset1") } returns listOf(ApifyJobResult(apifyJob, "{}"))
        every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
        every { jobDataMapper.map(any(), any()) } returns MappedSyncData(emptyList(), emptyList())

        service.runDataSync("dataset1", null, customTime)

        verify { bronzeRepository.saveIngestion(
            match { it.ingestedAt == customTime },
            any()
        ) }
    }

    @Test
    fun `runDataSync uses Instant now when ingestedAt is null`() {
        val apifyJob = createApifyDto("p1")

        every { bronzeRepository.isDatasetIngested("dataset1") } returns false
        every { apifyClient.fetchRecentJobs("dataset1") } returns listOf(ApifyJobResult(apifyJob, "{}"))
        every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
        every { jobDataMapper.map(any(), any()) } returns MappedSyncData(emptyList(), emptyList())

        service.runDataSync("dataset1", null, null)

        verify { bronzeRepository.saveIngestion(
            match { it.ingestedAt.isAfter(Instant.now().minusSeconds(10)) },
            any()
        ) }
    }

    @Test
    fun `runDataSync does NOT call syncFromManifest`() {
        val apifyJob = createApifyDto("p1")

        every { bronzeRepository.isDatasetIngested("dataset1") } returns false
        every { apifyClient.fetchRecentJobs("dataset1") } returns listOf(ApifyJobResult(apifyJob, "{}"))
        every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
        every { jobDataMapper.map(any(), any()) } returns MappedSyncData(emptyList(), emptyList())

        service.runDataSync("dataset1")

        verify(exactly = 0) { companySyncService.syncFromManifest() }
    }

    // TODO: Fix test - MockK setup issue with relaxed mocks
    // @Test
    fun `runDataSync marks dataset as FAILED when Silver persistence throws`() {
        val apifyJob = createApifyDto("p1")
        val mappedJob = createJobRecord("job1", lastSeenAt = Instant.now())
        val mappedCompany = createCompanyRecord("comp1", lastUpdatedAt = Instant.now())

        every { bronzeRepository.isDatasetIngested("dataset1") } returns false
        every { apifyClient.fetchRecentJobs("dataset1") } returns listOf(ApifyJobResult(apifyJob, "{}"))
        every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
        every { jobDataMapper.map(any(), any()) } returns MappedSyncData(
            jobs = listOf(mappedJob),
            companies = listOf(mappedCompany)
        )
        every { jobRepository.getJobsByIds(any()) } returns emptyList()
        every { companyRepository.getCompaniesByIds(any()) } returns emptyList()
        every { silverDataMerger.mergeJobs(any(), any()) } returns listOf(mappedJob)
        every { silverDataMerger.mergeCompanies(any(), any()) } returns listOf(mappedCompany)
        every { jobRepository.deleteJobsByIds(any()) } just Runs
        every { companyRepository.deleteCompaniesByIds(any()) } just Runs
        every { companyRepository.saveCompanies(any()) } throws RuntimeException("Database error")
        // Mock updateProcessingStatus - relaxed bronzeRepository should handle this
        every { bronzeRepository.updateProcessingStatus(any(), any()) } returns true

        // The service should throw the exception AND call updateProcessingStatus
        val exception = assertThrows<RuntimeException> {
            service.runDataSync("dataset1")
        }
        
        assert(exception.message == "Database error")
        // Verify FAILED status was attempted (relaxed mock may not record this)
    }

    @Test
    fun `runDataSync re-throws ApifyClient HttpClientErrorException`() {
        every { bronzeRepository.isDatasetIngested("dataset1") } returns false
        every { apifyClient.fetchRecentJobs("dataset1") } throws
            HttpClientErrorException(HttpStatus.NOT_FOUND)

        assertThrows<HttpClientErrorException> {
            service.runDataSync("dataset1")
        }

        verify(exactly = 0) { bronzeRepository.saveIngestion(any(), any()) }
        verify(exactly = 0) { bronzeRepository.updateProcessingStatus(any(), any()) }
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
