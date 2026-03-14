# Crawler Efficiency Strategy: Discovery vs. Harvesting

## Problem Statement
Broad site crawling is computationally expensive and hits token limits (TPM) on complex corporate sites. Site traversal can consume 80% of a crawl's budget before reaching relevant job boards. By separating "Discovery" (finding boards) from "Harvesting" (extracting jobs), we can reduce costs by >90% while improving data freshness.

## Proposed Strategy: Two-Tier Execution

### 1. Discovery Mode (The "Scout")
- **Objective**: Identify high-value "Seed URLs" (Index Pages).
- **Triggers**:
    - **Staleness**: Seed age > 30 days.
    - **Low Yield**: Harvesting returns < 50% of the historical average for that seed.
    - **Failure**: Harvesting returns 0 jobs or persistent 404/410 errors.
- **Classification (Signal Quality)**:
    - `tech-filtered`: Page already scoped to tech roles. **Density > 60%**.
    - `general`: All-company listing. **Density < 60%**. Requires post-extraction filtering.
    - `unknown`: Discovered but not yet classified. Harvested with `general` filters by default.
- **Parameters & Bounds**:
    - **Hard Cap**: 100 pages maximum.
    - **Circuit Breaker**: Stop early once 3 distinct **High-Confidence** seed candidates are found.
        - *High-Confidence Heuristic*: URL match (`/jobs`) + Pagination found + Tech Density > 60%.
    - **Failure Fallback**: If Discovery finds 0 candidates, the system triggers a `DISCOVERY_FAILED` alert for manual URL entry. No LLM extraction attempted.
    - **Rate Limiting**: Runs at lower concurrency to avoid triggering bot defenses during deep traversal.

### 2. Harvesting Mode (The "Surgical Strike")
- **Objective**: Efficiently extract fresh jobs from known high-value pages.
- **Post-Extraction Filtering (for `general` or `unknown` seeds)**:
    - Jobs are matched against a **Technology Keyword List**: `engineer`, `developer`, `architect`, `sre`, `data`, `platform`, `security`, `infrastructure`, `automation`, `devops`, `ml`, `ai`, `qa`, `analyst`.
    - **Note**: This filter is broad and may capture roles like "Sales Analyst" or "Data Entry". This is a known trade-off for v1 to ensure high recall.
    - If a role fails this filter, it is logged but **dropped** before database ingestion.
- **Intelligent Traversal (Pagination Exhaustion)**:
    - The crawler always exhausts **Pagination Links** (numeric/offset increments) to ensure capture of hiring ramps.
    - **Pattern Resilience**: If a stored `paginationPattern` fails to match or returns a 404, the crawler falls back to autonomous re-detection or marks the seed as `stale`.
    - Changes in `lastKnownPageCount` are logged as **Growth/Contraction Signals**.
- **Neighbor Restriction (Anti-Leakage)**:
    - Enqueuing in harvest mode is restricted to:
        1. **Same Hostname/Subdomain** (e.g., `careers.xero.com`).
        2. **Same Path Prefix** (e.g., `*/jobs/*`).
        3. **Negative Filter**: Avoid links containing conflicting keywords (`marketing`, `sales`).
- **JS Rendering**: The crawler strictly uses **Playwright** to ensure full support for JS-heavy boards (Ashby, Greenhouse, Lever, etc.).

## Technical Implementation

### Category Handling & Inference
Categories are user-supplied inputs to the crawl request. During Discovery, Gemini classifies the page type; if >60% of detected titles are tech-related, the seed is promoted to `tech-filtered`.

### Persistence & Schema
```json
{
  "companyId": "xero",
  "crawlSeeds": [
    {
      "url": "https://careers.xero.com/jobs/?team=Engineering",
      "category": "tech-filtered",
      "status": "active", // active | stale | failed | unverified
      "paginationPattern": "?page={n}",
      "lastKnownPageCount": 2,
      "lastKnownJobCount": 26,
      "lastVerified": "2026-03-14"
    }
  ]
}
```

## Verification & QA Plan
1. **Recall**: Seed Harvesting finds ≥ 95% of jobs from a deep Broad Crawl.
2. **Precision**: Zero "Department Leakage" on `tech-filtered` seeds.
3. **Efficiency**: Page visitors < 15% of equivalent Broad Crawl.

### Regression Suite
- Maintain a `SeedRegistry` of 5+ complex companies (Xero, Airwallex, etc.).
- Run monthly "Differential Tests" comparing deep broad crawls vs. surgical seed harvests.
- Alert on any deviation > 10% in job count.
