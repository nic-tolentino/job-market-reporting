package com.techmarket.persistence.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * Implementation of [BronzeRepository] for Google Cloud Storage.
 * Stores raw ingestion data as compressed files in GCS with metadata in BigQuery.
 */
@Repository
class BronzeGcsRepository(
    private val gcsConfig: GcsConfig,
    private val metadataRepository: IngestionMetadataRepository,
    private val objectMapper: ObjectMapper,
    private val storage: Storage = StorageOptions.getDefaultInstance().service
) : BronzeRepository {

    private val log = LoggerFactory.getLogger(BronzeGcsRepository::class.java)

    /**
     * Save raw ingestion data to GCS as compressed files.
     * @param manifest The metadata manifest with file paths
     * @param dataChunks The raw JSON data chunks to write (one per file in manifest.files)
     */
    override fun saveIngestion(
        manifest: BronzeIngestionManifest,
        dataChunks: List<ByteArray>
    ): BronzeIngestionManifest {
        log.info("Saving Bronze ingestion: datasetId=${manifest.datasetId}, files=${manifest.fileCount}")

        val uploadedFiles = mutableListOf<String>()

        try {
            // Upload files to GCS - dataChunks must match manifest.files count
            uploadedFiles.addAll(manifest.files.mapIndexed { index, gcsPath ->
                uploadToGcs(gcsPath, dataChunks[index])
                gcsPath
            })

            // Save manifest metadata to BigQuery
            val savedManifest = manifest.copy(files = uploadedFiles)
            metadataRepository.saveManifest(savedManifest)

            log.info("Bronze ingestion saved successfully: ${savedManifest.files.size} files")
            return savedManifest
        } catch (e: Exception) {
            log.error("Failed to save Bronze ingestion: ${e.message}. Cleaning up orphaned GCS files...")

            // Cleanup: Delete any GCS files that were already uploaded
            uploadedFiles.forEach { gcsPath ->
                try {
                    deleteFromGcs(gcsPath)
                    log.debug("Cleaned up orphaned GCS file: $gcsPath")
                } catch (cleanupEx: Exception) {
                    log.warn("Failed to cleanup orphaned GCS file $gcsPath: ${cleanupEx.message}")
                }
            }

            throw e
        }
    }

    private fun uploadToGcs(gcsPath: String, data: ByteArray) {
        val bucket = storage.get(gcsConfig.bucketName)
        val blobName = gcsPath.removePrefix("gs://${gcsConfig.bucketName}/")
        val blobId = BlobId.of(gcsConfig.bucketName, blobName)
        val blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType("application/json")
            .apply {
                if (gcsConfig.compressionEnabled) {
                    setContentEncoding("gzip")
                }
            }
            .build()

        val uploadData = if (gcsConfig.compressionEnabled) compressGzip(data) else data

        bucket.create(blobInfo.name, uploadData)
        log.debug("Uploaded file to GCS: $gcsPath (${uploadData.size} bytes)")
    }

    private fun deleteFromGcs(gcsPath: String): Boolean {
        return try {
            val blobName = gcsPath.removePrefix("gs://${gcsConfig.bucketName}/")
            val blobId = com.google.cloud.storage.BlobId.of(gcsConfig.bucketName, blobName)
            val deleted = storage.delete(blobId)
            log.debug("Deleted GCS file: $gcsPath, success=$deleted")
            deleted
        } catch (e: Exception) {
            log.warn("Failed to delete GCS file $gcsPath: ${e.message}")
            false
        }
    }

    override fun getManifest(datasetId: String): BronzeIngestionManifest? {
        return metadataRepository.getManifest(datasetId)
    }

    override fun isDatasetIngested(datasetId: String): Boolean {
        return metadataRepository.isDatasetIngested(datasetId)
    }

    override fun listManifests(
        source: String?,
        fromDate: java.time.Instant?,
        toDate: java.time.Instant?
    ): List<BronzeIngestionManifest> {
        return metadataRepository.listManifests(source, fromDate, toDate)
    }

    override fun updateProcessingStatus(datasetId: String, status: ProcessingStatus): Boolean {
        return metadataRepository.updateProcessingStatus(datasetId, status)
    }

    override fun readFile(filePath: String): InputStream {
        val bucket = storage.get(gcsConfig.bucketName)
        val blobName = filePath.removePrefix("gs://${gcsConfig.bucketName}/")
        val blob = bucket.get(blobName)

        // Return decompressed stream for transparent reading (if compression enabled)
        val data = blob.getContent()
        return if (gcsConfig.compressionEnabled) {
            GZIPInputStream(ByteArrayInputStream(data))
        } else {
            ByteArrayInputStream(data)
        }
    }

    private fun compressGzip(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzipOutputStream ->
            gzipOutputStream.write(data)
        }
        return outputStream.toByteArray()
    }
}
