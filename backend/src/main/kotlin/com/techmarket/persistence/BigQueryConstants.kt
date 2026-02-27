package com.techmarket.persistence

object BigQueryTables {
    const val JOBS = "raw_jobs"
    const val COMPANIES = "raw_companies"
    const val INGESTIONS = "raw_ingestions"
    const val SEARCH_MISSES = "search_misses"
    const val USER_FEEDBACK = "user_feedback"
}

object JobFields {
    const val JOB_ID = "jobId"
    const val COMPANY_ID = "companyId"
    const val COMPANY_NAME = "companyName"
    const val SOURCE = "source"
    const val COUNTRY = "country"
    const val TITLE = "title"
    const val LOCATIONS = "locations"
    const val JOB_IDS = "jobIds"
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
}

object CompanyFields {
    const val COMPANY_ID = "companyId"
    const val NAME = "name"
    const val LOGO_URL = "logoUrl"
    const val DESCRIPTION = "description"
    const val WEBSITE = "website"
    const val EMPLOYEES_COUNT = "employeesCount"
    const val INDUSTRIES = "industries"
    const val TECHNOLOGIES = "technologies"
    const val HIRING_LOCATIONS = "hiringLocations"
    const val INGESTED_AT = "ingestedAt"
}

object IngestionFields {
    const val ID = "id"
    const val SOURCE = "source"
    const val RAW_PAYLOAD = "rawPayload"
    const val INGESTED_AT = "ingestedAt"
}

object AnalyticsFields {
    const val TERM = "term"
    const val TIMESTAMP = "timestamp"
    const val CONTEXT = "context"
    const val MESSAGE = "message"
}
