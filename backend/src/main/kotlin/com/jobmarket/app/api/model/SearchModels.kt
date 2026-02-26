package com.jobmarket.app.api.model

data class SearchSuggestionDto(
        val type: String, // "TECHNOLOGY" or "COMPANY"
        val id: String, // The slug/ID to navigate to
        val name: String // The display name
)

data class SearchSuggestionsResponse(val suggestions: List<SearchSuggestionDto>)

data class FeedbackRequest(val context: String?, val message: String)
