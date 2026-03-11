# ATS Integration Plan — Self-Hosted AI Crawler

A detailed implementation plan for a self-hosted Crawlee + Gemini Flash crawler that provides broad job data coverage across all 1,257 companies, with progressive optimisation as we learn the ATS landscape.

---

## 1. Strategy: Crawl First, Optimise Later

### 1.1 The Problem

87.8% of our 1,257 companies have **no identified ATS**. The current approach — build specific ATS integrations first, add a generic fallback later — is premature optimisation. We're guessing at the distribution before we have data.

### 1.2 The Approach

Invert the strategy:

1. **Phase 1 — General AI Crawler**: Deploy a self-hosted Crawlee + Gemini Flash pipeline that can crawl *any* company's career page and extract structured job data. This gives us immediate broad coverage at minimal cost.
2. **Phase 2 — Observe & Learn**: The crawler naturally reveals which ATS systems companies use, what page structures are common, and where extraction quality is weakest. Use this data to update company manifests and build a real ATS distribution picture.
3. **Phase 3 — Targeted Optimisation**: Build specific ATS API integrations (Lever, Ashby) or structured scrapers only where the data proves it's worth it — where API data is meaningfully richer or more reliable than crawled data.

### 1.3 Why Crawlee + Gemini Flash?

| Criterion | Choice | Reasoning |
|:---|:---|:---|
| **Crawler engine** | Crawlee (`PlaywrightCrawler`) | Open-source, built by the Apify team. Handles JavaScript rendering, anti-bot, pagination. Runs in Docker on Cloud Run. |
| **LLM extraction** | Gemini 2.0 Flash | $0.075/1M input + $0.30/1M output. ~$0.0001/page. We already use GCP. |
| **Hosting** | Cloud Run (Docker) | Fits our existing infrastructure. Scales to zero. Free tier covers our volume. |
| **Alternative considered** | Crawl4AI | Python-based, newer. Less mature ecosystem than Crawlee. Could revisit if Node.js becomes a burden. |

> [!TIP]
> The entire crawler is a **separate microservice** from our Kotlin backend. It accepts HTTP requests (crawl company X), returns structured JSON, and the backend treats it like any other `AtsClient`. This keeps the Kotlin codebase clean.

---

## 2. Architecture

### 2.1 System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  Cloud Scheduler (nightly 2am NZST)                             │
│      │                                                          │
│      ▼                                                          │
│  Cloud Tasks Queue                                              │
│      │  (1 task per company, rate-limited 2/sec)                │
│      ▼                                                          │
│  Kotlin Backend (Cloud Run)                                     │
│      │  POST /api/internal/ats/sync/{companyId}                 │
│      │                                                          │
│      ├──▶ ATS Client (Greenhouse, Lever, etc.)                  │
│      │       Direct API call → Raw JSON → Bronze                │
│      │                                                          │
│      └──▶ CrawlerClient (new)                                   │
│              │  POST http://crawler-service/crawl                │
│              │  { url, companyId, crawlConfig }                  │
│              ▼                                                   │
│        ┌──────────────────────────────────────┐                  │
│        │  Crawler Service (Cloud Run)         │                  │
│        │  Node.js + Crawlee + Playwright      │                  │
│        │                                      │                  │
│        │  1. PlaywrightCrawler fetches page    │                  │
│        │  2. Content extractor strips nav/ads  │                  │
│        │  3. Gemini Flash parses jobs          │                  │
│        │  4. Validator checks data quality     │                  │
│        │  5. Returns NormalizedJob[] JSON      │                  │
│        └──────────────────────────────────────┘                  │
│              │                                                   │
│              ▼                                                   │
│  Bronze Layer (raw HTML + LLM response stored in GCS)            │
│  Silver Layer (validated NormalizedJob merged into BigQuery)      │
│  Company Manifest Update (detected ATS written back to manifest) │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Component Breakdown

| Component | Language | Where | New/Existing |
|:---|:---|:---|:---|
| `CrawlerClient` | Kotlin | Backend | **New** — implements `AtsClient` interface |
| `CrawlerNormalizer` | Kotlin | Backend | **New** — implements `AtsNormalizer` (passthrough, since crawler returns pre-normalised data) |
| Crawler Service | Node.js/TypeScript | Separate Cloud Run service | **New** microservice |
| `AtsDetector` | TypeScript | Crawler Service | **New** — detects ATS provider from page metadata |
| `CompanyCrawlConfig` | JSON schema | Company manifests | **New** field in manifest |

---

## 3. Crawler Service Design

### 3.1 API Contract

```
POST /crawl
Content-Type: application/json

{
  "companyId": "airwallex",
  "url": "https://www.airwallex.com/careers",
  "crawlConfig": {
    "maxPages": 5,
    "followJobLinks": true,
    "extractionPrompt": null,         // null = use default prompt
    "cssSelectors": null,             // null = rely on LLM
    "knownAtsProvider": "ASHBY",      // hint from manifest
    "timeout": 30000
  }
}

Response 200:
{
  "companyId": "airwallex",
  "crawlMeta": {
    "pagesVisited": 3,
    "totalJobsFound": 47,
    "detectedAtsProvider": "ASHBY",
    "detectedAtsIdentifier": "airwallex",
    "crawlDurationMs": 8200,
    "extractionModel": "gemini-2.0-flash",
    "extractionConfidence": 0.92
  },
  "jobs": [
    {
      "platformId": "crawl-airwallex-senior-software-engineer-2026-03-10",
      "source": "Crawler",
      "title": "Senior Software Engineer",
      "companyName": "Airwallex",
      "location": "Melbourne, AU",
      "descriptionHtml": null,
      "descriptionText": "We are looking for...",
      "salaryMin": null,
      "salaryMax": null,
      "salaryCurrency": null,
      "employmentType": "Full-time",
      "seniorityLevel": "Senior",
      "workModel": "Hybrid",
      "department": "Engineering",
      "postedAt": "2026-03-08",
      "applyUrl": "https://jobs.ashbyhq.com/airwallex/abc123",
      "platformUrl": "https://www.airwallex.com/careers/senior-software-engineer"
    }
  ]
}
```

### 3.2 Crawl Pipeline (Internal)

