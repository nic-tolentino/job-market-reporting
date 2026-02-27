package com.techmarket.api

import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.persistence.tech.TechRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tech")
class TechController(private val techRepository: TechRepository) {

        @Cacheable("tech")
        @GetMapping("/{techName}")
        fun getTechDetails(@PathVariable techName: String): TechDetailsPageDto {
                return techRepository.getTechDetails(techName)
        }
}
