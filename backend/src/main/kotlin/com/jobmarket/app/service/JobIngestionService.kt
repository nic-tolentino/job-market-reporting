package com.jobmarket.app.service

import com.jobmarket.app.client.ApifyClient
import com.jobmarket.app.repository.JobPostingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JobIngestionService(
        private val apifyClient: ApifyClient,
        private val jobDataMapper: JobDataMapper,
        private val jobPostingRepository: JobPostingRepository
) {

    private val log = LoggerFactory.getLogger(JobIngestionService::class.java)

    /**
     * Executes the end-to-end ingestion pipeline.
     * 1. Fetches raw DTOs from Apify.
     * 2. Maps/Cleans data into BigQuery records.
     * 3. Saves them to the configured storage repository.
     */
    fun runIngestion() {
        log.info("Starting Job Ingestion Pipeline...")

        val rawJobs = apifyClient.fetchRecentJobs()
        if (rawJobs.isEmpty()) {
            log.info("No jobs fetched from Apify. Aborting ingestion.")
            return
        }

        val mappedJobs = jobDataMapper.mapToBigQueryRecords(rawJobs)
        log.info("Successfully mapped ${mappedJobs.size} jobs for storage.")

        jobPostingRepository.saveAll(mappedJobs)
        log.info("Job Ingestion Pipeline completed successfully.")
    }
}
