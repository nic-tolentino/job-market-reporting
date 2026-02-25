package com.jobmarket.app.persistence

import com.jobmarket.app.dashboard.model.TechTrendDto
import com.jobmarket.app.persistence.model.BigQueryJobRecord

interface JobRepository {
    fun saveAll(jobs: List<BigQueryJobRecord>)
    fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto>
}