```
Career Page URL
    │
    ▼
[1] PlaywrightCrawler
    - Launches headless Chromium
    - Navigates to career page
    - Waits for dynamic content (networkidle)
    - Handles pagination ("Load More", "Next Page")
    - Follows links to individual job detail pages
    │
    ▼
[2] Content Extractor
    - Strips navigation, footer, ads, cookie banners
    - Extracts main content area
    - Captures structured data (JSON-LD, microdata) if present
    - Captures page metadata (og:tags, canonical URL)
    │
    ▼
[3] ATS Detector (optional, runs in parallel)
    - Checks page HTML for known ATS signatures:
      ⎡  iframe src containing "boards.greenhouse.io"
      ⎢  Script tags loading "lever.co", "ashby", etc.
      ⎢  Workday URL patterns in links
      ⎢  JSON-LD with ATS metadata
      ⎢  meta tags like "generator: Workable"
      ⎣  Known CSS class patterns per ATS
    - Returns: { provider, identifier, confidence }
    │
    ▼
[4] Gemini Flash Extraction
    - Sends cleaned content + extraction prompt to Gemini API
    - Prompt includes company-specific context (see Section 4)
    - Returns structured JSON matching NormalizedJob schema
    │
    ▼
[5] Validator
    - Checks each extracted job against quality rules:
      ⎡  title: non-empty, reasonable length (3-200 chars)
      ⎢  location: parseable (city, country, or "Remote")
      ⎢  employmentType: known enum or null
      ⎢  postedAt: valid ISO date or null
      ⎣  applyUrl: valid URL or null
    - Assigns confidence score per job
    - Rejects jobs below threshold (< 0.5 confidence)
    │
    ▼
[6] Response Builder
    - Packages valid jobs + crawl metadata
    - Returns JSON to Kotlin backend
```

### 3.3 Career Page URL Discovery

For the 1,104 companies with `ats.provider: "NONE"`, we need to find their career pages. Strategy:

1. **Manifest `website` field** → Try `{website}/careers`, `{website}/jobs`, `{website}/about/careers`
2. **Common patterns** → `/careers`, `/jobs`, `/work-with-us`, `/join-us`, `/opportunities`
3. **Google search** → `site:{domain} careers OR jobs` (fallback for non-standard URLs)
4. **Manual curation** → For high-priority companies, verify URLs and store in manifest

New company manifest field:

```json
{
  "crawl_config": {
    "careers_url": "https://www.airwallex.com/careers",
    "discovered_at": "2026-03-15T00:00:00Z",
    "discovery_method": "auto"  // "auto" | "manual" | "google_search"
  }
}
```

---

## 4. Per-Company Prompt Customisation

This is the key to improving extraction quality over time without writing custom scrapers.

### 4.1 Default Extraction Prompt

```
You are a job listing extraction assistant. Extract all job postings from the
following career page content.

Company: {{companyName}}
Industry: {{industries}}
Known locations: {{officeLocations}}

For each job, return a JSON object with these fields:
- title (string, required)
- location (string — city, state/region, country)
- department (string or null)
- employmentType ("Full-time" | "Part-time" | "Contract" | "Internship" | null)
- seniorityLevel ("Junior" | "Mid" | "Senior" | "Lead" | "Principal" | "Director" | "VP" | null)
- workModel ("Remote" | "Hybrid" | "On-site" | null)
- salaryMin (integer, annual, or null)
- salaryMax (integer, annual, or null)
- salaryCurrency (ISO 4217 code, or null)
- description (first 500 characters of the job description)
- postedAt (ISO date string, or null)
- applyUrl (direct application URL, or null)

Rules:
- Only include CURRENT, OPEN job postings. Ignore expired or example listings.
- If salary is given as hourly/daily, convert to annual estimate.
- If location is ambiguous, use the company's known locations as context.
- Return a JSON array. If no jobs found, return [].

Career page content:
{{pageContent}}
```

### 4.2 Company-Specific Prompt Overrides

Store customisations in the company manifest under a new `crawl_config` field:

```json
{
  "crawl_config": {
    "careers_url": "https://www.airwallex.com/careers",
    "extraction_hints": {
      "salary_format": "Salary is listed in AUD, formatted as '$X,XXX - $Y,YYY per annum'",
      "location_format": "Locations are listed as 'City, Country'",
      "department_selector": "Jobs are grouped by department headings (h2)",
      "known_issues": "Some jobs list 'Multiple Locations' — extract the first listed city"
    },
    "css_preprocess": {
      "job_list_selector": ".careers-listing .job-card",
      "remove_selectors": [".cookie-banner", ".newsletter-signup"]
    },
    "pagination": {
      "type": "load_more_button",
      "selector": "button.load-more",
      "max_pages": 10
    }
  }
}
```

The prompt template dynamically appends these hints:

```
{{#if extractionHints}}
Additional context for this company:
{{#each extractionHints}}
- {{@key}}: {{this}}
{{/each}}
{{/if}}
```

### 4.3 Progressive Prompt Learning Loop

The system gets smarter over time through this cycle:

```
  ┌──────────────────────────────────────┐
  │  1. Crawl with default prompt        │
  │     (or current company prompt)       │
  └────────────┬─────────────────────────┘
               ▼
  ┌──────────────────────────────────────┐
  │  2. Validate extracted data          │
  │     - Compare job count vs last run  │
  │     - Check field completion rates   │
  │     - Flag anomalies                 │
  └────────────┬─────────────────────────┘
               ▼
  ┌──────────────────────────────────────┐
  │  3. Score extraction quality         │
  │     - % of jobs with title ✓         │
  │     - % with valid location ✓        │
  │     - % with salary data ✓           │
  │     - Consistency vs previous run    │
  └────────────┬─────────────────────────┘
               ▼
  ┌──────────────────────────────────────┐
  │  4. If quality < threshold:          │
  │     - Flag for human review          │
  │     - Store raw HTML for debugging   │
  │     - Suggest prompt improvements    │
  │                                      │
  │  5. If quality is good:              │
  │     - Lock in prompt for next run    │
  │     - Update manifest confidence     │
  └──────────────────────────────────────┘
```

Over time, this creates a **library of company-specific extraction configurations** — essentially a knowledge base of "how to crawl company X" — without writing any custom code.

---

## 5. ATS Detection & Company Data Feedback Loop

### 5.1 How the Crawler Detects ATS Providers

The `AtsDetector` module runs alongside the content extractor. It checks:

| Signal | Example | Confidence |
|:---|:---|:---|
| **iframe src** | `<iframe src="https://boards.greenhouse.io/airwallex">` | High |
| **Script tags** | `<script src="https://jobs.ashbyhq.com/widget.js">` | High |
| **Apply URL patterns** | `href="https://lever.co/apply/..."` | High |
| **Workday URL** | Links to `*.wd3.myworkdayjobs.com/*` | High |
| **JSON-LD** | `"@context": "http://schema.org", "@type": "JobPosting"` | Medium |
| **Meta tags** | `<meta name="generator" content="Workable">` | Medium |
| **CSS class patterns** | `.greenhouse-embedded`, `.lever-jobs-list` | Medium |
| **Known DOM patterns** | Workday's `${slug}/searchpage` React app structure | Medium |

