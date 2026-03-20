# Seed Discovery Pipeline

How we find and verify career page URLs (`crawler.seeds`) for companies that
don't have a supported ATS API integration.

**Last updated:** March 2026
**Relates to:** `discovery-pipeline.md`, `uncrawled-coverage-strategies.md`, `adr-008-crawler-service.md`

---

## Context

The AI crawler (ADR-008) needs a starting URL for each company — the page that
lists all open roles. Without one, it can't crawl. This pipeline discovers and
maintains those seed URLs.

**Coverage as of March 2026** (all pipeline stages complete, Gemini run ×2):

| State | Companies | % |
|-------|-----------|---|
| ✅ Supported ATS — direct API | 428 | 31% |
| 🟢 Active crawlable seed | 319 | 23% |
| 🟡 Seed found, not yet active (empty/blocked/unknown) | 397 | 29% |
| 🔴 No coverage yet | 227 | 17% |
| **Crawlable total** | **1,144** | **83%** |

Seed status breakdown:
- ⬜ `empty` (376) — page loads, jobs not visible in static HTML; likely SPA
- ✅ `active` (319) — job indicators confirmed in static HTML
- 🔒 `blocked` (84) — 403 or thin response; Playwright may still work
- ❓ `unknown` (18) — timeouts or not yet checked
- 🔗 `linkedin` / 🚫 `excluded` (2) — manually set terminal statuses

Stage 6 (Gemini search grounding) results — two runs total (March 2026):
- Run 1: ~340 companies queried; ~146 seeds written
- Run 2: 260 companies queried; 38 seeds written; 7 timeouts fixed via 60s hard limit
- Combined: ~209 companies now have gemini-search seeds
- Also: 7 seeds manually probed from ATS identifier patterns (Workday/Personio/Comeet)
- Prompt upgraded: expanded ATS list to 15 platforms including Deel, Rippling, Pinpoint
- `google_search_retrieval` deprecated — both APIs now use `google_search` tool

The 227 remaining companies with no coverage are the hardest cases:
- ~95 Spanish SMEs (Gemini returns NO_RESULT consistently)
- ~78 Australian companies (small/niche)
- ~24 NZ companies
- ~30 remainder (US, IN, other)
The AI crawler (ADR-008) is the final fallback — Playwright probes any page.

---

## Discovery methods (ordered by effort vs. ROI)

### Step 1 — Mine apply URLs from BigQuery

**Script:** `scripts/ats/discover_seeds_from_apply_urls.py`
**When to run:** After each data ingestion cycle
**Cost:** Zero — data already exists in BigQuery

Every job we've ever ingested has `applyUrls` stored in BigQuery. Even for
companies whose ATS we don't integrate with, those URLs reveal the underlying
platform. We strip the job-specific ID to derive the career listing page URL.

```
Input:  https://company.wd3.myworkdayjobs.com/External/job/Sydney/Engineer_JOB-12345
Output: https://company.wd3.myworkdayjobs.com/External   (seed)

Input:  https://company.bamboohr.com/careers/456
Output: https://company.bamboohr.com/careers             (seed)

Input:  https://join.com/companies/acme/1234-senior-engineer
Output: https://join.com/companies/acme                  (seed)
```

Providers covered: Workday, BambooHR, SuccessFactors, Recruitee, Breezy,
Join.com, Employment Hero (company subdomains only), Personio, JobAdder, PageUp,
Taleo, iCIMS, Snaphire, Factorial, Zoho, Elmo, Comeet.

Seeds are written with `status: pending` — run `verify_seeds.py` afterwards.

Also flags companies where a *supported* ATS (GH/LV/AS/SR/TT/WL) appears in
apply URLs but is missing from the manifest — these go to the ATS config scripts
(see `discovery-pipeline.md` Stages 1–2).

**Yield (March 2026):** 64 seeds written; 50 confirmed active after verification.

---

### Step 2 — Career page path probing

**Script:** `scripts/ats/probe_career_paths.py`
**When to run:** Monthly, or after new companies are added
**Cost:** ~10–20 min runtime (async, 10 concurrent HTTP requests)

Probes predictable career page paths against each company's `website` field:

```
/careers    /jobs    /about/careers    /work-with-us    /join-us
/join-the-team    /opportunities    /vacancies    /about-us/careers
```

For each path that returns a non-404 response:
1. **Follows redirects** — the final URL often reveals the ATS (e.g. redirects to `company.greenhouse.io`)
2. **Scans HTML** for ATS embed signatures (same patterns as `scan_careers_pages.py`)
3. **Counts job indicators** to determine if the page has visible open roles
4. Writes: ATS config (if supported provider detected), or seed URL with `active`/`empty` status

Falls back to homepage link extraction if no career path matches — fetches `{website}` and scans `<a href>` tags for career-related links.

**Yield (March 2026):** ~100 seeds written across 504 companies probed.

---

### Step 3 — Seed verification

**Script:** `scripts/ats/verify_seeds.py`
**When to run:** After Steps 1, 2, or 6 add seeds; also monthly to refresh stale statuses
**Cost:** ~5–10 min for a full sweep

