package com.techmarket.sync.ats

import com.techmarket.sync.ats.greenhouse.GreenhouseNormalizer
import org.springframework.stereotype.Component

@Component
class AtsNormalizerFactory(private val greenhouseNormalizer: GreenhouseNormalizer) {
    fun getNormalizer(provider: AtsProvider): AtsNormalizer {
        return when (provider) {
            AtsProvider.GREENHOUSE -> greenhouseNormalizer
            else ->
                    throw IllegalArgumentException(
                            "No AtsNormalizer implementation for provider: $provider"
                    )
        }
    }
}
