package com.techmarket.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration for the SmartRecruiters ATS integration. */
@ConfigurationProperties(prefix = "smartrecruiters")
data class SmartRecruitersProperties(val baseUrl: String = "https://api.smartrecruiters.com")
