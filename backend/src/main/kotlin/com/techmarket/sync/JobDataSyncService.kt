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

/**
 * Orchestrates the data synchronization and reprocessing pipelines between Apify, Bronze (raw), and
 * Silver (structured) layers.
 */
@Service
class JobDataSyncService(
        private val apifyClient: ApifyClient,
        private val jobDataMapper: RawJobDataMapper,
        private val jobRepository: JobRepository,
        private val companyRepository: CompanyRepository,
        private val ingestionRepository: IngestionRepository,
        private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {

        private val log = LoggerFactory.getLogger(JobDataSyncService::class.java)

        /**
         * Executes the end-to-end data synchronization pipeline.
         * 1. Fetches raw JSON records from Apify (External Scraper).
         * 2. Ingests raw payloads into the Bronze Layer (raw_ingestions table) for audit and
         * reprocessing.
         * 3. Maps and cleanses data into structured Job and Company records using [JobDataMapper].
         * 4. Saves the structured data into the Silver Layer (raw_jobs, raw_companies tables).
         */
        @CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
        fun runDataSync(datasetId: String) {
                log.info("Starting Job Data Sync Pipeline for dataset: $datasetId")

                // 1. Fetch from source
                val apifyResults = apifyClient.fetchRecentJobs(datasetId)
                log.info("Fetched ${apifyResults.size} raw records from Apify.")

                if (apifyResults.isEmpty()) {
                        log.info("No jobs fetched from Apify. Aborting sync.")
                        return
                }

                val syncTime = Instant.now()
                // 2. Prepare Bronze Layer records (Audit-trail)
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

                // 3. Map to Silver Layer (Structured Data)
                val rawJobs = apifyResults.map { RawJob(it.dto, syncTime) }
                val mappedData = jobDataMapper.map(rawJobs)
                log.info(
                        "Silver Layer: Successfully mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} companies from ${rawJobs.size} raw records."
                )

                // 4. Persist Silver records
                companyRepository.saveCompanies(mappedData.companies)
                jobRepository.saveJobs(mappedData.jobs)

                log.info("Data Sync Pipeline completed successfully.")
        }

        /**
         * Re-runs the mapping and transformation pipeline using all historical records from the
         * Bronze Layer. Use this when:
         * - Extraction logic or deduplication rules change in [JobDataMapper].
         * - The Silver layer schema is updated and old data needs to be re-parsed.
         *
         * This process is safe because it recreates the Silver layer from the immutable Bronze
         * source.
         */
        @CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
        fun reprocessHistoricalData() {
                log.info("Starting Historical Data Reprocessing (Silver Layer Refresh)...")

                // 1. Fetch all raw payloads from Bronze Layer
                val rawRecords = ingestionRepository.getRawIngestions()
                log.info(
                        "Bronze Layer: Found ${rawRecords.size} historical records for reprocessing."
                )

                if (rawRecords.isEmpty()) {
                        log.info("No historical records found. Aborting reprocessing.")
                        return
                }

                // 2. Parse JSON back into DTOs and wrap with original ingestion time
                val rawJobs =
                        rawRecords.mapNotNull { record ->
                                try {
                                        val dto =
                                                objectMapper.readValue(
                                                        record.rawPayload,
                                                        com.techmarket.sync.model.ApifyJobDto::class
                                                                .java
                                                )
                                        RawJob(dto, record.ingestedAt)
                                } catch (e: Exception) {
                                        log.warn(
                                                "Failed to parse raw payload for record ${record.id}: ${e.message}"
                                        )
                                        null
                                }
                        }

                log.info("Parsed ${rawJobs.size} valid jobs from historical records. Remapping...")

                // 3. Re-map using latest logic
                val mappedData = jobDataMapper.map(rawJobs)
                log.info(
                        "Silver Layer: Freshly mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} companies."
                )

                // 4. Wipe Silver tables and re-insert
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
