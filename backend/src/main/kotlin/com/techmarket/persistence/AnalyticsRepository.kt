package com.techmarket.persistence

import com.techmarket.api.model.LandingPageDto
import com.techmarket.api.model.SearchSuggestionsResponse

interface AnalyticsRepository {
    fun getLandingPageData(): LandingPageDto
    fun getSearchSuggestions(): SearchSuggestionsResponse
    fun saveSearchMiss(term: String)
    fun saveFeedback(context: String?, message: String)
}
