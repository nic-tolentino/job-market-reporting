package com.techmarket.persistence.model

import java.time.Instant

data class CompanyRecord(
        val companyId: String,
        val name: String,
        val logoUrl: String?,
        val description: String?,
        val website: String?,
        val employeesCount: Int?,
        val industries: String?,
        val technologies: List<String>,
        val ingestedAt: Instant
)
