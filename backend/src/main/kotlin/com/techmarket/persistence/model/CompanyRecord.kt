package com.techmarket.persistence.model

import com.techmarket.models.VisaSponsorshipInfo
import java.time.Instant

data class CompanyRecord(
        val companyId: String,
        val name: String,
        val alternateNames: List<String>,
        val logoUrl: String?,
        val description: String?,
        val website: String?,
        val employeesCount: Int?,
        val industries: String?,
        val technologies: List<String>,
        val hiringLocations: List<String>,
        val isAgency: Boolean = false,
        val isSocialEnterprise: Boolean = false,
        val hqCountry: String? = null,
        val operatingCountries: List<String> = emptyList(),
        val officeLocations: List<String> = emptyList(),
        val remotePolicy: String? = null,
        val visaSponsorship: VisaSponsorshipInfo? = null,
        val verificationLevel: VerificationLevel = VerificationLevel.VERIFIED,
        val lastUpdatedAt: Instant
)
