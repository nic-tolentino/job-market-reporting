# Crawler Extraction Improvement Plan

**Date:** March 2026
**Status:** Active — Phase 0 expanded, Sprint 1 extractors shipped, identification sprint 2 complete
**Related docs:**
- `docs/data/ats/ats-identification-plan.md` — ATS identification roster and tooling
- `docs/data/ats/ats-integration-plan.md` — Full crawl-first strategy and ATS integration architecture
- `docs/architecture/adr-008-crawler-service.md` — Crawler service ADR
- `docs/data/autechjobs-historical-data.md` — autechjobs SQL dump analysis and ingestion plan

---

## Overview

We are taking a two-pronged approach to improving job extraction quality and coverage:

1. **Prong 1 — ATS Identification + Native Extractors**: Identify which ATS each company uses and build free, API-native extractors for the providers with public JSON APIs (Greenhouse, Lever, Ashby). This gives us structured, high-quality data for the companies we can identify.

2. **Prong 2 — Self-Improving LLM Crawler**: Improve the generic crawler so it learns from each run. Store per-company extraction hints that are passed back to Gemini on future crawls, and build a snapshot fixture test suite so we can validate changes against a broad corpus of real pages.

These two prongs are complementary. Prong 1 maximises quality for identifiable companies; Prong 2 ensures coverage for everyone else and gets better over time.

---

## Prong 1: ATS Identification + Native Extractors

### Current State

As of 2026-03-17 (after identification sprint 2 — autechjobs + domain-slug probing):

| Metric | Count |
|:---|---:|
| Total companies in manifest | 1,371 |
| Companies with any ATS identified | **579 (42%)** |
| Companies with **free-API** ATS (Greenhouse/Lever/Ashby) | **240 (17.5%)** |
| Companies with SR or TT identified (implementable, not yet active) | 250 (18.2%) |
| Companies with paid/custom ATS (Workday, Workable, etc.) | 89 (6.5%) |
| Companies with no ATS identified | 792 (57.8%) |

Free-API provider breakdown: Lever ×104, Greenhouse ×92, Ashby ×44.
SR/TT breakdown: SmartRecruiters ×151, TeamTailor ×99.

**Identification tools built:**
- `scripts/ats/extract_ats_from_apply_urls.py` — Method B: extract slugs from BigQuery `raw_jobs.applyUrls`
- `scripts/ats/probe_free_ats.py` — Method A: concurrent slug probing against GH/LV/AS APIs
- `scripts/ats/scan_careers_pages.py` — Method C: static HTML scan of seed URLs + robots.txt check
- `scripts/ats/validate_ats_configs.py` — validates all ATS configs against live APIs
- `scripts/ats/probe_vanity_domains.py` — Method D: domain-slug probing across GH/LV/AS/SR/TT (extended March 2026)
- `scripts/ats/parse_autechjobs.py` — cross-references autechjobs SQL companies against manifests; extracts ATS slugs from 161k historical job URLs; supports `--apply-to-manifests`, `--inject-seeds`, `--export-jsonl`
- `scripts/ats/create_autechjobs_manifests.py` — creates manifest stubs for autechjobs SQL companies not yet in our dataset

**Backend extractors built (Sprint 1):**
- `LeverClient.kt` + `LeverNormalizer.kt` — full Lever Job Board API integration
- `AshbyClient.kt` + `AshbyNormalizer.kt` — full Ashby posting-api integration
- Both wired into `AtsClientFactory`, `AtsNormalizerFactory`, and `application.yml`
- ATS configs seeded from manifests via existing `CompanySyncService.syncFromManifest()` → `POST /api/admin/pipeline/sync-companies`

**Scope:** Greenhouse, Lever, and Ashby are the only providers currently supported by the pipeline's native extractors. SmartRecruiters and TeamTailor both have public APIs (see §1.5) and are the obvious next implementation targets.

---

### 1.1 Identifying Free-API ATS Companies (Fastest Path)

There are three identification methods, ordered from fastest to slowest:

#### Method A: Slug probing (fastest — no browser, ~minutes for all 1261 companies)

Greenhouse, Lever, and Ashby all have predictable public API URLs:
- `https://boards-api.greenhouse.io/v1/boards/{slug}/jobs`
- `https://api.lever.co/v0/postings/{slug}?limit=1`
- `https://api.ashbyhq.com/posting-api/job-board/{slug}`

A 200 response with at least one job = confirmed match. A 404 = not that provider/slug. No authentication, no browser.

**The approach:** For each company without an ATS config, generate a list of slug candidates derived from the manifest, then probe all three APIs concurrently:

Slug candidates to try (in order):
1. `company.id` (manifest ID, e.g. `datacom`)
2. `company.id` with hyphens removed (e.g. `datacom`)
3. `company.name` lowercased, hyphenated (e.g. `trade-me`)
4. `company.name` lowercased, no spaces (e.g. `trademe`)
5. Each entry in `company.alternateNames` slugified

