package com.techmarket.persistence.analytics

import com.techmarket.model.TechCategory
import com.techmarket.dto.*

/**
 * Repository interface for fetching advanced market insights from BigQuery.
 * Focuses on technology category (domain) aggregations and trends.
 */
interface InsightsBigQueryRepository {
    
    /**
     * Fetches all technologies belonging to a specific category with their metrics.
     */
    fun getTechnologiesByCategory(
        category: TechCategory,
        country: String?
    ): List<TechnologyDto>

    /**
     * Fetches recent jobs belonging to any technology in a specific category.
     */
    fun getJobsByCategory(
        category: TechCategory,
        country: String?
    ): List<JobRoleDto>

    /**
     * Fetches companies hiring in a specific technology category.
     */
    fun getCompaniesByCategory(
        category: TechCategory,
        country: String?
    ): List<CompanyDto>

    /**
     * Calculates market trends for a technology category.
     */
    fun getCategoryTrends(
        category: TechCategory,
        country: String?
    ): CategoryTrendsDto

    /**
     * Counts unique jobs in a category.
     */
    fun countJobsByCategory(
        category: TechCategory,
        country: String?
    ): Int

    /**
     * Counts unique companies hiring in a category.
     */
    fun countCompaniesByCategory(
        category: TechCategory,
        country: String?
    ): Int

    /**
     * Returns the number of technologies mapped to a category.
     */
    fun countTechnologiesByCategory(
        category: TechCategory
    ): Int

    /**
     * Fetches summaries for all categories in a high-performance batch query.
     * 
     * ARCHITECTURAL NOTE: This method replaces the N+1 pattern where the controller would loop over 
     * categories and call countJobsByCategory/countCompaniesByCategory/getCategoryTrends for each.
     * 
     * IMPLEMENTATION: Uses UNION ALL to aggregate metrics across all categories in a single BigQuery pass.
     * While this technically scans the jobs table multiple times (once per category union), BigQuery's 
     * columnar execution engine makes this significantly faster and more cost-effective than 40+ 
     * individual round-trips from the application.
     * 
     * REDUNDANCY: countJobsByCategory and countCompaniesByCategory remain in the interface for 
     * single-category Hub pages, but the Hub Listing page MUST use this batch method for performance.
     */
    fun getAllCategorySummaries(country: String?): List<DomainSummaryDto>
}