### 5.2 Automatic Company Manifest Updates

When the crawler detects an ATS provider for a previously-unidentified company, it triggers a manifest update:

```
Crawler detects ATS
       │
       ▼
POST /api/internal/companies/{id}/ats-detection
{
  "detectedProvider": "LEVER",
  "detectedIdentifier": "envato-2",
  "confidence": 0.95,
  "evidence": "iframe src='https://jobs.lever.co/envato-2'",
  "detectedAt": "2026-03-15T02:00:00Z"
}
       │
       ▼
Backend updates company manifest:
  ats.provider: "NONE" → "LEVER"
  ats.identifier: "" → "envato-2"
  ats.detection_method: "crawler"
  ats.detection_confidence: 0.95
  ats.detected_at: "2026-03-15T02:00:00Z"
```

### 5.3 Data-Driven Strategy Decisions

As ATS detection data accumulates, we can generate reports:

```sql
-- Which ATS providers are most common among our companies?
SELECT
  ats_provider,
  COUNT(*) as company_count,
  AVG(extraction_confidence) as avg_confidence,
  AVG(jobs_extracted) as avg_jobs_per_company
FROM company_crawl_results
WHERE crawl_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
GROUP BY ats_provider
ORDER BY company_count DESC;
```

This drives decisions like:
- "We discovered 40 Workday companies — worth building a dedicated Workday scraper?"
- "Lever extraction quality is 95% via crawling — no need for a direct API client"
- "BambooHR extraction is only 60% quality — maybe worth building a direct client for those 6 companies"

---

## 6. Integration with Existing Backend

### 6.1 New Kotlin Components

#### `CrawlerClient.kt` (implements `AtsClient`)

```kotlin
@Component
class CrawlerClient(
    private val restTemplate: RestTemplate,
    @Value("\${crawler.service.url}") private val crawlerUrl: String
) : AtsClient {

    override fun fetchJobs(identifier: String): String {
        // identifier = company ID for crawler
        val config = loadCrawlConfig(identifier)
        val request = CrawlRequest(
            companyId = identifier,
            url = config.careersUrl,
            crawlConfig = config
        )
        val response = restTemplate.postForObject(
            "$crawlerUrl/crawl",
            request,
            String::class.java
        )
        return response ?: throw RuntimeException("Crawler returned null for $identifier")
    }
}
```

#### `CrawlerNormalizer.kt` (implements `AtsNormalizer`)

The crawler already returns data in `NormalizedJob` format, so this is mostly a passthrough with validation:

```kotlin
@Component
class CrawlerNormalizer(
    private val objectMapper: ObjectMapper
) : AtsNormalizer {

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        val response = objectMapper.treeToValue(rawData, CrawlResponse::class.java)
        return response.jobs.map { job ->
            NormalizedJob(
                platformId = job.platformId,
                source = "Crawler",
                title = job.title,
                companyName = job.companyName,
                // ... map remaining fields
                rawPayload = objectMapper.writeValueAsString(job)
            )
        }
    }
}
```

#### `AtsProvider.kt` update

```kotlin
enum class AtsProvider(val displayName: String) {
    GREENHOUSE("Greenhouse"),
    LEVER("Lever"),
    ASHBY("Ashby"),
    JOBADDER("JobAdder"),
    EMPLOYMENT_HERO("Employment Hero"),
    SNAPHIRE("SnapHire"),
    APIFY("LinkedIn-Apify"),
    CRAWLER("AI-Crawler");  // NEW
}
```

### 6.2 Company Manifest Schema Update

Add `crawl_config` to the existing company JSON:

```json
{
  "name": "Airwallex",
  "website": "https://www.airwallex.com",
  "ats": {
    "provider": "ASHBY",
    "identifier": "airwallex",
    "detection_method": "crawler",
    "detection_confidence": 0.95,
    "detected_at": "2026-03-15T02:00:00Z"
  },
  "crawl_config": {
    "careers_url": "https://www.airwallex.com/careers",
    "discovered_at": "2026-03-15T00:00:00Z",
    "discovery_method": "auto",
    "extraction_hints": {},
    "last_crawl_at": "2026-03-15T02:00:00Z",
    "last_crawl_quality": 0.92,
    "last_crawl_jobs_count": 47
  }
}
```

---

## 7. Bronze Layer Storage Integration

