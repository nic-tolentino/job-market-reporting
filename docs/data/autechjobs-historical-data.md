# autechjobs Historical Data — Analysis & Ingestion Plan

**Status:** Analysis complete, ingestion pending
**Last Updated:** 2026-03-17
**Source:** `data/third-party/autechjobs/Cloud_SQL_Export_2026-03-17.sql`
**Received from:** Friend/external contributor (MySQL dump of their production database)

---

## File Location

SQL dumps from this source belong in:

```
data/third-party/autechjobs/
└── Cloud_SQL_Export_2026-03-17.sql   ← move here from data/
```

The `data/third-party/` directory is the home for all externally-sourced datasets that aren't
part of our own manifest/crawler pipeline. Future exports from this source should follow the
naming convention `Cloud_SQL_Export_YYYY-MM-DD.sql`.

---

## Dataset Overview

| Dimension | Value |
|-----------|-------|
| Database name | `autechjobs_production` |
| Engine | MySQL 5.7.44 (Google Cloud SQL) |
| Export date | 2026-03-17 |
| Tables | `company` (149 rows), `job` (161,239 rows) |
| Date range | 2010-09-21 → 2026-03-16 |
| Active period | 2020–present (~95% of data) |
| Geography | Australia & New Zealand (Sydney-first, then multi-city) |

### Year-by-year job volume

| Year | Rows |
|------|------|
| 2020 | 6,138 |
| 2021 | 30,942 |
| 2022 | 38,008 |
| 2023 | 21,850 |
| 2024 | 27,529 |
| 2025 | 27,785 |
| 2026 | 6,925 (Jan–Mar) |

---

## Schema

### `company` (149 rows)

