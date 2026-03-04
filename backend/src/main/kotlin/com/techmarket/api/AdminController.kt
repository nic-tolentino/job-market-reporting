package com.techmarket.api

import com.techmarket.config.ApifyProperties
import com.techmarket.persistence.job.JobRepository
import com.techmarket.sync.JobDataSyncService
import com.techmarket.sync.TechRoleClassifier
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class AdminController(
        private val jobDataSyncService: JobDataSyncService,
        private val apifyProperties: ApifyProperties,
        private val jobRepository: JobRepository,
        private val techRoleClassifier: TechRoleClassifier
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
    ): ResponseEntity<String> {
        val expectedSecret = apifyProperties.webhookSecret
        if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
            log.warn("Unauthorized admin attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        val effectiveId = datasetId ?: apifyProperties.datasetId

        if (effectiveId.isNullOrBlank()) {
            return ResponseEntity.badRequest()
                    .body(
                            "Error: Provide a datasetId as a query param (?datasetId=...) or configure apify.dataset-id in application.yml"
                    )
        }

        log.info("Admin triggered manual sync for dataset: $effectiveId")
        try {
            jobDataSyncService.runDataSync(effectiveId)
            return ResponseEntity.ok("Manual Data Sync Pipeline executed for dataset: $effectiveId")
        } catch (e: Exception) {
            log.error("Failed to trigger manual sync", e)
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
}
