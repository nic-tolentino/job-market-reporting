package com.techmarket.sync.ats

import com.techmarket.sync.ats.ashby.AshbyNormalizer
import com.techmarket.sync.ats.greenhouse.GreenhouseNormalizer
import com.techmarket.sync.ats.lever.LeverNormalizer
import com.techmarket.sync.ats.smartrecruiters.SmartRecruitersNormalizer
import com.techmarket.sync.ats.teamtailor.TeamTailorNormalizer
import com.techmarket.sync.ats.workable.WorkableNormalizer
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AtsNormalizerFactoryTest {

    private val greenhouseNormalizer      = mockk<GreenhouseNormalizer>()
    private val leverNormalizer           = mockk<LeverNormalizer>()
    private val ashbyNormalizer           = mockk<AshbyNormalizer>()
    private val smartRecruitersNormalizer = mockk<SmartRecruitersNormalizer>()
    private val teamTailorNormalizer      = mockk<TeamTailorNormalizer>()
    private val workableNormalizer        = mockk<WorkableNormalizer>()

    private val factory = AtsNormalizerFactory(
        greenhouseNormalizer, leverNormalizer, ashbyNormalizer,
        smartRecruitersNormalizer, teamTailorNormalizer, workableNormalizer
    )

    @Test
    fun `returns GreenhouseNormalizer for GREENHOUSE provider`() {
        assertInstanceOf(GreenhouseNormalizer::class.java, factory.getNormalizer(AtsProvider.GREENHOUSE))
    }

    @Test
    fun `returns LeverNormalizer for LEVER provider`() {
        assertInstanceOf(LeverNormalizer::class.java, factory.getNormalizer(AtsProvider.LEVER))
    }

    @Test
    fun `returns AshbyNormalizer for ASHBY provider`() {
        assertInstanceOf(AshbyNormalizer::class.java, factory.getNormalizer(AtsProvider.ASHBY))
    }

    @Test
    fun `returns SmartRecruitersNormalizer for SMARTRECRUITERS provider`() {
        assertInstanceOf(SmartRecruitersNormalizer::class.java, factory.getNormalizer(AtsProvider.SMARTRECRUITERS))
    }

    @Test
    fun `returns TeamTailorNormalizer for TEAMTAILOR provider`() {
        assertInstanceOf(TeamTailorNormalizer::class.java, factory.getNormalizer(AtsProvider.TEAMTAILOR))
    }

    @Test
    fun `returns WorkableNormalizer for WORKABLE provider`() {
        assertInstanceOf(WorkableNormalizer::class.java, factory.getNormalizer(AtsProvider.WORKABLE))
    }

    @Test
    fun `throws for unsupported providers`() {
        listOf(AtsProvider.WORKDAY, AtsProvider.BAMBOOHR, AtsProvider.CRAWLER).forEach { provider ->
            assertThrows(IllegalArgumentException::class.java) { factory.getNormalizer(provider) }
        }
    }
}
