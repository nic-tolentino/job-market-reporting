# ATS Integration — Multi-Source Job Data Pipeline

This document defines the strategy for integrating with Applicant Tracking System (ATS) providers and supplementary job data sources (scrapers, job boards) to build a comprehensive, multi-source job data pipeline. It reflects the current state of the codebase as of **March 2026** and prioritizes ATS systems based on our real-world company roster analysis.

> **Status update (March 2026):** Greenhouse, Lever, Ashby, SmartRecruiters, TeamTailor, and Workable are all fully implemented. 419 companies (30.5% of 1,372) are now covered by direct ATS API integrations. The remaining coverage challenge is the **620 CUSTOM** companies and **67 Workday** companies — see `uncrawled-coverage-strategies.md` for the full roadmap.

---

## 1. Current State

### What's Already Built

The backend has a functional ATS integration framework with the following components:

| Component | Path | Status |
|:---|:---|:---|
| `AtsProvider` enum | `sync/ats/AtsProvider.kt` | ✅ Implemented |
| `AtsClient` interface | `sync/ats/AtsClient.kt` | ✅ Implemented |
| `AtsNormalizer` interface | `sync/ats/AtsNormalizer.kt` | ✅ Implemented |
| `NormalizedJob` model | `sync/ats/model/NormalizedJob.kt` | ✅ Implemented |
| `AtsClientFactory` | `sync/ats/AtsClientFactory.kt` | ✅ Implemented |
| `AtsNormalizerFactory` | `sync/ats/AtsNormalizerFactory.kt` | ✅ Implemented |
| `GreenhouseClient` + `GreenhouseNormalizer` | `sync/ats/greenhouse/` | ✅ Implemented |
| `LeverClient` + `LeverNormalizer` | `sync/ats/lever/` | ✅ Implemented |
| `AshbyClient` + `AshbyNormalizer` | `sync/ats/ashby/` | ✅ Implemented (v1+v2 API) |
| `SmartRecruitersClient` + `SmartRecruitersNormalizer` | `sync/ats/smartrecruiters/` | ✅ Implemented |
| `TeamTailorClient` + `TeamTailorNormalizer` | `sync/ats/teamtailor/` | ✅ Implemented |
| `WorkableClient` + `WorkableNormalizer` | `sync/ats/workable/` | ✅ Implemented |
| `AtsJobDataSyncService` | `sync/AtsJobDataSyncService.kt` | ✅ Implemented (full Bronze→Silver flow) |
| `AtsJobDataMapper` | `sync/AtsJobDataMapper.kt` | ✅ Implemented |
| `CompanyAtsConfig` model | `persistence/model/CompanyAtsConfig.kt` | ✅ Implemented |
| `AtsConfigBigQueryRepository` | `persistence/ats/AtsConfigBigQueryRepository.kt` | ✅ Implemented |
| `AtsSyncController` | `api/AtsSyncController.kt` | ✅ Implemented |
| `TechRoleClassifier` | Used in sync pipeline | ✅ Filters non-tech roles |

**Key insight**: The infrastructure is provider-agnostic. Adding a new ATS only requires implementing `AtsClient` and `AtsNormalizer` — everything downstream (Bronze persistence, Silver mapping, dedup, merge) already works.

### What We Know About Our Companies

As of March 2026, **1,372 companies** with all ATS configs validated (199 stale entries cleaned up):

| Provider | Companies | % of total | API Type | Integration Status |
|:---|:---:|:---:|:---|:---|
| **CUSTOM** | 620 | 45.2% | — | ❌ Requires crawler/scraper |
| **SmartRecruiters** | 145 | 10.6% | ✅ Public (Job Board API) | ✅ Integrated |
| **Greenhouse** | 79 | 5.8% | ✅ Public (Boards API) | ✅ Integrated |
| **Workable** | 76 | 5.5% | ✅ Public (POST /jobs) | ✅ Integrated |
| **Lever** | 75 | 5.5% | ✅ Public (v0 Postings API) | ✅ Integrated |
| **Workday** | 67 | 4.9% | ⚠️ Restricted (per-tenant auth) | ❌ Scraper needed |
| **Ashby** | 40 | 2.9% | ✅ Public (Posting API) | ✅ Integrated |
| **OTHER** | 14 | 1.0% | — | ❌ No identifier |
| **SuccessFactors** | 8 | 0.6% | ⚠️ Enterprise only | ❌ Scraper needed |
| **JobAdder** | 6 | 0.4% | ⚠️ OAuth 2.0 | Low priority |
| **BambooHR** | 6 | 0.4% | ⚠️ API key per tenant | Low priority |
| **TeamTailor** | 4 | 0.3% | ✅ Public (feed/jobs.json) | ✅ Integrated |
| **Other** (Personio, Snaphire, Join, etc.) | 27 | 2.0% | Varies | Low priority |
| **No ATS config** | 202 | 14.7% | — | ❌ Requires crawler |
| **Total** | **1,372** | **100%** | | |

