package com.techmarket.api

import com.techmarket.api.model.SearchSuggestionsResponse
import com.techmarket.persistence.analytics.AnalyticsRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(private val analyticsRepository: AnalyticsRepository) {

    private val log = LoggerFactory.getLogger(SearchController::class.java)

    @Cacheable(value = [CacheConstants.CACHE_SEARCH], key = CacheConstants.SEARCH_KEY, condition = "#term == null")
    @GetMapping("/suggestions")
    fun getSuggestions(
        @RequestParam(required = false) term: String?,
        @RequestParam(required = false) country: String?
    ): SearchSuggestionsResponse {
        // If we have a term, try to find direct matches first
        if (!term.isNullOrBlank()) {
            val results = analyticsRepository.getSearchSuggestions(term, country)
            
            if (results.suggestions.isEmpty()) {
                log.info("Tracking search miss for term: $term")
                analyticsRepository.saveSearchMiss(term)
            }
            
            return results
        }

        // Default: fetch master list (popular suggestions)
        log.info("Fetching master search suggestions list from database")
        return analyticsRepository.getSearchSuggestions(null, country)
    }
}
