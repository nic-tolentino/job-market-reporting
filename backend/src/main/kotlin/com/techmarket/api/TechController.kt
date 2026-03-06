package com.techmarket.api

import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.persistence.tech.TechRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tech")
class TechController(private val techRepository: TechRepository) {

        @Cacheable(value = [CacheConstants.CACHE_TECH], key = CacheConstants.TECH_KEY)
        @GetMapping("/{techName}")
        fun getTechDetails(
                @PathVariable techName: String,
                @RequestParam(required = false) country: String?
        ): TechDetailsPageDto {
                return techRepository.getTechDetails(techName, country)
        }
}
