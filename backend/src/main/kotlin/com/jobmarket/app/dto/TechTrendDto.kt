package com.jobmarket.app.dto

import java.time.LocalDate

data class TechTrendDto(val technology: String, val weekStarting: LocalDate, val jobCount: Long)
