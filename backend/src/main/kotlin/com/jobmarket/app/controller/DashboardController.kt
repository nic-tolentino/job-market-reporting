package com.jobmarket.app.controller

import com.jobmarket.app.dto.TechTrendDto
import com.jobmarket.app.repository.JobRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/trends")
class DashboardController(private val jobRepository: JobRepository) {

    @GetMapping("/tech")
    fun getTechTrends(
            @RequestParam(required = false, defaultValue = "6") monthsBack: Int
    ): List<TechTrendDto> {
        // In the future we can pass the country filter to the repository here
        return jobRepository.getTechTrendsByWeek(monthsBack)
    }
}
