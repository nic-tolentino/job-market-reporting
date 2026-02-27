package com.techmarket.api

import com.techmarket.api.model.FeedbackRequest
import com.techmarket.persistence.analytics.AnalyticsRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(private val analyticsRepository: AnalyticsRepository) {

    private val log = LoggerFactory.getLogger(FeedbackController::class.java)

    @PostMapping
    fun submitFeedback(@RequestBody request: FeedbackRequest): ResponseEntity<Void> {
        if (request.message.isNotBlank()) {
            analyticsRepository.saveFeedback(request.context, request.message)
        }
        return ResponseEntity.accepted().build()
    }
}
