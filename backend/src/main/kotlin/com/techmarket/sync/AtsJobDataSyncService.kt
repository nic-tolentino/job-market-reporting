package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.BronzeRepository
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.ats.AtsClientFactory
import com.techmarket.sync.ats.AtsNormalizerFactory
import com.techmarket.sync.ats.SyncStatus
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

/**
 * Orchestrates the synchronization of job data from direct ATS providers.
 * Follows a Bronze-first pattern where raw data is persisted to GCS before normalization.
 */
@Service
class AtsJobDataSyncService(
    private val atsConfigRepository: AtsConfigRepository,
    private val clientFactory: AtsClientFactory,
    private val normalizerFactory: AtsNormalizerFactory,
    private val bronzeRepository: BronzeRepository,
    private val mapper: AtsJobDataMapper,
    private val merger: SilverDataMerger,
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository,
    private val objectMapper: ObjectMapper,
    private val classifier: TechRoleClassifier,
    private val gcsConfig: GcsConfig
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
        val dateStr = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneOffset.UTC)
            .format(syncTime)
        val datasetId = "ats-${config.atsProvider.name.lowercase()}-${config.identifier}-$dateStr"

        log.info(
            "ATS Sync: Starting sync for ${config.atsProvider} board: ${config.identifier} (Company: $companyId, Dataset: $datasetId)"
        )

        // 0. Guard against multiple syncs per day for the same board
        if (bronzeRepository.isDatasetIngested(datasetId)) {
            log.warn("ATS Sync: Dataset $datasetId has already been ingested today. Skipping.")
            return
        }

        try {
            // 1. Fetch from ATS API
            val client = clientFactory.getClient(config.atsProvider)
            val rawPayload = client.fetchJobs(config.identifier)

            // 2. Persist to Bronze Layer (GCS) immediately
            val manifest = createBronzeManifest(
                rawPayload = rawPayload,
                datasetId = datasetId,
                syncTime = syncTime,
                source = config.atsProvider.displayName
            )
            bronzeRepository.saveIngestion(manifest, listOf(rawPayload.toByteArray()))

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
                // Bronze layer ingestion was successful - mark as COMPLETED even though no data to map
                bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)
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
                // Bronze layer ingestion was successful - mark as COMPLETED even though no tech jobs to map
                bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)
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

            // 6. Persist to Silver Layer and update processing status
            try {
                jobRepository.saveJobs(mergedJobs)
                companyRepository.saveCompanies(mergedCompanies)

                // Update Bronze layer processing status to COMPLETED
                bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)

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
                // Silver layer persistence failed - mark as FAILED
                log.error("Failed to persist Silver layer data: ${e.message}. Marking dataset as FAILED.")
                bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.FAILED)
                throw e
            }
        } catch (e: Exception) {
            log.error("ATS Sync: Failed to sync company $companyId: ${e.message}", e)
            atsConfigRepository.updateSyncStatus(companyId, SyncStatus.FAILED, syncTime)
            throw e
        }
    }

    private fun createBronzeManifest(
        rawPayload: String,
        datasetId: String,
        syncTime: Instant,
        source: String
    ): BronzeIngestionManifest {
        val dateStr = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneOffset.UTC)
            .format(syncTime)

        val fileName = "gs://${gcsConfig.bucketName}/ats/${source.lowercase()}/$dateStr/dataset-$datasetId/jobs-0001.json.gz"
        val uncompressedSize = rawPayload.length.toLong()
        val compressedSize = (uncompressedSize * 0.2).toLong()  // ~80% compression with gzip

        val compressionRatio = if (uncompressedSize > 0) {
            compressedSize.toDouble() / uncompressedSize
        } else {
            1.0
        }

        return BronzeIngestionManifest(
            datasetId = datasetId,
            source = source,
            ingestedAt = syncTime,
            targetCountry = null,
            recordCount = 1,  // ATS payloads are single documents
            fileCount = 1,
            uncompressedSizeBytes = uncompressedSize,
            compressedSizeBytes = compressedSize,
            compressionRatio = compressionRatio,
            files = listOf(fileName),
            processingStatus = ProcessingStatus.PENDING
        )
    }

    private fun getCompanyName(companyId: String): String {
        return companyRepository.getCompaniesByIds(listOf(companyId)).firstOrNull()?.name
            ?: "Unknown Company"
    }
}
