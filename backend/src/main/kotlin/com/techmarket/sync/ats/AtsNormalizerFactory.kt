package com.techmarket.sync.ats

import com.techmarket.sync.ats.ashby.AshbyNormalizer
import com.techmarket.sync.ats.greenhouse.GreenhouseNormalizer
import com.techmarket.sync.ats.lever.LeverNormalizer
import com.techmarket.sync.ats.smartrecruiters.SmartRecruitersNormalizer
import com.techmarket.sync.ats.teamtailor.TeamTailorNormalizer
import com.techmarket.sync.ats.workable.WorkableNormalizer
import org.springframework.stereotype.Component

@Component
class AtsNormalizerFactory(
    private val greenhouseNormalizer: GreenhouseNormalizer,
    private val leverNormalizer: LeverNormalizer,
    private val ashbyNormalizer: AshbyNormalizer,
    private val smartRecruitersNormalizer: SmartRecruitersNormalizer,
    private val teamTailorNormalizer: TeamTailorNormalizer,
    private val workableNormalizer: WorkableNormalizer
) {
    fun getNormalizer(provider: AtsProvider): AtsNormalizer {
        return when (provider) {
            AtsProvider.GREENHOUSE      -> greenhouseNormalizer
            AtsProvider.LEVER          -> leverNormalizer
            AtsProvider.ASHBY          -> ashbyNormalizer
            AtsProvider.SMARTRECRUITERS -> smartRecruitersNormalizer
            AtsProvider.TEAMTAILOR     -> teamTailorNormalizer
            AtsProvider.WORKABLE       -> workableNormalizer
            else ->
                throw IllegalArgumentException(
                    "No AtsNormalizer implementation for provider: $provider"
                )
        }
    }
}
