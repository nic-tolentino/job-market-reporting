# Coverage Strategies for Uncrawled Companies

Options and trade-offs for extracting job data from the **953 companies** not currently covered by our direct ATS API integrations.

**Current state (March 2026):**
- 419 companies covered by direct APIs (GH, LV, AS, SR, TT, WL) — 30.5% of 1,372
- **620 CUSTOM** — proprietary/embedded career pages
- **202 no ATS config** — career page URL unknown
- **67 Workday** — per-tenant auth required
- **64 other small ATSs** — SuccessFactors (8), BambooHR (6), JobAdder (6), Personio (5), Snaphire (4), others

---

## Option A: AI Crawler (Recommended Primary Strategy)

**Target:** CUSTOM (620) + no-config (202) = 822 companies
**Effort:** High upfront, low ongoing
**Cost:** ~$15/month

Deploy a self-hosted Crawlee + Gemini Flash service that crawls any career page and extracts structured job data via LLM. Full design in `ats-integration-plan.md` and `adr-008-crawler-service.md`.

### How it works
1. `PlaywrightCrawler` fetches and renders the career page (handles JS SPAs, pagination)
2. Content extractor strips nav/footer/scripts
3. Gemini 2.0 Flash parses jobs from cleaned HTML → `NormalizedJob[]`
4. `AtsDetector` checks for ATS signatures (iframes, script tags, URL patterns) — updates manifest if found
5. Kotlin backend's `CrawlerClient` receives the response and persists to Bronze

### What CUSTOM actually means
Many of the 620 CUSTOM companies likely fall into these sub-categories:
- **Embedded ATS widget** (e.g., Lever iframe, Greenhouse embed) — ATS detector will identify these on first crawl
- **Workday-hosted** — many "CUSTOM" companies may resolve to a Workday URL; the crawler will detect and update the manifest
- **True custom CMS** — requires LLM extraction every run
- **LinkedIn-only** — no external career page; need job board fallback

### Realistic expectations
- ~30-40% of CUSTOM companies will be reclassified to a known ATS provider on first crawl
- ~50-60% will work well with default LLM extraction
- ~10-20% will need per-company extraction hints (JS-heavy SPAs, anti-bot, login walls)
- ~5-10% may be unreachable (careers behind login, email-only applications)

### Career page URL discovery
For the 202 companies with no config, probe common paths from the manifest `website` field:
1. `{website}/careers`, `{website}/jobs`, `{website}/about/careers`
2. `{website}/work-with-us`, `{website}/join-us`, `{website}/opportunities`
3. As a fallback, Google search: `site:{domain} careers`

### Pros / Cons
| Pros | Cons |
|------|------|
| Covers all 822 companies at once | Requires Node.js/TypeScript microservice |
| LLM handles arbitrary page structures | LLM dependency (Gemini API outage) |
| ATS detection feedback loop enriches manifests | Prompt injection risk from career pages |
| No per-company API keys needed | Slower than direct API (~5-30s per company) |
| ~$15/month vs ~$100/month for Apify-only | Quality varies (SPAs, anti-bot, login walls) |

---

## Option B: Workday-Specific Scraper

**Target:** 67 Workday companies
**Effort:** Medium (1-2 weeks)
**Cost:** $0 (self-hosted on Cloud Run)

Workday career sites follow a predictable pattern: `{slug}.wd{N}.myworkdayjobs.com/searchpage`. The underlying data is served via a JSON API (`/wday/cxs/{tenant}/jobs`), making it possible to build a reliable scraper without an API key.

### Known Workday companies in our roster
Key companies: Autodesk, Spark NZ, BNZ, PwC, Red Hat, Fisher & Paykel, Westpac NZ, DXC Technology, Synechron, Tencent, JBT Marel, EROAD, Unisys, Watercare, CAE, Corpay, and ~50 more.

### Implementation approach
```typescript
// Workday JSON API (no auth required for public postings)
GET https://{tenant}.wd{N}.myworkdayjobs.com/wday/cxs/{tenant}/jobs
Body: { "limit": 20, "offset": 0, "searchText": "", "locations": [] }

// Returns structured JSON with title, location, department, description, applyUrl
```

