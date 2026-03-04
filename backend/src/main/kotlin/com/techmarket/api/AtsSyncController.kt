package com.techmarket.api

import com.techmarket.config.ApifyProperties
import com.techmarket.sync.AtsJobDataSyncService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Internal API for triggering ATS data synchronizations. Usually invoked by Cloud Tasks or Admin
 * UI.
 */
@RestController
@RequestMapping("/api/internal")
class AtsSyncController(
        private val atsJobDataSyncService: AtsJobDataSyncService,
        private val apifyProperties: ApifyProperties
) {
    private val log = LoggerFactory.getLogger(AtsSyncController::class.java)

    /** Triggers a manual sync for a specific company's ATS data. */
    @PostMapping("/ats-sync")
    fun triggerAtsSync(
            @RequestParam companyId: String,
            @RequestHeader("x-internal-secret", required = false) providedSecret: String?
    ): ResponseEntity<String> {
        checkAuthorized(providedSecret)?.let {
            log.warn("Unauthorized ATS sync attempt for $companyId. Invalid or missing secret.")
            return it
        }

        log.info("ATS Sync: Manual trigger received for company: $companyId")
        return try {
            atsJobDataSyncService.syncCompany(companyId)
            ResponseEntity.ok("ATS Data Sync completed for company: $companyId")
        } catch (e: Exception) {
            log.error("ATS Sync: Failed to execute manual sync for $companyId", e)
            ResponseEntity.internalServerError().body("Error: ${e.message}")
        }
    }

    @PostMapping("/ats-sync-all")
    fun triggerAllAtsSyncs(
            @RequestHeader("x-internal-secret", required = false) providedSecret: String?
    ): ResponseEntity<String> {
        checkAuthorized(providedSecret)?.let {
            return it
        }

        log.info("ATS Sync: Manual trigger received for ALL enabled companies")
        return try {
            atsJobDataSyncService.syncAllEnabled()
            ResponseEntity.ok("ATS Global Data Sync completed")
        } catch (e: Exception) {
            log.error("ATS Sync: Failed to execute global sync", e)
            ResponseEntity.internalServerError().body("Error: ${e.message}")
        }
    }

    private fun checkAuthorized(providedSecret: String?): ResponseEntity<String>? {
        val expectedSecret = apifyProperties.webhookSecret
        if (expectedSecret.isNullOrBlank() || providedSecret != expectedSecret) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }
        return null
    }
}
