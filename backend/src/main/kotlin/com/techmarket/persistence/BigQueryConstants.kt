package com.techmarket.persistence

object BigQueryTables {
    const val JOBS = "raw_jobs"
    const val COMPANIES = "raw_companies"
    const val INGESTIONS = "raw_ingestions"
    const val SEARCH_MISSES = "search_misses"
    const val USER_FEEDBACK = "user_feedback"
    const val ATS_CONFIGS = "company_ats_configs"
}

object JobFields {
    const val JOB_ID = "jobId"
    const val JOB_IDS = "jobIds"
    const val PLATFORM_JOB_IDS = JOB_IDS
    const val COMPANY_ID = "companyId"
    const val COMPANY_NAME = "companyName"
    const val SOURCE = "source"
    const val COUNTRY = "country"
    const val TITLE = "title"
    const val LOCATIONS = "locations"
    const val APPLY_URLS = "applyUrls"
    const val SENIORITY_LEVEL = "seniorityLevel"
    const val TECHNOLOGIES = "technologies"
    const val SALARY_MIN = "salaryMin"
    const val SALARY_MAX = "salaryMax"
    const val POSTED_DATE = "postedDate"
    const val BENEFITS = "benefits"
    const val EMPLOYMENT_TYPE = "employmentType"
    const val WORK_MODEL = "workModel"
    const val JOB_FUNCTION = "jobFunction"
    const val DESCRIPTION = "description"
    const val CITY = "city"
    const val STATE_REGION = "stateRegion"
    const val INGESTED_AT = "ingestedAt"
    const val LAST_SEEN_AT = "lastSeenAt"
    const val PLATFORM_LINKS = "platformLinks"
}

/**
 * Field names for the salary STRUCT in BigQuery.
 * Used for schema definition and data extraction.
 * Note: disclaimer is NOT persisted - it's computed at BFF level from source.
 */
object SalaryFields {
    const val AMOUNT = "amount"
    const val CURRENCY = "currency"
    const val PERIOD = "period"
    const val SOURCE = "source"
    const val IS_GROSS = "isGross"
}

object CompanyFields {
    const val COMPANY_ID = "companyId"
    const val NAME = "name"
    const val ALTERNATE_NAMES = "alternateNames"
    const val LOGO_URL = "logoUrl"
    const val DESCRIPTION = "description"
    const val WEBSITE = "website"
    const val EMPLOYEES_COUNT = "employeesCount"
    const val INDUSTRIES = "industries"
    const val TECHNOLOGIES = "technologies"
    const val HIRING_LOCATIONS = "hiringLocations"
    
    // New curated fields
    const val IS_AGENCY = "isAgency"
    const val IS_SOCIAL_ENTERPRISE = "isSocialEnterprise"
    const val HQ_COUNTRY = "hqCountry"
    const val OPERATING_COUNTRIES = "operatingCountries"
    const val OFFICE_LOCATIONS = "officeLocations"
    const val REMOTE_POLICY = "remotePolicy"
    const val VISA_SPONSORSHIP = "visa_sponsorship"
    const val VERIFICATION_LEVEL = "verificationLevel"

    const val INGESTED_AT = "ingestedAt"
    const val LAST_UPDATED_AT = "lastUpdatedAt"
}

object IngestionFields {
    const val ID = "id"
    const val SOURCE = "source"
    const val RAW_PAYLOAD = "rawPayload"
    const val INGESTED_AT = "ingestedAt"
    const val DATASET_ID = "datasetId"
}

object AnalyticsFields {
    const val TERM = "term"
    const val TIMESTAMP = "timestamp"
    const val CONTEXT = "context"
    const val MESSAGE = "message"
    // Landing page stats
    const val TOTAL_VACANCIES = "totalVacancies"
    const val REMOTE_COUNT = "remoteCount"
    const val HYBRID_COUNT = "hybridCount"
    // Common fields
    const val NAME = "name"
    const val COUNT = "count"
    const val ID = "id"
    const val LOGO = "logo"
    const val ACTIVE_ROLES = "activeRoles"
    const val TYPE = "type"
}

object AtsConfigFields {
    const val COMPANY_ID = "companyId"
    const val ATS_PROVIDER = "atsProvider"
    const val IDENTIFIER = "identifier"
    const val ENABLED = "enabled"
    const val LAST_SYNCED_AT = "lastSyncedAt"
    const val SYNC_STATUS = "syncStatus"
}

object CommonLiterals {
    const val UNKNOWN = "Unknown"
    const val UNKNOWN_COMPANY = "Unknown Company"
    const val HYBRID_FRIENDLY = "Hybrid Friendly"
    const val UNVERIFIED = "unverified"
    const val VERIFIED = "VERIFIED"
}

object QueryParams {
    const val COMPANY_ID = "companyId"
    const val JOB_ID = "jobId"
    const val COUNTRY = "country"
    const val SENIORITY = "seniority"
}
