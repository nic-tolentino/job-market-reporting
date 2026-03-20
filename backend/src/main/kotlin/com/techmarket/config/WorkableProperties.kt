package com.techmarket.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration for the Workable ATS integration. */
@ConfigurationProperties(prefix = "workable")
data class WorkableProperties(val baseUrl: String = "https://apply.workable.com")
