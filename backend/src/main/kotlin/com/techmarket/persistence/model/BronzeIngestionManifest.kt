package com.techmarket.persistence.model

import java.time.Instant

/**
 * Metadata manifest for a Bronze layer ingestion.
 * Stored in BigQuery for querying, with pointers to GCS files.
 */
data class BronzeIngestionManifest(
    val datasetId: String,
    val source: String,  // "apify", "workday", "greenhouse", etc.
    val ingestedAt: Instant,
    val targetCountry: String?,
    val schemaVersion: String = "1.0",
    val recordCount: Int,
    val fileCount: Int,
    val uncompressedSizeBytes: Long,
    val compressedSizeBytes: Long,
    val compressionRatio: Double,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val files: List<String>,  // GCS paths: "gs://bucket/path/to/file.json.gz"
    val metadataId: String? = null  // BigQuery row ID
)

/**
 * Processing status for Bronze layer ingestions.
 * Tracks the progress of data through the pipeline.
 */
enum class ProcessingStatus {
    PENDING,      // Uploaded to GCS, not yet processed to Silver
    COMPLETED,    // Successfully mapped to Silver layer
    FAILED        // Silver layer mapping failed
}