The crawler must integrate with our existing [file-based cold storage](file:///Users/nic/Projects/job-market-gemini/docs/architecture/006-file-based-cold-storage.md) (ADR 006), which stores raw data as **NDJSON+gzip files in GCS** with a BigQuery metadata index. The key design principle is **few large files** — not one file per company.

### 7.1 Storage Structure

Crawler data fits into the existing `gs://techmarket-bronze-ingestions/` bucket:

```
gs://techmarket-bronze-ingestions/
├── apify/                          # Existing Apify data
│   └── 2026/03/10/dataset-{id}/
│       ├── jobs-0001.json.gz       # 5,000 records per chunk
│       └── jobs-0002.json.gz
├── ats/                            # Existing ATS data
│   ├── greenhouse/{company-id}/
│   │   └── 2026-03-10.json.gz
│   └── lever/{company-id}/
│       └── 2026-03-10.json.gz
└── crawler/                        # NEW — AI Crawler data
    └── 2026/03/10/
        ├── nightly-crawl/
        │   ├── jobs-0001.json.gz   # Batched: ~500 jobs per chunk
        │   └── jobs-0002.json.gz   # All companies in one dataset
        └── raw-html/               # Page HTML for audit/debugging
            ├── airwallex.html.gz
            └── halter.html.gz
```

### 7.2 Dataset Batching Strategy

Rather than creating one dataset per company (which would produce 1,257 tiny files nightly), the crawler **batches all nightly crawl results into a single dataset**:

| Aspect | Design |
|:---|:---|
| **Dataset ID** | `crawler-nightly-{date}` (e.g., `crawler-nightly-2026-03-15`) |
| **Chunk size** | 500 records per NDJSON+gzip file (matching ATS chunk size) |
| **Expected volume** | ~2,000-5,000 jobs/night → 4-10 files per dataset |
| **One file per company?** | ❌ No — all companies batched into shared chunks |
| **Raw HTML storage** | Separate directory, one gzip file per company (for debugging only) |

This aligns with the cold storage preference for **fewer, larger files**. A typical nightly crawl produces ~5 chunked files rather than 1,257 individual ones.

### 7.3 How It Works in Code

The `CrawlerClient` returns raw JSON per company (like any `AtsClient`). The batching happens at the **sync orchestrator level** — a new `CrawlerBatchSyncService` that:

1. **Collects** — Dispatches crawl requests to the Crawler Service for each company via Cloud Tasks
2. **Aggregates** — Accumulates `NormalizedJob` results in memory (or a temp file) across companies
3. **Batches** — Once all companies are crawled (or at a threshold), writes a single `BronzeIngestionManifest` with chunked NDJSON+gzip files
4. **Indexes** — Creates one BigQuery metadata row per nightly dataset (not per company)

```kotlin
// Pseudo-code for the batch sync
class CrawlerBatchSyncService(
    private val crawlerClient: CrawlerClient,
    private val bronzeRepository: BronzeRepository,
    private val companyRepository: CompanyRepository
) {
    fun syncAllCompanies() {
        val date = LocalDate.now()
        val datasetId = "crawler-nightly-$date"

        // Guard against re-running
        if (bronzeRepository.isDatasetIngested(datasetId)) return

        val allJobs = mutableListOf<String>()  // Raw NDJSON lines

        for (company in companyRepository.getCrawlEnabledCompanies()) {
            try {
                val rawPayload = crawlerClient.fetchJobs(company.id)
                allJobs.add(rawPayload)  // Each line is one company's crawl result
            } catch (e: Exception) {
                log.warn("Crawl failed for ${company.id}: ${e.message}")
            }
        }

        // Batch into chunks and persist to Bronze
        val chunks = allJobs.chunked(CHUNK_SIZE).map { chunk ->
            chunk.joinToString("\n").toByteArray()
        }
        val manifest = BronzeIngestionManifest(
            datasetId = datasetId,
            source = "AI-Crawler",
            recordCount = allJobs.size,
            fileCount = chunks.size,
            // ... other fields
        )
        bronzeRepository.saveIngestion(manifest, chunks)
    }
}
```

### 7.4 Raw HTML Archival

Raw HTML is stored **separately** from the job data, because:
- It's only needed for debugging extraction issues
- It's much larger than structured job data (~500KB/page vs ~2KB/job)
- It can use a more aggressive lifecycle policy (delete after 30 days)

```
gs://techmarket-bronze-ingestions/crawler/2026/03/10/raw-html/
├── airwallex.html.gz      # Full page HTML for audit trail
├── halter.html.gz
└── ...
```

A separate GCS lifecycle rule can archive or delete raw HTML after 30 days, while the structured job data follows the standard lifecycle (90 days → COLDLINE → ARCHIVE).

### 7.5 Reprocessing

Because Bronze stores the raw crawler response (including the LLM's structured output), we can **reprocess** crawler data through the Silver pipeline at any time — just like we can reprocess Apify or ATS data. If we improve the `CrawlerNormalizer` or change field mappings, we re-read the Bronze NDJSON files and re-map everything.

> [!TIP]
> The raw HTML archive also enables **re-extraction** — if we improve our prompts, we can re-run the LLM against cached HTML without re-crawling the live pages. This is particularly useful during the prompt learning loop (Section 4.3).

---

## 8. Cost Analysis

### 8.1 Crawler Service Costs (1,257 companies nightly)

| Resource | Monthly Cost | Notes |
|:---|:---|:---|
| **Cloud Run (Crawler)** | **~$5-10** | ~1,257 invocations/night × 30 nights. 1 vCPU, 2GB RAM, ~30s avg per crawl. Free tier covers 180K vCPU-seconds; we'd use ~37,710. |
| **Gemini 2.0 Flash** | **~$0.50** | ~1,257 pages × 5K tokens input + 1K output × 30 nights. Well within free tier if using Vertex AI credits. |
| **Cloud Run (Backend)** | **$0** | Already running; marginal cost of additional requests is zero. |
| **GCS (Bronze HTML)** | **~$1-2** | ~500KB avg per page × 1,257 × 30 = ~19GB/month. Standard storage @ $0.026/GB. |
| **BigQuery (Silver)** | **~$5** | Existing cost, marginally increased. |
| **Total** | **~$12-18/month** | |

### 8.2 Comparison with Alternatives

| Approach | Monthly Cost | Coverage | Maintenance |
|:---|:---|:---:|:---|
| **Self-hosted Crawlee + Gemini Flash** | ~$15/month | 100% of companies | Medium — maintain crawler service |
| **Apify actors only** | ~$80-100/month | ~30% directly, rest via LinkedIn | Low — Apify maintains actors |
| **Build 12+ ATS clients** | $0 API cost | ~12% of companies | High — maintain 12+ normalizers |
| **Merge.dev middleware** | $300-1000/month | 50+ ATS via unified API | Low — vendor managed |

> [!IMPORTANT]
> The self-hosted crawler is **5-7x cheaper than Apify** for the same coverage, and covers *all* companies rather than just those on known ATS platforms. The trade-off is engineering effort to build and maintain the crawler service.

---

## 9. Implementation Phases

### Phase 1 — MVP Crawler (1-2 weeks)

**Goal**: Crawl 50 companies nightly and validate the approach.

**🤖 Code tasks:**
- [ ] 🤖 Scaffold Node.js/TypeScript Crawler Service (`npx crawlee create`)
- [ ] 🤖 Implement `PlaywrightCrawler` with basic career page navigation
- [ ] 🤖 Implement content extractor (strip nav/footer, extract main content)
- [ ] 🤖 Implement Gemini Flash extraction service with default prompt
- [ ] 🤖 Implement basic job validator (title+location minimum threshold)
- [ ] 🤖 Implement `AtsDetector` (iframe/script/URL pattern matching)
- [ ] 🤖 Build HTTP API (`POST /crawl` → JSON response)
- [ ] 🤖 Dockerise for Cloud Run deployment
- [ ] 🤖 Build `CrawlerClient` + `CrawlerNormalizer` in Kotlin backend
- [ ] 🤖 Add `CRAWLER` to `AtsProvider` enum
- [ ] 🤖 Wire into `AtsJobDataSyncService`

**👤 Your tasks:**
- [ ] 👤 Verify career page URLs for 50 test companies (mix of identified/unidentified ATS)
- [ ] 👤 Deploy Crawler Service to Cloud Run
- [ ] 👤 Run first batch and validate extraction quality

**Estimated effort**: ~8-10 days
**Coverage result**: 50 companies crawled nightly with validated extraction quality

---

### Phase 2 — Scale & Feedback Loop (1-2 weeks)

**Goal**: Crawl all 1,257 companies. Implement the ATS detection feedback loop.

**🤖 Code tasks:**
- [ ] 🤖 Implement career page URL discovery (auto-probe `/careers`, `/jobs`, etc.)
- [ ] 🤖 Implement ATS detection → manifest update endpoint
- [ ] 🤖 Add `crawl_config` field to company manifest schema
- [ ] 🤖 Build extraction quality scoring (confidence, field completion rates)
- [ ] 🤖 Add crawl metadata logging to BigQuery (for analytics)
- [ ] 🤖 Implement rate limiting and retry logic (respect robots.txt)
- [ ] 🤖 Set up Cloud Scheduler → Cloud Tasks → Backend → Crawler pipeline

**👤 Your tasks:**
- [ ] 👤 Review ATS detection results after first full crawl
- [ ] 👤 Write extraction hints for top 20 companies with quality < 0.7
- [ ] 👤 Set up Cloud Scheduler cron job (nightly 2am NZST)
- [ ] 👤 Set up monitoring alerts (crawl failure rate > 20%)

**Estimated effort**: ~5-8 days
**Coverage result**: All 1,257 companies crawled nightly. ATS distribution data flowing.

---

### Phase 3 — Optimise Based on Data (Ongoing)

**Goal**: Use crawl data to make informed decisions about specific ATS integrations.

**Decision framework:**
| If crawl data shows... | Then... |
|:---|:---|
| Lever extraction quality > 90% via crawling | Skip building `LeverClient` — crawling is good enough |
| Lever extraction quality < 80% | Build `LeverClient` — direct API gives better data |
| 50+ companies detected on Workday | Build Workday scraper — enough volume to justify |
| ATS X has < 5 companies | Keep crawling — not worth a dedicated integration |

**🤖 Code tasks (build on demand):**
- [ ] 🤖 Build per-company prompt customisation UI/CLI
- [ ] 🤖 Implement A/B prompt testing (try multiple prompts, pick best)
- [ ] 🤖 Build ATS-specific CSS pre-processors (for known ATS page structures)
- [ ] 🤖 Build `LeverClient` + `AshbyClient` (if data shows quality gap)
- [ ] 🤖 Integrate Seek scraping (Apify actor or custom Crawlee spider)
- [ ] 🤖 Build extraction quality dashboard

**👤 Your tasks:**
- [ ] 👤 Monthly review of crawl quality metrics
- [ ] 👤 Decide which ATS clients to build based on data
- [ ] 👤 Curate extraction hints for low-quality companies

---

## 10. Testing Strategy & Quality Assurance

### 10.1 Test Coverage Requirements

| Component | Minimum Coverage | Test Types | Owner |
|:---|:---|:---|:---|
| Crawler Service (TypeScript) | 85% | Unit + Integration | Backend |
| `AtsDetector` | 90% | Unit | Backend |
| Content Extractor | 85% | Unit | Backend |
| Validator | 95% | Unit + Integration | Backend |
| `CrawlerClient` (Kotlin) | 80% | Unit + Integration | Backend |
| `CrawlerNormalizer` (Kotlin) | 85% | Unit | Backend |
| E2E crawl pipeline | N/A | E2E (5 test companies) | Both |

### 10.2 Crawler Service Tests

**Unit Tests (Pure Functions):**
```typescript
// AtsDetector.test.ts
describe('AtsDetector', () => {
  it('detects Greenhouse from iframe src', () => {
    const html = '<iframe src="https://boards.greenhouse.io/airwallex">';
    expect(detectAts(html)).toEqual({
      provider: 'GREENHOUSE',
      identifier: 'airwallex',
      confidence: 0.95
    });
  });
  
  it('detects Lever from script tag', () => {
    const html = '<script src="https://assets.lever.co/widget.js">';
    expect(detectAts(html)).toEqual({
      provider: 'LEVER',
      confidence: 0.90
    });
  });
  
  it('returns null for unknown ATS', () => {
    const html = '<div class="custom-jobs">';
    expect(detectAts(html)).toBeNull();
  });
});

// Validator.test.ts
describe('JobValidator', () => {
  it('rejects job without title', () => {
    const job = { location: 'Sydney', applyUrl: 'https://...' };
    expect(validateJob(job)).toEqual({
      valid: false,
      confidence: 0.3,
      errors: ['Missing required field: title']
    });
  });
  
  it('accepts valid job with title and location', () => {
    const job = {
      title: 'Senior Engineer',
      location: 'Sydney, AU',
      applyUrl: 'https://...'
    };
    expect(validateJob(job)).toEqual({
      valid: true,
      confidence: 0.85
    });
  });
  
  it('rejects title longer than 200 chars', () => {
    const job = { title: 'A'.repeat(201), location: 'Sydney' };
    expect(validateJob(job).valid).toBe(false);
  });
});
```

**Integration Tests (Mocked Gemini):**
```typescript
// CrawlerPipeline.integration.test.ts
describe('CrawlerPipeline', () => {
  let mockGemini: MockGeminiService;
  let crawler: CrawlerService;
  
  beforeEach(() => {
    mockGemini = new MockGeminiService();
    crawler = new CrawlerService(mockGemini);
  });
  
  it('extracts jobs from sample career page', async () => {
    const sampleHtml = loadFixture('airwallex-careers.html');
    mockGemmi.mockResponse({ jobs: [createMockJob()] });
    
    const result = await crawler.crawl({
      url: 'https://www.airwallex.com/careers',
      companyId: 'airwallex'
    });
    
    expect(result.jobs.length).toBeGreaterThan(0);
    expect(result.crawlMeta.detectedAtsProvider).toBe('ASHBY');
  });
  
  it('handles pagination correctly', async () => {
    // Test with multi-page fixture
  });
  
  it('respects maxPages config', async () => {
    // Verify pagination stops at limit
  });
});
```

### 10.3 Backend Tests (Kotlin)

```kotlin
// CrawlerClientTest.kt
@SpringBootTest
class CrawlerClientTest {
    
    @Autowired
    private lateinit var crawlerClient: CrawlerClient
    
    @MockBean
    private lateinit var restTemplate: RestTemplate
    
    @Test
    fun `fetchJobs returns crawler response`() {
        val mockResponse = """{"jobs": [{"title": "Engineer", ...}]}"""
        whenever(restTemplate.postForObject(any(), any(), any()))
            .thenReturn(mockResponse)
        
        val result = crawlerClient.fetchJobs("airwallex")
        
        assertThat(result).isEqualTo(mockResponse)
    }
    
    @Test
    fun `fetchJobs throws on null response`() {
        whenever(restTemplate.postForObject(any(), any(), any()))
            .thenReturn(null)
        
        assertThrows<RuntimeException> {
            crawlerClient.fetchJobs("airwallex")
        }
    }
}

// CrawlerNormalizerTest.kt
class CrawlerNormalizerTest {
    
    private val normalizer = CrawlerNormalizer(ObjectMapper())
    
    @Test
    fun `normalize maps crawler jobs to NormalizedJob`() {
        val rawData = ObjectMapper().readTree("""
            {"jobs": [{"title": "Senior Engineer", "location": "Sydney, AU"}]}
        """)
        
        val result = normalizer.normalize(rawData)
        
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("Senior Engineer")
        assertThat(result[0].source).isEqualTo("Crawler")
    }
    
    @Test
    fun `normalize rejects invalid jobs`() {
        val rawData = ObjectMapper().readTree("""
            {"jobs": [{"location": "Sydney"}]}  // Missing title
        """)
        
        val result = normalizer.normalize(rawData)
        
        assertThat(result).isEmpty()
    }
}
```

### 10.4 E2E Tests

```typescript
// tests/e2e/crawler-e2e.test.ts
describe('E2E Crawler Pipeline', () => {
  const testCompanies = [
    'airwallex',    // Ashby
    'canva',        // Greenhouse
    'safetyculture' // Lever
  ];
  
  test.each(testCompanies)('crawls %s successfully', async (company) => {
    const response = await fetch('http://localhost:8080/crawl', {
      method: 'POST',
      body: JSON.stringify({
        companyId: company,
        url: `https://${company}.com/careers`
      })
    });
    
    expect(response.status).toBe(200);
    const result = await response.json();
    expect(result.jobs.length).toBeGreaterThan(0);
    expect(result.crawlMeta.extractionConfidence).toBeGreaterThan(0.7);
  });
  
  test('handles company with no jobs gracefully', async () => {
    const response = await fetch('http://localhost:8080/crawl', {
      method: 'POST',
      body: JSON.stringify({
        companyId: 'empty-company',
        url: 'https://example.com/careers'
      })
    });
    
    expect(response.status).toBe(200);
    const result = await response.json();
    expect(result.jobs).toEqual([]);
  });
});
```

### 10.5 Test Execution in CI

```yaml
# .github/workflows/crawler-test.yml
jobs:
  test-crawler:
    runs-on: ubuntu-latest
    steps:
      - name: Run TypeScript unit tests
        run: cd crawler-service && npm test
        
      - name: Run Kotlin backend tests
        run: ./gradlew test --tests "*Crawler*"
        
      - name: Run E2E tests
        run: npm run test:e2e -- --grep "Crawler"
        
      - name: Check coverage
        run: cd crawler-service && npm run coverage
        env:
          COVERAGE_THRESHOLD: 85
