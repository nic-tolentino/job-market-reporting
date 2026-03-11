package com.techmarket.models

data class CompanyListingItem(
    val id: String,
    val name: String,
    val logo: String,
    val visaSponsorship: VisaSponsorshipInfo?,
    val activeRoles: Int
)
