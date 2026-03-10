package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.BronzeRepository
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.model.ApifyJobResult
import com.techmarket.util.Constants.SOURCE_APIFY
import com.techmarket.util.Constants.SOURCE_ATS
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

/**
 * Orchestrates the data synchronization and reprocessing pipelines between Apify, Bronze (raw), and
 * Silver (structured) layers.
 * 
 * Uses file-based cold storage (GCS) for Bronze layer with BigQuery metadata indexing.
 */
@Service
class JobDataSyncService(
    private val apifyClient: ApifyClient,
    private val jobDataMapper: RawJobDataMapper,
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository,
    private val bronzeRepository: BronzeRepository,
    private val silverDataMerger: SilverDataMerger,
    private val companySyncService: CompanySyncService,
    private val objectMapper: ObjectMapper,
    private val gcsConfig: GcsConfig
) {

    private val log = LoggerFactory.getLogger(JobDataSyncService::class.java)

    /**
     * Executes the end-to-end data synchronization pipeline.
     * 1. Fetches raw JSON records from Apify (External Scraper).
     * 2. Stores raw payloads as compressed files in GCS (Bronze Layer) with metadata in BigQuery.
     * 3. Maps and cleanses data into structured Job and Company records using [JobDataMapper].
     * 4. Saves the structured data into the Silver Layer (raw_jobs, raw_companies tables).
     * 
     * @param datasetId The Apify dataset ID to ingest
     * @param targetCountry Optional country filter for job mapping
     * @param ingestedAt Optional custom ingestion time (defaults to current time). Use this for historical data or backfilling.
     */
    @CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
    fun runDataSync(datasetId: String, targetCountry: String? = null, ingestedAt: Instant? = null) {
        log.info("Starting Job Data Sync Pipeline for dataset: $datasetId (Target Country: ${targetCountry ?: "Unspecified"}, Ingestion Time: ${ingestedAt ?: "now"})")

        // 0. Check if already ingested
        if (bronzeRepository.isDatasetIngested(datasetId)) {
            log.warn("Dataset $datasetId has already been ingested. Skipping to avoid duplication. " +
                    "If you need to re-ingest, manually delete the records from the ingestion_metadata table.")
            return
        }

        // 1. Fetch from source
        val apifyResults = apifyClient.fetchRecentJobs(datasetId)
        log.info("Fetched ${apifyResults.size} raw records from Apify.")

        if (apifyResults.isEmpty()) {
            log.info("No jobs fetched from Apify. Aborting sync.")
            return
        }

        // Use custom ingestion time if provided, otherwise use current time
        val syncTime = ingestedAt ?: Instant.now()

        // 2. Prepare Bronze Layer files (GCS cold storage)
        // Chunk data to match manifest file structure
        val chunkSize = getChunkSize(SOURCE_APIFY)
        val chunks = apifyResults.chunked(chunkSize)
        val dataChunks = chunks.map { chunk ->
            // Create NDJSON format: one JSON object per line
            chunk.joinToString("\n") { it.rawJson }.toByteArray()
        }
        val manifest = createBronzeManifest(apifyResults, datasetId, syncTime, targetCountry, SOURCE_APIFY, chunkSize)
        bronzeRepository.saveIngestion(manifest, dataChunks)
        log.info("Bronze Layer: Saved ${manifest.fileCount} compressed files to GCS with datasetId: $datasetId")

        // 3. Fetch Manifest Companies for Phase 2 Mapping
        // Note: Company manifest sync should be run separately via /api/admin/sync-companies endpoint
        val manifestCompanies = companyRepository.getAllCompanies().associateBy { it.companyId }

        // 5. Phase 2: Map to Silver Layer (Structured Data)
        val rawJobs = apifyResults.map { RawJob(it.dto, syncTime) }
        val mappedData = jobDataMapper.map(rawJobs, manifestCompanies, targetCountry)
        log.info(
            "Silver Layer: Successfully mapped ${mappedData.jobs.size} jobs and ${mappedData.companies.size} unverified companies from ${rawJobs.size} raw records."
        )

        // 6. Merge with existing Silver data
        val jobIds = mappedData.jobs.map { it.jobId }.distinct()
        val companyIds = mappedData.companies.map { it.companyId }.distinct()

        val existingJobs = jobRepository.getJobsByIds(jobIds)
        val existingCompanies = companyRepository.getCompaniesByIds(companyIds)

        val mergedJobs = silverDataMerger.mergeJobs(mappedData.jobs, existingJobs)
        val mergedCompanies = silverDataMerger.mergeCompanies(mappedData.companies, existingCompanies)

        log.info(
            "Silver Layer: Merged ${mergedJobs.size} jobs and ${mergedCompanies.size} companies. Performing targeted update (Delete-then-Insert)..."
        )

        try {
            // 7. Persist Silver records (Delete existing matches, then save merged)
            if (jobIds.isNotEmpty()) {
                jobRepository.deleteJobsByIds(jobIds)
            }
            if (companyIds.isNotEmpty()) {
                companyRepository.deleteCompaniesByIds(companyIds)
            }

            companyRepository.saveCompanies(mergedCompanies)
            jobRepository.saveJobs(mergedJobs)

            // 8. Update Bronze layer processing status to COMPLETED
            val statusUpdated = bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)
            if (!statusUpdated) {
                log.warn("Failed to update processing status to COMPLETED for dataset $datasetId")
            }

            log.info("Data Sync Pipeline completed successfully.")
        } catch (e: Exception) {
            // Silver layer persistence failed - mark as FAILED
            log.error("Failed to persist Silver layer data: ${e.message}. Marking dataset as FAILED.")
            val statusUpdated = bronzeRepository.updateProcessingStatus(datasetId, ProcessingStatus.FAILED)
            if (!statusUpdated) {
                log.error("Failed to update processing status to FAILED for dataset $datasetId")
            }
            throw e
        }
    }

    /**
     * Re-runs the mapping and transformation pipeline using all historical records from the Bronze
     * Layer. Use this when:
     * - Extraction logic or deduplication rules change in [JobDataMapper].
     * - The Silver layer schema is updated and old data needs to be re-parsed.
     *
     * This process is safe because it recreates the Silver layer from the immutable Bronze source.
     * 
     * Uses batched processing to avoid OOM errors with large historical datasets.
     */
    @CacheEvict(value = ["landing", "tech", "company", "search"], allEntries = true)
    fun reprocessHistoricalData() {
        log.info("Starting Historical Data Reprocessing (Silver Layer Refresh)...")

        log.info("Silver Layer Cleanup: Wiping current tables...")
        jobRepository.deleteAllJobs()
        companyRepository.deleteAllCompanies()

        // 1. Fetch all manifests from Bronze Layer
        val manifests = bronzeRepository.listManifests()
        log.info("Bronze Layer: Found ${manifests.size} historical manifests for reprocessing.")

        if (manifests.isEmpty()) {
            log.info("No historical records found. Aborting reprocessing.")
            return
        }

        // 2. Refresh Master Manifest Companies first
        try {
            companySyncService.syncFromManifest()
        } catch (e: Exception) {
            log.error("Failed to sync company manifest: ${e.message}. Continuing with existing data.")
        }
        val manifestCompanies = companyRepository.getAllCompanies().associateBy { it.companyId }

        // 3. Process manifests in batches with incremental persistence to avoid OOM
        val manifestBatchSize = 10  // Process 10 manifests at a time
        var totalJobsProcessed = 0
        var totalCompaniesProcessed = 0

        manifests.chunked(manifestBatchSize).forEachIndexed { batchIndex, manifestBatch ->
            log.info("Processing manifest batch ${batchIndex + 1}/${(manifests.size + manifestBatchSize - 1) / manifestBatchSize}")

            val batchJobs = mutableListOf<JobRecord>()
            val batchCompanies = mutableListOf<CompanyRecord>()

            manifestBatch.forEach { manifest ->
                manifest.files.forEach { filePath ->
                    try {
                        val inputStream = bronzeRepository.readFile(filePath)
                        val rawJobs = parseJobsFromStream(inputStream, manifest.ingestedAt)

                        if (rawJobs.isNotEmpty()) {
                            val mappedData = jobDataMapper.map(rawJobs, manifestCompanies)
                            batchJobs.addAll(mappedData.jobs)
                            batchCompanies.addAll(mappedData.companies)
                        }
                    } catch (e: Exception) {
                        log.warn("Failed to parse GCS file $filePath: ${e.message}")
                    }
                }
            }

            // Persist batch incrementally to avoid memory buildup
            if (batchJobs.isNotEmpty() || batchCompanies.isNotEmpty()) {
                companyRepository.saveCompanies(batchCompanies)
                jobRepository.saveJobs(batchJobs)
                totalJobsProcessed += batchJobs.size
                totalCompaniesProcessed += batchCompanies.size
                log.info("Persisted batch: ${batchJobs.size} jobs, ${batchCompanies.size} companies (Total: $totalJobsProcessed jobs, $totalCompaniesProcessed companies)")
            }
        }

        log.info("Parsed and persisted $totalJobsProcessed jobs and $totalCompaniesProcessed companies from historical records.")

        if (totalJobsProcessed == 0 && totalCompaniesProcessed == 0) {
            log.info("No valid data found in historical records. Aborting reprocessing.")
            return
        }

        log.info("Historical Data Reprocessing completed successfully.")
    }

    /**
     * Get chunk size based on data source.
     * Larger chunks for crawls (fewer files), smaller for frequent ingestions.
     */
    private fun getChunkSize(source: String): Int {
        return when (source) {
            SOURCE_APIFY -> 5000  // Large crawls: fewer, bigger files
            SOURCE_ATS -> 500     // Small, frequent ingestions
            else -> 2000          // Balanced default
        }
    }

    private fun createBronzeManifest(
        results: List<ApifyJobResult>,
        datasetId: String,
        syncTime: Instant,
        targetCountry: String?,
        source: String,
        chunkSize: Int = getChunkSize(source)
    ): BronzeIngestionManifest {
        val dateStr = DateTimeFormatter.ISO_LOCAL_DATE
            .withZone(ZoneOffset.UTC)
            .format(syncTime)

        val chunks = results.chunked(chunkSize)
        val files = chunks.mapIndexed { index, _ ->
            "gs://${gcsConfig.bucketName}/$source/$dateStr/dataset-$datasetId/jobs-${String.format("%04d", index + 1)}.json.gz"
        }

        // Calculate sizes for compression tracking
        val uncompressedSize = estimateSize(results)
        val compressedSize = (uncompressedSize * 0.2).toLong()  // ~80% compression with gzip

        // Guard against division by zero for empty datasets
        val compressionRatio = if (uncompressedSize > 0) {
            compressedSize.toDouble() / uncompressedSize
        } else {
            1.0  // Default to 1.0 for empty datasets
        }

        return BronzeIngestionManifest(
            datasetId = datasetId,
            source = source,
            ingestedAt = syncTime,
            targetCountry = targetCountry,
            recordCount = results.size,
            fileCount = files.size,
            uncompressedSizeBytes = uncompressedSize,
            compressedSizeBytes = compressedSize,
            compressionRatio = compressionRatio,
            files = files,
            processingStatus = ProcessingStatus.PENDING
        )
    }

    private fun estimateSize(results: List<ApifyJobResult>): Long {
        return results.sumOf { result -> result.rawJson.length.toLong() }
    }

    private fun parseJobsFromStream(
        inputStream: java.io.InputStream,
        ingestedAt: Instant
    ): List<RawJob> {
        val rawJobs = mutableListOf<RawJob>()
        
        inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                try {
                    val dto = objectMapper.readValue(line, com.techmarket.sync.model.ApifyJobDto::class.java)
                    rawJobs.add(RawJob(dto, ingestedAt))
                } catch (e: Exception) {
                    log.warn("Failed to parse job from line: ${e.message}")
                }
            }
        }
        
        return rawJobs
    }
}