```

---

## 11. Security & Compliance

### 11.1 Raw HTML Storage Security

**Storage Location:** `gs://techmarket-bronze-ingestions/crawler/{date}/raw-html/`

**Security Measures:**

1. **Input Sanitization** — Before sending to LLM:
   ```typescript
   function sanitizeContent(html: string): string {
     return html
       .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
       .replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')
       .replace(/on\w+="[^"]*"/g, '')  // Remove event handlers
       .replace(/javascript:/gi, '');
   }
   ```

2. **Prompt Injection Defense:**
   - Wrap content in XML tags: `<page-content>{{content}}</page-content>`
   - System prompt explicitly ignores instructions within content
   - Validate output schema strictly (reject unexpected fields)
   - Log suspicious patterns for review

3. **GCS Bucket Policies:**
   - Private access only (service account)
   - Lifecycle policy: delete raw HTML after 30 days
   - Object versioning enabled for audit trail
   - Encryption at rest (GCS default)

4. **Why Store Raw HTML?**
   - Debug extraction failures
   - Re-extraction with improved prompts (no re-crawl needed)
   - Compliance audit (what data did we scrape on date X?)

### 11.2 Robots.txt Compliance

```typescript
// robots.ts
import { RobotsParser } from 'robots-parser';

class RobotsChecker {
  private cache = new Map<string, RobotsParser>();
  
  async canFetch(url: string, userAgent: string = 'DevAssemblyBot'): Promise<boolean> {
    const urlObj = new URL(url);
    const robotsUrl = `${urlObj.protocol}//${urlObj.hostname}/robots.txt`;
    
    let parser = this.cache.get(urlObj.hostname);
    if (!parser) {
      const robotsTxt = await fetch(robotsUrl).then(r => r.text());
      parser = new RobotsParser(userAgent, robotsTxt);
      this.cache.set(urlObj.hostname, parser);
    }
    
    return parser.isAllowed(url);
  }
}
```

**Rate Limiting:**
- Default: 2 requests/second per domain
- Respect `Crawl-delay` in robots.txt
- Add 1-2 second delay between requests to same domain

### 11.3 GDPR Considerations

**Checklist:**
- [ ] Do not store personal data (names, emails, phone numbers)
- [ ] Add data retention policy (90 days for job data)
- [ ] Document legal basis for processing (legitimate interest)
- [ ] Add privacy policy disclosure about job data collection
- [ ] Implement data deletion on request

---

## 12. Parallelization & Performance

### 12.1 Parallelization Strategy

| Level | Mechanism | Concurrency | Config |
|:---|:---|:---|:---|
| **Company-level** | Cloud Tasks queue | 2 requests/sec | `rateLimits.maxConcurrentDispatches` |
| **Page-level** | PlaywrightCrawler | 5-10 pages | `maxRequestsPerCrawl` |
| **LLM calls** | Batch to Gemini | 10 concurrent | `maxConcurrency` |

### 12.2 Cloud Tasks Configuration

```yaml
# cloud-tasks.yaml
queue: crawler-queue
rateLimits:
  maxConcurrentDispatches: 2
  maxDispatchesPerSecond: 2
