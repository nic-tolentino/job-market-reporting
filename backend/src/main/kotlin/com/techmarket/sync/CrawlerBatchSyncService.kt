package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.BronzeRepository
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.ats.CrawlerClient
import com.techmarket.sync.ats.CrawlerNormalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

/**
 * Orchestrates batch synchronization of job data from the Crawler Service.
 * 
 * Unlike individual ATS syncs, this service batches all crawler results into a single
 * nightly dataset to avoid creating thousands of tiny files.
 */
@Service
class CrawlerBatchSyncService(
    private val crawlerClient: CrawlerClient,
    private val crawlerNormalizer: CrawlerNormalizer,
    private val bronzeRepository: BronzeRepository,
    private val companyRepository: CompanyRepository,
    private val jobRepository: JobRepository,
    private val merger: SilverDataMerger,
    private val mapper: AtsJobDataMapper,
    private val classifier: TechRoleClassifier,
    private val objectMapper: ObjectMapper,
    private val gcsConfig: GcsConfig,
    private val atsConfigRepository: AtsConfigRepository
) {
    private val log = LoggerFactory.getLogger(CrawlerBatchSyncService::class.java)

    companion object {
        private const val CHUNK_SIZE = 500  // Records per NDJSON file
    }

    /**
     * Executes a full nightly crawl for all companies without direct ATS integration.
     */
    @CacheEvict(value = ["jobs", "companies", "search"], allEntries = true)
    fun syncAllCompanies() {
        val date = LocalDate.now(ZoneOffset.UTC)
        val datasetId = "crawler-nightly-${date}"

        log.info("Crawler Sync: Starting nightly crawl for all companies (Dataset: $datasetId)")

        // Guard against re-running
        if (bronzeRepository.isDatasetIngested(datasetId)) {
            log.warn("Crawler Sync: Dataset $datasetId has already been ingested today. Skipping.")
            return
        }

        val syncTime = Instant.now()
        val companiesToCrawl = getCompaniesToCrawl()

        log.info("Crawler Sync: Found ${companiesToCrawl.size} companies to crawl")

        // Use thread-safe collection for concurrent access
        val allJobsNdjson = Collections.synchronizedList(mutableListOf<String>())
        val crawlResults = Collections.synchronizedList(mutableListOf<CrawlResult>())

        // Crawl companies in parallel with concurrency limit
        // Using coroutines for parallel processing within this instance
        // For true distributed crawling with rate limiting, use Cloud Tasks (Phase 2)
        runBlocking {
            val batchSize = 10  // Max concurrent crawls (adjust based on Cloud Run memory)
            val batches = companiesToCrawl.chunked(batchSize)
            
            for ((batchIndex, batch) in batches.withIndex()) {
                log.info("Crawler Sync: Processing batch ${batchIndex + 1}/${batches.size} (${batch.size} companies)")
                
                val deferredResults = batch.map { companyId ->
                    async(Dispatchers.IO) {
                        try {
                            val rawPayload = crawlerClient.fetchJobs(companyId)
                            allJobsNdjson.add(rawPayload)
                            CrawlResult(companyId, true, null)
                        } catch (e: Exception) {
                            log.warn("Crawler Sync: Crawl failed for $companyId: ${e.message}")
                            CrawlResult(companyId, false, e.message)
                        }
                    }
                }
                
                crawlResults.addAll(deferredResults.awaitAll())
            }
        }

        log.info(
            "Crawler Sync: Crawled ${crawlResults.count { it.success }}/${companiesToCrawl.size} companies successfully"
        )

        // Batch into chunks and persist to Bronze
        val chunks = allJobsNdjson.chunked(CHUNK_SIZE).map { chunk ->
            chunk.joinToString("\n").toByteArray()
        }

        val manifest = createBronzeManifest(
            datasetId = datasetId,
            syncTime = syncTime,
            recordCount = allJobsNdjson.size,
            fileCount = chunks.size
        )

        bronzeRepository.saveIngestion(manifest, chunks)
        log.info("Crawler Sync: Bronze layer ingestion complete (${chunks.size} files)")

        // Process and normalize jobs
        processAndNormalizeJobs(allJobsNdjson, datasetId, syncTime)

        // Update processing status
        bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)

        val successCount = crawlResults.count { it.success }
        val failureCount = crawlResults.count { !it.success }
        log.info(
            "Crawler Sync: Nightly crawl complete. Success: $successCount, Failed: $failureCount"
        )
    }

    /**
     * Syncs a single company via the crawler.
     */
    @CacheEvict(value = ["jobs", "companies", "search"], allEntries = true)
    fun syncCompany(companyId: String) {
        log.info("Crawler Sync: Starting sync for company $companyId")

        val syncTime = Instant.now()
        val date = LocalDate.now(ZoneOffset.UTC)
        val datasetId = "crawler-single-$companyId-${date}"

        try {
            // Fetch from crawler
            val rawPayload = crawlerClient.fetchJobs(companyId)

            // Persist to Bronze
            val manifest = createBronzeManifest(
                datasetId = datasetId,
                syncTime = syncTime,
                recordCount = 1,
                fileCount = 1
            )
            bronzeRepository.saveIngestion(manifest, listOf(rawPayload.toByteArray()))

            // Process and normalize
            processAndNormalizeJobs(listOf(rawPayload), datasetId, syncTime)

            bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)
            log.info("Crawler Sync: Successfully synced $companyId")
        } catch (e: Exception) {
            log.error("Crawler Sync: Failed to sync $companyId: ${e.message}", e)
            bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.FAILED)
            throw e
        }
    }

    /**
     * Gets list of companies that should be crawled.
     * Excludes companies with direct ATS integrations.
     */
    private fun getCompaniesToCrawl(): List<String> {
        // Get all companies
        val allCompanies = companyRepository.getAllCompanies()

        // Get companies with ATS configs
        val atsCompanyIds = atsConfigRepository.getEnabledConfigs()
            .map { it.companyId }
            .toSet()

        // Return companies without ATS integration
        return allCompanies.filterNot { it.companyId in atsCompanyIds }
            .map { it.companyId }
    }

    /**
     * Processes and normalizes crawled jobs, then merges into Silver layer.
     */
    private fun processAndNormalizeJobs(
        ndjsonPayloads: List<String>,
        datasetId: String,
        syncTime: Instant
    ) {
        var totalJobsProcessed = 0

        ndjsonPayloads.forEach { payload ->
            try {
                val rootNode = objectMapper.readTree(payload)
                val normalizedJobs = crawlerNormalizer.normalize(rootNode)

                if (normalizedJobs.isEmpty()) {
                    return@forEach
                }

                // Filter to tech roles
                val techJobs = normalizedJobs.filter { classifier.isTechRole(it) }
                if (techJobs.isEmpty()) {
                    return@forEach
                }

                // Map to Silver entities
                val companyId = extractCompanyId(payload)
                val mappedData = mapper.map(techJobs, companyId, syncTime)

                // Merge with existing data
                val existingJobs = jobRepository.getJobsByIds(mappedData.jobs.map { it.jobId })
                val existingCompanies = companyRepository.getCompaniesByIds(
                    mappedData.companies.map { it.companyId }
                )

                val mergedJobs = merger.mergeJobs(mappedData.jobs, existingJobs)
                val mergedCompanies = merger.mergeCompanies(mappedData.companies, existingCompanies)

                // Persist to Silver
                jobRepository.saveJobs(mergedJobs)
                companyRepository.saveCompanies(mergedCompanies)

                totalJobsProcessed += mergedJobs.size
            } catch (e: Exception) {
                log.warn("Crawler Sync: Failed to process payload: ${e.message}")
            }
        }

        log.info("Crawler Sync: Processed and persisted $totalJobsProcessed jobs to Silver layer")
    }

    /**
     * Extracts company ID from crawler payload.
     */
    private fun extractCompanyId(payload: String): String {
        return try {
            val json = objectMapper.readTree(payload)
            json.get("companyId")?.asText() ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun createBronzeManifest(
        datasetId: String,
        syncTime: Instant,
        recordCount: Int,
        fileCount: Int
    ): BronzeIngestionManifest {
        val dateStr = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneOffset.UTC)
            .format(syncTime)

        val baseDir = "gs://${gcsConfig.bucketName}/crawler/$dateStr/nightly-crawl"
        val files = (1..fileCount).map { i ->
            "$baseDir/jobs-${i.toString().padStart(4, '0')}.json.gz"
        }

        // Estimate sizes
        val avgRecordSize = 2000L  // ~2KB per job record
        val uncompressedSize = recordCount * avgRecordSize
        val compressedSize = (uncompressedSize * 0.2).toLong()

        return BronzeIngestionManifest(
            datasetId = datasetId,
            source = "AI-Crawler",
            ingestedAt = syncTime,
            targetCountry = null,
            recordCount = recordCount,
            fileCount = fileCount,
            uncompressedSizeBytes = uncompressedSize,
            compressedSizeBytes = compressedSize,
            compressionRatio = 0.2,
            files = files,
            processingStatus = ProcessingStatus.PENDING
        )
    }

    /**
     * Result of a single company crawl attempt.
     */
    data class CrawlResult(
        val companyId: String,
        val success: Boolean,
        val error: String?
    )
}
