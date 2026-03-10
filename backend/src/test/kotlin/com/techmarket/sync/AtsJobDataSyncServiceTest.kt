package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.BronzeRepository
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.CompanyAtsConfig
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.ats.*
import com.techmarket.sync.ats.model.NormalizedJob
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtsJobDataSyncServiceTest {

        @MockK lateinit var atsConfigRepository: AtsConfigRepository
        @MockK lateinit var clientFactory: AtsClientFactory
        @MockK lateinit var normalizerFactory: AtsNormalizerFactory
        @MockK lateinit var bronzeRepository: BronzeRepository
        @MockK lateinit var mapper: AtsJobDataMapper
        @MockK lateinit var merger: SilverDataMerger
        @MockK lateinit var jobRepository: JobRepository
        @MockK lateinit var companyRepository: CompanyRepository
        @MockK lateinit var classifier: TechRoleClassifier
        private val objectMapper = ObjectMapper()
        private val gcsConfig = GcsConfig(
                bucketName = "test-bucket",
                projectId = "test-project",
                compressionEnabled = true
        )

        private lateinit var service: AtsJobDataSyncService

        @BeforeEach
        fun setup() {
                MockKAnnotations.init(this)
                service =
                        AtsJobDataSyncService(
                                atsConfigRepository,
                                clientFactory,
                                normalizerFactory,
                                bronzeRepository,
                                mapper,
                                merger,
                                jobRepository,
                                companyRepository,
                                objectMapper,
                                classifier,
                                gcsConfig
                        )
                every { classifier.isTechRole(any<NormalizedJob>()) } returns true
        }

        @Test
        fun `should perform full sync successfully`() {
                val companyId = "test-comp"
                val config =
                        CompanyAtsConfig(
                                companyId,
                                AtsProvider.GREENHOUSE,
                                "board-1",
                                true,
                                null,
                                SyncStatus.SUCCESS
                        )
                val rawPayload = """{"jobs": []}"""
                val client = mockk<AtsClient>()
                val normalizer = mockk<AtsNormalizer>()
                val normalizedJob =
                        NormalizedJob(
                                platformId = "1",
                                source = "Greenhouse",
                                title = "Title",
                                companyName = "Company",
                                location = null,
                                descriptionHtml = null,
                                descriptionText = null,
                                salaryMin = null,
                                salaryMax = null,
                                salaryCurrency = null,
                                employmentType = null,
                                seniorityLevel = null,
                                workModel = null,
                                department = null,
                                postedAt = null,
                                firstPublishedAt = null,
                                applyUrl = null,
                                platformUrl = null,
                                rawPayload = "{}"
                        )

                // Use real records or relaxed mocks to avoid "no answer found" on property access
                val jobRecord = mockk<JobRecord>(relaxed = true)
                every { jobRecord.jobId } returns "job-1"
                val companyRecord = mockk<CompanyRecord>(relaxed = true)
                every { companyRecord.companyId } returns companyId

                val mappedData = MappedSyncData(listOf(companyRecord), listOf(jobRecord))

                every { atsConfigRepository.getConfig(companyId) } returns config
                every { bronzeRepository.isDatasetIngested(any()) } returns false
                every { clientFactory.getClient(AtsProvider.GREENHOUSE) } returns client
                every { client.fetchJobs("board-1") } returns rawPayload
                every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
                every { bronzeRepository.updateProcessingStatus(any(), any()) } returns true
                every { normalizerFactory.getNormalizer(AtsProvider.GREENHOUSE) } returns normalizer
                every { normalizer.normalize(any()) } returns listOf(normalizedJob)
                every { companyRepository.getCompaniesByIds(any()) } returns emptyList()
                every { mapper.map(any(), any(), any()) } returns mappedData
                every { jobRepository.getJobsByIds(any()) } returns emptyList()
                every { merger.mergeJobs(any(), any()) } returns mappedData.jobs
                every { merger.mergeCompanies(any(), any()) } returns mappedData.companies
                every { jobRepository.saveJobs(any()) } just Runs
                every { companyRepository.saveCompanies(any()) } just Runs
                every { atsConfigRepository.updateSyncStatus(any(), any(), any()) } just Runs

                service.syncCompany(companyId)

                verify { bronzeRepository.saveIngestion(any(), any()) }
                verify { jobRepository.saveJobs(mappedData.jobs) }
                verify { companyRepository.saveCompanies(mappedData.companies) }
                verify {
                        atsConfigRepository.updateSyncStatus(companyId, SyncStatus.SUCCESS, any())
                }
                verify { bronzeRepository.updateProcessingStatus(match { it.startsWith("ats-greenhouse-board-1-") }, ProcessingStatus.COMPLETED) }
        }

        @Test
        fun `should handle sync failures and update status`() {
                val companyId = "test-comp"
                val config =
                        CompanyAtsConfig(
                                companyId,
                                AtsProvider.GREENHOUSE,
                                "board-1",
                                true,
                                null,
                                SyncStatus.SUCCESS
                        )

                every { atsConfigRepository.getConfig(companyId) } returns config
                every { bronzeRepository.isDatasetIngested(any()) } returns false
                every { clientFactory.getClient(AtsProvider.GREENHOUSE) } throws
                        RuntimeException("API Error")
                every { atsConfigRepository.updateSyncStatus(any(), any(), any()) } just Runs

                try {
                        service.syncCompany(companyId)
                } catch (e: Exception) {}

                verify { atsConfigRepository.updateSyncStatus(companyId, SyncStatus.FAILED, any()) }
        }

        @Test
        fun `should early return with SUCCESS when no jobs are found`() {
                val companyId = "test-comp"
                val config =
                        CompanyAtsConfig(
                                companyId,
                                AtsProvider.GREENHOUSE,
                                "board-1",
                                true,
                                null,
                                SyncStatus.SUCCESS
                        )
                val rawPayload = """{"jobs": []}"""
                val client = mockk<AtsClient>()
                val normalizer = mockk<AtsNormalizer>()

                every { atsConfigRepository.getConfig(companyId) } returns config
                every { bronzeRepository.isDatasetIngested(any()) } returns false
                every { clientFactory.getClient(AtsProvider.GREENHOUSE) } returns client
                every { client.fetchJobs("board-1") } returns rawPayload
                every { bronzeRepository.saveIngestion(any(), any()) } returns mockk()
                every { bronzeRepository.updateProcessingStatus(any(), any()) } returns true
                every { normalizerFactory.getNormalizer(AtsProvider.GREENHOUSE) } returns normalizer
                every { normalizer.normalize(any()) } returns emptyList() // EMPTY
                every { companyRepository.getCompaniesByIds(any()) } returns emptyList()
                every { atsConfigRepository.updateSyncStatus(any(), any(), any()) } just Runs

                service.syncCompany(companyId)

                verify { bronzeRepository.saveIngestion(any(), any()) }
                verify {
                        atsConfigRepository.updateSyncStatus(companyId, SyncStatus.SUCCESS, any())
                }
                verify { bronzeRepository.updateProcessingStatus(match { it.startsWith("ats-greenhouse-board-1-") }, ProcessingStatus.COMPLETED) }
                verify(exactly = 0) { jobRepository.saveJobs(any()) }
        }

        @Test
        fun `should skip sync if already ingested today`() {
                val companyId = "test-comp"
                val config =
                        CompanyAtsConfig(
                                companyId,
                                AtsProvider.GREENHOUSE,
                                "board-1",
                                true,
                                null,
                                SyncStatus.SUCCESS
                        )

                every { atsConfigRepository.getConfig(companyId) } returns config
                // Mock already ingested
                every { bronzeRepository.isDatasetIngested(match { it.startsWith("ats-greenhouse-board-1-") }) } returns true

                service.syncCompany(companyId)

                // Should return before any interaction
                verify(exactly = 0) { clientFactory.getClient(any()) }
                verify(exactly = 0) { bronzeRepository.saveIngestion(any(), any()) }
        }
}
