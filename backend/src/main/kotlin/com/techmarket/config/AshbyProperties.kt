package com.techmarket.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration for the Ashby ATS integration. */
@ConfigurationProperties(prefix = "ashby")
data class AshbyProperties(val baseUrl: String = "https://api.ashbyhq.com")
