package com.techmarket.persistence

object BigQueryTables {
    const val JOBS = "raw_jobs"
    const val COMPANIES = "raw_companies"
    const val INGESTIONS = "raw_ingestions"
    const val INGESTION_METADATA = "ingestion_metadata"
    const val SEARCH_MISSES = "search_misses"
    const val USER_FEEDBACK = "user_feedback"
    const val ATS_CONFIGS = "company_ats_configs"
    const val CRAWLER_SEEDS = "crawler_seeds"
    const val CRAWL_RUNS = "crawl_runs"
}

object CrawlerSeedFields {
    const val COMPANY_ID = "company_id"
    const val URL = "url"
    const val CATEGORY = "category"
    const val STATUS = "status"
    const val PAGINATION_PATTERN = "pagination_pattern"
    const val LAST_KNOWN_JOB_COUNT = "last_known_job_count"
    const val LAST_KNOWN_PAGE_COUNT = "last_known_page_count"
    const val LAST_CRAWLED_AT = "last_crawled_at"
    const val LAST_DURATION_MS = "last_duration_ms"
    const val ERROR_MESSAGE = "error_message"
    const val CONSECUTIVE_ZERO_YIELD_COUNT = "consecutive_zero_yield_count"
    const val ATS_PROVIDER = "ats_provider"
    const val ATS_IDENTIFIER = "ats_identifier"
    const val ATS_DIRECT_URL = "ats_direct_url"
}

object CrawlRunFields {
    const val RUN_ID = "run_id"
    const val BATCH_ID = "batch_id"
    const val COMPANY_ID = "company_id"
    const val SEED_URL = "seed_url"
    const val IS_TARGETED = "is_targeted"
    const val STARTED_AT = "started_at"
    const val DURATION_MS = "duration_ms"
    const val PAGES_VISITED = "pages_visited"
    const val JOBS_RAW = "jobs_raw"
    const val JOBS_VALID = "jobs_valid"
    const val JOBS_TECH = "jobs_tech"
    const val JOBS_FINAL = "jobs_final"
    const val CONFIDENCE_AVG = "confidence_avg"
    const val ATS_PROVIDER = "ats_provider"
    const val ATS_IDENTIFIER = "ats_identifier"
    const val ATS_DIRECT_URL = "ats_direct_url"
    const val PAGINATION_PATTERN = "pagination_pattern"
    const val STATUS = "status"
    const val ERROR_MESSAGE = "error_message"
    const val MODEL_USED = "model_used"
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

    // Health check fields for dead link detection
    const val URL_STATUS = "url_status"
    const val URL_LAST_CHECKED = "url_last_checked"
    const val URL_LAST_KNOWN_ACTIVE = "url_last_known_active"
    const val URL_CHECK_FAILURES = "url_check_failures"
    const val HTTP_STATUS_CODE = "http_status_code"
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
    const val VISA_SPONSORSHIP_DETAIL = "visa_sponsorship_detail"
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

/**
 * Field names for the ingestion_metadata table (Bronze layer file-based storage).
 * This table stores metadata about GCS files containing raw ingestion data.
 */
object IngestionMetadataFields {
    const val DATASET_ID = "dataset_id"
    const val SOURCE = "source"
    const val INGESTED_AT = "ingested_at"
    const val TARGET_COUNTRY = "target_country"
    const val SCHEMA_VERSION = "schema_version"
    const val RECORD_COUNT = "record_count"
    const val FILE_COUNT = "file_count"
    const val UNCOMPRESSED_SIZE_BYTES = "uncompressed_size_bytes"
    const val COMPRESSED_SIZE_BYTES = "compressed_size_bytes"
    const val COMPRESSION_RATIO = "compression_ratio"
    const val PROCESSING_STATUS = "processing_status"
    const val FILES = "files"
    const val METADATA_ID = "metadata_id"
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

object HubFields {
    const val TECHNOLOGY = "technology"
    const val JOB_COUNT = "jobCount"
    const val COMPANY_COUNT = "companyCount"
    const val AVG_SALARY_MAX = "avgSalaryMax"
    const val SALARY_MIN = "salaryMin"
    const val SALARY_MAX = "salaryMax"
    const val JOB_URL = "jobUrl"
    const val TOTAL_JOBS = "totalJobs"
    const val TOTAL_COMPANIES = "totalCompanies"
    const val CURRENT_MONTH_JOBS = "currentMonthJobs"
    const val PREV_MONTH_JOBS = "prevMonthJobs"
    const val LAST_6_MONTHS_JOBS = "last6MonthsJobs"
    const val MONTH = "month"
    const val CATEGORY_NAME = "categoryName"
    const val CURRENT_JOBS = "currentJobs"
    const val PREV_JOBS = "prevJobs"
    const val GLOBAL_TOTAL_JOBS = "globalTotalJobs"
    
    // Internal query aliases
    const val CATEGORY_COUNT = "categoryCount"
    const val TOTAL_COUNT = "totalCount"
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

/**
 * Aliases for JOINed company fields in queries.
 * Used when selecting company data alongside job data to avoid column name conflicts.
 */
object CompanyAliases {
    const val COMPANY_ID = "comp_companyId"
    const val NAME = "comp_name"
    const val LOGO_URL = "comp_logo"
    const val DESCRIPTION = "comp_desc"
    const val WEBSITE = "comp_web"
    const val HIRING_LOCATIONS = "comp_hiringLocations"
    const val HQ_COUNTRY = "comp_hqCountry"
    const val VERIFICATION_LEVEL = "comp_verificationLevel"
}
