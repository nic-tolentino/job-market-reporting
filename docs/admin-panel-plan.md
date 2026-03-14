# Admin Panel — Implementation Plan

## Overview

A protected internal tool for operating, monitoring, and curating the DevAssembly data
pipeline. It sits inside the existing `frontend/` React app as a `/admin` route family,
drawing data from two backends:

- **Crawler Service** (Node.js / Express) — company seed configs, crawl triggering,
  crawl run history
- **Backend BFF** (Kotlin / Spring Boot) — ingestion history, BigQuery job data,
  analytics, dead-letter queue, cache management

Both are already deployed to Cloud Run. The admin panel calls them directly — no new
gateway is needed.

---

## Context & Constraints

| Thing | Detail |
|---|---|
| Frontend stack | React 18 + Vite + TypeScript + Tailwind + React Query + Zustand + Recharts |
| Backend | Spring Boot 3 (Kotlin), existing `AdminController` with partial endpoints |
| Crawler service | Express (TypeScript), `/crawl` and `/crawl/batch` endpoints |
| Data warehouse | BigQuery dataset `techmarket` — tables `raw_jobs`, `raw_companies`, `raw_ingestions`, `crawler_seeds` (current seed state), `crawl_runs` (execution history) |
| Company source of truth | `data/companies/**/*.json` — 1,262 files, schema-validated |
| Cold storage | GCS `techmarket-bronze-ingestions` (90-day Coldline, 1-year Archive) |
| Background jobs | Cloud Tasks queue `sync-tasks` |
| Auth | No auth currently exists — must add before shipping (see §Authentication) |

### Data persistence architecture

Operational state is split across two BigQuery tables, both written by the Kotlin backend
after each crawl (see `docs/data/crawler-persistence-plan.md` for full schema):

- **`crawler_seeds`** — upserted after every crawl; holds current seed state
  (`last_known_job_count`, `last_crawled_at`, `status`, `consecutive_zero_yield_count`,
  `ats_provider`, `ats_direct_url`, etc.). Powers the company table's health columns.
- **`crawl_runs`** — append-only; one row per crawl execution. Powers history timelines,
  trend charts, and efficiency analytics. This table is new (defined in the persistence
  plan as part of Phase 3) and must be created before the admin panel's analytics views
  can be built.

The crawler service itself remains stateless — it returns `CrawlResponse` over HTTP and
the backend owns all persistence. The admin panel reads both tables via backend endpoints.

---

## Authentication

Admin routes must be protected before anything ships to production. Recommended approach:

**Short-term** — environment-variable token checked server-side. The backend adds an
`Authorization: Bearer <ADMIN_TOKEN>` check on all `/admin/**` endpoints. The frontend
stores the token in `sessionStorage` and sends it on every request. Token is set in Cloud
Run environment variables.

**Long-term** — Google OAuth via Identity-Aware Proxy (IAP) on the Cloud Run backend URL.
Free, zero-code, tied to the same Google workspace already used for GCP.

The `/admin` route in the frontend should show a login screen (token entry) if no token
is stored. No public registration — token is distributed out-of-band.

---

## Information Architecture

```
/admin
├── /                       → Dashboard
├── /companies              → Company table
│   └── /:id               → Company detail (slide-in panel)
├── /crawls                 → Crawl run history + active monitor
│   └── /:runId            → Single run detail
├── /pipeline               → Ingestion history, dead letters, dataset tools
├── /jobs                   → Job browser (extracted records)
├── /analytics              → Charts & optimisation signals
└── /settings               → Crawler config, cache, prompt templates
```

---

## Section Specifications

### 1. Dashboard `/admin`

**Purpose:** Immediate situational awareness. Open it, know the state of the system in
under 10 seconds.

**Layout:** Three rows.

**Row 1 — Health strip** (coloured status chips)
- Crawler service: reachable / unreachable (ping `/health`)
- Backend: reachable / unreachable
- Last successful sync: `N hours ago` (from `raw_ingestions` max `ingestedAt`)
- Dead letter queue depth: `N items` (red if > 0)
- Cloud Tasks queue: queue depth

**Row 2 — Key metrics** (6 big-number cards)
- Total companies in manifest: **1,262**
- Companies with ACTIVE seed: **N** (% of total)
- Total jobs in Silver layer (`raw_jobs` count)
- Jobs added in last 7 days
- Estimated Gemini cost last 24h (derived from `crawl_runs` token counts × model
  pricing — gives early warning of runaway crawls burning tokens on junk pages)
