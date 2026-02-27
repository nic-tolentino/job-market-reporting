package com.techmarket.persistence.analytics

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.TableResult
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnalyticsMapperTest {

    @Test
    fun `mapLandingPageData accurately calculates percentages and maps data`() {
        // Arrange Mock Table Results
        val mockStatsResult = mockk<TableResult>()
        val mockTechResult = mockk<TableResult>()
        val mockCompaniesResult = mockk<TableResult>()

        val statsRow = mockk<FieldValueList>()

        // Mocking the deeply nested Google Cloud SDK responses
        val totalFieldValue = mockk<FieldValue>()
        every { totalFieldValue.longValue } returns 200L
        every { statsRow.get("totalVacancies") } returns totalFieldValue

        val remoteFieldValue = mockk<FieldValue>()
        every { remoteFieldValue.longValue } returns 100L // 50%
        every { statsRow.get("remoteCount") } returns remoteFieldValue

        val hybridFieldValue = mockk<FieldValue>()
        every { hybridFieldValue.longValue } returns 50L // 25%
        every { statsRow.get("hybridCount") } returns hybridFieldValue

        every { mockStatsResult.values } returns listOf(statsRow)

        // Mock empty tech and companies to isolate stats test
        every { mockTechResult.values } returns emptyList()
        every { mockCompaniesResult.values } returns emptyList()

        // Act
        val result =
                AnalyticsMapper.mapLandingPageData(
                        mockStatsResult,
                        mockTechResult,
                        mockCompaniesResult
                )

        // Assert
        assertEquals(200, result.globalStats.totalVacancies)
        assertEquals(50, result.globalStats.remotePercentage)
        assertEquals(25, result.globalStats.hybridPercentage)
        assertEquals("N/A", result.globalStats.topTech) // Because techResult was empty
    }
}
