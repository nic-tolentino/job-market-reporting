package com.techmarket.persistence.ingestion

import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import java.io.InputStream
import java.time.Instant

/**
 * Repository for Bronze layer storage operations.
 * Supports both GCS file storage and metadata indexing.
 */
interface BronzeRepository {
    /**
     * Save raw ingestion data to GCS as compressed files.
     * @param manifest The metadata manifest with file paths
     * @param dataChunks The raw JSON data chunks to write (one per file in manifest.files)
     * @return The saved manifest with confirmed GCS file paths
     */
    fun saveIngestion(manifest: BronzeIngestionManifest, dataChunks: List<ByteArray>): BronzeIngestionManifest

    /**
     * Retrieve a specific ingestion manifest by dataset ID.
     */
    fun getManifest(datasetId: String): BronzeIngestionManifest?

    /**
     * Check if a dataset has already been ingested.
     */
    fun isDatasetIngested(datasetId: String): Boolean

    /**
     * List all manifests for historical reprocessing.
     * Optionally filtered by source and date range.
     */
    fun listManifests(
        source: String? = null,
        fromDate: Instant? = null,
        toDate: Instant? = null
    ): List<BronzeIngestionManifest>

    /**
     * Read raw JSON data from a GCS file.
     */
    fun readFile(filePath: String): InputStream

    /**
     * Update the processing status for a dataset.
     * @return true if successful, false otherwise
     */
    fun updateProcessingStatus(datasetId: String, status: ProcessingStatus): Boolean
}
