package com.jobmarket.app.sync

import com.jobmarket.app.persistence.JobRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
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
    @Async
    fun runDataSync() {
        log.info("Starting Job Data Sync Pipeline...")

        val rawJobs = apifyClient.fetchRecentJobs()
        if (rawJobs.isEmpty()) {
            log.info("No jobs fetched from Apify. Aborting sync.")
            return
        }

        val mappedData = jobDataMapper.mapSyncData(rawJobs)
        log.info(
                "Successfully mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} companies for storage."
        )

        jobRepository.saveCompanies(mappedData.companies)
        jobRepository.saveJobs(mappedData.jobs)

        log.info("Job Data Sync Pipeline completed successfully.")
    }
}
