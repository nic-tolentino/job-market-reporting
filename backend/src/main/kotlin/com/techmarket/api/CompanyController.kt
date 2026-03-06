package com.techmarket.api

import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.persistence.company.CompanyRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/company")
class CompanyController(private val companyRepository: CompanyRepository) {

        @Cacheable(value = [CacheConstants.CACHE_COMPANY], key = CacheConstants.COMPANY_KEY)
        @GetMapping("/{companyId}")
        fun getCompanyProfile(
                @PathVariable companyId: String,
                @RequestParam(required = false) country: String?
        ): CompanyProfilePageDto {
                return companyRepository.getCompanyProfile(companyId, country)
        }
}
