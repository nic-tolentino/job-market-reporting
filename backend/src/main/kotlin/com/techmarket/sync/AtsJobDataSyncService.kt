package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.IngestionRepository
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.RawIngestionRecord
import com.techmarket.sync.ats.AtsClientFactory
import com.techmarket.sync.ats.AtsNormalizerFactory
import com.techmarket.sync.ats.SyncStatus
import java.time.Instant
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

/**
 * Orchestrates the synchronization of job data from direct ATS providers. Follows a Bronze-first
 * pattern where raw data is persisted before normalization.
 */
@Service
class AtsJobDataSyncService(
        private val atsConfigRepository: AtsConfigRepository,
        private val clientFactory: AtsClientFactory,
        private val normalizerFactory: AtsNormalizerFactory,
        private val ingestionRepository: IngestionRepository,
        private val mapper: AtsJobDataMapper,
        private val merger: SilverDataMerger,
        private val jobRepository: JobRepository,
        private val companyRepository: CompanyRepository,
        private val objectMapper: ObjectMapper,
        private val classifier: TechRoleClassifier
) {

        private val log = LoggerFactory.getLogger(AtsJobDataSyncService::class.java)

        /** Triggers a full sync for all enabled ATS configurations. */
        fun syncAllEnabled() {
                log.info("ATS Sync: Starting global sync for all enabled companies")
                val configs = atsConfigRepository.getEnabledConfigs()
                configs.forEach { config ->
                        try {
                                syncCompany(config.companyId)
                        } catch (e: Exception) {
                                log.error(
                                        "ATS Sync: Global sync failed for company ${config.companyId}: ${e.message}"
                                )
                                // Continue with other companies
                        }
                }
                log.info("ATS Sync: Global sync completed for ${configs.size} companies")
        }

        /** Executes a full sync for a specific company by its ID. */
        @CacheEvict(value = ["jobs", "companies", "search"], allEntries = true)
        fun syncCompany(companyId: String) {
                val config = atsConfigRepository.getConfig(companyId)
                if (config == null || !config.enabled) {
                        log.warn(
                                "ATS Sync: No enabled config found for company $companyId. Aborting."
                        )
                        return
                }

                val syncTime = Instant.now()
                val dateStr = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                        .withZone(java.time.ZoneOffset.UTC)
                        .format(syncTime)
                val datasetId = "ats-${config.atsProvider.name.lowercase()}-${config.identifier}-$dateStr"

                log.info(
                        "ATS Sync: Starting sync for ${config.atsProvider} board: ${config.identifier} (Company: $companyId, Dataset: $datasetId)"
                )

                // 0. Guard against multiple syncs per day for the same board
                if (ingestionRepository.isDatasetIngested(datasetId)) {
                        log.warn("ATS Sync: Dataset $datasetId has already been ingested today. Skipping.")
                        return
                }

                try {
                        // 1. Fetch from ATS API
                        val client = clientFactory.getClient(config.atsProvider)
                        val rawPayload = client.fetchJobs(config.identifier)

                        // 2. Persist to Bronze Layer immediately
                        val ingestionRecord =
                                RawIngestionRecord(
                                        id = UUID.randomUUID().toString(),
                                        source = config.atsProvider.displayName,
                                        ingestedAt = syncTime,
                                        rawPayload = rawPayload,
                                        datasetId = datasetId
                                )
                        ingestionRepository.saveRawIngestions(listOf(ingestionRecord))

                        // 3. Normalize raw data
                        val rootNode =
                                try {
                                        objectMapper.readTree(rawPayload)
                                } catch (e: Exception) {
                                        throw IllegalArgumentException(
                                                "ATS Sync: Failed to parse raw JSON payload: ${e.message}",
                                                e
                                        )
                                }
                        val normalizer = normalizerFactory.getNormalizer(config.atsProvider)
                        val normalizedJobs =
                                normalizer.normalize(rootNode).map {
                                        it.copy(companyName = getCompanyName(companyId))
                                }

                        if (normalizedJobs.isEmpty()) {
                                log.info(
                                        "ATS Sync: No jobs found for ${config.atsProvider} board: ${config.identifier}"
                                )
                                atsConfigRepository.updateSyncStatus(
                                        companyId,
                                        SyncStatus.SUCCESS,
                                        syncTime
                                )
                                return
                        }

                        // 4. Map to Silver Layer entities
                        val techJobs = normalizedJobs.filter { classifier.isTechRole(it) }
                        log.info(
                                "ATS Sync: Filtered ${normalizedJobs.size} -> ${techJobs.size} tech roles for company $companyId"
                        )

                        if (techJobs.isEmpty()) {
                                atsConfigRepository.updateSyncStatus(
                                        companyId,
                                        SyncStatus.SUCCESS,
                                        syncTime
                                )
                                return
                        }

                        val mappedData = mapper.map(techJobs, companyId, syncTime)

                        // 5. Merge with existing data
                        val existingJobs =
                                jobRepository.getJobsByIds(mappedData.jobs.map { it.jobId })
                        val existingCompanies =
                                companyRepository.getCompaniesByIds(
                                        mappedData.companies.map { it.companyId }
                                )

                        val mergedJobs = merger.mergeJobs(mappedData.jobs, existingJobs)
                        val mergedCompanies =
                                merger.mergeCompanies(mappedData.companies, existingCompanies)

                        // 6. Persist to Silver Layer
                        jobRepository.saveJobs(mergedJobs)
                        companyRepository.saveCompanies(mergedCompanies)

                        // 7. Update sync status
                        atsConfigRepository.updateSyncStatus(
                                companyId,
                                SyncStatus.SUCCESS,
                                syncTime
                        )
                        log.info(
                                "ATS Sync: Successfully completed sync for $companyId. Synced ${mergedJobs.size} jobs."
                        )
                } catch (e: Exception) {
                        log.error("ATS Sync: Failed to sync company $companyId: ${e.message}", e)
                        atsConfigRepository.updateSyncStatus(companyId, SyncStatus.FAILED, syncTime)
                        throw e
                }
        }

        private fun getCompanyName(companyId: String): String {
                return companyRepository.getCompaniesByIds(listOf(companyId)).firstOrNull()?.name
                        ?: "Unknown Company"
        }
}
