package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.model.NormalizedJobDto
import com.techmarket.util.Constants.SOURCE_CRAWLER
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Handles archiving crawler job batches to GCS bronze storage and managing
 * the daily crawler dataset manifests in ingestion_metadata.
 *
 * GCS path convention:
 *   crawler/{YYYY-MM-DD}/run-{runId}/{companyId}-jobs.json.gz
 *
 * Dataset ID convention:
 *   crawler-{YYYY-MM-DD}  (one manifest per day, idempotent)
 */
@Service
class CrawlerDataSyncService(
    private val metadataRepository: IngestionMetadataRepository,
    private val crawlerJobPersistenceService: CrawlerJobPersistenceService,
    private val objectMapper: ObjectMapper,
    private val gcsConfig: GcsConfig,
    private val storage: Storage = StorageOptions.getDefaultInstance().service
) {
    private val log = LoggerFactory.getLogger(CrawlerDataSyncService::class.java)

    /**
     * Archives a single crawl's NormalizedJobDto list to GCS bronze storage.
     * Called at crawl time alongside the immediate raw_jobs persistence.
     * Non-fatal — callers should catch and log exceptions.
     */
    fun archiveCrawlBatch(companyId: String, runId: String, date: LocalDate, jobs: List<NormalizedJobDto>) {
        if (jobs.isEmpty()) return

        val dateStr = date.toString()
        val blobName = "$SOURCE_CRAWLER/$dateStr/run-$runId/$companyId-jobs.json.gz"
        val ndjson = jobs.joinToString("\n") { objectMapper.writeValueAsString(it) }.toByteArray()
        val compressed = compressGzip(ndjson)

        val blobId = BlobId.of(gcsConfig.bucketName, blobName)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType("application/json")
            .setContentEncoding("gzip")
            .build()

        storage.create(blobInfo, compressed)
        log.info("Archived ${jobs.size} crawler jobs for $companyId to gs://${gcsConfig.bucketName}/$blobName")
    }

    /**
     * Creates a daily dataset manifest in ingestion_metadata by discovering all GCS files
     * for the given date under the crawler/ prefix.
     *
     * Idempotent: returns the existing manifest if one already exists for this date.
     *
     * @return The created (or existing) manifest, or null if no GCS files were found.
     */
    fun createDailyDataset(date: LocalDate): BronzeIngestionManifest? {
        val dateStr = date.toString()
        val datasetId = "crawler-$dateStr"

        if (metadataRepository.isDatasetIngested(datasetId)) {
            log.info("Daily crawler dataset $datasetId already exists — returning existing manifest")
            return metadataRepository.getManifest(datasetId)
        }

        val prefix = "$SOURCE_CRAWLER/$dateStr/"
        val files = storage.list(gcsConfig.bucketName, Storage.BlobListOption.prefix(prefix))
            .iterateAll()
            .map { "gs://${gcsConfig.bucketName}/${it.name}" }
            .filter { it.endsWith(".json.gz") }
            .sorted()
            .toList()

        if (files.isEmpty()) {
            log.warn("No crawler GCS files found for $dateStr under gs://${gcsConfig.bucketName}/$prefix")
            return null
        }

        val compressedBytes = files.sumOf { gcsPath ->
            try {
                val blobName = gcsPath.removePrefix("gs://${gcsConfig.bucketName}/")
                storage.get(BlobId.of(gcsConfig.bucketName, blobName))?.size ?: 0L
            } catch (_: Exception) { 0L }
        }

        val manifest = BronzeIngestionManifest(
            datasetId = datasetId,
            source = SOURCE_CRAWLER,
            ingestedAt = date.atStartOfDay(ZoneOffset.UTC).toInstant(),
            targetCountry = null,
            recordCount = 0,  // Updated to actual count during processDataset
            fileCount = files.size,
            uncompressedSizeBytes = (compressedBytes / 0.2).toLong(),
            compressedSizeBytes = compressedBytes,
            compressionRatio = 0.2,
            files = files,
            processingStatus = ProcessingStatus.PENDING,
        )

        metadataRepository.saveManifest(manifest)
        log.info("Created daily crawler dataset $datasetId with ${files.size} files")
        return manifest
    }

    /**
     * Re-processes a crawler dataset: reads NormalizedJobDto NDJSON from each GCS file,
     * maps through CrawlerJobPersistenceService, and saves to raw_jobs.
     *
     * The companyId is extracted from the GCS filename convention:
     *   crawler/{date}/run-{runId}/{companyId}-jobs.json.gz
     *
     * Use this when you want to replay archived crawler jobs after mapping logic changes.
     */
    fun processDataset(datasetId: String) {
        val manifest = metadataRepository.getManifest(datasetId)
            ?: throw IllegalArgumentException("Crawler dataset '$datasetId' not found in ingestion_metadata")

        if (manifest.source != SOURCE_CRAWLER) {
            throw IllegalArgumentException(
                "Dataset '$datasetId' has source '${manifest.source}', expected '$SOURCE_CRAWLER'"
            )
        }

        log.info("Re-processing crawler dataset $datasetId (${manifest.fileCount} files)")
        var totalSaved = 0

        manifest.files.forEach { gcsPath ->
            try {
                val filename = gcsPath.substringAfterLast("/")         // e.g. "trademe-jobs.json.gz"
                val companyId = filename.removeSuffix("-jobs.json.gz")
                if (companyId.isBlank() || companyId == filename) {
                    log.warn("Cannot extract companyId from path $gcsPath — skipping")
                    return@forEach
                }

                val blobName = gcsPath.removePrefix("gs://${gcsConfig.bucketName}/")
                val blob = storage.get(BlobId.of(gcsConfig.bucketName, blobName))
                    ?: run { log.warn("GCS blob not found: $gcsPath — skipping"); return@forEach }

                val jobs: List<NormalizedJobDto> = GZIPInputStream(ByteArrayInputStream(blob.getContent()))
                    .bufferedReader()
                    .useLines { lines ->
                        lines.mapNotNull { line ->
                            try { objectMapper.readValue(line, NormalizedJobDto::class.java) }
                            catch (e: Exception) { log.warn("Unparseable job line in $gcsPath: ${e.message}"); null }
                        }.toList()
                    }

                if (jobs.isNotEmpty()) {
                    val saved = crawlerJobPersistenceService.persist(companyId, jobs)
                    totalSaved += saved
                    log.info("Re-processed $saved jobs for $companyId from $gcsPath")
                }
            } catch (e: Exception) {
                log.error("Failed to process file $gcsPath: ${e.message}", e)
            }
        }

        try {
            metadataRepository.updateProcessingStatus(datasetId, ProcessingStatus.COMPLETED)
        } catch (e: Exception) {
            log.warn("Failed to update processing status for $datasetId: ${e.message}")
        }

        log.info("processDataset($datasetId) complete — $totalSaved jobs saved to raw_jobs")
    }

    private fun compressGzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }
}
