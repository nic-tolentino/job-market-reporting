package com.techmarket.models

import com.google.cloud.bigquery.FieldValueList
import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.CommonLiterals.HYBRID_FRIENDLY
import com.techmarket.persistence.CommonLiterals.UNKNOWN
import com.techmarket.persistence.CommonLiterals.VERIFIED
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.getBooleanOrDefault
import com.techmarket.persistence.getLongOrNull
import com.techmarket.persistence.getSalaryOrNull
import com.techmarket.persistence.getString
import com.techmarket.persistence.getStringOrDefault
import com.techmarket.persistence.getStringList
import com.techmarket.persistence.getStringListOrNull
import com.techmarket.persistence.getStringOrNull
import com.techmarket.persistence.getTimestampOrDefault
import java.time.Instant

/**
 * Type-safe representation of a job record from BigQuery.
 * Encapsulates both the data structure and hydration logic.
 * 
 * The companion object [from] method handles all null-safety and type conversion,
 * so the rest of the codebase works with clean, typed data.
 */
data class JobRow(
    val jobId: String,
    val jobIds: List<String>,
    val applyUrls: List<String?>,
    val platformLinks: List<String?>,
    val locations: List<String>,
    val title: String,
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
         */
        fun fromJobRow(field: FieldValueList): JobRow {
            return JobRow(
                jobId = field.getString(JobFields.JOB_ID),
                jobIds = field.getStringList(JobFields.JOB_IDS),
                applyUrls = field.getStringListOrNull(JobFields.APPLY_URLS),
                platformLinks = field.getStringListOrNull(JobFields.PLATFORM_LINKS),
                locations = field.getStringList(JobFields.LOCATIONS),
                title = field.getString(JobFields.TITLE),
                salaryMin = field.getSalaryOrNull(JobFields.SALARY_MIN),
                salaryMax = field.getSalaryOrNull(JobFields.SALARY_MAX),
                postedDate = field.getString(JobFields.POSTED_DATE),
                technologies = field.getStringList(JobFields.TECHNOLOGIES),
                benefits = field.getStringList(JobFields.BENEFITS),
                city = field.getStringOrDefault(JobFields.CITY, UNKNOWN),
                stateRegion = field.getStringOrDefault(JobFields.STATE_REGION, UNKNOWN),
                seniorityLevel = field.getString(JobFields.SENIORITY_LEVEL),
                source = field.getString(JobFields.SOURCE),
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
    val verificationLevel: String = VERIFIED,
    val lastUpdatedAt: Instant = Instant.EPOCH
) {
    companion object {
        fun fromCompanyRow(field: FieldValueList): CompanyRow {
            return CompanyRow(
                companyId = field.getString(CompanyFields.COMPANY_ID),
                name = field.getString(CompanyFields.NAME),
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
                verificationLevel = field.getStringOrDefault(CompanyFields.VERIFICATION_LEVEL, VERIFIED),
                lastUpdatedAt = field.getTimestampOrDefault(CompanyFields.LAST_UPDATED_AT, Instant.EPOCH)
            )
        }
    }
}
