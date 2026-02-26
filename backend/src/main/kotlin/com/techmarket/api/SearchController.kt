package com.techmarket.api

import com.techmarket.api.model.SearchSuggestionsResponse
import com.techmarket.persistence.JobRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/search")
class SearchController(private val jobRepository: JobRepository) {

    private val log = LoggerFactory.getLogger(SearchController::class.java)

    @Cacheable("search", condition = "#term == null")
    @GetMapping("/suggestions")
    fun getSuggestions(@RequestParam(required = false) term: String?): SearchSuggestionsResponse {
        if (!term.isNullOrBlank()) {
            log.info("Tracking search miss and returning empty suggestions for term: $term")
            jobRepository.saveSearchMiss(term)
            return SearchSuggestionsResponse(emptyList())
        }

        log.info("Fetching search suggestions from database")
        return jobRepository.getSearchSuggestions()
    }
}
