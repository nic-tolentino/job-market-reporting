package com.techmarket.api.model

import java.time.LocalDate

data class JobPageDto(
        val details: JobDetailsDto,
        val locations: List<JobLocationDto>,
        val company: JobCompanyDto,
        val similarRoles: List<JobRoleDto>
)

data class JobDetailsDto(
        val title: String,
        val description: String?,
        val seniorityLevel: String,
        val employmentType: String?,
        val workModel: String?,
        val postedDate: LocalDate?,
        val jobFunction: String?,
        val technologies: List<String>,
        val benefits: List<String>?
)

data class JobLocationDto(
        val location: String,
        val applyUrl: String?,
        val link: String?,
        val jobId: String
)

data class JobCompanyDto(
        val companyId: String,
        val name: String,
        val logoUrl: String,
        val description: String,
        val website: String,
        val hiringLocations: List<String>
)
