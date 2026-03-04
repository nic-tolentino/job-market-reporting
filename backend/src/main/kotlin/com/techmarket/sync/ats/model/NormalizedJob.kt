package com.techmarket.sync.ats.model

/**
 * A unified intermediate representation of a job posting harvested from any ATS. This is the common
 * format that all [com.techmarket.sync.ats.AtsNormalizer] implementations produce.
 */
data class NormalizedJob(
        val platformId: String,
        val source: String, // e.g., "Greenhouse", "Lever"
        val title: String?,
        val companyName: String,
        val location: String?, // Raw string to be parsed by RawJobDataParser
        val descriptionHtml: String?,
        val descriptionText: String?,
        val salaryMin: Int?,
        val salaryMax: Int?,
        val salaryCurrency: String?,
        val employmentType: String?,
        val seniorityLevel: String?, // Pre-parsed if available natively in ATS
        val workModel:
                String?, // Pre-parsed if available natively in ATS (e.g., "Remote", "On-site")
        val department: String?,
        val postedAt: String?,
        val firstPublishedAt: String?,
        val applyUrl: String?,
        val platformUrl: String?,
        val rawPayload: String // The original JSON blob for the Bronze layer audit trail
)