This covers the majority of real-world cases — most companies use a variation of their name as the ATS slug.

**Script to build:** `scripts/ats/probe_free_ats.py`

```python
# Pseudocode — see full implementation below
for company in manifest_companies_without_ats:
    candidates = generate_slug_candidates(company)
    for slug in candidates:
        results = probe_all_three_apis(slug)  # async, concurrent
        if results.greenhouse:
            candidates_found.append((company, 'GREENHOUSE', slug, results.greenhouse.job_count))
        if results.lever:
            candidates_found.append((company, 'LEVER', slug, results.lever.job_count))
        if results.ashby:
            candidates_found.append((company, 'ASHBY', slug, results.ashby.job_count))
```

Output: a CSV/JSON report of candidates found, for manual review before writing to manifests.

**Estimate:** 1100 companies × 5 slug variants × 3 APIs = ~16,500 requests. At 50 concurrent requests, this completes in under 5 minutes.

#### Method B: Apply URL extraction from BigQuery (batch, reliable, covers historical job data)

Our `raw_jobs` table in BigQuery contains `applyUrl` values from past crawls and Apify scrapes. These URLs often contain the ATS domain and identifier directly:
- `https://boards.greenhouse.io/xero/jobs/12345` → Greenhouse, slug = `xero`
- `https://jobs.lever.co/myob-2/abc-def` → Lever, slug = `myob-2`
- `https://jobs.ashbyhq.com/alan/abc-def` → Ashby, slug = `alan`

**BigQuery query to run:**

```sql
SELECT
  companyId,
  REGEXP_EXTRACT(applyUrl, r'boards\.greenhouse\.io/([^/?#]+)')  AS greenhouse_slug,
  REGEXP_EXTRACT(applyUrl, r'jobs\.lever\.co/([^/?#]+)')         AS lever_slug,
  REGEXP_EXTRACT(applyUrl, r'jobs\.ashbyhq\.com/([^/?#]+)')      AS ashby_slug
FROM `{dataset}.raw_jobs`
WHERE applyUrl IS NOT NULL
  AND (
    applyUrl LIKE '%greenhouse.io%' OR
    applyUrl LIKE '%lever.co%' OR
    applyUrl LIKE '%ashbyhq.com%'
  )
GROUP BY 1, 2, 3, 4
ORDER BY companyId
```

This gives us high-confidence slug extractions for companies where we've already crawled jobs with apply URLs. Cross-reference results with company manifests to fill gaps.

**Limitation:** Only works for companies where we've previously seen an apply URL. Companies that use LinkedIn Easy Apply exclusively won't appear here.

#### Method C: Seed URL scanning (thorough, browser-based, ~hours)

For companies with seed URLs that Methods A and B miss, run a scanner against their seed URLs:
1. Fetch seed URL with Playwright (handles SPAs)
2. Check effective URL after redirects — many careers pages redirect directly to `jobs.lever.co/{slug}`
3. Scan rendered HTML for ATS domain markers
4. Check `<script src>` and `<iframe src>` attributes for ATS embeds
5. (Optionally) intercept network requests from the page — some ATS embeds make API calls to their provider that reveal the slug

This is slower but catches embedded ATS widgets that don't appear in the page URL.

**Script to update:** `scripts/ats/discover_ats.py` — currently reads from a CSV; update to read directly from manifest files.

---

### 1.2 Recommended Execution Order

Run these in sequence, stopping when you have enough coverage:

