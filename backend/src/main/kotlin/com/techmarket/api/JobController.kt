package com.techmarket.api

import com.techmarket.api.model.JobPageDto
import com.techmarket.persistence.JobRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/job")
class JobController(private val jobRepository: JobRepository) {
    private val log = LoggerFactory.getLogger(JobController::class.java)

    @GetMapping("/{jobId}")
    fun getJobDetails(@PathVariable jobId: String): ResponseEntity<JobPageDto> {
        log.info("Fetching job details for ID: {}", jobId)
        try {
            val result = jobRepository.getJobDetails(jobId)
            return if (result != null) {
                ResponseEntity.ok(result)
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            log.error("Failed to fetch job details for ID: $jobId", e)
            return ResponseEntity.internalServerError().build()
        }
    }
}
