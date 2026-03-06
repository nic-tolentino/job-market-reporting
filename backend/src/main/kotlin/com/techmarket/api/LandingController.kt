package com.techmarket.api

import com.techmarket.api.model.LandingPageDto
import com.techmarket.persistence.analytics.AnalyticsRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/landing")
class LandingController(private val analyticsRepository: AnalyticsRepository) {

    @Cacheable(value = [CacheConstants.CACHE_LANDING], key = CacheConstants.COUNTRY_ONLY_KEY)
    @GetMapping
    fun getLandingPageData(@RequestParam(required = false) country: String?): LandingPageDto {
        return analyticsRepository.getLandingPageData(country)
    }
}
