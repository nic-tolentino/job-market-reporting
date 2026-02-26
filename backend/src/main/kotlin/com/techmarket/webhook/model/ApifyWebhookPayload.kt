package com.techmarket.webhook.model

data class ApifyWebhookPayload(
        val userId: String?,
        val createdAt: String?,
        val eventType: String?,
        val eventData: Map<String, Any>?,
        val resource: Map<String, Any>?
)
