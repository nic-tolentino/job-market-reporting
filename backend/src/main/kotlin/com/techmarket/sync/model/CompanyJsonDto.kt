package com.techmarket.sync.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.techmarket.models.VisaSponsorshipInfo
import com.techmarket.persistence.model.VerificationLevel

data class CompanyJsonDto(
    val id: String,
    val name: String,
    @JsonProperty("alternateNames")
    val alternateNames: List<String> = emptyList(),
    val description: String? = null,
    val website: String? = null,
    @JsonProperty("logoUrl")
    val logoUrl: String? = null,
    val industries: List<String> = emptyList(),
    @JsonProperty("company_type")
    val companyType: String? = null,
    @JsonProperty("is_agency")
    val isAgency: Boolean = false,
    @JsonProperty("is_social_enterprise")
    val isSocialEnterprise: Boolean = false,
    @JsonProperty("hq_country")
    val hqCountry: String? = null,
    @JsonProperty("operating_countries")
    val operatingCountries: List<String> = emptyList(),
    @JsonProperty("office_locations")
    val officeLocations: List<String> = emptyList(),
    @JsonProperty("remote_policy")
    val remotePolicy: String? = null,
    @JsonDeserialize(using = VisaSponsorshipDeserializer::class)
    @JsonProperty("visa_sponsorship")
    val visaSponsorship: VisaSponsorshipInfo? = null,
    @JsonProperty("employees_count")
    val employeesCount: Int? = null,
    @JsonProperty("verification_level")
    val verificationLevel: VerificationLevel = VerificationLevel.VERIFIED,
    @JsonProperty("updated_at")
    val updatedAt: String? = null,
    @JsonProperty("ats")
    val atsConfig: AtsConfigDto? = null,
    val crawler: CrawlerJsonDto? = null
)

data class CrawlerJsonDto(
    val seeds: List<CrawlerSeedDto> = emptyList(),
    val discovery: CrawlerDiscoveryDto? = null
)

data class CrawlerSeedDto(
    val url: String,
    val category: String?,
    val status: String?
)

data class CrawlerDiscoveryDto(
    val status: String?,
    val lastAttemptedAt: String? = null,
    val notes: String? = null,
    val errorMessage: String? = null
)

data class AtsConfigDto(
    val provider: String,
    val identifier: String
)