HTTP-checks each seed and writes a status back to the manifest:

| Status | Meaning | Action |
|--------|---------|--------|
| `active` | ≥3 job indicator hits | Ready for the AI crawler |
| `empty` | Page loads, no jobs visible | Likely SPA — crawler will render it |
| `dead` | 404 / DNS fail / off-domain redirect | Remove or update the URL |
| `blocked` | 403 or thin HTML (<300 bytes) | Anti-bot — crawler may still work |
| `unknown` | Timeout or transient error | Retry next run |

```bash
python3 scripts/ats/verify_seeds.py                      # re-check none/unknown/empty/blocked
python3 scripts/ats/verify_seeds.py --all                # re-check everything
python3 scripts/ats/verify_seeds.py --company-id foo     # single company
python3 scripts/ats/verify_seeds.py --statuses none unknown
```

> **`empty` ≠ no jobs.** Many career pages use SPAs that load jobs via JS after
> the initial HTML response. The AI crawler (Playwright) will render these and
> find jobs that static HTTP verification misses.

---

### Step 4 — Gemini search grounding (on-demand)

**Script:** `scripts/ats/discover_seeds_gemini.py` *(to be built)*
**When to run:** On-demand for high-priority companies that remain unseeded
**Cost:** ~$0.0001 per company (Gemini Flash with search grounding)

For the ~298 companies still unseeded after Steps 1–3, query Gemini with search
grounding enabled:

```
What is the career listings page URL for "{Company Name}"?
Their website is {website}. Return only the URL, nothing else.
```

Gemini fetches live Google results without us rendering anything. All returned
URLs are HTTP-verified by `verify_seeds.py` before being written as seeds.

Designed for: companies with unusual career page paths, recently-rebranded
companies, and companies that consistently resist mechanical probing.

---

### Step 5 — Playwright rendering (last resort / AI crawler)

For companies that remain unseeded after all prior steps, the AI crawler itself
(ADR-008) is the fallback — it uses Playwright to render JS-heavy SPAs and
extract job listings via Gemini Flash. This is the crawl step, not a seed
discovery step.

---

## Seed schema

Seeds live under `crawler.seeds` in each company's manifest JSON:

```json
{
  "crawler": {
    "seeds": [
      {
        "url":      "https://company.wd3.myworkdayjobs.com/External",
        "category": "careers",
        "source":   "apply-url-mining",
        "status":   "active"
      }
    ]
  }
}
```

| Field | Values | Notes |
|-------|--------|-------|
| `url` | Full URL | Career listing page, not a specific job |
| `category` | `careers` | Always `careers` for job listing seeds |
| `source` | `apply-url-mining`, `path-probe`, `homepage-scan`, `gemini-search`, `autechjobs`, `manual` | How the seed was found |
| `status` | `active`, `empty`, `dead`, `blocked`, `unknown`, `pending`, `linkedin`, `excluded` | Set by `verify_seeds.py`; `pending` = not yet checked; `linkedin`/`excluded` = manually set, never re-verified |

---

## Running the pipeline for new companies

Single company (e.g. after adding a new manifest):
```bash
CID=my-new-company
python3 scripts/ats/discover_seeds_from_apply_urls.py --company-id $CID --apply
python3 scripts/ats/probe_career_paths.py --company-id $CID --apply
python3 scripts/ats/verify_seeds.py --company-id $CID
```

Full sweep (e.g. after a data ingestion cycle):
```bash
python3 scripts/ats/discover_seeds_from_apply_urls.py --apply   # Step 1
python3 scripts/ats/probe_career_paths.py --apply               # Step 2
python3 scripts/ats/verify_seeds.py                             # Step 3
```

See `discovery-pipeline.md` for the broader pipeline that includes ATS config
discovery (Stages 0–2) alongside seed discovery (Stages 3–5).

---

## Known limitations and watch-outs

**Employment Hero (shared platform):** `jobs.employmenthero.com` is the generic
shared board — it's not company-specific. The apply URL mining script skips this
and only emits seeds when the company has its own subdomain (e.g.
`acme.employmenthero.com`).

**TeamTailor feed deprecated:** `/feed/jobs.json` returns 404 as of 2026. The
`probe_vanity_domains.py` script was patched to fall back to HTML marker
detection. When exact TT slugs are known from apply URLs (they often include
numeric suffixes like `company-1677622320`), use those directly rather than
relying on domain-derived slug probing.

**Workday `myworkdayjobs.com` domains:** The path probing step often returns
`blocked` because Workday's React frontend requires JS rendering. Use
`discover_seeds_from_apply_urls.py` (which extracts the exact tenant + board
path from historical apply URLs) to get reliable Workday seeds.

**robots.txt and rate limiting:** The path probe respects `allow_redirects` and
avoids hammering domains. For large enterprise sites, the first `/careers`
response often redirects to a platform-specific URL revealing the ATS — we take
that and stop probing further paths.

**SPA-heavy career pages:** A page classified `empty` is not useless — it will
be passed to the AI crawler, which uses Playwright to fully render it. The
distinction is `dead` (broken URL, do not crawl) vs `empty` (page exists, no
static content visible, send to Playwright).
