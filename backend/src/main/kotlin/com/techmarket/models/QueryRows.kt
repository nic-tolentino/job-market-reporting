package com.techmarket.models

import com.google.cloud.bigquery.FieldValueList
import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.CommonLiterals.HYBRID_FRIENDLY
import com.techmarket.persistence.CommonLiterals.UNKNOWN
import com.techmarket.persistence.CommonLiterals.VERIFIED
import com.techmarket.persistence.CompanyAliases
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.getBooleanOrDefault
import com.techmarket.persistence.getLongOrNull
import com.techmarket.persistence.getSalaryOrNull
import com.techmarket.persistence.getString
import com.techmarket.persistence.getStringOrDefault
import com.techmarket.persistence.getStringOrThrow
import com.techmarket.persistence.getStringList
import com.techmarket.persistence.getStringListOrNull
import com.techmarket.persistence.getStringOrNull
import com.techmarket.persistence.getTimestampOrDefault
import java.time.Instant

/**
 * Type-safe representation of a job record from BigQuery.
 * Encapsulates both the data structure and hydration logic.
 *
 * The companion object [fromJobRow] method handles all null-safety and type conversion,
 * so the rest of the codebase works with clean, typed data.
 */
data class JobRow(
    val jobId: String,
    val jobIds: List<String>,
    val applyUrls: List<String?>,
    val platformLinks: List<String?>,
    val locations: List<String>,
    val title: String,
    val companyId: String,
    val companyName: String,
    val description: String?,
    val employmentType: String?,
    val jobFunction: String?,
    val salaryMin: NormalizedSalary?,
    val salaryMax: NormalizedSalary?,
    val postedDate: String,
    val technologies: List<String>,
    val benefits: List<String>,
    val city: String,
    val stateRegion: String,
    val seniorityLevel: String,
    val source: String,
    val lastSeenAt: Instant,
    val country: String? = null,
    val workModel: String? = null
) {
    companion object {
        /**
         * Hydrates a JobRow from BigQuery FieldValueList.
         * Handles all null-safety and type conversion in one place.
         * 
         * jobId uses getStringOrThrow — a missing ID means the SQL forgot to SELECT it,
         * which is a query bug and should fail loudly rather than silently corrupt data.
         */
        fun fromJobRow(field: FieldValueList): JobRow {
            return JobRow(
                jobId = field.getStringOrThrow(JobFields.JOB_ID),
                jobIds = field.getStringList(JobFields.JOB_IDS),
                applyUrls = field.getStringListOrNull(JobFields.APPLY_URLS),
                platformLinks = field.getStringListOrNull(JobFields.PLATFORM_LINKS),
                locations = field.getStringList(JobFields.LOCATIONS),
                title = field.getStringOrDefault(JobFields.TITLE, "Unknown Title"),
                companyId = field.getStringOrDefault(JobFields.COMPANY_ID, "unknown"),
                companyName = field.getStringOrDefault(JobFields.COMPANY_NAME, "Unknown Company"),
                description = field.getStringOrNull(JobFields.DESCRIPTION),
                employmentType = field.getStringOrNull(JobFields.EMPLOYMENT_TYPE),
                jobFunction = field.getStringOrNull(JobFields.JOB_FUNCTION),
                salaryMin = field.getSalaryOrNull(JobFields.SALARY_MIN),
                salaryMax = field.getSalaryOrNull(JobFields.SALARY_MAX),
                postedDate = field.getStringOrDefault(JobFields.POSTED_DATE, ""),
                technologies = field.getStringList(JobFields.TECHNOLOGIES),
                benefits = field.getStringList(JobFields.BENEFITS),
                city = field.getStringOrDefault(JobFields.CITY, UNKNOWN),
                stateRegion = field.getStringOrDefault(JobFields.STATE_REGION, UNKNOWN),
                seniorityLevel = field.getStringOrDefault(JobFields.SENIORITY_LEVEL, "Mid-Level"),
                source = field.getStringOrDefault(JobFields.SOURCE, "Unknown"),
                lastSeenAt = field.getTimestampOrDefault(JobFields.LAST_SEEN_AT, Instant.EPOCH),
                country = field.getStringOrNull(JobFields.COUNTRY),
                workModel = field.getStringOrNull(JobFields.WORK_MODEL)
            )
        }
    }
}

/**
 * Type-safe representation of a company record from BigQuery.
 * 
 * Default values are provided for all fields to ensure the Row is always
 * in a valid state. This makes the mapper's job simpler - it just transforms
 * to DTOs without worrying about null handling.
 */
