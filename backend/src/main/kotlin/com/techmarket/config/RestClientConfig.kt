package com.techmarket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun apifyRestClient(builder: RestClient.Builder, apifyProperties: ApifyProperties): RestClient {
        return builder.baseUrl(apifyProperties.baseUrl)
                .defaultHeader("Authorization", "Bearer ${apifyProperties.token}")
                .build()
    }

    @Bean
    fun greenhouseRestClient(
            builder: RestClient.Builder,
            properties: GreenhouseProperties
    ): RestClient {
        return builder.baseUrl(properties.baseUrl).build()
    }

    @Bean
    fun leverRestClient(builder: RestClient.Builder, properties: LeverProperties): RestClient {
        return builder.baseUrl(properties.baseUrl).build()
    }

    @Bean
    fun ashbyRestClient(builder: RestClient.Builder, properties: AshbyProperties): RestClient {
        return builder.baseUrl(properties.baseUrl).build()
    }

    @Bean
    fun smartRecruitersRestClient(builder: RestClient.Builder, properties: SmartRecruitersProperties): RestClient {
        return builder.baseUrl(properties.baseUrl).build()
    }

    /** TeamTailor uses company-specific subdomains; no fixed base URL. */
    @Bean
    fun teamTailorRestClient(builder: RestClient.Builder): RestClient {
        return builder.build()
    }

    @Bean
    fun workableRestClient(builder: RestClient.Builder, properties: WorkableProperties): RestClient {
        return builder.baseUrl(properties.baseUrl).build()
    }
}