- Avg cost per job extracted last 24h (total cost ÷ `jobs_final` sum)

**Row 3 — Two panels side by side**

*Left: Recent activity feed*
Last 20 events across all event types, newest first:
- `[CRAWL] karbon — 32 jobs, 34s, ACTIVE`
- `[INGEST] dataset-abc123 — 847 rows, 2 failures`
- `[SYNC] company manifest sync — 3 added, 1 updated`
- `[HEALTH] 14 dead links detected`

*Right: Alert list*
Auto-generated from data state. Clears when resolved:
- N companies have been discovered as BLOCKED/NO_MATCH — needs manual seed
- N seeds have had 0 yield for 3+ consecutive runs (STALE)
- N ingestion dead letters awaiting retry
- N jobs with STALE url status (potential dead listings)

**Data sources:**
- Health: `GET /health` on crawler service + backend
- Metrics: BigQuery `raw_jobs` count, `raw_ingestions` latest record
- Activity feed: new endpoint `GET /admin/activity` (see §New API Endpoints)
- Alerts: derived from company manifest scan + BigQuery queries

---

### 2. Company Management `/admin/companies`

**Purpose:** The primary operational view. Managing 1,262 companies, their seed URLs,
crawl status, and ATS configs.

#### 2a. Company Table

Columns (all sortable):
| Column | Source | Notes |
|---|---|---|
| Company name + logo | manifest | |
| Verification level | manifest `verification_level` | colour-coded badge |
| ATS provider | manifest `ats.provider` | or "Unknown" |
| Seed status | manifest `crawler.seeds[0].status` | ACTIVE / STALE / BLOCKED / none |
| Discovery status | manifest `crawler.discovery.status` | BLOCKED / NO_MATCH / none |
| Last crawled | `crawler_seeds.last_crawled_at` | via backend admin endpoint |
| Jobs (last run) | `crawler_seeds.last_known_job_count` | with trend arrow from `crawl_runs` |
| Confidence (last run) | `crawl_runs` latest row | |
| HQ country | manifest | flag emoji |

**Filters (multi-select, stacks additively):**
- Seed status (ACTIVE / STALE / BLOCKED / none)
- Discovery status (BLOCKED / NO_MATCH / untried)
- ATS provider (all known providers)
- Verification level
- HQ country
- "Zero yield last run" toggle
- "Never crawled" toggle

**Saved filter presets** (one-click):
- *Discovery queue* — no ACTIVE seed, no discovery block, never crawled
- *Needs attention* — STALE seed OR 3+ consecutive zero-yield runs
- *Working well* — ACTIVE seed, >0 jobs last run, confidence >0.6
- *High ROI Discovery* — `employees_count > 500` AND no ACTIVE seed (large companies most
  likely to yield a high job count if discovery succeeds — highest-priority targets)
- *Flaky Seeds* — success rate < 50% over last 10 runs (seeds returning inconsistent
  results, candidates for URL review or category change)

**Batch operations** (checkbox + action dropdown):
- Trigger crawl (queued, respects concurrency limit)
- Set seed category (tech-filtered / general / careers)
- Mark discovery as SKIP
- Export to CSV

#### 2b. Company Detail Panel (slide-in from right)

Triggered by clicking any table row. Does not navigate away.

**Header:** Company name, logo, website link, verification level badge, ATS badge.

**Tabs:**

*Seeds tab*
- List of all seed URLs with status, category, last crawl result
- Inline edit: URL field, category dropdown, save button
- "Add seed" button → URL + category fields
- "Run crawl now" button per seed (single targeted crawl)
- `atsDirectUrl` hint shown if present (with "Use as seed" button)

*Crawl history tab*
- Timeline of last 20 crawl runs: date, duration, pages, jobs found, confidence, status
- Sparkline chart: jobs found over time
- Link to full run detail

*Jobs tab*
- Last N jobs extracted for this company (paginated)
- Title, location, posted date, ATS source, url status badge
- "View in job browser" link

*Config tab*
- Raw manifest JSON editor (Monaco or CodeMirror, schema-validated)
- Save writes to BigQuery `crawler_seeds` (seed-level config) and `raw_companies` (company
  metadata) via backend endpoints — **not** to the JSON files. The JSON manifests remain
  the human-curated source of truth; the admin panel targets the operational layer.
- Validation errors shown inline before save

---

### 3. Crawl Operations `/admin/crawls`

**Purpose:** Monitor running crawls, review history, trigger new runs.

