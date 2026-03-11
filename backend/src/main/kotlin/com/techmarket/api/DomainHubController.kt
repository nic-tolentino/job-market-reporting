package com.techmarket.api

import com.techmarket.model.TechCategory
import com.techmarket.dto.*
import com.techmarket.persistence.analytics.InsightsBigQueryRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for technology domain hub pages.
 * Provides aggregated insights and data for each technology category.
 */
@RestController
@RequestMapping("/api/hubs")
class DomainHubController(
    private val repository: InsightsBigQueryRepository
) {

    /**
     * Retrieves complete domain hub data for a technology category.
     * @param category the category slug (e.g., "frontend", "mobile")
     * @param country optional country filter
     * @return DomainHubDto with aggregated metrics, technologies, jobs, and companies
     */
    @GetMapping("/{category}")
    fun getDomainHub(
        @PathVariable category: String,
        @RequestParam(required = false) country: String?
    ): ResponseEntity<DomainHubDto> {
        val techCategory = TechCategory.fromSlug(category)

        // NOTE: There is slight redundancy here astrends.totalJobs also returns job counts.
        // We use repository.countJobsByCategory as the primary source for the top-level metric 
        // to ensure consistency with the Hub Listing page counts.
        val technologies = repository.getTechnologiesByCategory(techCategory, country)
        val jobs = repository.getJobsByCategory(techCategory, country)
        val companies = repository.getCompaniesByCategory(techCategory, country)
        val trends = repository.getCategoryTrends(techCategory, country)
        val totalJobsCount = repository.countJobsByCategory(techCategory, country)

        val dto = DomainHubDto(
            category = CategoryDto(
                slug = techCategory.slug,
                displayName = techCategory.displayName,
                description = techCategory.description
            ),
            totalJobs = totalJobsCount,
            activeCompanies = companies.size,
            technologies = technologies,
            topCompanies = companies.take(10),
            recentJobs = jobs.take(20),
            trends = trends,
            marketShare = trends.marketShare,
            growthRate = trends.growthRate
        )

        return ResponseEntity.ok(dto)
    }

    /**
     * Lists all domain hubs with summary statistics.
     * @param country optional country filter
     * @return list of DomainSummaryDto for all categories
     */
    @GetMapping
    fun getAllDomainHubs(
        @RequestParam(required = false) country: String?
    ): ResponseEntity<List<DomainSummaryDto>> {
        val summaries = repository.getAllCategorySummaries(country)
        return ResponseEntity.ok(summaries)
    }
}

