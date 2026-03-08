package com.techmarket.persistence.model

import com.techmarket.model.NormalizedSalary
import java.time.Instant
import java.time.LocalDate

data class JobRecord(
        val jobId: String, // Stable semantic ID: {company}.{country}.{title}.{date}
        val platformJobIds: List<String>, // original source IDs (e.g. LinkedIn IDs)
        val applyUrls: List<String>,
        val platformLinks: List<String>,
        val locations: List<String>,
        val companyId: String,
        val companyName: String,
        val source: String,
        val country: String,
        val city: String,
        val stateRegion: String,
        val title: String,
        val seniorityLevel: String,
        val technologies: List<String>,
        val salaryMin: NormalizedSalary?,
        val salaryMax: NormalizedSalary?,
        val postedDate: LocalDate?,
        val benefits: List<String>,
        val employmentType: String?,
        val workModel: String?,
        val jobFunction: String?,
        val description: String?,
        val lastSeenAt: Instant
)
