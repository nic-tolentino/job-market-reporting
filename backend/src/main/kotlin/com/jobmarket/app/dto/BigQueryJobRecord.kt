package com.jobmarket.app.dto

import java.time.LocalDate

data class BigQueryJobRecord(
        val job_id: String,
        val source: String,
        val country: String,
        val title: String,
        val company: String,
        val location: String,
        val seniority_level: String?,
        val technologies: List<String>,
        val salary_min: Int?,
        val salary_max: Int?,
        val posted_date: LocalDate?,
        val raw_description: String?,
        val companyLogo: String?,
        val benefits: List<String>?,
        val applicantsCount: String?,
        val applyUrl: String?,
        val jobPosterName: String?,
        val employmentType: String?,
        val jobFunction: String?,
        val industries: String?,
        val companyDescription: String?,
        val companyWebsite: String?,
        val companyEmployeesCount: Int?,
        val raw_location: String?,
        val raw_seniority_level: String?,
        val ingested_at: java.time.Instant
)