#### 3a. Active Monitor

Live view (polls every 5s) when a batch crawl is running:

```
Batch Run #47 — started 3m ago
Progress: ████████░░░░░░░░ 23/150 companies
Est. remaining: 18m

Currently processing: notion.so
Queue: revolut, hnry, leonardo-ai, karbon ... (+127 more)

Recent completions:
  ✓ karbon        32 jobs   34s   GREENHOUSE
  ✓ airwallex      6 jobs   20s   —
  ✗ myob           0 jobs   17s   FAILED — Download triggered
```

Stop/pause button. Expandable log stream.

#### 3b. Crawl Run History

Table of all completed batch runs:
| Column | Notes |
|---|---|
| Run ID | short hash |
| Started / duration | |
| Companies | count attempted |
| Success rate | % returning >0 jobs |
| Total jobs found | |
| Triggered by | manual / scheduled |

Click to open run detail: per-company breakdown, failures list, aggregate stats.

#### 3c. Schedule Management

Table of scheduled crawl rules:
| Company / Group | Frequency | Last run | Next run | Enabled |
|---|---|---|---|---|
| All ACTIVE seeds | Weekly | 3 days ago | In 4 days | ✓ |
| STALE seeds | Monthly | 18 days ago | In 12 days | ✓ |
| karbon | Weekly | 1 day ago | In 6 days | ✓ |

"Add rule" button. Toggle enable/disable per rule. Frequency: daily / weekly / fortnightly
/ monthly.

(Implementation note: scheduling uses Cloud Scheduler → Cloud Tasks → Crawler Service.
Cloud Scheduler fires the cron; it enqueues a Cloud Tasks job, which POSTs to
`/crawl/batch` with retry semantics. The admin panel shows and edits Cloud Scheduler job
configs via backend endpoints — it does not implement the scheduler itself.)

#### 3d. Manual Crawl Trigger

Form:
- Company selector (autocomplete from manifest)
- OR: raw URL field + company ID (for discovery testing)
- Seed URL override
- Category override
- Max pages override
- "Run now" → POST to `/crawl`, shows live log stream inline

---

### 4. Data Pipeline `/admin/pipeline`

**Purpose:** Manage the Bronze → Silver ingestion flow, Apify datasets, dead letters.

#### 4a. Ingestion History

Table of all ingestion events from `raw_ingestions`:
| Dataset ID | Source | Ingested at | Row count | Status | Duration |
|---|---|---|---|---|---|
| abc123 | Apify | 2 hours ago | 847 | SUCCESS | 4.2s |
| def456 | Crawler | 1 day ago | 134 | PARTIAL (3 failed) | 12s |

Click to see: raw JSON preview (first record), row-level failures, re-process button.

#### 4b. Dead Letter Queue

All ingestion records that failed and were not retried:
- Dataset ID, error message, failed at, retry count
- "Retry" button (re-submits to Cloud Tasks)
- "Discard" button (marks as ignored)
- "Retry all" batch action

Filtered view: by error type (validation / network / schema mismatch).

#### 4c. Manual Ingest

```
Dataset ID: [___________________________]  Source: [Apify ▾]
                                          [Ingest Now]
```

Triggers `POST /admin/pipeline/ingest`. Shows result inline.

#### 4d. Reprocess

Re-run Silver-layer processing on a historical Bronze record (e.g. after fixing an
extraction bug):

- Select ingestion ID (autocomplete from history)
- Confirmation: "This will re-map N rows through the current RawJobDataMapper logic"
- "Reprocess" → calls existing `reprocessHistoricalData()` backend endpoint

#### 4e. Company Manifest Sync

Manual trigger for `CompanySyncService` (sync `data/companies/**` → BigQuery
`raw_companies`):
- "Sync now" button
- Progress: N added, N updated, N unchanged, N errors
- Error list: which companies failed validation

---

### 5. Job Browser `/admin/jobs`

**Purpose:** Inspect extracted jobs, spot-check quality, manage stale listings.

#### 5a. Job Table

Paginated (50/page). Columns:
| Column | Notes |
|---|---|
| Title | |
| Company | with logo |
| Location | |
| Seniority | |
| Technologies | tag list |
| Source | Crawler / Greenhouse / Lever / etc. |
| URL status | ACTIVE / STALE / BLOCKED (from health check) |
| Posted at | |
| Ingested at | |

Filters:
- Company (autocomplete)
- Source / ATS provider
- URL status
- Seniority level
- Country
- Date range (posted / ingested)
- Technology (autocomplete)

