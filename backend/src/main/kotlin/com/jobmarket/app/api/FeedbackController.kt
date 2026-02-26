package com.jobmarket.app.api

import com.jobmarket.app.api.model.FeedbackRequest
import com.jobmarket.app.persistence.JobRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/feedback")
class FeedbackController(private val jobRepository: JobRepository) {

    private val log = LoggerFactory.getLogger(FeedbackController::class.java)

    @PostMapping
    fun submitFeedback(@RequestBody request: FeedbackRequest): ResponseEntity<Void> {
        if (request.message.isNotBlank()) {
            jobRepository.saveFeedback(request.context, request.message)
        }
        return ResponseEntity.accepted().build()
    }
}
