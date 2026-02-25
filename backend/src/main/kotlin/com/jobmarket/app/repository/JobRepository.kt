package com.jobmarket.app.repository

import com.jobmarket.app.dto.BigQueryJobRecord
import com.jobmarket.app.dto.TechTrendDto

interface JobRepository {
    fun saveAll(jobs: List<BigQueryJobRecord>)
    fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto>
}
