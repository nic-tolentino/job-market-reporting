package com.techmarket.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration for the Greenhouse ATS integration. */
@ConfigurationProperties(prefix = "greenhouse")
data class GreenhouseProperties(val baseUrl: String = "https://boards-api.greenhouse.io/v1")