**Coverage summary:**
- **419 companies (30.5%)** are covered by direct ATS API integrations
- **751 companies (54.7%)** have ATS configs we cannot crawl via API
- **202 companies (14.7%)** have no ATS config at all

---

## 2. Tiered Integration Strategy

### Tier 1: Public APIs — ✅ COMPLETE

All six public API providers are now integrated:

| Provider | Companies | Status |
|:---|:---:|:---|
| **Greenhouse** | 79 | ✅ `GreenhouseClient` + `GreenhouseNormalizer` |
| **SmartRecruiters** | 145 | ✅ `SmartRecruitersClient` + `SmartRecruitersNormalizer` |
| **Workable** | 76 | ✅ `WorkableClient` + `WorkableNormalizer` |
| **Lever** | 75 | ✅ `LeverClient` + `LeverNormalizer` |
| **Ashby** | 40 | ✅ `AshbyClient` + `AshbyNormalizer` (v1 + v2) |
| **TeamTailor** | 4 | ✅ `TeamTailorClient` + `TeamTailorNormalizer` |

**Combined coverage**: 419 companies (30.5% of total)

---

### Tier 2: Complex / Enterprise APIs — Scrape Instead

These providers require per-tenant auth or have no practical public API.

| Provider | Companies | Challenge | Recommendation |
|:---|:---:|:---|:---|
| **Workday** | 67 | Per-tenant Staffing API auth | **Scrape** — predictable URL: `{slug}.wd{N}.myworkdayjobs.com` |
| **SuccessFactors** | 8 | Enterprise SAP integration | **Scrape** |
| **JobAdder** | 6 | OAuth 2.0 partner registration | Low priority — 6 companies |
| **BambooHR** | 6 | API key per tenant | Low priority — 6 companies |
| **Snaphire/Personio/Others** | ~27 | Varies | Low priority |

> [!CAUTION]
> **Workday (67 companies, 4.9%)** is the most impactful remaining target. Its API is tenant-gated but career pages follow predictable URL patterns. A dedicated Workday scraper is the pragmatic path — see `uncrawled-coverage-strategies.md`.

---

### Tier 3: Generic Fallback — 822 companies (59.9%)

These include the **620 CUSTOM** companies (proprietary career pages, embedded widgets, bespoke CMS) and the **202 with no ATS config**. They need the AI crawler.

See **Sections 4–5** (NLP extraction and scraping services) and `uncrawled-coverage-strategies.md` for the full roadmap.

---

## 3. ATS Provider API Reference

### 3.1 Greenhouse (Public Job Board API) — ✅ IMPLEMENTED

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs` |
| **Single Job** | `GET https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs/{job_id}` |
| **Auth** | ❌ None required (public data) |
| **Rate Limits** | Reasonable — standard HTTP polling is fine |
| **Format** | JSON |
| **Key Fields** | `id`, `title`, `location.name`, `content` (HTML), `departments[].name`, `offices[].name`, `updated_at`, `absolute_url` |
| **Compensation** | Not exposed in public API |
| **Pagination** | Query param `?content=true` to include body; all jobs returned in one response |

**Implementation**: `GreenhouseClient.kt` + `GreenhouseNormalizer.kt` are fully built and tested.

**Validated identifiers:**
- Cabify: `cabify`
- Canonical: `canonical`
- Clover Health: `cloverhealth`
- Karbon: `karbon`
- Mr Apple: `mrapplecareers`
- PALO IT: `paloit`
- Pushpay: `pushpay`
- ROLLER: `roller`
- Re-Leased: `released`
- Remote: `remotecom`
- Riot Games: `embed`
- Rocket Lab: `rocketlab`
- Speechify: `speechify`
- Topsort: `topsort`
- Xero: `xero`

---

### 3.2 Lever (Postings API v0) — ❌ NOT YET IMPLEMENTED

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://api.lever.co/v0/postings/{company_slug}` |
| **Single Job** | `GET https://api.lever.co/v0/postings/{company_slug}/{posting_id}` |
| **Auth** | ❌ None required for public postings |
| **Format** | JSON |
| **Key Fields** | `id`, `text` (title), `categories.location`, `categories.department`, `categories.team`, `categories.commitment` (employment type), `description`, `descriptionPlain`, `lists[]`, `hostedUrl`, `applyUrl`, `createdAt`, `workplaceType` |
| **Compensation** | `salaryRange` object with `min`, `max`, `currency`, `interval` |
| **Pagination** | `skip` and `limit` params (default limit 100) |

**Notes**: Extremely simple. Salary data available when companies publish it. `categories` gives us structured department/team/employment type.

