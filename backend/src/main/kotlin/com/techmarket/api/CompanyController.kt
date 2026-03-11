package com.techmarket.api

import com.techmarket.api.model.CompanyDetailsDto
import com.techmarket.api.model.CompanyListingItemDto
import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.persistence.company.CompanyRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

    @GetMapping("/listing")
    fun getCompanyListing(
        @RequestParam(required = false) visaSponsorship: Boolean?,
        @RequestParam(required = false) country: String?
    ): ResponseEntity<List<CompanyListingItemDto>> {
        val visaOnly = visaSponsorship == true
        val companies = companyRepository.getCompanyListing(visaOnly, country)
        
        // Map from Persistence model (CompanyListingItem) to API DTO (CompanyListingItemDto)
        // Note: Decoupling persistence from API layer to allow future divergence.
        val dtos = companies.map { item ->
            CompanyListingItemDto(
                id = item.id,
                name = item.name,
                logo = item.logo,
                visaSponsorship = item.visaSponsorship,
                activeRoles = item.activeRoles
            )
        }
        
        return ResponseEntity.ok(dtos)
    }
}
