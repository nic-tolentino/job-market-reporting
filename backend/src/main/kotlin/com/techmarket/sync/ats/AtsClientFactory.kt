package com.techmarket.sync.ats

import com.techmarket.sync.ats.greenhouse.GreenhouseClient
import org.springframework.stereotype.Component

@Component
class AtsClientFactory(private val greenhouseClient: GreenhouseClient) {
    fun getClient(provider: AtsProvider): AtsClient {
        return when (provider) {
            AtsProvider.GREENHOUSE -> greenhouseClient
            else ->
                    throw IllegalArgumentException(
                            "No AtsClient implementation for provider: $provider"
                    )
        }
    }
}
