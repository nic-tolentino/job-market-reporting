package com.techmarket.sync.model

data class ApifyJobDto(
        val id: String?,
        val title: String?,
        val companyName: String?,
        val companyLogo: String?,
        val location: String?,
        val salaryInfo: List<String>?,
        val postedAt: String?,
        val benefits: List<String>?,
        val applicantsCount: String?,
        val applyUrl: String?,
        val descriptionHtml: String?,
        val descriptionText: String?,
        val jobPosterName: String?,
        val seniorityLevel: String?,
        val employmentType: String?,
        val jobFunction: String?,
        val industries: String?,
        val companyDescription: String?,
        val companyWebsite: String?,
        val companyEmployeesCount: Int?
)
