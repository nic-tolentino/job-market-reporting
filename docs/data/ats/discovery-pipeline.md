# ATS & Seed Discovery Pipeline

Formal definition of the repeatable pipeline for discovering ATS configurations
and career page seeds for companies in the manifest.

**Last updated:** March 2026

---

## Overview

The pipeline runs in stages. Each stage hands off to the next: fast/free sources
first, progressively slower/costlier methods only for companies not resolved
earlier.

```
Stage 0: Validate existing configs    → Remove stale ATS boards
Stage 1: ATS from apply URLs          → GH/LV/AS configs from BigQuery
Stage 2: Vanity domain probing        → GH/LV/AS/SR/TT configs via API
Stage 3: Seed mining from apply URLs  → Seeds for Workday/BambooHR/etc.
Stage 4: Career path probing          → Seeds via /careers, /jobs, etc.
Stage 5: Seed verification            → HTTP-check all seeds, write status
Stage 6: (On-demand) Gemini search    → Seeds for hard-to-find pages
```

Each stage is idempotent — running it twice produces the same result. Run only
the stages relevant to new data (e.g. just Stage 3+5 after a BQ ingestion that
brought in new apply URLs).

---

## Stage 0 — Validate existing ATS configs

**Script:** `scripts/ats/validate_ats_configs.py`
**Frequency:** After any bulk manifest changes or monthly
**Runtime:** ~5 min (live API calls to GH/LV/AS/SR/TT/WL)

Checks every `ats` config in the manifest against the live ATS API. Removes
configs for boards that return 404/410 (company closed or changed platform).
Warns on boards that are valid but have no open jobs.

```bash
python3 scripts/ats/validate_ats_configs.py
# Fix any errors it reports, then re-run to confirm 0 errors
```

---

## Stage 1 — ATS configs from BigQuery apply URLs

**Script:** `scripts/ats/extract_ats_from_apply_urls.py`
**Frequency:** After each BQ ingestion cycle
**Runtime:** ~30s (one BQ query)

Queries all `applyUrls` in BigQuery for companies without ATS config. Extracts
Greenhouse / Lever / Ashby board slugs via regex. Outputs a CSV for review, then
optionally writes confirmed slugs to manifests.

```bash
python3 scripts/ats/extract_ats_from_apply_urls.py          # review CSV first
python3 scripts/ats/extract_ats_from_apply_urls.py --apply-to-manifests
# Then: validate_ats_configs.py to confirm
```

**Why before Stage 2:** Slug from apply URL is more reliable than domain-derived
slug (avoids false positives from common words as domain names).

---

## Stage 2 — Vanity domain probing

**Script:** `scripts/ats/probe_vanity_domains.py`
**Frequency:** Monthly, or after adding new companies
**Runtime:** ~3–10 min depending on company count

For companies with a website but no ATS config, derives a slug from the domain
name (e.g. `atlassian` from `atlassian.com`) and probes five supported ATS APIs:
Greenhouse, Lever, Ashby, SmartRecruiters, TeamTailor.

```bash
python3 scripts/ats/probe_vanity_domains.py                 # review hits CSV
python3 scripts/ats/probe_vanity_domains.py --apply-to-manifests

# Run on specific companies only (e.g. after Stage 3 flags candidates):
python3 scripts/ats/probe_vanity_domains.py --company-ids foo bar baz --apply-to-manifests
```

**Note on TeamTailor:** The `/feed/jobs.json` endpoint is deprecated. Verification
falls back to HTML marker detection (`teamtailor.com` in page source). When apply
URLs contain the exact TT slug (which includes numeric suffixes), use the slug
directly rather than relying on domain-derived slugs.

---

## Stage 3 — Seed URLs from BigQuery apply URLs

**Script:** `scripts/ats/discover_seeds_from_apply_urls.py`
**Frequency:** After each BQ ingestion cycle
**Runtime:** ~30s (one BQ query)