Click row → job detail modal: all fields, raw JSON, applyUrl link, "Mark as reviewed",
"Flag as incorrect".

#### 5b. Health Check

Trigger `JobHealthCheckService` for a specific company or all jobs:
- "Run URL health check" button
- Progress indicator
- Results: N ACTIVE, N STALE, N BLOCKED, N TIMEOUT

---

### 6. Analytics `/admin/analytics`

**Purpose:** Understand pipeline performance and identify optimisation opportunities.
All charts use Recharts (already in frontend).

#### 6a. Crawl Efficiency

*Bar chart: Time spent vs jobs returned per company (last run)*
Sorted by jobs/second descending. Immediately shows which companies give best ROI and
which are burning time for nothing.

*Scatter plot: Crawl duration (x) vs jobs found (y)*
Coloured by ATS provider. Clusters reveal which ATS types are fast/slow and
high/low yield.

#### 6b. ATS Provider Breakdown

*Pie chart: ATS distribution across all companies with seeds*

*Table: Per-ATS stats*
| ATS | Companies | Avg jobs/crawl | Avg confidence | Zero-yield rate | Avg duration | Est. cost/crawl |
|---|---|---|---|---|---|---|
| ASHBY | 12 | 47 | 0.65 | 8% | 38s | $0.004 |
| GREENHOUSE | 34 | 18 | 0.63 | 22% | 31s | $0.006 |
| WORKDAY | 28 | 0 | 0.00 | 100% | 71s | $0.011 |
| — (unknown) | 89 | 4 | 0.55 | 61% | 24s | $0.003 |

Cost derived from `crawl_runs` token counts × current model pricing. Workday's high cost +
100% zero-yield makes it the most wasteful ATS — call this out prominently.

**This table is the primary optimisation signal.** Workday = 100% zero-yield means all
Workday companies need manual seed URLs. Ashby = most reliable.

#### 6c. Tech Filter Funnel

Per company (or aggregate across all):
```
Gemini extracted:  1,247 jobs
Passed validation:   891 jobs  (71%)
Passed tech filter:  412 jobs  (46% of valid)
After dedup:         389 jobs  (94% of tech-filtered)

Noise ratio: 56% (835 of 1,247 jobs dropped — non-tech roles on general seeds)
```

**Noise Ratio** = jobs dropped by the tech filter ÷ total Gemini-extracted jobs. A high
noise ratio (>50%) on a company with a `general` or `careers` seed means switching that
seed to a tech-specific filter board would improve cost-efficiency significantly.

Large drops at "passed tech filter" indicate: category is wrong (general when should be
tech-filtered), or tech keywords need expansion.

#### 6d. Discovery Success Rates

*Stacked bar: Discovery outcome by ATS provider*
Categories: Found jobs / Found page but 0 jobs (ATS iframe) / No careers link / Error

Answers: "For which ATS types does discovery actually work?"

#### 6e. Seed Health Over Time

*Line chart: Per-company job yield over last N crawl runs*
Shows GROWTH/CONTRACTION trends. Companies consistently dropping are candidates for
seed URL review.

*Table: Consecutive zero-yield runs*
Companies that have returned 0 jobs for 2, 3, 4+ consecutive runs — ordered by streak
length. These are your stalest seeds.

#### 6f. Confidence Distribution

*Histogram: Extraction confidence scores across all jobs*
A spike at 0.50–0.55 (barely passing) is a warning sign that extraction quality is
marginal. Should mostly be 0.60+.

---

### 7. Settings `/admin/settings`

**Purpose:** Low-frequency config changes. Not the primary daily-use section.

**Extraction prompt templates**
- View/edit the default Gemini extraction prompt
- Per-company prompt overrides (stored in manifest `crawlConfig.extractionPrompt`)
- "Test prompt" → run against a live URL inline

**Cache management**
- View cache keys + TTLs (from Spring Cache)
- "Invalidate all" button
- "Invalidate for country" dropdown

**Crawler config**
- Edit global defaults: max pages (discovery / targeted), pagination limit,
  inter-extraction delay, network idle timeout
- These map to `DISCOVERY_*` and `PAGE_BUDGET_*` constants — changes write to an
  env-var override file and require crawler service restart

**Tech keywords**
- View/edit `TECH_KEYWORDS` and `NEGATIVE_KEYWORDS` arrays
- Changes apply to future crawls only

---

## New API Endpoints Required

