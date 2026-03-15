package com.techmarket.api

import com.techmarket.persistence.analytics.AnalyticsRepository
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.techmarket.util.HealthCheckConstants.UrlStatus.isClosed
import java.time.Instant

/**
 * Controller for admin-only analytics and feedback data.
 * Aggregates high-level metrics (Total Jobs, Active Jobs) and ingestion history.
 * 
 * Authentication is handled globally by [com.techmarket.config.AdminTokenInterceptor].
 */
@RestController
@RequestMapping("/api/admin/analytics")
class AnalyticsAdminController(
    private val analyticsRepository: AnalyticsRepository,
    private val jobRepository: JobRepository,
    private val ingestionRepository: IngestionMetadataRepository
) {

    /**
     * Retrieves a summary of analytics data including global statistics, job counts,
     * recent ingestion manifests, top technologies, and top companies.
     *
     * @return A [ResponseEntity] containing a map of summary data.
     */
    @GetMapping("/summary")
    fun getSummary(): ResponseEntity<Any> {
        val landingData = analyticsRepository.getLandingPageData()
        val totalJobs = jobRepository.count()
        val activeJobs = jobRepository.countActive()
        val recentManifests = ingestionRepository.listManifests(null, Instant.now().minusSeconds(86400 * 7), null)

        val report = mapOf(
            "globalStats" to landingData.globalStats,
            "totalJobsInPersistence" to totalJobs,
            "activeJobs" to activeJobs,
            "recentIngestions" to recentManifests.take(5).map {
                mapOf(
                    "datasetId" to it.datasetId,
                    "recordCount" to it.recordCount,
                    "status" to it.processingStatus,
                    "ingestedAt" to it.ingestedAt
                )
            },
            "topTech" to landingData.topTech,
            "topCompanies" to landingData.topCompanies
        )

        return ResponseEntity.ok(report)
    }

    @GetMapping("/feedback")
    fun getFeedback(): ResponseEntity<Any> {
        return ResponseEntity.ok(analyticsRepository.getAllFeedback())
    }
}