Queries all `applyUrls` in BigQuery for companies without supported ATS and
without live seeds. Strips job-specific IDs to derive the career listing page URL.
Covers 18 non-integrated ATS platforms: Workday, BambooHR, SuccessFactors,
Recruitee, Breezy, Join.com, Employment Hero (company subdomains only), Personio,
JobAdder, PageUp, Taleo, iCIMS, Snaphire, Factorial, Zoho, Elmo, Comeet, and more.

Also flags companies where a supported ATS (GH/LV/AS/SR/TT/WL) appears in apply
URLs but isn't in the manifest — these should be processed by Stage 1+2 instead.

```bash
python3 scripts/ats/discover_seeds_from_apply_urls.py       # preview CSV
python3 scripts/ats/discover_seeds_from_apply_urls.py --apply
# Then: verify_seeds.py to check status of new seeds
```

Seeds are written with `status: pending` — run Stage 5 to classify them.

---

## Stage 4 — Career page path probing

**Script:** `scripts/ats/probe_career_paths.py`
**Frequency:** Monthly, or after new companies are added
**Runtime:** ~10–20 min for full manifest (async, 10 concurrent)

For companies with a website but no seeds or supported ATS, probes common career
page paths (`/careers`, `/jobs`, `/work-with-us`, etc.) plus homepage link
extraction as fallback.

For each candidate URL:
- **ATS detected** → writes ATS config (if supported provider) or seed URL (if not)
- **Job indicators found** → writes seed with `status: active`
- **Page loads, no jobs** → writes seed with `status: empty`

```bash
python3 scripts/ats/probe_career_paths.py                   # dry-run
python3 scripts/ats/probe_career_paths.py --apply
python3 scripts/ats/probe_career_paths.py --company-id foo --apply
```

This stage complements Stage 3: Stage 3 uses BQ data (companies we've seen
before), Stage 4 uses live HTTP probing (including brand new companies).

---

## Stage 5 — Seed verification

**Script:** `scripts/ats/verify_seeds.py`
**Frequency:** After Stages 3, 4, or 6 add new seeds; also monthly to refresh statuses
**Runtime:** ~5–10 min

HTTP-checks every seed with `status: pending` (or `status: unknown`) and
classifies it:

| Status | Meaning |
|--------|---------|
| `active` | ≥3 job indicator regex hits — crawlable now |
| `empty` | Page loads, no jobs visible (SPA or quiet period) |
| `dead` | 404 / DNS failure / off-domain redirect |
| `blocked` | 403 or JS-only SPA |
| `unknown` | Timeout or other transient error |

Run after any bulk seed additions to get a clean current-state view.

> Status `empty` does not mean no jobs exist — many companies use SPAs that
> render jobs via JS after the initial HTML response. The AI crawler (Playwright)
> will reveal whether an `empty` page actually has jobs.

---

## Stage 6 — Gemini search grounding (on-demand)

**Script:** `scripts/ats/discover_seeds_gemini.py` *(pending)*
**Frequency:** On-demand for high-priority companies
**Runtime:** ~1s per company

For companies that remain unseeded after all prior stages, query Gemini with
search grounding:

```
What is the career listings page URL for "{Company Name}" ({website})?
Return only the URL, nothing else.
```

Gemini fetches live Google results without us needing to render pages. Results
are HTTP-verified before being written to manifests.

Use for: companies with unusual career page paths, recently-rebranded companies,
or companies where mechanical probing consistently fails.

---

## Running the full pipeline

### After a bulk data ingestion:
```bash
python3 scripts/ats/extract_ats_from_apply_urls.py --apply-to-manifests  # Stage 1
python3 scripts/ats/discover_seeds_from_apply_urls.py --apply             # Stage 3
python3 scripts/ats/verify_seeds.py                                       # Stage 5
```

### Monthly maintenance sweep:
```bash
python3 scripts/ats/validate_ats_configs.py                               # Stage 0
python3 scripts/ats/probe_vanity_domains.py --apply-to-manifests          # Stage 2
python3 scripts/ats/probe_career_paths.py --apply                         # Stage 4
python3 scripts/ats/verify_seeds.py                                       # Stage 5
python3 scripts/ats/validate_ats_configs.py                               # Stage 0 again
```

