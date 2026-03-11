package com.techmarket.dto

import java.time.Instant

/**
 * DTO representing a technology with its market metrics.
 */
data class TechnologyDto(
    val name: String,
    val jobCount: Int,
    val companyCount: Int,
    val avgSalary: Double?
)

/**
 * DTO representing a company and its hiring summary.
 */
data class CompanyDto(
    val id: String,
    val name: String,
    val jobCount: Int,
    val technologies: List<String>
)

/**
 * Simplified job role model for domain hub listings.
 */
data class JobRoleDto(
    val id: String,
    val title: String,
    val companyId: String,
    val companyName: String,
    val location: String,
    val country: String,
    val salaryMin: Long,
    val salaryMax: Long,
    val postedDate: Instant,
    val url: String
)

/**
 * Aggregated trend data for a technology category.
 */
data class CategoryTrendsDto(
    val totalJobs: Int,
    val totalCompanies: Int,
    val growthRate: Double,
    val marketShare: Double,
    val last6MonthsJobs: Int,
    val monthlyData: List<MonthlyTrendDto> = emptyList()
)

/**
 * Monthly data point for trend visualization.
 */
data class MonthlyTrendDto(
    val month: String,
    val jobCount: Int,
    val companyCount: Int
)
/**
 * DTO for serializing category information to JSON.
 */
data class CategoryDto(
    val slug: String,
    val displayName: String,
    val description: String
)

/**
 * Complete domain hub data transfer object.
 */
data class DomainHubDto(
    val category: CategoryDto,
    val totalJobs: Int,
    val activeCompanies: Int,
    val technologies: List<TechnologyDto>,
    val topCompanies: List<CompanyDto>,
    val recentJobs: List<JobRoleDto>,
    val trends: CategoryTrendsDto,
    val marketShare: Double,
    val growthRate: Double
)

/**
 * Summary data for domain hub listing.
 */
data class DomainSummaryDto(
    val category: CategoryDto,
    val jobCount: Int,
    val companyCount: Int,
    val techCount: Int,
    val growthRate: Double,
    val marketShare: Double
)
