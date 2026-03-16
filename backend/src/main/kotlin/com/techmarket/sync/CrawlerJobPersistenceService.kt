package com.techmarket.sync

import com.techmarket.persistence.job.JobRepository
import com.techmarket.sync.model.NormalizedJobDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Orchestrates persisting crawler-extracted jobs to the Silver Layer raw_jobs table.
 *
 * Flow: map NormalizedJobDto → JobRecord, fetch existing jobs for the company,
 * merge to deduplicate, then save via JobRepository.
 *
 * Designed to be called from both admin-triggered crawls and the future batch crawler.
 */
@Service
class CrawlerJobPersistenceService(
    private val mapper: CrawlerJobMapper,
    private val merger: SilverDataMerger,
    private val jobRepository: JobRepository,
) {
    private val log = LoggerFactory.getLogger(CrawlerJobPersistenceService::class.java)

    /**
     * Maps, merges, and saves the given jobs for [companyId].
     * Returns the number of records written.
     * Throws if the final save fails (caller decides whether to treat as fatal).
     */
    @org.springframework.cache.annotation.CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
    fun persist(companyId: String, jobs: List<NormalizedJobDto>): Int {
        if (jobs.isEmpty()) return 0

        val newRecords = mapper.toJobRecords(jobs, companyId)
        if (newRecords.isEmpty()) {
            log.warn("CrawlerJobMapper produced 0 records from ${jobs.size} raw jobs for $companyId")
            return 0
        }

        val existingRecords = try {
            jobRepository.findByCompanyId(companyId)
        } catch (e: Exception) {
            log.warn("Could not fetch existing jobs for $companyId — inserting without merge: ${e.message}")
            emptyList()
        }

        val merged = merger.mergeJobs(newRecords, existingRecords)

        jobRepository.saveJobs(merged)
        log.info("Persisted ${merged.size} jobs for $companyId (${newRecords.size} new, ${existingRecords.size} existing pre-merge)")
        return merged.size
    }
}