retryConfig:
  maxAttempts: 3
  minBackoff: 5s
  maxBackoff: 1m
```

### 12.3 Crawler Service Concurrency

```typescript
// crawler.config.ts
export const crawlerConfig = {
  maxRequestsPerCrawl: 50,
  maxConcurrency: 10,  // Parallel page crawls
  handlePageTimeoutSecs: 60,
  headless: true,
  launchContext: {
    launchOptions: {
      ignoreHTTPSErrors: true,
    }
  }
};
```

### 12.4 Performance Benchmarks

| Operation | Sequential | Parallel | Target |
|:---|:---|:---|:---|
| Crawl 50 companies (MVP) | ~25 min | ~8 min | <10 min |
| Crawl 1,257 companies | ~10 hours | ~2 hours | <3 hours |
| LLM extraction (per page) | ~2s | ~0.5s (batched) | <1s |

### 12.5 Caching Strategy

**Career Page Cache:**
```typescript
interface PageCache {
  url: string;
  contentHash: string;
  lastCrawled: Date;
  jobCount: number;
}

async function shouldRecrawl(url: string): Promise<boolean> {
  const cached = await getCache(url);
  if (!cached) return true;
  
  const currentHash = await hashContent(await fetch(url));
  if (currentHash !== cached.contentHash) return true;
  
  // Skip if unchanged and crawled within 7 days
  const daysSinceCrawl = daysBetween(cached.lastCrawled, new Date());
  return daysSinceCrawl > 7;
}
```

**Expected Savings:** ~60% reduction in LLM calls for stable career pages.

---

## 13. Error Handling & Recovery

### 13.1 Error Types & Retry Strategy

| Error type | Retry strategy | Fallback |
|:---|:---|:---|
| Gemini API timeout | Exponential backoff (3 retries) | Skip company, flag for manual review |
| Playwright navigation fail | Retry with increased timeout | Try mobile user agent |
| Cloud Run cold start | Warm instance at 1:55am | Ignore (first request absorbs latency) |
| Invalid JSON from LLM | Re-prompt with "fix JSON errors" | Reject company result, log for debugging |
| Rate limited (429) | Wait `Retry-After` header | Queue for later |
| robots.txt blocked | No retry | Log, skip company |

### 13.2 Circuit Breaker Pattern

```typescript
// circuit-breaker.ts
class CircuitBreaker {
  private failures = 0;
  private state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' = 'CLOSED';
  private lastFailureTime?: Date;
  
  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (this.state === 'OPEN') {
      if (Date.now() - this.lastFailureTime!.getTime() > 60000) {
        this.state = 'HALF_OPEN';
      } else {
        throw new Error('Circuit breaker OPEN');
      }
    }
    