**Validated identifiers:**
- 360Learning: `360learning`
- ANYbotics: `anybotics`
- Blinq: `blinq`
- Cin7: `cin7`
- Enable: `enable`
- Envato: `envato-2`
- Foodstuffs: `foodstuffs`
- Hatch: `q-ctrl`
- InDebted: `indebted`
- Jobgether: `jobgether`
- KPMG New Zealand: `kpmgnz`
- Kogan.com: `kogan`
- Kraken: `kraken123`
- MYOB: `myob-2`
- Megaport: `megaport`
- Okendo: `okendo`
- Onit: `onit`
- Planner 5D: `planner5d`
- Procreate: `procreate`
- Q-CTRL: `q-ctrl`
- Veepee: `veepee`

---

### 3.3 Ashby (Posting API) — ❌ NOT YET IMPLEMENTED

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://api.ashbyhq.com/posting-api/job-board/{company_slug}` |
| **Auth** | ❌ None required for public job board |
| **Format** | JSON |
| **Key Fields** | `jobs[].id`, `jobs[].title`, `jobs[].location`, `jobs[].department`, `jobs[].employmentType`, `jobs[].descriptionHtml`, `jobs[].publishedAt`, `jobs[].jobUrl`, `jobs[].applyUrl` |
| **Compensation** | Available via `?includeCompensation=true` — returns `compensationTierSummary` string |
| **Pagination** | All jobs returned in a single response |

**Notes**: Returns everything in one call. Compensation summary needs parsing into min/max values.

**Validated identifiers:**
- Airwallex: `airwallex`
- Alan: `alan`
- EngFlow: `engflow`
- Fetch: `fetch-pet-health`
- Halter: `halter`
- Heidi Health: `heidihealth`
- Magic Eden: `magiceden`
- Neon One: `HighlightTA`
- Notion: `notion`
- Partly: `partly`
- Plain: `plain`
- Prosper AI: `prosper-ai`
- TravelPerk: `Perk`

---

### 3.4 Workable (REST API) — Tier 2

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://{subdomain}.workable.com/spi/v3/jobs` |
| **Auth** | ✅ API token required (per account, `r_jobs` permission) |
| **Format** | JSON |
| **Key Fields** | Job title, description, location, department, employment type, application URL |
| **Notes** | Returns all jobs (draft, published, closed, archived) — filter for `state: published`. 8 companies identified. Each company must generate an API token from their Workable admin panel. |

---

### 3.5 SmartRecruiters (Job Board API) — Tier 2

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://api.smartrecruiters.com/v1/companies/{companyId}/postings` |
| **Auth** | ✅ API key or partner token required |
| **Format** | JSON |
| **Key Fields** | Job title, location, department, description, application URL |
| **Notes** | Job Board API is specifically designed for job board vendors. 7 companies including Air NZ, Canva, Deloitte, Eurofins, Vector, Visa. Apify has a SmartRecruiters scraper actor as a fallback. |

---

### 3.6 BambooHR (Careers API) — Tier 2

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://api.bamboohr.com/api/gateway.php/{subdomain}/v1/applicant_tracking/jobs` |
| **Auth** | ✅ API key required (per tenant, no public API) |
| **Format** | JSON |
| **Notes** | 6 companies identified (Dawn Aerospace, Kami, Letterboxd, Lyssna, MATTR, Optimal). Each company must provide their BambooHR API key. Third-party service Fantastic.jobs aggregates BambooHR listings via a single endpoint. |

---

### 3.7 Teamtailor (Client API) — Tier 2

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://api.teamtailor.com/v1/jobs` |
| **Auth** | ✅ API key (token auth) per tenant |
| **Format** | JSON |
| **Notes** | 4 companies (Gallagher, Henry Schein One, PredictHQ). Also supports XML feeds and HTTP Webhooks. XML feed may be publicly shareable. |

---

### 3.8 Workday — Tier 3 (Scrape Instead)

| Aspect | Detail |
|:---|:---|
| **URL Pattern** | `{slug}.wd{N}.myworkdayjobs.com` (N = 1–5) |
| **Auth** | ✅ Requires per-tenant integration user (Staffing REST API) |
| **Our companies** | 25 (Autodesk, BNZ, Beca, Spark NZ, PwC, Red Hat, Westpac NZ, etc.) |
| **Recommendation** | **Do not build a Workday API client.** The per-tenant auth model is impractical. Use Apify's Workday scraper actor or build a custom Crawlee-based scraper that targets the predictable `{slug}.wd{N}.myworkdayjobs.com` pattern. |

---

### 3.9 Apify (Supplementary — LinkedIn Scraper) — ✅ IMPLEMENTED

| Aspect | Detail |
|:---|:---|
| **Endpoint** | `GET https://api.apify.com/v2/datasets/{dataset_id}/items` |
| **Auth** | ✅ API Key (already implemented) |
| **Format** | JSON |
| **Key Fields** | Already mapped — see existing `ApifyJobDto` |
| **Role** | **Catch-all** for companies not covered by direct ATS integrations or dedicated scrapers |

