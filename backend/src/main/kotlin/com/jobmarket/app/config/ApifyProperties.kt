package com.jobmarket.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "apify")
data class ApifyProperties(
    val baseUrl: String = "https://api.apify.com/v2",
    val datasetId: String = "",
    val token: String = ""
)