    try {
      const result = await fn();
      if (this.state === 'HALF_OPEN') this.state = 'CLOSED';
      this.failures = 0;
      return result;
    } catch (e) {
      this.failures++;
      this.lastFailureTime = new Date();
      if (this.failures >= 10) this.state = 'OPEN';
      throw e;
    }
  }
}
```

### 13.3 Monitoring Dashboard

**Track Daily:**
- Crawl success rate (% companies completed)
- Average extraction confidence
- Jobs extracted per company (trend line)
- LLM token usage + cost
- Cloud Run instance count + latency
- Error breakdown by type

**Alert Thresholds:**
- Crawl failure rate > 20%
- Average confidence < 0.7
- Gemini API error rate > 5%
- Cloud Run latency p95 > 30s

---

## 14. Documentation Standards

### 14.1 Code Documentation

All crawler components must include:

```typescript
/**
 * Detects ATS provider from page HTML using signature matching.
 * 
 * Checks for:
 * - iframe src patterns (Greenhouse, Ashby)
 * - Script tag sources (Lever, Workable)
 * - Meta tags (generator, application-name)
 * - URL patterns in links
 * 
 * @param html - Raw page HTML
 * @returns ATS detection result or null if unknown
 * 
 * @example
 * ```typescript
 * const result = detectAts('<iframe src="boards.greenhouse.io/acme">');
 * // Returns: { provider: 'GREENHOUSE', identifier: 'acme', confidence: 0.95 }
 * ```
 * 
 * @see AtsDetectorTest for test coverage
 */
function detectAts(html: string): AtsDetectionResult | null { ... }
```

### 14.2 ADR for Crawler Architecture

Create `docs/architecture/adr-XXX-crawler-service.md`:

1. **Context**: 87.8% companies have no identified ATS
2. **Decision**: Build self-hosted Crawlee + Gemini Flash crawler
3. **Consequences**:
   - Node.js microservice to maintain
   - LLM costs (~$15/month)
   - Broad coverage (100% companies)
4. **Alternatives considered**:
   - Apify actors (rejected: 5-7x cost)
   - Build 12+ ATS clients (rejected: premature optimization)

### 14.3 Runbook: Crawler Operations

```markdown
## Crawler Service Runbook

### Deploy
```bash
cd crawler-service
npm run build
docker build -t gcr.io/PROJECT/crawler:latest .
docker push gcr.io/PROJECT/crawler:latest
gcloud run deploy crawler --image gcr.io/PROJECT/crawler:latest
```

### Monitor
- Cloud Run dashboard: [link]
- BigQuery crawl results: `SELECT * FROM crawl_results WHERE date = CURRENT_DATE()`
- Error logs: `gcloud logging read "resource.type=cloud_run_revision AND severity>=ERROR"`