### For a single new company added to the manifest:
```bash
CID=my-new-company

python3 scripts/ats/discover_seeds_from_apply_urls.py --company-id $CID --apply
python3 scripts/ats/probe_career_paths.py --company-id $CID --apply
python3 scripts/ats/verify_seeds.py --company-id $CID
```

---

## Coverage projection

| After running | Supported ATS | Active seeds | Total crawlable |
|---------------|--------------|--------------|-----------------|
| Baseline (Jan 2026) | 419 (30%) | 0 | 30% |
| After Stages 1–5 (pre-cleanup) | 437 (32%) | 208 (15%) | 47% |
| After dead-seed cleanup | 426 (31%) | 212 (15%) | 66% |
| After Stage 6 run 1 (Gemini) | 426 (31%) | 250 (18%) | 73% |
| **March 2026 — after Stage 6 run 2 + manual probing** | **428 (31%)** | **319 (23%)** | **83%** |
| After AI Crawler deployment | — | — | ~90%+ |

The jump from 73% → 83% came from a second Gemini run with an improved prompt
(15 ATS platforms listed explicitly, including Deel and Rippling) plus manual
seed construction from Workday/Personio/Comeet ATS identifiers.

The 479 companies with seeds but status `empty` or `blocked` will become
crawlable once the AI crawler (ADR-008) is deployed — Playwright renders SPAs
that static HTTP verification misses.

The remaining 227 companies with no coverage are primarily Spanish SMEs and
small AU/NZ companies that consistently return NO_RESULT from Gemini. The AI
crawler (ADR-008) is the final fallback — it probes any page with Playwright.

---

## Script inventory

| Script | Stage | Purpose |
|--------|-------|---------|
| `validate_ats_configs.py` | 0 | Live-check all ATS configs |
| `extract_ats_from_apply_urls.py` | 1 | GH/LV/AS slugs from BQ apply URLs |
| `probe_vanity_domains.py` | 2 | Domain-slug probe for GH/LV/AS/SR/TT |
| `discover_seeds_from_apply_urls.py` | 3 | Seed URLs from BQ apply URLs (all providers) |
| `probe_career_paths.py` | 4 | Path probe + homepage scan |
| `verify_seeds.py` | 5 | HTTP-check seeds, write status |
| `discover_seeds_gemini.py` | 6 | Gemini search grounding (on-demand) |
| `scan_careers_pages.py` | — | Scan *existing* seeds for ATS embed patterns |
| `fix_stale_ats_configs.py` | — | One-off bulk removal of dead ATS configs |

> `scan_careers_pages.py` and `probe_free_ats.py` pre-date this pipeline and
> overlap with later stages. They are kept for reference but the stages above
> supersede them for new work.

---

## Pipeline status (March 2026)

| Stage | Script | Status |
|-------|--------|--------|
| 0 — Validate configs | `validate_ats_configs.py` | ✅ Complete — 0 errors (re-run in progress for new configs) |
| 1 — ATS from BQ apply URLs | `extract_ats_from_apply_urls.py` | ✅ Complete |
| 2 — Vanity domain probing | `probe_vanity_domains.py` | ✅ Complete (+ `--company-ids` flag added) |
| 3 — Seeds from BQ apply URLs | `discover_seeds_from_apply_urls.py` | ✅ Complete — JobAdder excluded (forms, not listings) |
| 4 — Career path probing | `probe_career_paths.py` | ✅ Complete — ~120 seeds written |
| 5 — Seed verification + dead cleanup | `verify_seeds.py` | ✅ Complete — 323 dead seeds removed; 916 companies crawlable (66%) |
| 6 — Gemini search | `discover_seeds_gemini.py` | ✅ Complete — 2 runs; ~209 seeds written; 83% crawlable |