---

## 4. Generic NLP/AI Fallback for Unstructured Career Pages

For the **1,104 companies (87.8%)** with no identified ATS and the **41 companies (3.2%)** on enterprise ATS platforms (Workday, SuccessFactors, etc.) where direct API access is impractical, we need a generic fallback that can extract structured job data from arbitrary web pages.

### 4.1 The Problem

These companies' career pages vary wildly:
- Some use proprietary CMS platforms with embedded job widgets
- Some post directly to LinkedIn (Easy Apply) with no external careers page
- Some use enterprise ATS (Workday, SuccessFactors) with complex, JavaScript-heavy pages
- Some are small companies with simple HTML pages listing jobs

### 4.2 Option A: LLM-Powered Page Parsing (Recommended Default)

Use an LLM to extract structured job data from raw HTML/text of any career page. This is the most flexible approach and can handle arbitrary page structures without writing custom parsers.

**How it works:**
1. Fetch the career page HTML (via Apify, Crawlee, or direct HTTP)
2. Strip boilerplate (nav, footer, scripts) to extract the main content
3. Send the content to an LLM (Gemini, GPT-4o, Claude) with a structured extraction prompt
4. LLM returns JSON conforming to our `NormalizedJob` schema
5. Store raw HTML in Bronze, parsed results flow through normal Silver pipeline

**Prompt template:**
```
Extract all job postings from the following career page content.
For each job, return a JSON object with:
- title, location, department, employmentType, description,
  salaryMin, salaryMax, salaryCurrency, applyUrl, postedDate

If a field is not found, return null. Return a JSON array of jobs.

Page content:
{page_content}
```

**Cost estimate** (Gemini 2.0 Flash):
- Average career page: ~5,000 tokens input, ~1,000 tokens output
- Cost: ~$0.0001 per page (at $0.075/1M input + $0.30/1M output)
- For 1,000 companies weekly: **$0.43/month** (essentially free)
- Compare to Apify LinkedIn scraper: ~$1/1,000 jobs ≈ $4.50/month for 4,500 jobs

**Pros:**
- Works on any page structure without custom code
- Can extract nuanced data (salary from description text, seniority from context)
- Extremely cheap with Gemini Flash
- Can improve extraction quality just by updating the prompt

**Cons:**
- Requires reliable page fetching (JavaScript rendering for SPAs)
- Occasional hallucination / incorrect extraction (mitigate with validation)
- Need to discover career page URLs first (can automate with web search)
- Slower than direct API calls (~2-5 seconds per page vs <1 second for API)

> [!TIP]
> **LLM extraction is dramatically cheaper than building and maintaining custom scrapers** for the long tail of companies. At $0.0001/page, we could parse 10,000 career pages for $1.00. The engineering time saved by not building per-ATS scrapers more than justifies occasional extraction errors.

### 4.3 Option B: Rule-Based HTML Scraping (Cheap but Fragile)

Build a generic career page scraper using CSS selectors and heuristics to identify job listing patterns. This is what traditional scraping services do.

**How it works:**
1. Fetch career page HTML
2. Look for common job listing patterns (repeating `<div>` or `<li>` elements with titles, locations)
3. Extract using CSS selectors and regex
4. Heuristically parse location, salary, etc.

**Pros:** Fast, no LLM cost, works consistently on pages with stable structure
**Cons:** Breaks when pages change layout, requires per-site maintenance, can't extract nuanced data

**Verdict:** Not recommended as a primary strategy. Too fragile for 90+ diverse company pages. However, CSS-based extraction is still useful as pre-processing before LLM parsing (strip boilerplate to reduce token count).

### 4.4 Option C: Hybrid Approach (Recommended)

Combine scraping for page fetching with LLM for data extraction:

1. **Page fetching**: Use Apify/Crawlee/BrightData to fetch and render career pages (handles JavaScript, pagination, anti-bot protections)
2. **Data extraction**: Send the rendered content to Gemini Flash for structured extraction
3. **Validation**: Run extracted data through `RawJobDataParser` validation rules (non-empty title, valid location, etc.)
4. **Fallback**: If LLM extraction fails quality checks, flag for manual review

This gives us the reliability of professional scraping infrastructure with the flexibility of LLM-based parsing. Total cost for 1,000 companies weekly: **< $10/month** (scraping + LLM combined).

---

## 5. Scraping Services — Apify and Alternatives

### 5.1 Current State: Apify

We currently use Apify for LinkedIn scraping only. Apify has a broader ecosystem we should leverage:

| Actor | Target | Cost | Notes |
|:---|:---|:---|:---|
| **LinkedIn Jobs Scraper** | LinkedIn | ~$1/1,000 jobs | Currently used. Guest API, no login needed. |
| **Seek Job Scraper** | Seek (AU/NZ) | ~$2.50/1,000 results | Ready to use. Handles pagination. |
| **Workday Jobs Scraper** | Workday career sites | Varies | Covers our 23 Workday companies |
| **SmartRecruiters Scraper** | SmartRecruiters boards | Varies | Covers our 10 SmartRecruiters companies |
| **Indeed Scraper** | Indeed | ~$1.50/1,000 jobs | Broad coverage, useful for companies not on LinkedIn |
| **Generic Web Scraper** | Any URL | Varies | Can fetch arbitrary career pages for LLM parsing |

**Current monthly Apify cost**: ~$49/month plan (sufficient for our current volume)

### 5.2 Alternatives to Apify

| Service | Best For | Pricing | Recommendation |
|:---|:---|:---|:---|
| **BrightData** | Enterprise-grade scraping, LinkedIn, proxy infrastructure | $0.001/record for jobs API; $499+/mo for heavy use | Consider if we need higher LinkedIn volume or run into anti-bot issues. LinkedIn scraping starts at ~$500 for 151K page loads. Overkill for our current scale. |
| **Crawlee** (self-hosted) | Custom scrapers, Workday/SuccessFactors pages | Free (open-source, built by Apify) | **Recommended** for building custom scrapers we host on Cloud Run. Zero per-request cost. Ideal for Workday, SuccessFactors, and generic career page scraping. |
| **ScrapingBee** | Simple API-based scraping with proxy rotation | $49/mo for 250K credits | Alternative to Apify for simple HTTP scraping. Similar capabilities, different pricing model. |
| **Octoparse** | No-code visual scraping | $83/mo (Standard) | Not recommended — our team is technical enough for code-based solutions. |
| **Zyte (Scrapinghub)** | Managed scraping service | Custom pricing | Consider if we want to outsource scraper maintenance entirely. |
| **Fantastic.jobs** | ATS-specific job aggregation API | Pay-as-you-go | Aggregates jobs from BambooHR, Workable, Teamtailor via single API. Worth evaluating vs building individual clients. |

### 5.3 Multi-Source Scraping Recommendation

| Source | Tool | Coverage | Priority |
|:---|:---|:---|:---|
| **LinkedIn** | Apify (existing) | Primary source for all companies | Keep as-is |
| **Seek (AU/NZ)** | Apify Seek Actor | ~50-60% of ANZ tech jobs not on LinkedIn | **High priority** — massive coverage gap |
| **Trade Me Jobs (NZ)** | Custom Crawlee scraper | NZ domestic companies | Medium priority |
| **Workday career pages** | Apify Workday Actor or Crawlee | 23 companies (1.8%) | High priority |
| **SuccessFactors pages** | Custom Crawlee scraper | 8 companies | Low priority |
| **Generic career pages** | Crawlee + LLM extraction (Section 4) | 1,104 unidentified companies | Medium priority |

> [!IMPORTANT]
> **Seek is the #1 missing source.** Seek dominates the ANZ job board market (~90% of job seeker traffic). Many government, enterprise, and NZ-only companies post exclusively to Seek. Adding Seek could expand our database from ~450 to 1,000+ active tech roles. The Apify Seek Actor costs ~$2.50/1,000 results, meaning ~$2-5/month for our volume.

### 5.4 Cost Comparison Summary

| Approach | Monthly Cost (1,000 companies) | Coverage | Maintenance |
|:---|:---|:---:|:---|
| Direct ATS APIs (Tier 1) | **$0** | 3.9% of companies | Low — public APIs rarely change |
| Direct ATS APIs (Tier 2) | **$0** | +3.5% of companies | Medium — per-company onboarding |
| Apify LinkedIn (existing) | **~$49** (plan) | All companies on LinkedIn | Low — existing infrastructure |
| Apify Seek Actor | **~$20-30** | ANZ-focused companies | Low |
| Apify Workday Actor | **~$5-10** | 23 Workday companies | Low |
| Crawlee (self-hosted) | **$0** (Cloud Run free tier) | Custom targets | Medium — we maintain the scrapers |
| LLM extraction (Gemini Flash) | **~$0.43** | 1,100+ misc career pages | Low — prompt-based |
| BrightData | **$499+** | Enterprise-scale LinkedIn | Not recommended yet |
| Unified ATS middleware (Merge.dev) | **$300-1,000+** | 50+ ATS providers | Low — vendor manages |
| **Total (recommended stack)** | **~$80-100/month** | **90%+ of market** | |

---

## 6. Unified ATS Middleware (Merge / Knit / Kombo)

