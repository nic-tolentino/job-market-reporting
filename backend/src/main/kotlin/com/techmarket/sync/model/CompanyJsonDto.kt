package com.techmarket.sync.model

import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty("visa_sponsorship")
    val visaSponsorship: Boolean = false,
    @JsonProperty("employees_count")
    val employeesCount: Int? = null,
    @JsonProperty("verification_level")
    val verificationLevel: VerificationLevel = VerificationLevel.VERIFIED,
    @JsonProperty("updated_at")
    val updatedAt: String? = null
)
