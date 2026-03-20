package com.techmarket.sync.ats

import com.techmarket.sync.ats.ashby.AshbyClient
import com.techmarket.sync.ats.greenhouse.GreenhouseClient
import com.techmarket.sync.ats.lever.LeverClient
import com.techmarket.sync.ats.smartrecruiters.SmartRecruitersClient
import com.techmarket.sync.ats.teamtailor.TeamTailorClient
import com.techmarket.sync.ats.workable.WorkableClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AtsClientFactoryTest {

    private val greenhouseClient      = mockk<GreenhouseClient>()
    private val leverClient           = mockk<LeverClient>()
    private val ashbyClient           = mockk<AshbyClient>()
    private val smartRecruitersClient = mockk<SmartRecruitersClient>()
    private val teamTailorClient      = mockk<TeamTailorClient>()
    private val workableClient        = mockk<WorkableClient>()
    private val crawlerClient         = mockk<CrawlerClient>()

    private val factory = AtsClientFactory(
        greenhouseClient, leverClient, ashbyClient,
        smartRecruitersClient, teamTailorClient, workableClient,
        crawlerClient
    )

    @Test
    fun `returns GreenhouseClient for GREENHOUSE provider`() {
        assertInstanceOf(GreenhouseClient::class.java, factory.getClient(AtsProvider.GREENHOUSE))
    }

    @Test
    fun `returns LeverClient for LEVER provider`() {
        assertInstanceOf(LeverClient::class.java, factory.getClient(AtsProvider.LEVER))
    }

    @Test
    fun `returns AshbyClient for ASHBY provider`() {
        assertInstanceOf(AshbyClient::class.java, factory.getClient(AtsProvider.ASHBY))
    }

    @Test
    fun `returns SmartRecruitersClient for SMARTRECRUITERS provider`() {
        assertInstanceOf(SmartRecruitersClient::class.java, factory.getClient(AtsProvider.SMARTRECRUITERS))
    }

    @Test
    fun `returns TeamTailorClient for TEAMTAILOR provider`() {
        assertInstanceOf(TeamTailorClient::class.java, factory.getClient(AtsProvider.TEAMTAILOR))
    }

    @Test
    fun `returns WorkableClient for WORKABLE provider`() {
        assertInstanceOf(WorkableClient::class.java, factory.getClient(AtsProvider.WORKABLE))
    }

    @Test
    fun `returns CrawlerClient for CRAWLER provider`() {
        assertInstanceOf(CrawlerClient::class.java, factory.getClient(AtsProvider.CRAWLER))
    }

    @Test
    fun `throws for unsupported providers`() {
        listOf(AtsProvider.WORKDAY, AtsProvider.BAMBOOHR).forEach { provider ->
            assertThrows(IllegalArgumentException::class.java) { factory.getClient(provider) }
        }
    }
}
