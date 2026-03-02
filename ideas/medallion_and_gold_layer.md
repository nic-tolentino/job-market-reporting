# Idea: Medallion Architecture Shift & Gold Layer monthly Trends

This document outlines a proposal to restructure the data pipeline into a multi-layer Medallion architecture, enabling long-term historical analysis and high-performance monthly trends.

## 1. Architecture Overview

| Layer | Table(s) | Source | Logic |
| :--- | :--- | :--- | :--- |
| **Bronze** | `raw_ingestions` | External | Immutable raw JSON. Permanent audit trail. |
| **Silver** | `raw_jobs`, `raw_companies` | **Bronze** | Structured & cleansed. Holds detail-heavy data until aggregated. |
| **Gold** | `trends_*`, `market_stats` | **Silver** | **Physical Tables.** Materialized monthly aggregates & company states. |

## 2. Key Improvements

### Enhanced Company Metadata
- **`last_updated_at`**: Track when company info (logo, description) was last refreshed.
- **Historical Extraction**: Extract metadata even from "stale" jobs to build 100% complete company profiles.

### Physical Gold Layer & Monthly Metrics
Move from virtual queries to physical tables for performance and longer retention:
- **`monthly_tech_trends`**: Jobs per technology aggregated by month.
- **`monthly_company_trends`**: Job counts per company by month.
- **`monthly_geo_trends`**: Jobs per Country/City by month.
- **`tech_adoption_by_company`**: Track which companies use which tech, based on "active" roles (< 6 months).

### Job Lifecycle Tracking
- **`last_seen_at`**: Track how long a job posting remains active.
- **Metric**: Calculate "Average Job Open Time" as a market health indicator.

## 3. Storage Efficiency (Pruning)
- **Retention**: Prune roles from the **Silver** layer (e.g., older than 6 months) **only after** they have been aggregated into the **Gold** tables.
- **Recovery**: Since **Bronze** is permanent, the Silver/Gold layers can always be reconstructed if logic changes.

### Optimized Mapping Pipeline (3-Pass)
To address processing overhead, the Silver mapping from Bronze will follow a tiered approach:
- **Pass 1: Metadata Extraction**: Parse only what's needed for deduplication (ID, Company, Title, Seniority, PostedDate, Location). Resolve standardized geography (City, State, Country) upfront.
- **Pass 2: Deduplication**: Group by Company and Normalized Title. Identify the most recent "Primary Record" for each role.
- **Pass 3: Full Processing**: Execute "heavy" regex (Technology extraction, Salary parsing, Description sanitization) **ONLY** for the primary record of each unique role.
- **Modularity**: Split the parsing of **Companies** and **Jobs** into separate, testable functions to keep the mapper clean and maintainable.

### Standardization
- **Seniority**: Map varied industry levels to a standard sealed set (e.g., `INTERN`, `JUNIOR`, `MID`, `SENIOR`, `LEAD`, `PRINCIPAL`, `EXECUTIVE`) for better cross-industry reporting.

## 4. Implementation Steps (Tackle Later)
1. Update BigQuery schemas to include `last_updated_at` (Company) and `last_seen_at` (Job).
2. Update `RawJobDataParser.kt` with a standardized seniority mapping function.
3. Refactor `RawJobDataMapper.kt` to move from a "parse-all" approach to the 3-pass optimized pipeline.
4. Stop filtering 6-month-old jobs during the Bronze->Silver transition.
5. Create `GoldDataSyncService` for monthly trend materialization.
6. Add Silver layer pruning to the main sync pipeline.
