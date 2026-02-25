package com.jobmarket.app.repository

import com.jobmarket.app.dto.BigQueryJobRecord
import com.jobmarket.app.dto.TechTrendDto
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("local")
class JobPostingLocalRepository : JobPostingRepository {

    private val log = LoggerFactory.getLogger(JobPostingLocalRepository::class.java)

    override fun saveAll(jobs: List<BigQueryJobRecord>) {
        if (jobs.isEmpty()) {
            log.info("LOCAL: No jobs provided to save.")
            return
        }

        log.info("LOCAL: Mocking save of ${jobs.size} jobs to BigQuery.")
        // In local development, simply print out a sample so the developer can verify mapping is
        // working
        log.info("LOCAL First mapped job sample: {}", jobs.firstOrNull())
    }

    override fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto> {
        log.info("LOCAL: Returning mock Tech Trends data for frontend development.")
        val now = LocalDate.now()
        return listOf(
                TechTrendDto("Kotlin", now.minusDays(7), 45),
                TechTrendDto("Java", now.minusDays(7), 120),
                TechTrendDto("React", now.minusDays(7), 95),
                TechTrendDto("Kotlin", now.minusDays(14), 40),
                TechTrendDto("Java", now.minusDays(14), 115),
                TechTrendDto("React", now.minusDays(14), 100)
        )
    }
}
