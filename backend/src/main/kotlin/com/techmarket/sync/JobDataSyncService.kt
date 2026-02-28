package com.techmarket.sync

import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.IngestionRepository
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.RawIngestionRecord
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

@Service
class JobDataSyncService(
        private val apifyClient: ApifyClient,
        private val jobDataMapper: JobDataMapper,
        private val jobRepository: JobRepository,
        private val companyRepository: CompanyRepository,
        private val ingestionRepository: IngestionRepository,
        private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {

    private val log = LoggerFactory.getLogger(JobDataSyncService::class.java)

    /**
     * Executes the end-to-end data synchronization pipeline.
     * 1. Fetches raw DTOs from Apify.
     * 2. Maps/Cleans data into Job and Company records.
     * 3. Saves them to the configured storage repository.
     */
    @CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
    fun runDataSync(datasetId: String) {
        log.info("Starting Job Data Sync Pipeline for dataset: $datasetId")

        val apifyResults = apifyClient.fetchRecentJobs(datasetId)
        log.info("Fetched ${apifyResults.size} raw records from Apify.")

        if (apifyResults.isEmpty()) {
            log.info("No jobs fetched from Apify. Aborting sync.")
            return
        }

        val syncTime = Instant.now()
        val rawRecords =
                apifyResults.map {
                    RawIngestionRecord(
                            id = "${it.dto.id}-${UUID.randomUUID()}",
                            source = "LinkedIn-Apify",
                            ingestedAt = syncTime,
                            rawPayload = it.rawJson
                    )
                }

        log.info("Bronze Layer: Ingesting ${rawRecords.size} raw payloads.")
        ingestionRepository.saveRawIngestions(rawRecords)

        val rawJobs = apifyResults.map { it.dto }
        val mappedData = jobDataMapper.mapSyncData(rawJobs)
        log.info(
                "Silver Layer: Successfully mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} companies from ${rawJobs.size} raw records."
        )

        companyRepository.saveCompanies(mappedData.companies)
        jobRepository.saveJobs(mappedData.jobs)

        log.info("Data Sync Pipeline completed successfully.")
    }

    @CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
    fun reprocessHistoricalData() {
        log.info("Starting Historical Data Reprocessing (Silver Layer Refresh)...")
        val rawRecords = ingestionRepository.getRawIngestions()
        log.info("Bronze Layer: Found ${rawRecords.size} historical records for reprocessing.")

        if (rawRecords.isEmpty()) {
            log.info("No historical records found. Aborting reprocessing.")
            return
        }

        val rawJobs =
                rawRecords.mapNotNull { record ->
                    try {
                        objectMapper.readValue(
                                record.rawPayload,
                                com.techmarket.sync.model.ApifyJobDto::class.java
                        )
                    } catch (e: Exception) {
                        log.warn(
                                "Failed to parse raw payload for record ${record.id}: ${e.message}"
                        )
                        null
                    }
                }

        log.info("Parsed ${rawJobs.size} valid jobs from historical records. Remapping...")
        val mappedData = jobDataMapper.mapSyncData(rawJobs)
        log.info(
                "Silver Layer: Freshly mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} companies."
        )

        log.info(
                "Silver Layer Cleanup: Wiping current tables before re-inserting freshly mapped data..."
        )
        jobRepository.deleteAllJobs()
        companyRepository.deleteAllCompanies()

        log.info("Silver Layer: Re-inserting data...")
        companyRepository.saveCompanies(mappedData.companies)
        jobRepository.saveJobs(mappedData.jobs)
        log.info("Historical Data Reprocessing completed successfully.")
    }
}
