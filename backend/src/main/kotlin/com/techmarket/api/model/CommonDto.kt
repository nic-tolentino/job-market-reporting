package com.techmarket.api.model

import com.techmarket.model.NormalizedSalary
import java.time.Instant

data class CompanyLeaderboardDto(
        val id: String,
        val name: String,
        val logo: String,
        val activeRoles: Int
)

/**
 * Represents a job role in API responses.
 *
 * @property source The data source for this job (e.g., "LinkedIn", "Company Website", "SEEK")
 * @property lastUpdatedAt When this job was last updated/verified as active
 */
data class JobRoleDto(
        val id: String, // canonical ID (first jobId in the group)
        val title: String,
        val companyId: String,
        val companyName: String,
        val locations: List<String>, // all locations this role is advertised in
        val jobIds: List<String>, // original LinkedIn job IDs per location
        val applyUrls: List<String?>, // apply URLs per location
        val platformLinks: List<String?>, // source URLs per location
        val salaryMin: NormalizedSalary?,
        val salaryMax: NormalizedSalary?,
        val postedDate: String,
        val seniorityLevel: String,
        val technologies: List<String>,
        val source: String,
        val lastUpdatedAt: Instant
)

data class FeedbackRequest(val context: String?, val message: String)

data class FeedbackDto(val context: String?, val message: String, val timestamp: String)
