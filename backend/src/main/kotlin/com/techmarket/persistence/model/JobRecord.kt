package com.techmarket.persistence.model

import java.time.Instant
import java.time.LocalDate

data class JobRecord(
        // Parallel lists: each index corresponds to one location/posting for this deduplicated
        // role.
        val jobIds: List<String>, // original LinkedIn job IDs, one per location
        val applyUrls: List<String>, // apply URLs, one per location (empty if unavailable)
        val links: List<String>, // source URLs, one per location (empty if unavailable)
        val locations: List<String>, // location strings, one per location
        // Shared across all locations (taken from first non-null value in the group)
        val companyId: String,
        val companyName: String,
        val source: String,
        val country: String,
        val city: String,
        val stateRegion: String,
        val title: String,
        val seniorityLevel: String,
        val technologies: List<String>,
        val salaryMin: Int?,
        val salaryMax: Int?,
        val postedDate: LocalDate?,
        val benefits: List<String>,
        val employmentType: String?,
        val workModel: String?,
        val jobFunction: String?,
        val description: String?,
        val ingestedAt: Instant
)
