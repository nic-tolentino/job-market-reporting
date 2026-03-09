package com.techmarket.config

import com.google.cloud.tasks.v2.CloudTasksClient
import com.google.cloud.tasks.v2.CloudTasksSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException

/**
 * Configuration for Google Cloud Tasks client.
 */
@Configuration
class CloudTasksConfig {

    @Bean
    fun cloudTasksClient(): CloudTasksClient {
        val settings = CloudTasksSettings.newBuilder().build()
        return CloudTasksClient.create(settings)
    }
}
