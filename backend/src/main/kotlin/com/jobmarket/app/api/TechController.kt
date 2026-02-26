package com.jobmarket.app.api

import com.jobmarket.app.api.model.TechDetailsPageDto
import com.jobmarket.app.persistence.JobRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tech")
class TechController(private val jobRepository: JobRepository) {

        @Cacheable("tech")
        @GetMapping("/{techName}")
        fun getTechDetails(@PathVariable techName: String): TechDetailsPageDto {
                return jobRepository.getTechDetails(techName)
        }
}