### Crawler Service (Node.js / Express)

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/companies` | Paginated list of companies from manifests with last crawl metadata |
| `GET` | `/admin/companies/:id` | Single company: manifest + crawl history |
| `PATCH` | `/admin/companies/:id/seed` | Update a seed URL (writes back to JSON file) |
| `POST` | `/admin/companies/:id/crawl` | Trigger a single targeted crawl |
| `GET` | `/admin/crawls` | Paginated crawl run history |
| `GET` | `/admin/crawls/active` | Currently running crawls (SSE stream) |
| `GET` | `/admin/crawls/:runId` | Single run detail |
| `GET` | `/admin/activity` | Last 50 events across all event types |

### Backend BFF (Kotlin / Spring Boot) — extends existing `AdminController`

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/pipeline/ingestions` | Paginated ingestion history from `raw_ingestions` |
| `GET` | `/admin/pipeline/dead-letters` | Failed ingestion records |
| `POST` | `/admin/pipeline/dead-letters/:id/retry` | Re-queue a dead letter |
| `DELETE` | `/admin/pipeline/dead-letters/:id` | Discard a dead letter |
| `POST` | `/admin/pipeline/ingest` | Manually ingest a dataset by ID |
| `POST` | `/admin/pipeline/reprocess/:ingestionId` | Re-run Silver mapping on a Bronze record |
| `GET` | `/admin/jobs` | Paginated job browser (filters: company, source, status, country) |
| `GET` | `/admin/analytics/ats` | ATS provider breakdown stats |
| `GET` | `/admin/analytics/efficiency` | Per-company crawl efficiency metrics |
| `GET` | `/admin/analytics/funnel` | Tech filter funnel aggregate |
| `GET` | `/admin/analytics/confidence` | Confidence score histogram |
| `POST` | `/admin/manifest/sync` | Trigger CompanySyncService |
| `GET` | `/admin/health/full` | Deep health check (crawler service + BQ + GCS) |

---

## Persistence Prerequisites

The admin panel's data sources map directly to the two-table architecture in
`docs/data/crawler-persistence-plan.md`. Both BigQuery tables must exist before the
corresponding admin views can be built:

| Admin panel view | Requires |
|---|---|
| Company table — health columns (last crawled, jobs, status) | `crawler_seeds` (Phase 3 of persistence plan) |
| Company detail — crawl history timeline | `crawl_runs` (Phase 3 of persistence plan) |
| Analytics — efficiency scatter, ATS leaderboard, confidence histogram | `crawl_runs` |
| Analytics — seed health / consecutive zero-yield | `crawler_seeds` |
| Pipeline — ingestion history, dead letters | `raw_ingestions` (already exists) |
| Jobs browser | `raw_jobs` (already exists) |

**Sequencing implication:** the company table and pipeline views can be built immediately
against existing tables. The crawl history and analytics views are gated on Phase 3 of
the persistence plan completing. Build in that order.

---

## Implementation Phases

### Phase 1 — Structural foundation (1 week)
*Gets the admin panel shell and most-used views working.*

- [ ] Add `/admin` route family to React Router, protected by token auth
- [ ] `AdminLayout` component: sidebar nav, header with user indicator, toast system
- [ ] `GET /admin/companies` backend endpoint (joins manifests + `crawler_seeds`)
- [ ] Company table page: columns, sorting, basic filters, pagination
- [ ] Company detail panel: Seeds tab (view + edit), Crawl history tab (reads `crawl_runs`)
- [ ] Manual crawl trigger: single company form → live log via SSE

**Deliverable:** You can see all companies, edit seed URLs, and trigger crawls without
touching JSON files.

### Phase 2 — Pipeline observability (1 week)
*Makes the ingestion pipeline visible and operable.*

- [ ] Backend: `GET /admin/pipeline/ingestions` (query `raw_ingestions`)
- [ ] Backend: dead letter queue read + retry endpoints
- [ ] Backend: manual ingest + reprocess endpoints
- [ ] Pipeline page: ingestion history table, dead letter list, manual ingest form
- [ ] Dashboard page: health strip, key metric cards, alert list
- [ ] Activity feed (aggregates crawl history + ingestion events)

**Deliverable:** Full pipeline visibility. No more blind spots between crawl and ingest.

### Phase 3 — Analytics (1 week)
*Turns data into optimisation signals.*

