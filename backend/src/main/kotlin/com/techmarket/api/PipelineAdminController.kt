package com.techmarket.api

import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import com.techmarket.persistence.job.JobRepository
import com.techmarket.service.CloudTasksService
import com.techmarket.service.JobHealthCheckService
import com.techmarket.sync.CompanySyncService
import com.techmarket.sync.JobDataSyncService
import com.techmarket.util.CloudTasksConstants
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Admin endpoints for pipeline observability and management.
 */
@RestController
@RequestMapping("/api/admin/pipeline")
class PipelineAdminController(
    private val cloudTasksService: CloudTasksService,
    private val ingestionRepository: IngestionMetadataRepository,
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository,
    private val jobDataSyncService: JobDataSyncService,
    private val companySyncService: CompanySyncService,
    private val healthCheckService: JobHealthCheckService
) {
    private val log = LoggerFactory.getLogger(PipelineAdminController::class.java)

    @GetMapping("/queue")
    fun getQueueStats(): ResponseEntity<*> {
        return ResponseEntity.ok(cloudTasksService.getQueueMetadata())
    }

    @GetMapping("/history")
    fun getIngestionHistory(): ResponseEntity<*> {
        return try {
            val manifests = ingestionRepository.listManifests(null, null, null, 100)
            val results = manifests.map { manifest ->
                mapOf(
                    "eventId" to (manifest.metadataId ?: manifest.datasetId),
                    "datasetId" to manifest.datasetId,
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

    @PostMapping("/reprocess")
    fun reprocessAll(): ResponseEntity<*> {
        log.info("Admin triggered background reprocessing of all data")
        return try {
            jobDataSyncService.reprocessHistoricalData()
            ResponseEntity.ok(mapOf("status" to "ok", "message" to "Reprocessing complete"))
        } catch (e: Exception) {
            log.error("Reprocessing failed: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/ingest")
    fun ingestDataset(@RequestParam("datasetId") datasetId: String): ResponseEntity<*> {
        log.info("Admin triggered manual ingestion for dataset: $datasetId")
        val correlationId = UUID.randomUUID().toString()
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = datasetId,
            source = CloudTasksConstants.Source.MANUAL,
            country = null,
            triggeredBy = CloudTasksConstants.TriggeredBy.ADMIN,
            correlationId = correlationId
        )
        val taskName = cloudTasksService.queueSyncTask(taskPayload)
        return ResponseEntity.ok(mapOf(
            "status" to "queued",
            "taskName" to taskName,
            "correlationId" to correlationId
        ))
    }

    @PostMapping("/wipe-silver")
    fun wipeSilver(): ResponseEntity<*> {
        log.info("Admin triggered WIPE of Silver Layer tables")
        return try {
            jobRepository.deleteAllJobs()
            companyRepository.deleteAllCompanies()
            ResponseEntity.ok(mapOf("status" to "ok", "message" to "Silver tables wiped and recreated"))
        } catch (e: Exception) {
            log.error("Wipe failed: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/datasets/{datasetId}")
    fun deleteDataset(@PathVariable datasetId: String): ResponseEntity<*> {
        log.info("Admin triggered deletion of dataset: $datasetId")
        val success = jobDataSyncService.deleteDataset(datasetId)
        return if (success) {
            ResponseEntity.ok(mapOf("status" to "ok", "message" to "Dataset deleted"))
        } else {
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to delete dataset"))
        }
    }

    @PostMapping("/sync-companies")
    fun syncCompanies(): ResponseEntity<*> {
        log.info("Admin triggered company manifest sync")
        return try {
            companySyncService.syncFromManifest()
            ResponseEntity.ok(mapOf("status" to "ok", "message" to "Company manifest sync completed"))
        } catch (e: Exception) {
            log.error("Company sync failed: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/run-health-check")
    suspend fun runHealthCheck(): ResponseEntity<*> {
        log.info("Admin triggered manual health check")
        return try {
            val summary = healthCheckService.runHealthCheck()
            ResponseEntity.ok(mapOf("status" to "ok", "summary" to summary))
        } catch (e: Exception) {
            log.error("Health check failed: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }
}
