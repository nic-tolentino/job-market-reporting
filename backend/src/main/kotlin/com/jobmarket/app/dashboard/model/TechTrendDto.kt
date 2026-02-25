package com.jobmarket.app.dashboard.model

import java.time.LocalDate

data class TechTrendDto(val technology: String, val weekStarting: LocalDate, val jobCount: Long)
