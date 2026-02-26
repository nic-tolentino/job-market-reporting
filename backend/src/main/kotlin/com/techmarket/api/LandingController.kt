package com.techmarket.api

import com.techmarket.api.model.LandingPageDto
import com.techmarket.persistence.JobRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/landing")
class LandingController(private val jobRepository: JobRepository) {

    @Cacheable("landing")
    @GetMapping
    fun getLandingPageData(): LandingPageDto {
        return jobRepository.getLandingPageData()
    }
}