- [ ] Migrate to BigQuery `crawl_runs` table (replace JSON manifest history)
- [ ] Backend: analytics endpoints (ATS breakdown, efficiency, funnel, confidence)
- [ ] Analytics page: ATS leaderboard table, efficiency scatter, confidence histogram
- [ ] Crawl history page: batch run list, active monitor with SSE progress
- [ ] Company table: add "last crawled", "jobs last run", "trend" columns

**Deliverable:** You can identify which companies and ATS types need attention without
manually querying BigQuery.

### Phase 4 — Jobs browser + quality tools (1 week)
*Data quality inspection and curation.*

- [ ] Backend: paginated job browser with filters
- [ ] Backend: URL health check trigger
- [ ] Jobs page: table, filters, detail modal
- [ ] Analytics: discovery success rates, seed health over time, tech filter funnel
- [ ] Company detail: Jobs tab

**Deliverable:** Full job-level visibility and quality management.

### Phase 5 — Schedule management + settings (ongoing)
*Operational polish.*

- [ ] Schedule management table (CRUD on Cloud Tasks cron config)
- [ ] Settings page: prompt templates, tech keywords editor, cache controls
- [ ] Batch operations on company table
- [ ] Saved filter presets

---

## Frontend Component Plan

All components live under `frontend/src/admin/`.

```
src/admin/
├── AdminApp.tsx               # Root: auth check, layout, router
├── AdminLayout.tsx            # Sidebar + header shell
├── pages/
│   ├── DashboardPage.tsx
│   ├── CompaniesPage.tsx
│   ├── CompanyDetailPanel.tsx
│   ├── CrawlsPage.tsx
│   ├── ActiveCrawlMonitor.tsx
│   ├── PipelinePage.tsx
│   ├── JobsPage.tsx
│   ├── AnalyticsPage.tsx
│   └── SettingsPage.tsx
├── components/
│   ├── CompanyTable.tsx       # Sortable/filterable table
│   ├── FilterBar.tsx          # Multi-select filter row
│   ├── CrawlRunRow.tsx
│   ├── MetricCard.tsx         # Big-number KPI card
│   ├── StatusBadge.tsx        # Colour-coded status chip
│   ├── SeedEditor.tsx         # Inline seed URL form
│   ├── ManualCrawlForm.tsx
│   ├── LogStream.tsx          # SSE log tail
│   ├── AtsBreakdownTable.tsx
│   └── charts/
│       ├── EfficiencyScatter.tsx
│       ├── ConfidenceHistogram.tsx
│       ├── FunnelBar.tsx
│       └── YieldSparkline.tsx
├── hooks/
│   ├── useCompanies.ts        # React Query wrappers
│   ├── useCrawlRuns.ts
│   ├── usePipeline.ts
│   ├── useAnalytics.ts
│   └── useSSE.ts              # Active crawl live stream
├── lib/
│   ├── adminApi.ts            # API client for crawler service admin endpoints
│   ├── backendAdminApi.ts     # API client for backend admin endpoints
│   └── auth.ts                # Token storage + header injection
└── types/
    └── admin.ts               # Admin-specific TypeScript types
```

---

## Open Questions

1. **Auth mechanism** — token-in-env (fast) vs Google IAP (proper). IAP is strongly
   recommended for anything handling production data; it requires a Load Balancer in front
   of Cloud Run but is already in the GCP account.

2. **Crawl run persistence** ✅ *Resolved* — Go straight to BigQuery `crawl_runs` (defined
   in the persistence plan, Phase 3). No JSON manifest intermediary. The `crawl_runs` table
   is append-only and is the authoritative history record. Build analytics views directly
   against it.

3. **Manifest writes from the admin panel** ✅ *Resolved* — Admin panel seed/config edits
   write to BigQuery (`crawler_seeds`, `raw_companies`) via backend endpoints. The JSON
   manifests in `data/companies/` remain the human-curated source of truth and are only
   updated via git PRs (manually, or via the sync-back script defined in the persistence
   plan). Firestore was considered as an alternative operational store but rejected — it
   would add a third data store with no benefit over the already-designed BigQuery tables.

4. **Scheduler implementation** ✅ *Resolved* — Cloud Scheduler → Cloud Tasks → Crawler
   Service. Cloud Scheduler manages the recurring cron cadence; it enqueues Cloud Tasks
   jobs for concurrency control and retry. The admin panel's Schedule Management view
   reads/writes Cloud Scheduler job configs via backend endpoints.

5. **Admin panel deployment** — same Vercel deployment as the public frontend (just
   new routes, protected by token) vs separate deployment. Same deployment is simpler
   and fine as long as auth is solid.