Services like [Merge.dev](https://merge.dev), [Knit](https://getknit.dev), and [Kombo](https://kombo.dev) provide a single API aggregating 50+ ATS providers behind a unified schema.

### 6.1 When This Makes Sense

| Criteria | Our Situation | Verdict |
|:---|:---|:---|
| Number of ATS providers | 12+ identified | Approaching the threshold where middleware saves effort |
| Per-company onboarding cost | High for OAuth/API-key providers | Middleware handles auth lifecycle |
| Engineering maintenance burden | Low currently (only Greenhouse built) | Will grow as we add providers |
| Budget | $0/month target | Middleware costs $300-1,000+/month — conflicts with budget constraints |

### 6.2 Recommendation

**Don't adopt middleware yet.** Our budget target is near-zero, and we only need ~3 public API providers built to cover the highest-value segment. Here's when to reconsider:

1. If we need to onboard **50+ Tier 2 companies** (API-key-per-tenant becomes painful)
2. If we expand into international markets with **dozens of new ATS providers**
3. If the platform generates **revenue** that justifies $300+/month software costs

**For now**: Build Lever + Ashby + Workable clients (all under 2 days work each), and cover everything else via Apify + Crawlee + LLM extraction.

---

## 7. Unified Data Model — Field Mapping

Every ATS has a different shape. The table below shows how each provider's fields map to our existing `JobRecord` and `CompanyRecord` models.

### 7.1 JobRecord Mapping

| Our Field | Greenhouse | Lever | Ashby | Workable | SmartRecruiters | BambooHR | Apify (existing) |
|:---|:---|:---|:---|:---|:---|:---|:---|
| `jobId` | Generated slug | Generated slug | Generated slug | Generated slug | Generated slug | Generated slug | Generated slug |
| `platformJobIds` | `id` | `id` | `id` | Job ID | Job ID | Job ID | `id` |
| `title` | `title` | `text` | `title` | `title` | `name` | `jobTitle` | `title` |
| `companyName` | From config | From slug | From slug | From config | From API | From subdomain | `companyName` |
| `description` | `content` (HTML) | `descriptionPlain` | `descriptionHtml` | `description` | `jobAd.sections` | `description` | `descriptionText` |
| `city` | Parse `location.name` | Parse `categories.location` | Parse `location` | `location.city` ✅ | `location.city` ✅ | `location.city` ✅ | `location` (parsed) |
| `country` | Parse `offices[].location` | Parse `categories.location` | Parse `location` | `location.country` ✅ | `location.country` ✅ | — | `location` (parsed) |
| `salaryMin` | — | `salaryRange.min` ✅ | Parse `compensationTierSummary` | Via description | — | — | `salaryInfo` (parsed) |
| `salaryMax` | — | `salaryRange.max` ✅ | Parse `compensationTierSummary` | Via description | — | — | `salaryInfo` (parsed) |
| `employmentType` | — | `categories.commitment` ✅ | `employmentType` ✅ | `employment_type` ✅ | `typeOfEmployment` ✅ | `employmentStatus` ✅ | `employmentType` |
| `workModel` | Heuristic | `workplaceType` ✅ | Heuristic | Heuristic | Heuristic | Heuristic | Heuristic |
| `postedDate` | `updated_at` | `createdAt` | `publishedAt` | `published_on` | `releasedDate` | `datePosted` | `postedAt` |
| `applyUrls` | `absolute_url` | `applyUrl` | `applyUrl` | `application_url` | `ref_links` | `applicationUrl` | `applyUrl` |
| `source` | `"Greenhouse"` | `"Lever"` | `"Ashby"` | `"Workable"` | `"SmartRecruiters"` | `"BambooHR"` | `"LinkedIn-Apify"` |

✅ = Natively structured (no heuristic/parsing needed)

### 7.2 CompanyRecord Mapping

| Our Field | Source |
|:---|:---|
| `companyId` | Generated from company name via `IdGenerator` |
| `name` | From company registration / ATS metadata |
| `logoUrl` | Employment Hero: `recruitment_logo`; Others: from `data/companies.json` |
| `website` | From `data/companies.json` (most ATS APIs don't expose this) |
| `industries` | From `data/companies.json` (profiled per company) |
| `technologies` | Aggregated from all `JobRecord.technologies` for this company |
| `hiringLocations` | Aggregated from all `JobRecord` locations for this company |
| `lastUpdatedAt` | Latest sync timestamp |

---

## 8. Architecture — Multi-Source Pipeline

### 8.1 Design Overview

The existing Bronze → Silver → Gold pipeline extends naturally. Each source feeds into Bronze via a source-specific client + normalizer, and the shared pipeline handles everything downstream.

**Source types:**
1. **Direct ATS APIs** — `AtsClient` + `AtsNormalizer` → `NormalizedJob` → Bronze → Silver
2. **Apify scrapers** — `ApifyClient` → `ApifyJobDto` → Bronze → Silver (existing)
3. **Crawlee scrapers** — Custom scraper → raw HTML → Bronze → `NormalizedJob` → Silver
4. **LLM extraction** — Fetch page → LLM prompt → `NormalizedJob` → Bronze → Silver

All four source types converge at the `NormalizedJob` model, which then flows through `AtsJobDataMapper` → `SilverDataMerger` → Silver layer.

### 8.2 Key Design Decisions

#### A. Source Normalizer Pattern (Already Built)

Each ATS gets a **Client** (HTTP communication) and a **Normalizer** (data shape translation). This pattern is already implemented in the codebase via `AtsClient` and `AtsNormalizer` interfaces.

#### B. Source Priority & Conflict Resolution

When the same company has data from multiple sources:

| Scenario | Resolution |
|:---|:---|
| Same job in ATS + Apify | **ATS wins** — structured data more reliable. Apify supplements missing fields. |
| Same job in ATS + Seek | **ATS wins**. Seek supplements. |
| Job only in one source | Use that source directly |
| Company metadata conflicts | ATS is primary; scraped sources supplement `alternateNames` |

#### C. Cross-Source Deduplication

Jobs from different sources for the same company need deduplication. Our existing `(company, country, title)` key works. Additional signals:

1. **Platform-ID cross-reference** — maps `(source, platformId)` → `jobId`
2. **Fuzzy title matching** — "Senior Software Engineer" vs "Sr. Software Engineer"
3. **Temporal proximity** — two postings with same title within 7 days = likely duplicate

---

## 9. Implementation Phases (Updated March 2026)

### Phase 1 — Tier 1 Public APIs ✅ COMPLETE

All six public API providers (GH, LV, AS, SR, TT, WL) are built and wired into the ATS sync pipeline. 419 companies covered.

---

### Phase 2 — Scheduling + AI Crawler (current)

**Scope:** Automate nightly polling via Cloud Scheduler + Cloud Tasks. Build the AI crawler for CUSTOM and no-ATS-config companies.

**🤖 Code tasks:**
- [ ] 🤖 Set up Cloud Scheduler → Cloud Tasks → Cloud Run sync pipeline
- [ ] 🤖 Build Crawlee + Gemini Flash crawler service (see `ats-integration-plan.md`)
- [ ] 🤖 Build `CrawlerClient` + `CrawlerNormalizer` in Kotlin backend
- [ ] 🤖 Add career page URL discovery (probe `/careers`, `/jobs`, etc. from manifest `website`)
- [ ] 🤖 Add extraction quality validation and confidence scoring

**👤 Your tasks:**
- [ ] 👤 Set up Cloud Scheduler cron job (nightly 2am NZST)
- [ ] 👤 Verify career page URLs for top 50 CUSTOM companies
- [ ] 👤 Deploy crawler service to Cloud Run

**Coverage result:** 822 additional companies (CUSTOM + no-config) targeted by crawler

---

### Phase 3 — Workday Scraper + Job Boards

**Scope:** Dedicated Workday scraper for 67 companies. Add Seek as a broad ANZ data source.

**🤖 Code tasks:**
- [ ] 🤖 Build `WorkdayScraperClient` (Crawlee targeting `{slug}.wd{N}.myworkdayjobs.com`)
- [ ] 🤖 Build `SeekClient` wrapper around Apify Seek Job Scraper actor
- [ ] 🤖 Update deduplication to handle ATS ↔ Seek ↔ Workday overlaps

**Coverage result:** +67 Workday companies; Seek adds potentially 500+ ANZ tech roles

---

### Phase 4 — Polish + Data Quality

**🤖 Code tasks (build on demand):**
- [ ] 🤖 Build `/api/admin/sync-status` dashboard endpoint
- [ ] 🤖 Add data quality metrics (missing fields, parse failures per source)
- [ ] 🤖 Connect multi-source data to Gold layer trend tables
- [ ] 🤖 Evaluate Fantastic.jobs aggregation vs maintaining individual small-ATS clients

---

## 10. Scheduled Polling System

### 10.1 Architecture

Given our Cloud Run deployment and "$0/month" cost goal, the approach is **Cloud Scheduler → Cloud Tasks → Cloud Run**.

> [!IMPORTANT]
> **Bronze-first ingestion**: Raw data from every source is stored in Bronze **before** any normalization. This mirrors our existing `JobDataSyncService` pattern and means we can safely change normalization logic in the future and reprocess all historical data from the immutable Bronze source.

### 10.2 Schedule Configuration

| Parameter | Value | Reasoning |
|:---|:---|:---|
| **Frequency** | **Nightly** (2:00 AM NZST) | Near-daily freshness at zero additional cost |
| **Task dispatch rate** | 5 tasks/second | Avoids overwhelming ATS APIs |
| **Retry policy** | 3 retries with exponential backoff (10s, 60s, 300s) | Handles transient failures |
| **Task timeout** | 5 minutes per company | Enough for large job boards |
| **Apify sync** | Existing webhook + nightly fallback | Keeps current flow; adds safety net |

### 10.3 Cost Breakdown (Nightly, 1,000 companies)

| Resource | Monthly Cost |
|:---|:---|
| Cloud Scheduler | **$0** (1 job, free tier covers 3) |
| Cloud Tasks | **$0** (~31,000 ops, free tier covers 1M) |
| Cloud Run invocations | **$0** (~30,000 requests, free tier covers 2M) |
| GCP Secret Manager | **~$0.06** |
| BigQuery storage (incremental) | **~$5.00** |
| Apify (LinkedIn + Seek + Workday) | **~$80** |
| Gemini Flash (LLM extraction) | **~$0.43** |
| **Total** | **~$85-90/month** |

> [!TIP]
> Even at 1,000 companies synced nightly, we'd use ~31,000 of our 1,000,000 free Cloud Tasks operations (~3.1%). We could scale to **30,000+ companies** before hitting the Cloud Tasks paid tier. The primary cost driver is Apify, not GCP infrastructure.

---

## 11. Risks & Mitigations

| Risk | Impact | Mitigation |
|:---|:---|:---|
| ATS API deprecation/versioning | Normalizer breaks silently | **Schema validation** — each normalizer validates incoming JSON shape. Emit warnings on unknown/missing fields. |
| Apify LinkedIn scraper breaks (DOM change) | Primary data source goes down | **Multi-source strategy** — ATS direct + Seek + LLM extraction provide redundancy. Alert if scraped job count drops >20% week-over-week. |
| LLM hallucination in extraction | Incorrect job data pollutes Silver | **Validation gate** — every LLM-extracted `NormalizedJob` must pass minimum quality thresholds (non-empty title, valid location). Reject and flag failures. |
| Seek scraping blocked by anti-bot | Lose ANZ coverage | Use Apify's managed actor (handles proxy rotation). Fallback to BrightData if Apify is blocked. |
| OAuth token expiry (Tier 2 providers) | Company data goes stale | **Pre-emptive refresh** — schedule mid-week token refresh via Cloud Tasks. Alert on 401 responses. |
| Cross-source dedup false positives | Valid jobs incorrectly merged | **Multi-signal matching** — require 2+ signals (company + title + temporal proximity) for dedup. Log all dedup decisions for audit. |
| PII in job descriptions | Privacy violation | **Reuse `PiiSanitizer`** — run all description text through existing sanitizer before Silver persistence. |
| Scraping legality (LinkedIn ToS) | Legal risk | **Minimize reliance** — as ATS and Seek coverage grows, reduce LinkedIn scraping to only companies with no other source. Document that ATS data comes from official APIs. |

---

## 12. Open Questions

1. **Seek integration scope** — Should we scrape all NZ/AU tech jobs on Seek (broad coverage, high volume, more dedup work) or only scrape for companies already in our database (targeted, lower volume)?
2. **LLM extraction priority** — Should we pursue LLM extraction (Phase 3) before or after Seek integration (Phase 2)? Seek gives more immediate coverage, but LLM extraction covers the long tail.
3. **Fantastic.jobs evaluation** — This service aggregates BambooHR + Workable + Teamtailor jobs via a single API. At 18 companies combined, is their per-request pricing cheaper than building 3 separate clients?
4. **Trade Me Jobs** — Worth the effort? It's NZ-only and likely has significant overlap with Seek/LinkedIn for tech roles.
5. **Unified ATS middleware** — At what point do we reevaluate Merge.dev / Kombo? If we onboard 30+ Tier 2 companies? When we have revenue to justify the cost?
6. **Indeed API** — The Indeed Publisher API provides broad job board coverage. Worth adding as another source alongside Seek?
7. **Historical backfill** — When a company first connects their ATS, do we backfill all current open roles immediately or wait for the next scheduled sync?

---

## 13. Summary: Path to 90%+ Coverage

| What | Companies | Direct API coverage | When |
|:---|:---:|:---:|:---|
| **✅ Phase 1 (All Tier 1 APIs)** | 419 via ATS direct | 30.5% | Done |
| **+ Phase 2 (AI Crawler)** | +822 CUSTOM/no-config | ~90% (ATS + crawl) | Q2 2026 |
| **+ Phase 3 (Workday + Seek)** | +67 Workday; broad ANZ | ~95% | Q3 2026 |
| **+ Phase 4 (Data quality)** | All 1,372 | 95%+ | Ongoing |

> [!NOTE]
> **100% coverage via ATS APIs alone is not feasible** — 45.2% of companies use CUSTOM career pages with no public API. The hybrid approach (ATS direct APIs + AI crawler + Seek/Apify) is how we get to 90%+ coverage at a sustainable cost (~$15-20/month).