1. **Run Method B** (BigQuery query) — takes 2 minutes, produces high-confidence results for any company we've seen apply URLs from. Output to CSV.
2. **Run Method A** (slug probing) — takes ~5 minutes, covers the long tail. Run against all companies that Method B didn't identify.
3. **Manual review** — review both outputs together; discard false positives (e.g. a slug that returns jobs but doesn't match the right company).
4. **Write confirmed results to manifests** — use a script or do it manually for small batches.
5. **Run `validate_ats_configs.py`** — validates all confirmed entries against live APIs. Also validates the 49 already in the manifest to catch any that have gone stale.
6. **Run Method C** only for high-priority companies that Methods A and B missed (e.g. known large companies that you'd expect to use one of these three providers).

---

### 1.3 Validation: `validate_ats_configs.py`

The existing script already handles validation for Greenhouse/Lever/Ashby. It should be run:
- After any new ATS configs are added to manifests
- On a regular schedule (weekly) to catch providers that have changed their slug or taken down their board

**What it checks:**
- Provider is in the allowed list
- Identifier matches the expected format
- Live API call returns HTTP 200 with at least one job

**What it should also check (enhancements needed):**
- Job count sanity: if API returns 0 jobs, flag as `WARNING` (board may be real but empty, or inactive)
- Compare identifier in manifest against what the API returns (some providers return the slug in the response body)

Run it as:
```bash
python3 scripts/ats/validate_ats_configs.py
# or filter to a single provider:
python3 scripts/ats/validate_ats_configs.py --provider LEVER
```

### 1.2 Native Extractors for Free-API Providers

For companies with confirmed public ATS APIs, we should bypass the crawler entirely and fetch structured data directly. The three primary targets are:

#### Greenhouse

- **API:** `GET https://boards-api.greenhouse.io/v1/boards/{token}/jobs?content=true`
- **Fields available:** title, location, department, content (HTML description), absolute_url, updated_at
- **Limitation:** Does not include salary data. Does not include remote/work model.
- **Implementation:** Already partially exists as `GreenhouseClient.kt`. Needs to be wired into the daily batch pipeline.

#### Lever

- **API:** `GET https://api.lever.co/v0/postings/{slug}?mode=json`
- **Fields available:** text (title), categories (team, location, commitment, workplaceType), description, descriptionPlain, hostedUrl, createdAt, salaryRange
- **Notes:** `commitment` maps to employment type; `workplaceType` maps to work model. Salary range is sometimes present.
- **Implementation:** New `LeverClient.kt` needed. Model similar to `GreenhouseClient`.

#### Ashby

- **API:** `GET https://api.ashbyhq.com/posting-api/job-board/{slug}?includeCompensation=true`
- **Fields available:** title, team, location, employment_type, compensation (salary), job_posting_url, published_at
- **Notes:** Best-structured API of the three. Salary almost always present if company provides it.
- **Implementation:** New `AshbyClient.kt` needed. Consider this the reference implementation.

**Shared extractor contract:**

All ATS clients should implement a common interface:

```kotlin
interface AtsClient {
    val provider: AtsProvider
    fun fetchJobs(identifier: String): List<NormalizedJob>
}
```

The resulting `NormalizedJob` list should be indistinguishable from crawler-produced jobs — same validation, same pipeline.

### 1.3 Wiring ATS Clients into the Pipeline

ATS clients should be invoked from the daily batch alongside the crawler. The routing logic:

```
For each company in daily batch:
  if company.ats != null and provider in [GREENHOUSE, LEVER, ASHBY]:
    → use ATS client (skip crawler)
  else if company has seeds:
    → use crawler
  else:
    → skip (log as no_source)
```

**Priority override:** If the ATS client returns 0 jobs (e.g., API down, token invalid), fall back to the crawler for that company and log a `[ATS_FALLBACK]` warning. Do not silently skip.

### 1.4 Full ATS Provider Inventory

All providers found across our 1,371 manifests, grouped by implementability:

#### Tier 1 — Built and active (GH/LV/AS)

| Provider | Companies | Status |
|----------|-----------|--------|
| GREENHOUSE | 94 | ✅ `GreenhouseClient.kt` + normalizer — live |
| LEVER | 105 | ✅ `LeverClient.kt` + normalizer — live |
| ASHBY | 44 | ✅ `AshbyClient.kt` + normalizer — live |
| **Total** | **243** | Ready to activate (`sync-companies` + `ats-sync-all`) |

#### Tier 2 — Public API confirmed, not yet built (SR/TT/WL)

| Provider | Companies | API endpoint | Notes |
|----------|-----------|-------------|-------|
| SMARTRECRUITERS | 157 | `api.smartrecruiters.com/v1/companies/{slug}/postings` | See §1.5 for false-positive gotcha |
| TEAMTAILOR | 99 | `{slug}.teamtailor.com/feed/jobs.json` | Public JSON array; no auth |
| WORKABLE | 28 | `POST apply.workable.com/api/v3/accounts/{slug}/jobs` | Returns `{total, results[]}` |
| **Total** | **284** | ~527 companies reachable once all three built |

#### Tier 3 — Public API exists, low company count or data quality issues

| Provider | Companies | API endpoint | Blocker |
|----------|-----------|-------------|---------|
| PERSONIO | 5 | `{slug}.jobs.personio.com/api/v0/jobs` | **All 5 identifiers are wrong** — set to `jobs` from bad slug extraction. Real slug is the company subdomain (e.g. `billigence`, `fadata`). Needs manual correction first. |
| BREEZY | 1 | `{slug}.breezy.hr/json` | Returns clean JSON list with salary. Only 1 company (`stake`), low priority. |

#### Tier 4 — No public API (enterprise auth or proprietary)

| Provider | Companies | Reason |
|----------|-----------|--------|
| WORKDAY | 50 | Enterprise auth. Continue crawling via seed URLs. |
| SUCCESSFACTORS | 8 | SAP enterprise auth. |
| JOBADDER | 6 | AU recruitment SaaS. Identifiers are region codes (`au1`, `au3`) not slugs — bespoke per-client setup required. |
| BAMBOOHR | 6 | Only public endpoint is an HTML embed widget (`/jobs/embed2.php`), no JSON API. |
| SNAPHIRE | 4 | AU university ATS. No public API. 2 of 4 identifiers wrong (`www`). |
| FACTORIAL | 5 | HR/payroll platform. 2 of 5 identifiers wrong (`assets`, `apidoc`). |
| COMEET | 2 | Could not find a public endpoint. |
| JOIN | 4 | European job platform. Unverified. |
| PAGEUP | 2 | Enterprise ANZ ATS. |
| ORACLE_TALEO | 1 | Enterprise Oracle. |
| RIPPLING | 1 | API returns non-JSON. |
| INRECRUITING | 1 | Obscure EU ATS. |
| MYRECRUITMENTPLUS | 1 | Small AU ATS. |
| ZOHO | 1 | Complex auth. |

**Summary: 5 providers with public APIs covering 528 companies. 243 already built; 285 more buildable.**

---

### 1.5 Next-Tier Free APIs: SmartRecruiters, TeamTailor, Workable

#### SmartRecruiters (157 companies)

- **Board existence check:** `GET https://jobs.smartrecruiters.com/{slug}` — real boards redirect to `careers.smartrecruiters.com/{slug}`; fake slugs redirect to `jobs.smartrecruiters.com/` (root). **Do not use the postings API as an existence check** — it returns HTTP 200 for any slug, including made-up ones.
- **Job listing:** `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings?status=PUBLISHED&limit=100` — returns `{"totalFound": N, "content": [{id, name, location, department, typeOfEmployment, …}]}`. Unauthenticated.
- **Full job detail:** `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings/{jobId}` — adds `jobAd.sections` (description HTML).
- **Slug casing:** SR slugs are case-sensitive. Canonical casing is found in the redirect URL (`careers.smartrecruiters.com/{slug}`), which `probe_vanity_domains.py` captures automatically.
- **Implementation effort:** Medium — `SmartRecruitersClient.kt` + normalizer + pagination (uses `offset` param).

#### TeamTailor (99 companies)

- **Board verification:** `GET https://{slug}.teamtailor.com` — 404 for non-existent; real boards return 200 with `"teamtailor.com"` in body.
- **Jobs feed:** `GET https://{slug}.teamtailor.com/feed/jobs.json` — returns a JSON array. Fields: `id`, `title`, `apply-url`, `locations` (array), `department` (string), `tags`. No auth.
- **Implementation effort:** Low — simplest data model of the tier-2 providers.

#### Workable (28 companies)

- **Job listing:** `POST https://apply.workable.com/api/v3/accounts/{slug}/jobs` with body `{"query":"","location":[],"department":[],"worktype":[],"remote":[]}` — returns `{"total": N, "results": [{shortcode, title, remote, location, locations, type, department, …}]}`. No auth.
- **Job detail:** `GET https://apply.workable.com/{slug}/j/{shortcode}` — HTML page (no JSON detail endpoint found). Description requires crawling or accepting summary only.
- **Data quality note:** 3 identifiers were bad (`j`) and corrected to real slugs (`theta`, `mathspace`, `ct`) on 2026-03-17.
- **Implementation effort:** Medium — POST body, and description requires either skipping or crawling the job page.

#### Recommendation

Implement TT first (lowest effort), then SR (most companies), then Workable. Together they add 284 more companies, bringing total pipeline coverage from 243 → **527 companies**.

---

### 1.6 Test Coverage for Existing Integrations

All three tier-1 clients have unit tests using Spring's `MockRestServiceServer` and plain unit tests for normalizers. Tests pass CI.

#### What is covered

| File | Tests | What they cover |
|------|-------|----------------|
| `GreenhouseClientTest` | 2 | HTTP wiring: correct URL constructed, exception on non-200 |
| `GreenhouseNormalizerTest` | 6 | Standard job, office location priority, `first_published_at`, empty/missing array, skip-no-id |
| `LeverClientTest` | 2 | HTTP wiring: URL + `?mode=json`, exception on 404 |
| `LeverNormalizerTest` | 7 | Standard job, salary fields, team→department fallback, hostedUrl fallback, wrapped `{postings:[]}` shape, skip-no-id, empty array |
| `AshbyClientTest` | 3 | HTTP wiring, slug URL-encoding (spaces → `%20`), exception on 500 |
| `AshbyNormalizerTest` | 9 | Standard job, `isRemote→workModel`, multi-location join, employment type mapping, salary extraction, `descriptionPlain` preference, empty/missing array, skip-no-id |
| `AtsClientFactoryTest` | 5 | Factory returns correct client per provider, throws on unsupported |
| `AtsNormalizerFactoryTest` | 4 | Factory returns correct normalizer per provider, throws on unsupported |
| **Total** | **38** | |

#### Known gaps

- **Client tests use trivial mock responses** (`{"jobs":[]}`, `[{"id":"abc","text":"Engineer"}]`) — they confirm the URL is constructed correctly but don't test that the client handles real API response shapes. If the API added a required wrapper, the client test would still pass.
- **No live/integration tests** — no test actually calls the real Greenhouse/Lever/Ashby APIs. The normalizer tests are the closest thing, using representative hand-crafted JSON.
- **No contract tests** — if an API changes its response schema (e.g. Lever renames `hostedUrl` → `boardUrl`), nothing will catch it until the pipeline runs and produces empty normalised output.
- **Missing edge cases:** multi-department Greenhouse jobs, Lever `workplaceType` field, Ashby compensation with multiple `summaryComponents`.

#### Recommendation

The normalizer tests are the highest-value tests and are well-covered. The client tests are thin by design (they're just HTTP wiring). The real risk is API drift — consider adding one snapshot test per provider that replays a real API response captured today and asserts on the normalized output. This is the "snapshot fixture" approach described in §2.3.

---

## Prong 2: Self-Improving LLM Crawler

### The Core Problem

The generic crawler works, but its quality degrades for pages that:
- Use non-standard pagination (infinite scroll, client-side routing, custom "Load more" buttons)
- Have misleading or sparse DOM structures (job title in a `<div>` with no semantic markup)
- Trigger bot detection at higher volumes or without proper delays
- Return empty results on the first listing page but have 100+ jobs on subsequent pages

The crawler has no memory between runs. Each crawl starts cold, makes the same mistakes, and fails in the same ways.

### 2.1 Per-Company Extraction Hints

**Concept:** After each successful crawl run, store a small JSON hints object per seed URL that captures what worked. On the next crawl of that URL, pass the hints to Gemini as context.

**Schema (stored per `crawler_seeds` row):**

```typescript
interface ExtractionHints {
  // Pagination
  paginationType: 'query_param' | 'path_segment' | 'load_more_button' | 'infinite_scroll' | 'none';
  paginationParam?: string;       // e.g. "page", "start", "offset"
  paginationFirstValue?: number;  // e.g. 0 or 1
  maxPagesObserved?: number;

  // Job structure
  jobContainerSelector?: string;  // CSS selector for job list items if LLM struggled
  titleHint?: string;             // e.g. "titles appear inside <h2> with class 'job-title'"
  locationHint?: string;          // e.g. "location is always the first <span> after the title"

  // Known good patterns
  lastSuccessfulJobCount?: number;
  lastSuccessfulTitles?: string[];  // Sample of 3-5 titles from last good run

  // Crawl behaviour
  requiresJavaScript: boolean;
  interactionRequired?: string;    // e.g. "must click 'Load more' button to see full list"
  userAgentSensitive?: boolean;    // true if we've seen bot detection on this URL
  delayRequiredMs?: number;        // observed required inter-request delay

  // ATS overlay
  atsEmbedDetected?: string;       // e.g. "greenhouse", "lever" — detected in page source
}
```

**How hints are generated:**

After a successful crawl (jobCount > 0), the CrawlerService writes updated hints back to the seed record. The hints are derived from:

1. The pagination strategy that was used on the run
2. The Gemini response metadata (if Gemini returns confidence or notes)
3. Simple heuristics: if `pagesVisited == 1 && jobCount > 20`, record `paginationType: 'none'`; if `pagesVisited > 1`, record the pagination param used

**How hints are consumed:**

In `ContentExtractor.ts`, the Gemini system prompt is constructed dynamically. If hints are provided:

```typescript
function buildExtractionPrompt(hints?: ExtractionHints): string {
  let prompt = BASE_EXTRACTION_PROMPT;

  if (hints?.lastSuccessfulTitles?.length) {
    prompt += `\n\nPrevious successful extraction from this page found titles like: ${hints.lastSuccessfulTitles.join(', ')}. Use this to calibrate your extraction.`;
  }

  if (hints?.titleHint) {
    prompt += `\n\nTitle hint: ${hints.titleHint}`;
  }

  if (hints?.jobContainerSelector) {
    prompt += `\n\nJob listings are contained within elements matching: ${hints.jobContainerSelector}`;
  }

  return prompt;
}
```

**BigQuery schema change needed:**

Add a `hints` column (`JSON` type) to `crawler_seeds` table. The `CrawlerSeedRepository` needs a `updateHints(companyId, url, hints)` method.

### 2.2 Pagination Learning

The crawler currently guesses pagination by looking for "next page" links and common query params. This fails for:
- Sites that use `offset` or `start` instead of `page`
- Sites with JavaScript-only pagination where no link exists in the DOM

**Improved approach:**

After a crawl, record the pagination strategy that worked in `ExtractionHints.paginationType`. On subsequent runs:

1. If `paginationType == 'query_param'` and `paginationParam` is known: generate pages directly without needing to discover them
2. If `paginationType == 'load_more_button'`: tell Playwright to look for and click the button rather than looking for pagination links
3. If `paginationType == 'infinite_scroll'`: tell Playwright to scroll-to-bottom between extractions

This is the most impactful change for pages like Datacom that use non-standard pagination.

### 2.3 Snapshot Fixture Test Suite

**Goal:** Build a large, stable corpus of real page snapshots so we can run the extractor against them locally without needing to hit the live internet, and catch regressions when we change `ContentExtractor` or `JobValidator`.

**Why it works for SPAs:**

The key insight is that we store the **post-render HTML** (what Playwright sees after JavaScript execution), not the raw HTTP response. This means SPA pages are captured faithfully — the DOM state after the JS framework has rendered its content is what we snapshot.

**Snapshot structure:**

```
crawler-service/
  fixtures/
    snapshots/
      {companyId}/
        {seedSlug}/           # URL-derived slug, e.g. "careers-datacom-nz"
          page-1.html         # Simplified HTML from ContentExtractor.extractSimplifiedHtml()
          page-2.html
          expected-jobs.json  # Canonical expected output (manually reviewed)
          meta.json           # Snapshot metadata
```

**`meta.json` schema:**

```json
{
  "companyId": "datacom",
  "seedUrl": "https://datacom.com/nz/en/careers/search",
  "capturedAt": "2026-03-15T10:00:00Z",
  "crawlerVersion": "1.4.0",
  "pageCount": 3,
  "expectedJobCount": 12,
  "notes": "Datacom uses a custom pagination button. Jobs have AUCKLAND in all-caps."
}
```

**`expected-jobs.json` schema:**

This is a subset of the full `NormalizedJob` schema — only the fields we can reliably extract from the snapshot. Fields like `postedAt` that change over time are omitted or stored as patterns:

```json
{
  "minCount": 10,
  "maxCount": 20,
  "jobs": [
    {
      "title": "Mobile Software Engineer",
      "companyName": "Datacom",
      "location": "Auckland, New Zealand",
      "employmentType": null,
      "seniorityLevel": null
    }
  ],
  "assertions": [
    { "field": "title", "contains": "Engineer" },
    { "field": "location", "notNull": true }
  ]
}
```

**Snapshot capture tooling:**

Add a CLI command to the crawler service:

```bash
npx ts-node scripts/capture-snapshot.ts \
  --companyId datacom \
  --url "https://datacom.com/nz/en/careers/search" \
  --pages 3
```

This runs the real crawler against the URL, saves the simplified HTML output per page, and creates a skeleton `expected-jobs.json` that you then review and fill in.

**Test runner (`fixtures.test.ts`):**

```typescript
describe('Snapshot fixtures', () => {
  const fixtures = glob.sync('fixtures/snapshots/**/*.html');

  for (const htmlFile of fixtures) {
    const dir = path.dirname(htmlFile);
    const meta = JSON.parse(fs.readFileSync(`${dir}/meta.json`, 'utf8'));
    const expected = JSON.parse(fs.readFileSync(`${dir}/expected-jobs.json`, 'utf8'));
    const html = fs.readFileSync(htmlFile, 'utf8');

    it(`extracts jobs from ${meta.companyId}/${path.basename(htmlFile)}`, async () => {
      const jobs = await extractJobsFromHtml(html, meta);
      expect(jobs.length).toBeGreaterThanOrEqual(expected.minCount);
      expect(jobs.length).toBeLessThanOrEqual(expected.maxCount);

      for (const assertion of expected.assertions) {
        for (const job of jobs) {
          if (assertion.notNull) expect(job[assertion.field]).not.toBeNull();
          if (assertion.contains) expect(job[assertion.field]).toContain(assertion.contains);
        }
      }
    });
  }
});
```

**Which pages to snapshot first (priority order):**

| Company | Why |
|:---|:---|
| Datacom | Recently fixed; good baseline for listing-page jobs |
| Alan (Ashby) | Rate-limit victim; important to have test coverage for Ashby pages |
| Xero | High job count company; important smoke test |
| Trade Me | Complex pagination |
| Westpac | Banking sector representative |
| Weta FX | SuccessFactors embed |

Target: 20 snapshots in the first sprint, covering a variety of ATS types and pagination patterns.

### 2.4 Zero-Yield Detection and Alerting

When a crawl returns 0 jobs from a company that previously returned > 0, something has gone wrong. We already log `[ZERO_YIELD_WARNING]` in the crawler service; the next step is to surface this more prominently.

**Changes needed:**

1. Store `lastSuccessfulJobCount` and `lastCrawledAt` in `crawler_seeds` (or per-company in the manifest)
2. In the backend daily batch response handler, compare current job count to `lastSuccessfulJobCount`
3. If `currentCount == 0 && lastSuccessfulJobCount > 5`, emit a `[REGRESSION_WARNING]` log and include the company in a "needs attention" report
4. (Future) Send a Slack or email alert for regressions exceeding a threshold

### 2.5 Adaptive Crawl Quality Scoring

Currently, once a crawl passes `JobValidator`, we have no way to tell a high-quality run (30 jobs with descriptions, locations, seniority) from a marginal run (5 jobs with titles only, confidence = 0.51).

**Proposal:** After each run, compute a `extractionQuality` score per company:

```
quality = (jobCount / expectedJobCount) * 0.5
        + (avgConfidence) * 0.3
        + (descriptionCoverageRate) * 0.1
        + (locationCoverageRate) * 0.1
```

Where `expectedJobCount` comes from `ExtractionHints.lastSuccessfulJobCount`.

Store this per run in `crawl_runs`. Surface in the admin panel's company detail view. Use as a signal to prioritise which companies' snapshots to capture first and which seeds to investigate.

---

## Implementation Sequence

### Phase 0: ATS identification ✅ COMPLETE (sprint 1)

- [x] **ID-1** Run Method B (BigQuery apply URL extraction) — `extract_ats_from_apply_urls.py` → 44 new configs written
- [x] **ID-2** Build and run Method A `probe_free_ats.py` — 55 new configs written (34 GH, 13 LV, 10 AS)
- [x] **ID-3** Manual review and false positive cleanup (Riot Games `embed`, Heidi wrong slug, Xero inactive board)
- [x] **ID-4** ATS configs written to manifest JSON files — 153 total free-API companies
- [x] **ID-5** Run `validate_ats_configs.py` — 260 configs checked, 0 errors
- [x] **ID-6** Run Method C `scan_careers_pages.py` against 237 seed companies — 9 non-free ATSs detected, robots.txt written where applicable

### Phase 0 (continued): autechjobs SQL ingestion + domain-slug expansion ✅ COMPLETE (sprint 2, 2026-03-17)

**Source:** `data/third-party/autechjobs/Cloud_SQL_Export_2026-03-17.sql` — MySQL dump from a friend's AU tech job board (autechjobs.com.au). 149 companies, 161,239 jobs, 2010–2026. See `docs/data/autechjobs-historical-data.md` for full analysis.

- [x] **ID-7** Parsed SQL dump with `parse_autechjobs.py`: cross-referenced 149 SQL companies against existing manifests; extracted ATS slugs from historical job URLs (Lever ×14,715, SmartRecruiters ×10,461, Greenhouse ×5,348, Ashby ×197). Applied confirmed slugs to matched manifests.
- [x] **ID-8** Fixed false positive bugs: removed `"ai"` from suffix-strip list (prevented `openai → open` mismatch); fixed `macquarietechnologygroup` slug landing on `macquarie-group.json` (investment bank, not tech group).
- [x] **ID-9** Created 104 new manifest stubs with `create_autechjobs_manifests.py` for unmatched SQL companies — bringing total from 1,267 → 1,371. Includes per-company OVERRIDES for acquisitions (Afterpay, Auth0, Clipchamp, A Cloud Guru), HQ corrections, and agency flags.
- [x] **ID-10** Extended `probe_vanity_domains.py` with SmartRecruiters and TeamTailor probes. Fixed critical SR false-positive: the postings API returns HTTP 200 for any slug — correct check is the board redirect (`jobs.smartrecruiters.com/{slug}` → `careers.smartrecruiters.com/{slug}` for real boards, root for fake). Ran against all 1,012 companies without ATS: **142 SR hits applied, 114 TT hits applied** (26 TT skipped as already had SR).
- [x] **ID-11** Gemini web search (site:/inurl: operators on ATS domains): 14 companies researched, 13/14 already covered, confirmed 1 WSP update (TT → WORKDAY). Good validation signal; Gemini most useful for Workday companies that probing can't discover.

**Net result:** 579 manifests now have ATS configs (42%), up from 260 (20.5%). Free-API companies ready for the pipeline: **240** (up from 153).

**Ongoing:** Gemini continuing web search for remaining ~800 companies without ATS. Priority: large AU tech companies likely to use Workday or SR with non-obvious slugs.

### Sprint 1 ✅ COMPLETE

- [x] **P1.1** `GreenhouseClient` already existed — wired into factory and pipeline
- [x] **P1.2** `LeverClient.kt` + `LeverNormalizer.kt` built and tested (8 tests)
- [x] **P1.3** `AshbyClient.kt` + `AshbyNormalizer.kt` built and tested (9 tests)
- [x] **P1.4** `AtsClientFactory` and `AtsNormalizerFactory` updated with factory tests added
- [x] **P1.5** `CompanySyncService` ATS sync coverage added (5 new tests)
- [x] **P1.6** `application.yml` extended with `lever.base-url` and `ashby.base-url`
- [x] **P1.7** Trigger sync: `POST /api/admin/pipeline/sync-companies` seeds all 153 configs to BigQuery `ats_configs` table

**To activate:** call `POST /api/admin/pipeline/sync-companies`, then `POST /api/internal/ats-sync-all`.

### Sprint 2 (next)

**Activate pipeline first:**
- [ ] `POST /api/admin/pipeline/sync-companies` — seeds all 240 free-API configs to BigQuery `ats_configs` table
- [ ] `POST /api/internal/ats-sync-all` — triggers first GH/LV/AS extraction run

**Extend extractors to SR + TT + Workable (§1.5):**
- [ ] **P1.8** `TeamTailorClient.kt` + normalizer — fetch from `{slug}.teamtailor.com/feed/jobs.json`. Low effort. Unlocks 99 companies.
- [ ] **P1.9** `SmartRecruitersClient.kt` + normalizer — fetch from `api.smartrecruiters.com/v1/companies/{slug}/postings` (with pagination via `offset`). Medium effort. Unlocks 157 companies.
- [ ] **P1.10** `WorkableClient.kt` + normalizer — `POST apply.workable.com/api/v3/accounts/{slug}/jobs`. Medium effort. Note: no JSON detail endpoint; description will require crawl fallback or be omitted. Unlocks 28 companies.
- [ ] Wire all three into `AtsClientFactory` and `AtsNormalizerFactory`. Together brings coverage from 243 → **527 companies**.
- [ ] **P1.11** Fix 5 bad PERSONIO identifiers manually (all set to `jobs` — should be company subdomain). Then `PersonioClient.kt` if 5 companies is worth the effort.

**Self-improving crawler:**
- [ ] **P2.5** Implement `ExtractionHints` schema and `updateHints()` in `CrawlerSeedRepository`
- [ ] **P2.6** Pass hints to Gemini prompt in `ContentExtractor.ts`
- [ ] **P2.7** Capture 10 more snapshots (trade-me, westpac, weta, etc.)
- [ ] **P2.8** Add `lastSuccessfulJobCount` tracking and zero-yield regression warning in admin batch handler

**Historical data (autechjobs):**
- [ ] **ID-12** Export SQL dump to JSONL: `python3 scripts/ats/parse_autechjobs.py --export-jsonl --output data/third-party/autechjobs/historical_jobs.jsonl`
- [ ] **ID-13** Load to BigQuery: `bq load --source_format=NEWLINE_DELIMITED_JSON techmarket.autechjobs_historical historical_jobs.jsonl`
- [ ] **ID-14** Derive monthly `tech_job_counts` metrics back to 2020 for trend backfill (5+ years of data)

### Sprint 3

- [ ] **P2.9** Implement pagination learning from hints (`load_more_button`, `infinite_scroll` strategies)
- [ ] **P2.10** Surface extraction quality score in admin panel
- [ ] **ID-15** Re-run `probe_free_ats.py` against remaining unidentified companies using `website` field to generate additional slug candidates
- [ ] **ID-16** Build `probe_search_api.py` — automated Brave Search API queries (`site:boards.greenhouse.io {company}` etc.) to identify ATS for remaining ~800 companies without one. ~$9 for 3,000 queries at current Brave pricing.

---

## Success Metrics

| Metric | Baseline | Phase 0 sprint 1 | Phase 0 sprint 2 (now) | Target (Sprint 3) |
|:---|:---|:---|:---|:---|
| Total companies in manifest | 1,267 | 1,267 | **1,371** ✅ | 1,400+ |
| Companies with any ATS identified | 49 | 260 (20.5%) | **579 (42%)** ✅ | 650+ |
| Companies with free-API ATS (GH/LV/AS) | 49 | **153** ✅ | **240** ✅ | 300+ |
| Companies with SR/TT identified | 0 | 107 | **250** ✅ | 250 (stable) |
| Companies using native ATS clients in pipeline | 0 | **153 (ready to activate)** | **240 (ready)** | 490+ (after SR+TT) |
| Free-API ATS configs validated as live | Unknown | 260 checked, 0 errors | Not re-run | Run after activation |
| Snapshot fixture count | 0 | 0 | 0 | 30+ |
| Companies with extraction hints stored | 0 | 0 | 0 | All crawled |
| Zero-yield regressions caught | Unknown | Unknown | Unknown | Tracked per run |
| Historical job records (autechjobs) | 0 | 0 | **161,239 (staged)** | Loaded to BigQuery |

---

## Open Questions

1. **Snapshot staleness**: How often do we need to re-capture snapshots? Companies redesign their careers pages. Recommendation: recapture when a crawl regresses (zero-yield or quality drop), not on a schedule.

2. **Hints vs. ATS identification overlap**: If we detect an ATS embed via hints (e.g., `atsEmbedDetected: 'greenhouse'`), should we automatically update the company manifest? Yes, but with a confidence flag — treat as a candidate identification, not a confirmed one, until validated via the API.

3. **Cost of per-page snapshot storage**: `ContentExtractor.extractSimplifiedHtml()` strips most of the DOM already. A typical simplified HTML file is 20–80KB. 30 fixtures × 3 pages each = ~10MB. Negligible — store in Git.

4. **Gemini prompt token budget**: Passing `ExtractionHints` to Gemini adds ~200-400 tokens per request. At our current scale this is inconsequential, but worth monitoring.
