package com.techmarket.persistence.model

import java.time.Instant

data class RawIngestionRecord(
        val id: String, // Unique ID generated for this ingestion event or derived from source
        val source: String, // e.g., "LinkedIn-Apify"
        val ingestedAt: Instant,
        val rawPayload: String, // The raw JSON string
        val datasetId: String? = null
)
