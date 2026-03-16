package com.techmarket.sync.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Kotlin mirror of the TypeScript NormalizedJob type produced by the crawler-service.
 * Used to deserialize the "jobs" array from a CrawlResponse before mapping to JobRecord.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class NormalizedJobDto(
    val platformId: String?,
    val source: String?,
    val title: String?,
    val companyName: String?,
    val location: String?,
    val descriptionHtml: String?,
    val descriptionText: String?,
    val salaryMin: Double?,
    val salaryMax: Double?,
    val salaryCurrency: String?,
    val employmentType: String?,
    val seniorityLevel: String?,
    val workModel: String?,
    val department: String?,
    val postedAt: String?,
    val applyUrl: String?,
    val platformUrl: String?,
)
