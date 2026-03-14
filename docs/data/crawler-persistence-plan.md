# Crawler Data Persistence Plan

## 1. Goal

To implement a robust data persistence strategy for the `crawler-service` that safely differentiates between **Definition** (declarative configuration) and **Operational State** (learned metrics), aligning with the principles established in ADR-001.

## 2. Architecture

### 2.1 Git Manifests (Definition)
Company seed URLs are structurally stable configurations and belong in Git. This allows for manual filtering of high-yield career boards (e.g., Atlassian Engineering vs. General Careers).

- **Storage**: `data/companies/{company-id}.json`
- **Structure**:
  ```json
  "crawler": {
    "seeds": [
      {
        "url": "https://careers.xero.com/jobs/?team=Engineering",
        "category": "tech-filtered"
      }
    ]
  }
  ```

### 2.2 BigQuery (Operational State)
The learning outcomes from each crawl run (metrics and signals) are highly variable and belong in a database.

- **Storage**: A new `crawler_seeds` table in BigQuery.
- **Updates**: Driven automatically by `crawler-service` execution and backend sync processes.
- **Fields**:
  - `company_id` (STRING)
  - `url` (STRING)
  - `category` (STRING) (Source of truth: Git Manifest)
  - `status` (STRING) (`ACTIVE` | `BLOCKED` | `TIMEOUT` | `STALE`)
  - `pagination_pattern` (STRING) (Regex or CSS selector discovered during crawl)
  - `last_known_job_count` (INTEGER)
  - `last_known_page_count` (INTEGER)
  - `last_crawled_at` (TIMESTAMP)
  - `last_duration_ms` (INTEGER)
  - `error_message` (STRING) (Max 500 chars, first line/exception only)
  - `consecutive_zero_yield_count` (INTEGER) (Counter for yield-based staleness)

- **Primary Key**: Composite key `(company_id, url)`.
- **Infrastructure**: Table creation managed via Terraform (or equivalent schema migration scripts).

## 3. The Execution Loop

1. **Manifest Sync (Backend -> BigQuery)**: 
   During the standard company manifest sync, the backend will upsert the `crawler.seeds` definition from the local JSON files into the `crawler_seeds` table, carrying over any existing operational state if the URL matches. **Category Ownership**: The Git manifest is the source of truth for the `category`. If Discovery mode suggests a category change, it requires a human PR to the Git manifest; the backend sync will always overwrite BigQuery's `category` with Git's definition.
2. **Crawl Request (Backend -> Crawler)**: 
   When scheduling a crawl, the backend retrieves a given seed configuration from `crawler_seeds` in BigQuery and includes it as `seedData` in the HTTP request payload to `crawler-service`. **Graceful Fallback**: If the row is missing (e.g., sync lag), the crawler receives empty `seedData` and performs a standard crawl without historical context, gracefully falling back rather than failing.
3. **Execution & Metric Emission (Crawler)**:
   The crawler enters **Targeted Mode** if `seedData.url` is present:
   - **Skip Board Discovery**: Skips the initial "homepage-to-careers" navigation phase.
   - **Entry Point**: Injects the `seedUrl` directly into the enqueue queue.
   - **Retention**: Applies the same **pagination exhaustion** (50-page cap) and **neighbor restrictions** (staying within the ATS path) as Discovery Mode.
   - **Budget Adjustments**: In Targeted Mode, `maxPages` is increased from 5 to 15 to allow for deeper filtered board exploration.
   - **Signal Emission**: Detects `PAGINATION_GROWTH/CONTRACTION` based on `lastKnownPageCount` and returns the `pagination_pattern` if rel="next" or query params are stable.
4. **State Update (Backend Ownership)**:
   The crawler is a stateless execution engine. Persistence is the responsibility of the **Kotlin Backend**. Upon receiving the response, the backend updates the `crawler_seeds` table with the latest metrics from `CrawlMeta`:
   - `last_known_page_count`
   - `last_known_job_count`
   - `last_crawled_at` (set to `now()`)
   - `last_duration_ms`
   - `status` (Mapped from crawler exit state: `ACTIVE`, `BLOCKED`, or `TIMEOUT`)
   - `error_message` (Truncated to first 500 chars)
   - `pagination_pattern` (Updated if a stable pattern is detected)
   - `consecutive_zero_yield_count` (**Backend-calculated**: incremented if `totalJobsFound == 0`, reset to 0 otherwise)

## 4. Lifecycle Management (The STALE Write Path)

The `STALE` status is managed by the **Backend Maintenance Job**:
- **Age-based**: Any seed with `last_crawled_at > 30 days` is marked `STALE`.
- **Yield-based**: If `last_known_job_count` drops to 0 for 3 consecutive runs (tracked via `consecutive_zero_yield_count`), the seed is marked `STALE`.
- **Effect**: `STALE` seeds are eligible for a **Deep Discovery Run** (forcing the crawler into Discovery Mode starting from the homepage to see if the careers URL has changed).

## 5. Failure Mitigation — Tiered Approach

| Tier | Mechanism | When to use |
|------|-----------|-------------|
| 1 | Structured log on receipt (immediate) | Always — zero cost, implement now |
| 2 | Retry with exponential backoff on BQ write failure | If transient BQ errors are observed |
| 3 | Dead-letter queue (Cloud Pub/Sub or Cloud Tasks) | If crawl volume makes log-based replay impractical |
| 4 | Idempotent re-crawl | Last resort — re-trigger the crawl if result is unrecoverable |

## 6. Required Schema Updates

### Backend Data Models
- Update `CompanyJsonDto` to include `crawler.seeds`.
- Create a `CrawlerSeed` Entity/DTO class for BigQuery operations.
- Create `CrawlerSeedRepository` for BigQuery insertions/updates.

### Crawler Service Types
- Extend `CrawlMeta` in `types.ts` to surface `PAGINATION_GROWTH`, `PAGINATION_CONTRACTION`, and `JOB_COUNT_CHANGE` as typed events or deltas.

## 7. Rollout Strategy
1. **Phase 1 (Crawler Side - COMPLETED)**: 
   Successfully implemented robots.txt enforcement, strict pagination caps, signal detection (GROWTH/CONTRACTION), and tech-filtering. Added comprehensive unit and integration tests covering:
   - **Pagination Cap Test**: Verified in Scenario 1.
   - **General Seed Filtering Test**: Verified in Scenario 2.
   - **Growth Signal Detection Test**: Verified in Scenarios 3 and 4.
2. **Phase 2 (Data Definition - Current Phase)**: Define the JSON Schema for `crawler.seeds` and write a migration script to prepopulate existing companies.
3. **Phase 3 (Backend Integration)**: Implement the BigQuery table creation (via Terraform/script), backend sync logic, and integration to pass `seedData` to the crawler and process the returned signals.
