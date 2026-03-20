package com.techmarket.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configuration for the Lever ATS integration. */
@ConfigurationProperties(prefix = "lever")
data class LeverProperties(val baseUrl: String = "https://api.lever.co/v0")
