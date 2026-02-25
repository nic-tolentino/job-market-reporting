package com.jobmarket.app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {

    @Bean
    fun apifyRestClient(builder: RestClient.Builder, apifyProperties: ApifyProperties): RestClient {
        return builder
            .baseUrl(apifyProperties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${apifyProperties.token}")
            .build()
    }
}
