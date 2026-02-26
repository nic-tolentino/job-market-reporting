package com.jobmarket.app.persistence

import com.jobmarket.app.api.model.*
import com.jobmarket.app.dashboard.model.TechTrendDto
import com.jobmarket.app.persistence.model.CompanyRecord
import com.jobmarket.app.persistence.model.JobRecord
import com.jobmarket.app.persistence.model.RawIngestionRecord
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("local")
class JobLocalRepository : JobRepository {

    private val log = LoggerFactory.getLogger(JobLocalRepository::class.java)

    override fun saveRawIngestions(records: List<RawIngestionRecord>) {
        if (records.isEmpty()) {
            log.info("LOCAL: No raw ingestion records provided to save.")
            return
        }
        log.info("LOCAL: Mocking save of ${records.size} raw records to BigQuery.")
    }

    override fun saveJobs(jobs: List<JobRecord>) {
        if (jobs.isEmpty()) {
            log.info("LOCAL: No jobs provided to save.")
            return
        }

        log.info("LOCAL: Mocking save of ${jobs.size} jobs to BigQuery.")
        log.info("LOCAL First mapped job sample: {}", jobs.firstOrNull())
    }

    override fun saveCompanies(companies: List<CompanyRecord>) {
        if (companies.isEmpty()) {
            log.info("LOCAL: No companies provided to save.")
            return
        }

        log.info("LOCAL: Mocking save of ${companies.size} companies to BigQuery.")
        log.info("LOCAL First mapped company sample: {}", companies.firstOrNull())
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

    override fun getLandingPageData(): LandingPageDto {
        log.info("LOCAL: Returning stub LandingPageDto")
        return LandingPageDto(
                globalStats = GlobalStatsDto(0, 0, 0, ""),
                topTech = emptyList(),
                topCompanies = emptyList()
        )
    }

    override fun getTechDetails(techName: String): TechDetailsPageDto {
        log.info("LOCAL: Returning stub TechDetailsPageDto for $techName")
        return TechDetailsPageDto(
                techName = techName,
                seniorityDistribution = emptyList(),
                hiringCompanies = emptyList()
        )
    }

    override fun getCompanyProfile(companyId: String): CompanyProfilePageDto {
        log.info("LOCAL: Returning stub CompanyProfilePageDto for $companyId")
        return CompanyProfilePageDto(
                companyDetails =
                        CompanyDetailsDto(companyId, "Local Stub Company", "", "", 0, "", ""),
                techStack = emptyList(),
                insights = CompanyInsightsDto("", "", emptyList()),
                activeRoles = emptyList()
        )
    }
}
