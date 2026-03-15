package com.techmarket.api

import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import com.techmarket.service.CloudTasksService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin endpoints for pipeline observability.
 */
@RestController
@RequestMapping("/api/admin/pipeline")
class PipelineAdminController(
    private val cloudTasksService: CloudTasksService,
    private val ingestionRepository: IngestionMetadataRepository
) {
    private val log = LoggerFactory.getLogger(PipelineAdminController::class.java)

    @GetMapping("/queue")
    fun getQueueStats(): ResponseEntity<*> {
        return ResponseEntity.ok(cloudTasksService.getQueueMetadata())
    }

    @GetMapping("/history")
    fun getIngestionHistory(): ResponseEntity<*> {
        return try {
            val manifests = ingestionRepository.listManifests(null, null, null, 50)
            val results = manifests.map { manifest ->
                mapOf(
                    "eventId" to (manifest.metadataId ?: manifest.datasetId),
                    "startedAt" to manifest.ingestedAt.toEpochMilli(),
                    "source" to manifest.source,
                    "count" to manifest.recordCount,
                    "status" to manifest.processingStatus.name,
                    "type" to "INGESTION"
                )
            }
            ResponseEntity.ok(mapOf("data" to results))
        } catch (e: Exception) {
            log.error("Failed to fetch ingestion history: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }
}
