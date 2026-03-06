# Data Pipeline — Architecture, Review & Roadmap

This document captures the current state of the Tech Market data pipeline, recent refactoring work, test coverage analysis, and the future roadmap for Gold layer development.

---

## 1. Architecture Overview (Medallion Pattern)

```mermaid
graph LR
    A["Apify Scraper"] --> B["ApifyWebhookController"]
    B --> C["JobDataSyncService"]
    C --> D["Bronze Layer\n(raw_ingestions)"]
    C --> E["RawJobDataMapper"]
    E --> F["SilverDataMerger"]
    F --> G["Silver Layer\n(raw_jobs, raw_companies)"]
    G --> H["API Controllers"]
    H --> I["Frontend"]
    G -.-> J["Gold Layer\n(trends_*, market_stats)"]
```

| Layer | Table(s) | Source | Logic |
| :--- | :--- | :--- | :--- |
| **Bronze** | `raw_ingestions` | External (Apify) | Immutable raw JSON. Permanent audit trail. |
| **Silver** | `raw_jobs`, `raw_companies` | Bronze | Structured & cleansed. Merged with existing data on each sync. |
| **Gold** *(planned)* | `trends_*`, `market_stats` | Silver | Materialized monthly aggregates & company states. |

### Key Components

| Component | File | Responsibility |
|-----------|------|---------------|
| Orchestrator | `JobDataSyncService.kt` | Coordinates fetch → ingest → map → merge → persist |
| Transformer | `RawJobDataMapper.kt` | Filter → Group by Role → Group by Opening → Assemble |
| Parser | `RawJobDataParser.kt` | Location, tech, seniority, salary extraction |
| Merger | `SilverDataMerger.kt` | Merges new records with existing Silver data |
| ID Generator | `IdGenerator.kt` | Centralized slug/ID generation using `.` segment separators |
| PII Sanitizer | `PiiSanitizer.kt` | Redacts emails & phone numbers from free-text fields |

### Completed Refactoring
- ✅ Centralized ID generation into `IdGenerator` utility
- ✅ Switched to `.` segment separators: `{company}.{country}.{title}.{date}`
- ✅ Flattened `companyId` (removed `company/` prefix)
- ✅ Added dual-key job lookup (canonical slug + platform IDs)
- ✅ Implemented `SilverDataMerger` for non-destructive data updates
- ✅ Split mapping pipeline into testable stages (filter → group → assemble)
- ✅ Extracted PII sanitization into reusable `PiiSanitizer` utility
- ✅ Fixed stale `JobRecord.jobId` comment

---

## 2. Test Coverage

| Test File | Tests | Covers |
|-----------|-------|--------|
| `RawJobDataMapperTest` | 5 | Filter, grouping, lifecycle, assembly, full pipeline |
| `RawJobDataParserTest` | 20 | All 6 parser functions (country, location, work model, seniority, tech, salary, date) |
| `SilverDataMergerTest` | 2+ | Job merge, company merge |
| `JobDataSyncServiceTest` | 3 | Fetch-merge-delete-save, reprocess, malformed Bronze handling |
| `CompanyMapperTest` | 1 | Canonical job ID mapping |
| `AnalyticsMapperTest` | 1 | Landing page stats |
| `JobQueriesTest` | 2 | Similar jobs SQL generation |
| `SqlSafetyTest` | 7 | SQL injection + backtick wrapping |
| `TechFormatterTest` | — | Tech name formatting |
| `IdGeneratorTest` | 8 | Company IDs, job IDs, slugify, edge cases |
| `PiiSanitizerTest` | 7 | Email/phone redaction, null handling, clean text |

**Total: 11 test files, ~56+ test cases**

### Remaining Test Expansion
| Target | Items |
|--------|-------|
| `SilverDataMergerTest` | +4: null salary, identical timestamps, salary range merge, no-op merge |
| `RawJobDataMapperTest` | +2: `parseJobDetails` / `parseCompanyMetadata` isolation |
| `CompanyMapperTest` | +2: empty results, null company fields |
| `JobDataSyncServiceTest` | +1: empty dataset abort |

---

## 3. Improvements & Roadmap

### 🟡 Priority 2 — Code Quality

| Item | Detail |
|------|--------|
| Non-transactional Delete-then-Insert | Crash between delete and save = data loss. Consider staging table swap. |
| Incremental reprocessing | `reprocessHistoricalData` wipes Silver tables; add date-range filtering as data grows. |

### 🟢 Priority 3 — Feature Enhancements

#### A. Job Expiry / Staleness Detection
- Add `status` field (`ACTIVE`, `LIKELY_CLOSED`, `EXPIRED`)
- Scheduled task marks jobs `LIKELY_CLOSED` if `lastSeenAt` > 30 days
- Frontend filters or visually differentiates stale listings

#### B. Data Quality Monitoring
- `/api/admin/health` endpoint returning: total jobs/companies, data quality score, last sync time, Bronze→Silver compression ratio

#### C. Salary Normalization
- Detect currency (NZD, AUD, EUR, USD)
- Normalize to annual figures (detect "per hour", "per month")
- Store currency alongside min/max

#### D. Multi-Source Support
- Add `source` discriminator to Bronze layer
- Make `RawJobDataMapper` source-aware for different platform layouts

#### E. Cross-Sync Deduplication
- Current dedup is within a single sync batch by `(company, country, title)`
- `SilverDataMerger` handles matching `jobId`s, but a fuzzy-match fallback would catch reopened roles with slightly different date parts

---

## 4. Gold Layer — Future Implementation

### Physical Tables (Monthly Metrics)
| Table | Content |
|-------|---------|
| `monthly_tech_trends` | Jobs per technology aggregated by month |
| `monthly_company_trends` | Job counts per company by month |
| `monthly_geo_trends` | Jobs per Country/City by month |
| `tech_adoption_by_company` | Which companies use which tech, based on active roles (< 6 months) |

### Job Lifecycle Metrics
- **`last_seen_at`** *(already implemented)*: Tracks how long a posting remains active
- **Metric**: "Average Job Open Time" as a market health indicator

### Storage Efficiency (Pruning)
- Prune roles from Silver layer (e.g., older than 6 months) **only after** aggregation into Gold tables
- Bronze is permanent → Silver/Gold can always be reconstructed

### Standardization
- Map seniority levels to a sealed set: `INTERN`, `JUNIOR`, `MID`, `SENIOR`, `LEAD`, `PRINCIPAL`, `EXECUTIVE`

### Implementation Steps
1. Create `GoldDataSyncService` for monthly trend materialization
2. Build BigQuery schemas for Gold tables
3. Add Silver layer pruning to the main sync pipeline
4. Standardize seniority mapping in `RawJobDataParser`
5. Add incremental reprocessing support (date-range filtering)