### Reprocess Failed Crawls
```bash
# Get failed companies from BigQuery
bq query "SELECT company_id FROM crawl_results WHERE status='FAILED'"

# Re-crawl specific company
curl -X POST http://crawler/crawl -d '{"companyId": "airwallex"}'
```
```

---

## 15. Updated Implementation Phases

### Phase 1 — MVP Crawler (2-3 weeks)

**Goal**: Crawl 50 companies nightly and validate the approach.

**🤖 Code tasks:**
- [ ] 🤖 Scaffold Node.js/TypeScript Crawler Service (`npx create-crawlee`)
- [ ] 🤖 Implement `AtsDetector` (iframe/script/URL pattern matching)
- [ ] 🤖 Implement Content Extractor (strip nav/footer, sanitize HTML)
- [ ] 🤖 Implement Gemini Flash extraction service with default prompt
- [ ] 🤖 Implement Job Validator (title+location, confidence scoring)
- [ ] 🤖 Build HTTP API (`POST /crawl` → JSON response)
- [ ] 🤖 Add prompt injection defenses (sanitization, XML wrapping)
- [ ] 🤖 Implement robots.txt compliance checker
- [ ] 🤖 Write unit tests (85% coverage minimum)
- [ ] 🤖 Write integration tests with mocked Gemini
- [ ] 🤖 Dockerise for Cloud Run deployment
- [ ] 🤖 Build `CrawlerClient` + `CrawlerNormalizer` in Kotlin backend
- [ ] 🤖 Add `CRAWLER` to `AtsProvider` enum
- [ ] 🤖 Wire into `AtsJobDataSyncService`
- [ ] 🤖 Set up E2E tests (5 test companies)

**👤 Your tasks:**
- [ ] 👤 Verify career page URLs for 50 test companies (mix of identified/unidentified ATS)
- [ ] 👤 Deploy Crawler Service to Cloud Run
- [ ] 👤 Run first batch and validate extraction quality
- [ ] 👤 Review security implementation (prompt injection, sanitization)

**Estimated effort**: ~10-15 days
**Coverage result**: 50 companies crawled nightly with validated extraction quality

---

### Phase 2 — Scale & Feedback Loop (2-3 weeks)

**Goal**: Crawl all 1,257 companies. Implement the ATS detection feedback loop.

**🤖 Code tasks:**
- [ ] 🤖 Implement career page URL discovery (auto-probe `/careers`, `/jobs`, etc.)
- [ ] 🤖 Implement ATS detection → manifest update endpoint
- [ ] 🤖 Add `crawl_config` field to company manifest schema
- [ ] 🤖 Build extraction quality scoring (confidence, field completion rates)
- [ ] 🤖 Add crawl metadata logging to BigQuery (for analytics)
- [ ] 🤖 Implement rate limiting and retry logic (Cloud Tasks config)
- [ ] 🤖 Implement circuit breaker for Gemini API
- [ ] 🤖 Add caching layer (skip unchanged pages)
- [ ] 🤖 Set up Cloud Scheduler → Cloud Tasks → Backend → Crawler pipeline
- [ ] 🤖 Build monitoring dashboard (success rate, confidence, errors)
- [ ] 🤖 Add alerting (failure rate > 20%, confidence < 0.7)

**👤 Your tasks:**
- [ ] 👤 Review ATS detection results after first full crawl
- [ ] 👤 Write extraction hints for top 20 companies with quality < 0.7
- [ ] 👤 Set up Cloud Scheduler cron job (nightly 2am NZST)
- [ ] 👤 Set up monitoring alerts
- [ ] 👤 Create ADR for crawler architecture
- [ ] 👤 Write crawler operations runbook

**Estimated effort**: ~8-12 days
**Coverage result**: All 1,257 companies crawled nightly. ATS distribution data flowing.

---

### Phase 3 — Optimise Based on Data (Ongoing)

**Goal**: Use crawl data to make informed decisions about specific ATS integrations.

**Decision framework:**
| If crawl data shows... | Then... |
|:---|:---|
| Lever extraction quality > 90% via crawling | Skip building `LeverClient` — crawling is good enough |
| Lever extraction quality < 80% | Build `LeverClient` — direct API gives better data |
| 50+ companies detected on Workday | Build Workday scraper — enough volume to justify |
| ATS X has < 5 companies | Keep crawling — not worth a dedicated integration |

**🤖 Code tasks (build on demand):**
- [ ] 🤖 Build per-company prompt customisation UI/CLI
- [ ] 🤖 Implement A/B prompt testing (try multiple prompts, pick best)
- [ ] 🤖 Build ATS-specific CSS pre-processors (for known ATS page structures)
- [ ] 🤖 Build `LeverClient` + `AshbyClient` (if data shows quality gap)
- [ ] 🤖 Integrate Seek scraping (Apify actor or custom Crawlee spider)
- [ ] 🤖 Build extraction quality dashboard
- [ ] 🤖 Implement re-extraction pipeline (cached HTML → new prompt)

**👤 Your tasks:**
- [ ] 👤 Monthly review of crawl quality metrics
- [ ] 👤 Decide which ATS clients to build based on data
- [ ] 👤 Curate extraction hints for low-quality companies

---

## 16. Risks & Mitigations

| Risk | Impact | Mitigation |
|:---|:---|:---|
| Career page behind login/CAPTCHA | No data extracted | Flag as "requires manual" — use LinkedIn/Seek as backup source |
| JavaScript-heavy SPA not rendering | Missing job listings | Increase `waitUntil: 'networkidle'` timeout. Add page-specific wait logic in `crawl_config`. |
| LLM hallucination | Phantom job listings | Validation gate rejects jobs without title. Cross-reference job count vs previous crawl. |
| Rate limiting / IP blocking | Crawls fail | Rotate Cloud Run instances (different IPs). Respect `robots.txt`. Add delays between requests. |
| Cloud Run cold start latency | Slow crawl initiation | Keep minimum instance warm during crawl window (2-3am). |
| Prompt injection via page content | LLM produces unexpected output | Sanitise page content before sending to LLM. Validate output schema strictly. |
| Gemini API outage | All crawls fail | Circuit breaker pattern. Fallback to cached data or skip. |
| Maintaining Node.js microservice | Additional tech stack | Crawlee community is active. Use TypeScript for type safety. Keep service simple (1 endpoint). |

---

## 17. Open Questions

1. **Language choice** — Should the Crawler Service be Node.js/TypeScript (Crawlee native) or Python (Crawl4AI)? **Decision: TypeScript** — better Crawlee support, mature ecosystem.
2. **Career page caching** — Should we cache career page HTML between crawls to detect changes? **Decision: Yes** — reduces LLM calls by ~60%.
3. **Supplementary sources** — Should the crawler also check LinkedIn and Seek as additional data sources per company, or keep those as separate integrations? **Decision: Separate** — keep concerns isolated.
4. **Prompt versioning** — How do we track which prompt version was used for each extraction? **Decision: Store in crawlMeta** — `extractionPromptVersion: "1.0"`.
5. **Human-in-the-loop** — For the initial 50-company pilot, should we manually review all extractions, or only flag low-confidence ones? **Decision: Review all** — establish baseline quality expectations.

---

## 18. Summary

| Phase | What | Coverage | Cost | When |
|:---|:---|:---:|:---|:---|
| **Phase 1 (MVP)** | Crawl 50 companies, validate approach | ~4% | ~$2/month | Week 1-3 |
| **Phase 2 (Scale)** | Crawl all 1,257 companies, ATS feedback loop | 100% | ~$15/month | Week 4-6 |
| **Phase 3 (Optimise)** | Build specific integrations where data justifies | 100% (higher quality) | ~$15-25/month | Ongoing |

> [!NOTE]
> The beauty of this approach is that **Phase 1 is the only hard engineering work**. Phase 2 is mostly ops/config. Phase 3 is data-driven — we only build what the numbers tell us to build. The default (crawling + LLM) covers everything at baseline quality.
