package com.techmarket.config

import com.google.cloud.tasks.v2.CloudTasksClient
import com.google.cloud.tasks.v2.CloudTasksSettings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for Google Cloud Tasks client.
 * Uses default GCP credentials (automatic in Cloud Run).
 */
@Configuration
class CloudTasksConfig {

    @Bean
    fun cloudTasksClient(): CloudTasksClient {
        // Use default settings - GCP SDK will automatically use metadata server credentials
        val settings = CloudTasksSettings.newBuilder().build()
        return CloudTasksClient.create(settings)
    }
}
