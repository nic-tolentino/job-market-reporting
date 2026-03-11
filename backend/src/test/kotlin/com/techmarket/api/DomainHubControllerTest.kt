package com.techmarket.api

import com.techmarket.model.TechCategory
import com.techmarket.dto.*
import com.techmarket.persistence.analytics.InsightsBigQueryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import java.time.Instant

class DomainHubControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var repository: InsightsBigQueryRepository

    @BeforeEach
    fun setup() {
        repository = mockk<InsightsBigQueryRepository>(relaxed = true)
        val controller = DomainHubController(repository)
        mockMvc = standaloneSetup(controller).build()
    }

    @Test
    fun `GET api-hubs-mobile returns 200 with DomainHubDto`() {
        // Arrange
        val category = TechCategory.MOBILE
        every { repository.getTechnologiesByCategory(category, any()) } returns emptyList()
        every { repository.getJobsByCategory(category, any()) } returns emptyList()
        every { repository.getCompaniesByCategory(category, any()) } returns emptyList()
        every { repository.getCategoryTrends(category, any()) } returns CategoryTrendsDto(100, 10, 5.0, 2.5, 50, emptyList())
        every { repository.countJobsByCategory(category, any()) } returns 100

        // Act & Assert
        mockMvc.perform(get("/api/hubs/mobile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category.slug").value("mobile"))
            .andExpect(jsonPath("$.totalJobs").value(100))
            .andExpect(jsonPath("$.marketShare").value(2.5))
            .andExpect(jsonPath("$.growthRate").value(5.0))
    }

    @Test
    fun `GET api-hubs-invalid-category returns 404`() {
        // Act & Assert
        mockMvc.perform(get("/api/hubs/invalid-category"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET api-hubs delegates to getAllCategorySummaries`() {
        // Arrange
        val summaries = listOf(
            DomainSummaryDto(
                category = CategoryDto("frontend", "Frontend", "desc"),
                jobCount = 100,
                companyCount = 10,
                techCount = 5,
                growthRate = 2.0,
                marketShare = 1.0
            )
        )
        every { repository.getAllCategorySummaries(null) } returns summaries

        // Act & Assert
        mockMvc.perform(get("/api/hubs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].category.slug").value("frontend"))
        
        verify { repository.getAllCategorySummaries(null) }
    }

    @Test
    fun `GET api-hubs-frontend with country passes country to repository`() {
        // Arrange
        val category = TechCategory.FRONTEND
        val country = "AU"
        
        // Act
        mockMvc.perform(get("/api/hubs/frontend?country=$country"))
            .andExpect(status().isOk)

        // Assert
        verify { repository.getTechnologiesByCategory(category, country) }
        verify { repository.getJobsByCategory(category, country) }
        verify { repository.getCompaniesByCategory(category, country) }
        verify { repository.getCategoryTrends(category, country) }
        verify { repository.countJobsByCategory(category, country) }
    }

    @Test
    fun `GET api-hubs with country passes country to repository`() {
        // Act
        mockMvc.perform(get("/api/hubs?country=AU"))
            .andExpect(status().isOk)

        // Assert
        verify { repository.getAllCategorySummaries("AU") }
    }
}