The N in `wd{N}` varies by tenant (1-5). Discovery: probe `wd1` through `wd5` until you get a 200.

### Pros / Cons
| Pros | Cons |
|------|------|
| Structured JSON (reliable, no LLM needed) | Workday may change their internal API |
| 67 companies in one integration | Each company's `wd{N}` value needs discovery |
| Predictable URL pattern | Not an official API (ToS grey area) |
| Works better than generic LLM for Workday | Workday's React app is heavy — use direct API, not browser |

---

## Option C: Job Board Aggregation (Seek + LinkedIn + Indeed)

**Target:** Broad ANZ coverage, including companies on any platform
**Effort:** Low-Medium (Apify actors already exist)
**Cost:** ~$30-80/month (Apify usage)

Rather than crawling each company's career page, scrape the major job boards directly. This is a complementary strategy, not a replacement — it finds jobs from companies regardless of their ATS.

### Seek (highest priority for ANZ)
Seek dominates ANZ job searching (~90% of job seeker traffic). Many government and enterprise companies post exclusively to Seek. The [Apify Seek actor](https://apify.com/misceres/seek-jobs-scraper) is ready to use.

```
Search parameters for tech jobs:
- Keywords: software engineer, developer, data, devops, cloud, security, product, UX
- Locations: New Zealand, Australia
- Categories: Information & Communication Technology
- Cost: ~$2.50/1,000 results → ~$3-5/month for our volume
```

### LinkedIn (existing)
Already implemented via Apify actor. Continue as-is. Supplement with Seek rather than replace.

### Indeed
Broad international coverage. Useful for global companies with ANZ presence. Apify actor available. Worth adding if Seek doesn't cover a significant gap.

### Trade Me Jobs (NZ-specific)
NZ-only job board. Significant overlap with Seek for tech roles. Low marginal value vs effort.

### Pros / Cons
| Pros | Cons |
|------|------|
| Covers companies regardless of ATS | Job board data is less structured than direct API |
| Established Apify actors (low build effort) | No guarantee all companies post to each board |
| Seek is very high coverage for ANZ | Salary/benefits data often missing |
| Supplementary — doesn't replace direct APIs | Deduplication complexity increases |

---

## Option D: Dedicated Scrapers for Enterprise ATSs

**Target:** SuccessFactors (8), BambooHR (6), other small ATSs
**Effort:** Low-Medium per ATS
**Cost:** $0 (self-hosted)

These ATSs have either undocumented JSON endpoints or XML feeds that can be scraped reliably.

### SuccessFactors (SAP) — 8 companies
SAP SuccessFactors career sites expose jobs via a public REST endpoint:
```
GET https://jobs.sap.com/api/sf/external/jobsearch?site={tenantId}&start=0&count=50
```
Some tenants also have XML job feeds at predictable paths. The AI crawler may handle these adequately, but a dedicated scraper would be more reliable.

Key companies: Fisher & Paykel Healthcare, MBIE, Wētā FX, Z Energy.

### BambooHR — 6 companies
BambooHR has a public careers listing endpoint:
```
GET https://{subdomain}.bamboohr.com/careers/list
```
This returns JSON job listings without auth. Each company just needs their subdomain.

Key companies: Dawn Aerospace, Letterboxd, Lyssna, MATTR, Optimal.

### Fantastic.jobs (unified aggregator)
[Fantastic.jobs](https://fantastic.jobs) aggregates jobs from BambooHR, Workable, TeamTailor, and others via a single API. At 6-12 companies across these platforms, their per-request pricing may be cheaper than maintaining separate scrapers.

Evaluate: Does their pricing beat the sum of our direct integration costs for low-volume providers?

### Pros / Cons
| Pros | Cons |
|------|------|
| Structured data (no LLM) | ~6-8 companies each — low ROI |
| More reliable than generic crawler | Undocumented APIs can break without warning |
| BambooHR JSON endpoint is stable | Engineering effort per ATS |
| | Fantastic.jobs adds another vendor dependency |

---

## Option E: ATS Middleware (Merge.dev / Kombo)

**Target:** All ATS providers via unified API
**Effort:** Low (vendor manages integrations)
**Cost:** $300-1,000+/month

Services like [Merge.dev](https://merge.dev) and [Kombo](https://kombo.dev) provide a single API covering 50+ ATS providers. Each company grants access once; Merge handles token refresh, normalisation, and API versioning.

### When this makes sense
| Criteria | Current situation | Threshold to reconsider |
|---------|-----------------|------------------------|
| ATS providers to support | 6 integrated | >15 distinct providers needed |
| Per-company onboarding friction | Low (public APIs) | High (OAuth/API-key per company at scale) |
| Engineering maintenance burden | Low | Significant (multiple breaking API changes/year) |
| Monthly budget | ~$15 | Has revenue to justify $300+/month |

**Verdict:** Not now. Revisit when revenue justifies cost, or when Tier 2 onboarding friction becomes a bottleneck.

---

## Option F: Re-investigate CUSTOM Companies

**Target:** 620 CUSTOM companies
**Effort:** Low (script-based discovery)
**Cost:** $0

Many "CUSTOM" companies were labelled before we had automated ATS detection. A targeted re-investigation pass could reclassify 100-200 of them to known providers without building a full crawler.

### Automated rediscovery
Run `scripts/ats/probe_vanity_domains.py` style checks against the `website` field:
1. Check for ATS-specific HTTP headers/meta tags
2. Follow redirects from common paths (`/careers`, `/jobs`)
3. Check for known iframe sources (Greenhouse, Lever, Ashby embed scripts)
4. Check LinkedIn company page for ATS link patterns in job postings

This is essentially a lightweight version of the AI crawler's `AtsDetector` module — it can run ahead of the full crawler deployment as a quick win.

### Manual audit of top-priority companies
For the top 50 CUSTOM companies by company size/prominence, manually verify their career page and update the manifest. High ROI for prominent companies that likely have many open roles.

### Pros / Cons
| Pros | Cons |
|------|------|
| Zero cost, quick wins | Manual effort for accurate results |
| Reclassifies companies to integrated ATSs | Script-based detection has false negative rate |
| Reduces crawler workload | Doesn't solve the structural CUSTOM problem |

---

## Recommended Sequencing

| Phase | Strategy | Target | Timeline |
|-------|----------|--------|---------|
| **Now** | Re-investigate CUSTOM (Option F) | 620 CUSTOM → reclassify ~100-200 | 1-2 days |
| **Q2 2026** | AI Crawler MVP (Option A) | 822 companies | 3-4 weeks |
| **Q2 2026** | Seek integration (Option C) | Broad ANZ coverage | 1 week |
| **Q3 2026** | Workday scraper (Option B) | 67 companies | 1-2 weeks |
| **Q3 2026** | BambooHR scraper (Option D) | 6 companies | 2-3 days |
| **When justified** | Middleware (Option E) | All ATSs | Revenue-gated |

### Coverage projection

| After phase | Direct API companies | Crawler coverage | Total estimated coverage |
|-------------|---------------------|-----------------|------------------------|
| Today | 419 (30.5%) | 0% | ~30% |
| + CUSTOM reinvestigation | ~550 (40%) | 0% | ~40% |
| + AI Crawler | ~550 | ~60% of remainder | ~75% |
| + Seek | ~550 | ~75% of remainder | ~85% |
| + Workday | ~617 (45%) | ~75% | ~88% |
| + BambooHR, SuccessFactors | ~629 (46%) | ~75% | ~90% |

---

## Key Constraints & Watch-Outs

**robots.txt compliance** — always check before scraping. Most career pages allow crawling but respect rate limits.

**Anti-bot / CAPTCHA** — enterprise career pages (Workday, SuccessFactors) and large consumer companies (TikTok, Apple) may block automated access. The AI crawler should respect this and flag the company for manual review rather than retry aggressively.

**Login walls** — some companies require account creation to view all jobs. These are a dead end for automated crawling. Flag and skip.

**LinkedIn-only companies** — some companies post exclusively through LinkedIn Easy Apply with no external career page. These should be served by the LinkedIn/Seek scraper path, not the career page crawler.

**GDPR / ToS** — crawling public career pages to aggregate job listings is generally considered legitimate. We should avoid storing personal data (recruiter names/emails from job descriptions), respect `robots.txt`, and document our legal basis for data collection.
