package com.techmarket.sync.ats

import com.techmarket.sync.ats.ashby.AshbyClient
import com.techmarket.sync.ats.greenhouse.GreenhouseClient
import com.techmarket.sync.ats.lever.LeverClient
import com.techmarket.sync.ats.smartrecruiters.SmartRecruitersClient
import com.techmarket.sync.ats.teamtailor.TeamTailorClient
import com.techmarket.sync.ats.workable.WorkableClient
import org.springframework.stereotype.Component

@Component
class AtsClientFactory(
    private val greenhouseClient: GreenhouseClient,
    private val leverClient: LeverClient,
    private val ashbyClient: AshbyClient,
    private val smartRecruitersClient: SmartRecruitersClient,
    private val teamTailorClient: TeamTailorClient,
    private val workableClient: WorkableClient,
    private val crawlerClient: CrawlerClient
) {
    fun getClient(provider: AtsProvider): AtsClient {
        return when (provider) {
            AtsProvider.GREENHOUSE      -> greenhouseClient
            AtsProvider.LEVER          -> leverClient
            AtsProvider.ASHBY          -> ashbyClient
            AtsProvider.SMARTRECRUITERS -> smartRecruitersClient
            AtsProvider.TEAMTAILOR     -> teamTailorClient
            AtsProvider.WORKABLE       -> workableClient
            AtsProvider.CRAWLER        -> crawlerClient
            else ->
                throw IllegalArgumentException(
                    "No AtsClient implementation for provider: $provider"
                )
        }
    }
}
