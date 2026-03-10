package com.techmarket.persistence.ingestion

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Configuration for GCS bucket access.
 */
@Component
data class GcsConfig(
    @Value("\${gcs.bronze.bucket-name}")
    val bucketName: String,
    @Value("\${gcs.bronze.project-id}")
    val projectId: String,
    @Value("\${gcs.bronze.compression-enabled:true}")
    val compressionEnabled: Boolean = true
)
