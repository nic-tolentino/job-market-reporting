package com.techmarket.api

import com.techmarket.config.ApifyProperties
import com.techmarket.persistence.job.JobRepository
import com.techmarket.service.CloudTasksService
import com.techmarket.service.JobHealthCheckService
import com.techmarket.sync.CompanySyncService
import com.techmarket.sync.JobDataSyncService
import com.techmarket.sync.TechRoleClassifier
import com.techmarket.util.CloudTasksConstants
import com.techmarket.util.HealthCheckConstants.Endpoints.ADMIN_HEALTH_CHECK_RUN
import com.techmarket.util.HealthCheckConstants.Endpoints.ADMIN_HEALTH_CHECK_STATS
import com.techmarket.util.HealthCheckConstants.UrlStatus.ACTIVE
import com.techmarket.util.HealthCheckConstants.UrlStatus.UNKNOWN
import com.techmarket.util.HealthCheckConstants.UrlStatus.isClosed
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin")
class AdminController(
        private val cloudTasksService: CloudTasksService,
        private val apifyProperties: ApifyProperties,
        private val jobRepository: JobRepository,
        private val techRoleClassifier: TechRoleClassifier,
        private val companySyncService: CompanySyncService,
        private val jobDataSyncService: JobDataSyncService,
        private val healthCheckService: JobHealthCheckService
) {
    private val log = LoggerFactory.getLogger(AdminController::class.java)

    @PostMapping("/reprocess-jobs")
    fun reprocessJobs(
            @RequestHeader("x-apify-signature", required = false) providedSecret: String?
    ): ResponseEntity<String> {
        // Reuse the webhook secret for basic admin protection
        val expectedSecret = apifyProperties.webhookSecret
        if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
            log.warn("Unauthorized admin attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        log.info("Admin triggered reprocessing of historical job records.")
        // For the current MVP volume, running synchronously is fine.
        // For thousands of rows, this should be moved to a Cloud Tasks queue.
        try {
            jobDataSyncService.reprocessHistoricalData()
            return ResponseEntity.ok("Historical data reprocessing completed.")
        } catch (e: Exception) {
            log.error("Failed to reprocess jobs", e)
            return ResponseEntity.internalServerError().body("Error: ${e.message}")
        }
    }

    @PostMapping("/trigger-sync")
    fun triggerSync(
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            datasetId: String?,
            @RequestHeader("x-apify-signature", required = false) providedSecret: String?
    ): ResponseEntity<Any> {
        val expectedSecret = apifyProperties.webhookSecret
        if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
            log.warn("Unauthorized admin attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Unauthorized"))
        }

        val effectiveId = datasetId ?: apifyProperties.datasetId

        if (effectiveId.isNullOrBlank()) {
            return ResponseEntity.badRequest()
                    .body(
                            mapOf("error" to "Error: Provide a datasetId as a query param (?datasetId=...) or configure apify.dataset-id in application.yml")
                    )
        }

        log.info("Admin triggered manual sync for dataset: $effectiveId")
        
        // Queue task for background processing via Cloud Tasks
        val correlationId = UUID.randomUUID().toString()
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = effectiveId,
            source = CloudTasksConstants.Source.MANUAL,
            country = null,
            triggeredBy = CloudTasksConstants.TriggeredBy.ADMIN,
            correlationId = correlationId
        )

        val taskName = cloudTasksService.queueSyncTask(taskPayload)

        return ResponseEntity.ok(mapOf(
            "status" to "queued",
            "taskName" to taskName,
            "correlationId" to correlationId,
            "message" to "Sync task queued for background processing"
        ))
    }
    @PostMapping("/sync-companies")
    fun syncCompanies(
            @RequestHeader("x-apify-signature", required = false) providedSecret: String?
    ): ResponseEntity<String> {
        val expectedSecret = apifyProperties.webhookSecret
        if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
            log.warn("Unauthorized admin attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        log.info("Admin triggered manual sync for companies manifest.")
        try {
            companySyncService.syncFromManifest()
            return ResponseEntity.ok("Manual Company Manifest Sync executed")
        } catch (e: Exception) {
            log.error("Failed to trigger manual companies sync", e)
            return ResponseEntity.internalServerError().body("Error: ${e.message}")
        }
    }

    @PostMapping("/audit-classifier")
    fun auditClassifier(
            @RequestHeader("x-apify-signature", required = false) providedSecret: String?
    ): ResponseEntity<Any> {
        val expectedSecret = apifyProperties.webhookSecret
        if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
            log.warn("Unauthorized admin attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Unauthorized"))
        }

        log.info("Admin triggered classifier audit for all Silver layer jobs.")
        try {
            val allJobs = jobRepository.getAllJobs()
            val totalJobs = allJobs.size

            val techJobs = mutableListOf<com.techmarket.persistence.model.JobRecord>()
            val filteredJobs = mutableListOf<com.techmarket.persistence.model.JobRecord>()

            allJobs.forEach { job ->
                val isTech =
                        techRoleClassifier.isTechRole(
                                job.title,
                                job.jobFunction,
                                job.description ?: ""
                        )
                if (isTech) {
                    techJobs.add(job)
                } else {
                    filteredJobs.add(job)
                }
            }

            val filteredDetails =
                    filteredJobs.map { job ->
                        mapOf(
                                "jobId" to job.jobId,
                                "title" to job.title,
                                "company" to job.companyName,
                                "jobFunction" to job.jobFunction
                        )
                    }

            val response =
                    mapOf(
                            "totalJobs" to totalJobs,
                            "techJobs" to techJobs.size,
                            "filteredJobs" to filteredJobs.size,
                            "filteredDetails" to filteredDetails
                    )

            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            log.error("Failed to audit classifier", e)
            return ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }

    /**
     * Manually trigger a health check (for testing/debugging).
     */
    @PostMapping(ADMIN_HEALTH_CHECK_RUN)
    suspend fun runHealthCheck(): ResponseEntity<Map<String, Any>> {
        val summary = healthCheckService.runHealthCheck()

        return ResponseEntity.ok(mapOf(
            "status" to "completed",
            "summary" to mapOf(
                "totalChecked" to summary.totalChecked,
                "activeCount" to summary.activeCount,
                "closedCount" to summary.closedCount,
                "unverifiedCount" to summary.unverifiedCount,
                "avgResponseTimeMs" to summary.avgResponseTimeMs,
                "durationSeconds" to summary.durationSeconds
            )
        ))
    }

    /**
     * Get health check statistics.
     */
    @GetMapping(ADMIN_HEALTH_CHECK_STATS)
    fun getHealthCheckStats(): ResponseEntity<Map<String, Any>> {
        val allJobs = jobRepository.getAllJobs()
        val totalActiveJobs = allJobs.size
        val jobsCheckedToday = allJobs.count { it.urlLastChecked != null }
        val jobsNeedingCheck = allJobs.count {
            it.urlStatus == null ||
            it.urlStatus == UNKNOWN ||
            it.urlStatus == ACTIVE
        }
        val closedJobs = allJobs.count { isClosed(it.urlStatus) }
        val failureRate = if (totalActiveJobs > 0) {
            closedJobs.toDouble() / totalActiveJobs
        } else 0.0

        return ResponseEntity.ok(mapOf(
            "totalActiveJobs" to totalActiveJobs,
            "jobsCheckedToday" to jobsCheckedToday,
            "jobsNeedingCheck" to jobsNeedingCheck,
            "closedJobsCount" to closedJobs,
            "averageFailureRate" to failureRate
        ))
    }
}
