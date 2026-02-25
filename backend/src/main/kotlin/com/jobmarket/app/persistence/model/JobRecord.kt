package com.jobmarket.app.persistence.model

import java.time.Instant
import java.time.LocalDate

data class JobRecord(
        // TODO: Future-proofing - As we add more sources (Seek, Workday), jobId alone may not be
        // globally unique.
        // We should eventually transition to a composite key (jobId + source) or generate a
        // deterministic UUID.
        val jobId: String,
        val companyId: String,
        val companyName: String,
        val source: String,
        val country: String,
        val title: String,
        val location: String,
        val seniorityLevel: String,
        val technologies: List<String>,
        val salaryMin: Int?,
        val salaryMax: Int?,
        val postedDate: LocalDate?,
        val benefits: List<String>?,
        val employmentType: String?,
        val jobFunction: String?,
        val applyUrl: String?,
        val rawLocation: String?,
        val rawSeniorityLevel: String?,
        val ingestedAt: Instant
)
