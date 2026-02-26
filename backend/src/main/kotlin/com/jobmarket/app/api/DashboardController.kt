package com.jobmarket.app.api

import com.jobmarket.app.api.model.LandingPageDto
import com.jobmarket.app.persistence.JobRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(private val jobRepository: JobRepository) {

    @Cacheable("dashboard")
    @GetMapping("/landing")
    fun getLandingPageData(): LandingPageDto {
        return jobRepository.getLandingPageData()
    }
}
