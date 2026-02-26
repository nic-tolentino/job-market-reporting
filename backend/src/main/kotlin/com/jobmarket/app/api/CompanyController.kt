package com.jobmarket.app.api

import com.jobmarket.app.api.model.CompanyProfilePageDto
import com.jobmarket.app.persistence.JobRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/company")
class CompanyController(private val jobRepository: JobRepository) {

        @Cacheable("company")
        @GetMapping("/{companyId}")
        fun getCompanyProfile(@PathVariable companyId: String): CompanyProfilePageDto {
                return jobRepository.getCompanyProfile(companyId)
        }
}
