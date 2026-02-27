package com.techmarket.sync

import com.techmarket.persistence.JobRepository
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
        private val jobRepository: JobRepository
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
        log.info("Starting Job Data Sync Pipeline...")

        val apifyResults = apifyClient.fetchRecentJobs(datasetId)
        if (apifyResults.isEmpty()) {
            log.info("No jobs fetched from Apify. Aborting sync.")
            return
        }

        val syncTime = Instant.now()
        val rawRecords =
                apifyResults.map {
                    RawIngestionRecord(
                            id = "${it.dto.id}-${UUID.randomUUID()}", // combine apify job ID +
                            // uuid so it's always
                            // unique
                            source = "LinkedIn-Apify",
                            ingestedAt = syncTime,
                            rawPayload = it.rawJson
                    )
                }

        log.info("Ingesting ${rawRecords.size} raw payloads into Bronze Layer.")
        jobRepository.saveRawIngestions(rawRecords)

        val rawJobs = apifyResults.map { it.dto }
        val mappedData = jobDataMapper.mapSyncData(rawJobs)
        log.info(
                "Successfully mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} companies for Silver Layer storage."
        )

        jobRepository.saveCompanies(mappedData.companies)
        jobRepository.saveJobs(mappedData.jobs)

        log.info("Job Data Sync Pipeline completed successfully.")
    }
}
