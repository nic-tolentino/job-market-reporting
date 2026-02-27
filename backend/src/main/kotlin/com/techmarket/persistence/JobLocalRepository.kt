package com.techmarket.persistence

import com.techmarket.api.model.*
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.persistence.model.RawIngestionRecord
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

    override fun getRawIngestions(): List<RawIngestionRecord> {
        log.info("LOCAL: Returning empty list of raw ingestions")
        return emptyList()
    }

    override fun deleteAllJobs() {
        log.info("LOCAL: Mocking delete of all jobs")
    }

    override fun saveJobs(jobs: List<JobRecord>) {
        if (jobs.isEmpty()) {
            log.info("LOCAL: No jobs provided to save.")
            return
        }

        log.info("LOCAL: Mocking save of ${jobs.size} jobs to BigQuery.")
        log.info("LOCAL First mapped job sample: {}", jobs.firstOrNull())
    }

    override fun deleteAllCompanies() {
        log.info("LOCAL: Mocking delete of all companies")
    }

    override fun saveCompanies(companies: List<CompanyRecord>) {
        if (companies.isEmpty()) {
            log.info("LOCAL: No companies provided to save.")
            return
        }

        log.info("LOCAL: Mocking save of ${companies.size} companies to BigQuery.")
        log.info("LOCAL First mapped company sample: {}", companies.firstOrNull())
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

    override fun getSearchSuggestions(): SearchSuggestionsResponse {
        log.info("LOCAL: Mocking search suggestions")
        return SearchSuggestionsResponse(
                suggestions =
                        listOf(
                                SearchSuggestionDto("TECHNOLOGY", "react", "React"),
                                SearchSuggestionDto("TECHNOLOGY", "kotlin", "Kotlin"),
                                SearchSuggestionDto("TECHNOLOGY", "typescript", "TypeScript"),
                                SearchSuggestionDto("COMPANY", "google", "Google"),
                                SearchSuggestionDto("COMPANY", "atlassian", "Atlassian"),
                                SearchSuggestionDto("COMPANY", "canva", "Canva"),
                                SearchSuggestionDto("COMPANY", "amazon", "Amazon"),
                                SearchSuggestionDto("COMPANY", "xero", "Xero")
                        )
        )
    }

    override fun saveSearchMiss(term: String) {
        log.info("LOCAL: Mocking save of search miss: $term")
    }

    override fun saveFeedback(context: String?, message: String) {
        log.info("LOCAL: Mocking save of feedback [Context: $context]: $message")
    }
}