data class CompanyRow(
    val companyId: String,
    val name: String,
    val alternateNames: List<String> = emptyList(),
    val logoUrl: String? = null,
    val website: String? = null,
    val employeesCount: Int? = null,
    val industries: String? = null,
    val description: String? = null,
    val technologies: List<String> = emptyList(),
    val hiringLocations: List<String> = emptyList(),
    val isAgency: Boolean = false,
    val isSocialEnterprise: Boolean = false,
    val hqCountry: String? = null,
    val operatingCountries: List<String> = emptyList(),
    val officeLocations: List<String> = emptyList(),
    val remotePolicy: String? = null,
    val visaSponsorship: Boolean = false,
    val visaSponsorshipDetail: String? = null,
    val verificationLevel: String = VERIFIED,
    val lastUpdatedAt: Instant = Instant.EPOCH
) {
    companion object {
        fun fromCompanyRow(field: FieldValueList): CompanyRow {
            return CompanyRow(
                companyId = field.getStringOrDefault(CompanyFields.COMPANY_ID, "unknown"),
                name = field.getStringOrDefault(CompanyFields.NAME, "Unknown Company"),
                alternateNames = field.getStringList(CompanyFields.ALTERNATE_NAMES),
                logoUrl = field.getStringOrNull(CompanyFields.LOGO_URL),
                website = field.getStringOrNull(CompanyFields.WEBSITE),
                employeesCount = field.getLongOrNull(CompanyFields.EMPLOYEES_COUNT)?.toInt(),
                industries = field.getStringOrNull(CompanyFields.INDUSTRIES),
                description = field.getStringOrNull(CompanyFields.DESCRIPTION),
                technologies = field.getStringList(CompanyFields.TECHNOLOGIES),
                hiringLocations = field.getStringList(CompanyFields.HIRING_LOCATIONS),
                isAgency = field.getBooleanOrDefault(CompanyFields.IS_AGENCY, false),
                isSocialEnterprise = field.getBooleanOrDefault(CompanyFields.IS_SOCIAL_ENTERPRISE, false),
                hqCountry = field.getStringOrNull(CompanyFields.HQ_COUNTRY),
                operatingCountries = field.getStringList(CompanyFields.OPERATING_COUNTRIES),
                officeLocations = field.getStringList(CompanyFields.OFFICE_LOCATIONS),
                remotePolicy = field.getStringOrNull(CompanyFields.REMOTE_POLICY),
                visaSponsorship = field.getBooleanOrDefault(CompanyFields.VISA_SPONSORSHIP, false),
                visaSponsorshipDetail = field.getStringOrNull(CompanyFields.VISA_SPONSORSHIP_DETAIL),
                verificationLevel = field.getStringOrDefault(CompanyFields.VERIFICATION_LEVEL, VERIFIED),
                lastUpdatedAt = field.getTimestampOrDefault(CompanyFields.LAST_UPDATED_AT, Instant.EPOCH)
            )
        }
    }
}

/**
 * Type-safe representation of company data from a JOINed query result.
 * 
 * Used when fetching job details with joined company information.
 * Fields use company aliases (comp_name, comp_logo, etc.) to match
 * the SQL query column aliases.
 */
data class CompanyInfoRow(
    val companyId: String,
    val name: String,
    val logoUrl: String,
    val description: String,
    val website: String,
    val hiringLocations: List<String>,
    val hqCountry: String?,
    val verificationLevel: String
) {
    companion object {
        /**
         * Hydrates a CompanyInfoRow from a JOINed query result.
         * Uses CompanyAliases to access the aliased company fields.
         */
        fun fromJoinedRow(field: FieldValueList): CompanyInfoRow {
            return CompanyInfoRow(
                companyId = field.getStringOrDefault(CompanyAliases.COMPANY_ID, "unknown"),
                name = field.getStringOrDefault(CompanyAliases.NAME, "Unknown Company"),
                logoUrl = field.getStringOrDefault(CompanyAliases.LOGO_URL, ""),
                description = field.getStringOrDefault(CompanyAliases.DESCRIPTION, ""),
                website = field.getStringOrDefault(CompanyAliases.WEBSITE, ""),
                hiringLocations = field.getStringList(CompanyAliases.HIRING_LOCATIONS),
                hqCountry = field.getStringOrNull(CompanyAliases.HQ_COUNTRY),
                verificationLevel = field.getStringOrDefault(CompanyAliases.VERIFICATION_LEVEL, VERIFIED)
            )
        }
    }
}

/**
 * Type-safe representation of a job details page query result.
 * 
 * Wraps both the job data and the joined company data from a single
 * BigQuery row result. This is the main entry point for job details pages.
 */
data class JobDetailsRow(
    val job: JobRow,
    val company: CompanyInfoRow
) {
    companion object {
        /**
         * Hydrates a JobDetailsRow from a JOINed query result.
         * Delegates to JobRow.fromJobRow and CompanyInfoRow.fromJoinedRow.
         */
        fun fromJoinedRow(field: FieldValueList): JobDetailsRow {
            return JobDetailsRow(
                job = JobRow.fromJobRow(field),
                company = CompanyInfoRow.fromJoinedRow(field)
            )
        }
    }
}

/**
 * Type-safe representation of a seniority distribution aggregation row.
 * 
 * Used in TechMapper for mapping seniority distribution queries.
 */
data class SeniorityRow(
    val name: String,
    val value: Int
) {
    companion object {
        /**
         * Hydrates a SeniorityRow from an aggregation query result.
         * Handles null-safety for both name and value fields.
         */
        fun fromAggregationRow(field: FieldValueList): SeniorityRow {
            return SeniorityRow(
                name = field.getStringOrDefault("name", "Unknown"),
                value = field.getLongOrNull("value")?.toInt() ?: 0
            )
        }
    }
}

/**
 * Type-safe representation of a company leaderboard aggregation row.
 * 
 * Used in TechMapper for mapping company leaderboard queries.
 */
data class CompanyLeaderboardRow(
    val id: String,
    val name: String,
    val logo: String,
    val activeRoles: Int
) {
    companion object {
        /**
         * Hydrates a CompanyLeaderboardRow from an aggregation query result.
         * Handles null-safety for all fields with sensible defaults.
         */
        fun fromAggregationRow(field: FieldValueList): CompanyLeaderboardRow {
            return CompanyLeaderboardRow(
                id = field.getStringOrDefault("id", "unknown"),
                name = field.getStringOrDefault("name", "Unknown Company"),
                logo = field.getStringOrDefault("logo", ""),
                activeRoles = field.getLongOrNull("activeRoles")?.toInt() ?: 0
            )
        }
    }
}