```sql
CREATE TABLE `company` (
  `id`           int(11) NOT NULL AUTO_INCREMENT,
  `name`         varchar(255) NOT NULL,
  `description`  mediumtext,
  `visa_sponsor` tinyint(4) NOT NULL DEFAULT '0',
  `created_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `website`      varchar(512) DEFAULT NULL,
  `logo_url`     varchar(512) DEFAULT NULL,
  `jobs_page`    varchar(512) DEFAULT NULL
);
```

Key fields:
- `jobs_page` — direct URL to the company's careers/jobs page; 141/149 are populated
- `visa_sponsor` — boolean; useful signal for our own `visa_sponsorship` field
- `description` — company description text; useful for industries classification

### `job` (161,239 rows)

```sql
CREATE TABLE `job` (
  `id`               int(11) NOT NULL AUTO_INCREMENT,
  `company_id`       int(11) NOT NULL,
  `url`              varchar(1000) NOT NULL UNIQUE,
  `position`         varchar(255) NOT NULL,
  `department`       varchar(255) DEFAULT NULL,
  `description`      mediumtext,
  `summary`          mediumtext,
  `city`             varchar(255) DEFAULT NULL,
  `job_type`         varchar(255) DEFAULT NULL,
  `computer_percent` decimal(5,2) DEFAULT NULL,
  `business_percent` decimal(5,2) DEFAULT NULL,
  `classifier_data`  json DEFAULT NULL,
  `has_expired`      tinyint(4) NOT NULL DEFAULT '0',
  `scrape_count`     int(11) NOT NULL DEFAULT '1',
  `posted_at`        datetime DEFAULT NULL,
  `created_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `expired_at`       datetime DEFAULT NULL
);
```

Key fields:
- `url` — UNIQUE ATS-hosted job URL; source of ATS slug identification (see below)
- `has_expired` / `expired_at` — lifecycle tracking; useful for expiry prediction modelling
- `classifier_data` — JSON ML classifier output; may include tech-stack/domain labels
- `computer_percent` / `business_percent` — role type signals (technical vs. business)
- `department` — role categorisation (Software Engineering, Sales, etc.)

---

## ATS Distribution (from `job.url`)

Analysed against 41,532 parseable job URLs across the 161k rows:

| ATS | Jobs | % |
|-----|------|---|
| LEVER | 14,715 | 35.4% |
| SMARTRECRUITERS | 10,461 | 25.2% |
| Custom / Unknown | 6,167 | 14.8% |
| GREENHOUSE | 5,348 | 12.9% |
| WORKABLE | 2,994 | 7.2% |
| WORKDAY | 901 | 2.2% |
| CORNERSTONE | 593 | 1.4% |
| ASHBY | 197 | 0.5% |

### `jobs_page` URL ATS signals (149 companies)

| ATS | Companies |
|-----|-----------|
| LEVER | 7 |
| WORKABLE | 5 |
| SMARTRECRUITERS | 3 |
| ASHBY | 1 |
| CORNERSTONE (CSOD) | 1 |
| Custom / careers page | 123 |

---

## Company Matching Against Our Manifests

Of the 149 SQL companies:

| Status | Count |
|--------|-------|
| Matched to our manifests (by name) | 32 |
| Matched + already had correct ATS | 21 |
| Matched + new ATS extracted (applied) | 1 (`zip-co` → GREENHOUSE/zipcolimited) |
| **Not yet in our manifests** | **113** |

The 113 unmatched companies are predominantly AU/NZ tech companies and represent a high-quality
backfill opportunity — many have 500–4,000 historical job records each (Canva: 4,624; Xero: 1,678;
SafetyCulture: 555).

### Notable unmatched companies with known ATS slugs

| Company | ATS | Slug | SQL job count |
|---------|-----|------|---------------|
| Atlassian | LEVER | atlassian | 1,332 |
| SafetyCulture | LEVER | safetyculture-2 | 555 |
| AfterpayTouch | LEVER | afterpaytouch | 520 |
| Airwallex | LEVER + ASHBY | airwallex | 424 |
| Deputy | LEVER | deputy | 379 |
| Canva | LEVER + SMARTRECRUITERS | canva | 4,624 |
| Culture Amp | GREENHOUSE | cultureamp | 839 |
| The Iconic | GREENHOUSE | theiconic | 676 |
| Dovetail | GREENHOUSE | dovetail | 1 (sample) |

> Many of these companies already exist under slightly different names or IDs in our manifest.
> See `scripts/ats/parse_autechjobs.py` for the full cross-reference output.

---

## ATS Slugs Extracted (Ready to Apply)

Run `scripts/ats/parse_autechjobs.py --apply-to-manifests` to write all confirmed
matches. The script validates each slug against the live API before writing.

---

## Integration Plan

### Phase 1 — ATS Identification (no data ingestion yet) ✅ Partially done

1. ✅ Run `parse_autechjobs.py` to cross-reference company names and extract confirmed ATS slugs
2. ✅ Apply new `ats` fields to matched manifests
3. Add unmatched SQL companies to our manifest if they meet our inclusion criteria
   - Inclusion criteria: AU/NZ HQ or AU office, tech-adjacent industry, >50 historical jobs
   - ~40 companies are strong candidates; the rest are agencies, retail, or acquired

### Phase 2 — Seed Discovery

For each SQL company (matched or not):

1. Extract `company.jobs_page` URL as a new crawler seed (category: `careers`)
2. Flag staleness: these URLs are 1–5 years old — the first crawl will verify liveness
3. Add to manifest `crawler.seeds` array; mark source as `autechjobs`

Script: extend `parse_autechjobs.py --inject-seeds` (to be built)

### Phase 3 — Historical Job Ingestion (BigQuery)

Goal: backfill `raw_jobs` or a dedicated `autechjobs_historical` BigQuery table with the
161k rows for trend analysis.

#### Option A — Load into a separate BigQuery table

```
project: tech-market-insights
dataset: techmarket
table:   autechjobs_historical
```

**Schema mapping** (SQL → BigQuery):

| SQL field | BQ field | Notes |
|-----------|----------|-------|
| `job.url` | `applyUrl` | Unique job URL |
| `job.position` | `title` | Job title |
| `job.department` | `department` | Role category |
| `job.description` | `descriptionHtml` | Full job description |
| `job.city` | `location` | City string |
| `job.job_type` | `employmentType` | Full-time / Contract / etc. |
| `job.posted_at` | `postedAt` | Original post date |
| `job.has_expired` | `hasExpired` | Boolean |
| `job.expired_at` | `expiredAt` | Expiry timestamp |
| `company.name` | `companyName` | Denormalized for query ease |
| derived | `companyId` | Via name→manifest match |
| derived | `atsProvider` | Via URL pattern match |

**Load approach:**
1. Convert SQL dump to JSONL using `scripts/ats/parse_autechjobs.py --export-jsonl`
2. `bq load --source_format=NEWLINE_DELIMITED_JSON techmarket.autechjobs_historical output.jsonl`

#### Option B — Merge into existing `raw_jobs` table

Riskier — requires deduplication against existing rows on `applyUrl`. Prefer Option A initially
and merge selectively after validating field alignment.

### Phase 4 — Trend Backfill

Once historical data is in BigQuery, derive monthly `tech_job_counts` metrics back to 2020 using
the same aggregation queries as the live pipeline. This gives us 5+ years of trend data for
the analytics UI.

---

## Risks & Caveats

| Risk | Mitigation |
|------|------------|
| Stale ATS slugs — companies may have switched ATS since data was collected | Validate all extracted slugs against live API before writing to manifests |
| Overlapping company names — "The Iconic", "Finder" match to different entities | Manual review for multi-word or common-noun company names |
| `embed` Greenhouse slug — several companies share a Greenhouse "embed" widget board | Already filtered in `parse_autechjobs.py` and `extract_ats_from_apply_urls.py` |
| `classifier_data` JSON — unknown schema (proprietary ML output) | Inspect before relying on it; may not map cleanly to our industry taxonomy |
| `computer_percent` / `business_percent` — source model unknown | Treat as supplementary signal, not ground truth |
| PII in `job.description` — descriptions may contain recruiter contact details | Strip before loading to BigQuery; do not surface raw descriptions |

---

## Quick Reference

```bash
# Cross-reference companies, extract ATS slugs, dry run
python3 scripts/ats/parse_autechjobs.py --dry-run

# Apply confirmed ATS slugs to manifests
python3 scripts/ats/parse_autechjobs.py --apply-to-manifests

# Inject careers page URLs as crawler seeds
python3 scripts/ats/parse_autechjobs.py --inject-seeds --dry-run

# Export historical jobs to JSONL for BigQuery load
python3 scripts/ats/parse_autechjobs.py --export-jsonl --output data/third-party/autechjobs/historical_jobs.jsonl
```
