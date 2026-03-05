# ATS Identification Plan — Revised (March 2026)

This document outlines the plan for identifying which ATS (Applicant Tracking System) each company in our database uses, extracting their identifiers, and validating them for direct API integration.

> [!IMPORTANT]
> **Key discovery**: Only **32 of 182 companies** (17.6%) have a populated `applyUrl` in our LinkedIn-scraped job data. The remaining 150 companies use LinkedIn's native "Easy Apply" feature, so LinkedIn never exposes an external application URL. The URL-pattern-matching approach that worked for those 32 companies **cannot scale** to the rest — we need to scan their careers pages directly.

---

## 1. Current State

As of March 2026, our position is:

| Metric | Count | Coverage |
|:---|:---:|:---:|
| Total companies in database | 182 | — |
| Companies with ATS identified | 74 | 40.7% |
| Companies with NONE (unidentified) | 108 | 59.3% |
| Total jobs in database | 449 | — |
| Jobs from identified-ATS companies | 247 | 55.0% |
| Jobs from NONE companies | 202 | 45.0% |

### What we've done so far

1. **BigQuery `applyUrl` scan**: Ran `REGEXP_EXTRACT` on all `applyUrls` in `raw_jobs` to identify ATS providers from known domain patterns (e.g. `boards.greenhouse.io`, `jobs.lever.co`, `*.wd3.myworkdayjobs.com`).
2. **Public API validation**: Used `curl` against Greenhouse, Lever, and Ashby APIs to confirm tokens and count open jobs.
3. **Manual browser inspection**: Visited careers pages for ASB (→ SnapHire), ANZ (→ SuccessFactors), and Wētā FX (→ SuccessFactors) to identify hidden ATS markers.
4. **Results documented** in `ideas/ats-identification-findings.md`.

### Why 108 companies are NONE

The root cause is **LinkedIn Easy Apply**. Of the 182 companies:
- **32 companies** have at least one job with a populated `applyUrl` — LinkedIn redirected the applicant to an external site, which exposed the ATS domain.
- **150 companies** have all their jobs with empty `applyUrls` — these companies use LinkedIn's native "Easy Apply" feature, so LinkedIn handles the application internally and never exposes an external URL.

Our Apify scraper is already capturing all available `applyUrls` — there is nothing more to extract from LinkedIn. These companies almost certainly _do_ use an ATS for their own careers pages, but we need to discover it by visiting those pages directly.

---

## 2. Goal

Increase ATS identification coverage from **40.7% → 80%+** of companies and **55% → 80%+** of jobs.

### Priority tiers

| Tier | Companies | Criteria | Target |
|:---|:---:|:---|:---|
| **Tier 1** | ~23 | Phase 1 ATS (Greenhouse, Lever, Ashby) — already have public APIs | Fully integrate: identify + validate + enable direct polling |
| **Tier 2** | ~17 | Workday — large enterprise segment | Identify + document; integration TBD |
| **Tier 3** | ~34 | All other known ATS (SmartRecruiters, BambooHR, Workable, etc.) | Identify + document for future phases |
| **Tier 4** | ~108 | Currently NONE | Investigate each to determine true ATS |

---

## 3. Identification Strategy (Revised)

### Phase A: Automated careers page scanning (highest ROI)

Since LinkedIn cannot tell us the ATS for Easy Apply companies, we need to visit each company's careers page directly and look for ATS markers.

**Approach**: Build an automated script that, for each NONE company:
1. Looks up the company's `website` from `raw_companies` (or searches for their careers page)
2. Fetches the careers page HTML
3. Follows any redirects (many redirect to `boards.greenhouse.io/...`, `jobs.lever.co/...`, etc.)
4. Searches the page source for known ATS domain markers
5. Extracts the identifier/slug if found
6. Records the result in the findings file

**ATS markers to scan for:**
```
boards.greenhouse.io, boards-api.greenhouse.io,
jobs.lever.co, api.lever.co,
jobs.ashbyhq.com, api.ashbyhq.com,
*.wd[0-9].myworkdayjobs.com,
*.snaphire.com,
*.bamboohr.com,
*.teamtailor.com,
apply.workable.com,
careers.smartrecruiters.com,
*.successfactors.com,
*.jobadder.com,
*.employmenthero.com,
*.elmotalent.co.nz,
*.breezy.hr,
*.aplitrak.com,
*.contacthr.com
```

**Script outline** (to be added to `scripts/ats/`):
```bash
#!/bin/bash
# discover_ats.sh — Automated ATS discovery via careers page scanning

# 1. Extract NONE companies and their websites from BigQuery
bq query --format=csv '...' > /tmp/none_companies.csv

# 2. For each company, try to find and scan their careers page
while IFS=, read -r companyName website; do
  # Try common careers page patterns
  for path in "/careers" "/jobs" "/work-with-us" ""; do
    page=$(curl -sL -o /tmp/page.html -w "%{url_effective}" "${website}${path}" 2>/dev/null)
    
    # Check final URL after redirects
    if echo "$page" | grep -qiE 'greenhouse|lever\.co|ashbyhq'; then
      echo "$companyName: FOUND via redirect -> $page"
      break
    fi
    
    # Check page source for ATS markers
    match=$(grep -ioE 'greenhouse\.io|lever\.co|ashbyhq|workday|successfactors|snaphire|bamboohr|teamtailor|workable|smartrecruiters' /tmp/page.html | head -1)
    if [ -n "$match" ]; then
      echo "$companyName: FOUND $match in page source"
      break
    fi
  done
done < /tmp/none_companies.csv
```

### Phase B: Manual investigation (remaining companies)

Any companies that remain unidentified after Phase A will be manually investigated. This is expected to be a small subset.

**Process per company:**
1. Google `"{company name}" careers` or check the `website` field
2. Visit the careers page and inspect:
   - URL: Does it redirect to a known ATS domain?
   - Page source: Search for ATS markers
   - Network requests: Check for API calls to known ATS domains
   - iframes / script tags: Check for embedded ATS widgets
3. Record the ATS provider and identifier in the findings file

**Known leads for high-value companies** (from earlier research):
- Spark New Zealand → likely Workday (`sparknz.wd3.myworkdayjobs.com`)
- Southern Cross Health Insurance → likely SnapHire (`southerncross.snaphire.com`)
- Tower Insurance → likely SnapHire (`tower.snaphire.com`)
- Microsoft → proprietary careers portal

---

## 4. Validation Process

For each identified ATS, validate the extracted identifier:

### Providers with public APIs (can validate immediately)

| Provider | Validation Command | Success Criteria |
|:---|:---|:---|
| **Greenhouse** | `curl -s "https://boards-api.greenhouse.io/v1/boards/{token}/jobs"` | Returns JSON with `jobs` array |
| **Lever** | `curl -s "https://api.lever.co/v0/postings/{slug}?limit=1"` | Returns non-empty JSON array |
| **Ashby** | `curl -s "https://api.ashbyhq.com/posting-api/job-board/{slug}"` | Returns JSON with `jobs` array |

### Providers without public APIs (identification only)

| Provider | How to Confirm | Notes |
|:---|:---|:---|
| **Workday** | Check if `{slug}.wd{N}.myworkdayjobs.com` resolves | Try wd1-wd5 variants |
| **SuccessFactors** | Look for `career*.successfactors.com` in page source | Common for banks |
| **SnapHire** | Check if `{slug}.snaphire.com` resolves | Common NZ provider |
| **SmartRecruiters** | Check if `careers.smartrecruiters.com/{slug}` loads | |
| **BambooHR** | Check if `{slug}.bamboohr.com/careers` loads | |
| **Workable** | Check if `apply.workable.com/{slug}` loads | |
| **Teamtailor** | Check if `{slug}.teamtailor.com` loads | |

---

## 5. Existing Tooling

Located in `scripts/ats/`:

| Script | Purpose |
|:---|:---|
| `validate_ats.sh` | Validates Greenhouse, Lever, Ashby tokens via public APIs. Also checks Workday domains. |
| `csv_to_md.py` | Converts BigQuery CSV exports to Markdown tables for the findings file. |
| `update_findings.py` | Injects validated roster data into `ideas/ats-identification-findings.md`. |

---

## 6. Extraction & Validation by Provider

### 6.1 Greenhouse

**Extract:** `board_token` from URL `boards.greenhouse.io/{board_token}`

**Validate:**
```
curl -s "https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs" | head -c 200
```
A valid response returns JSON with a `jobs` array. An invalid token returns a 404.

---

### 6.2 Lever

**Extract:** `company_slug` from URL `jobs.lever.co/{company_slug}`

**Validate:**
```
curl -s "https://api.lever.co/v0/postings/{company_slug}?limit=1" | head -c 200
```
A valid response returns a JSON array of postings. An invalid slug returns an empty array `[]`.

---

### 6.3 Ashby

**Extract:** `company_slug` from URL `jobs.ashbyhq.com/{company_slug}`

**Validate:**
```
curl -s "https://api.ashbyhq.com/posting-api/job-board/{company_slug}" | head -c 200
```
A valid response returns JSON with a `jobs` array. Invalid slugs return an error.

**Tip:** Add `?includeCompensation=true` to also get salary data.

---

### 6.4 JobAdder

**Identification:** Look for `jobadder.com` in careers page source or network requests.

**Note:** JobAdder requires **OAuth 2.0** — we can't just test with curl. Flag the company as `JOBADDER` with a placeholder identifier.

---

### 6.5 Employment Hero

**Identification:** Look for `employmenthero.com` in careers page source or widget URLs.

**Extract:** `org_id` from the widget URL pattern: `api.employmenthero.com/api/v1/organisations/{org_id}/jobs`

**Note:** Requires Platinum subscription for API access.

---

### 6.6 SnapHire

**Identification:** Look for `snaphire.com` in the careers page URL or source, or the `ajid` parameter in URLs.

**Note:** API access requires TAS developer registration. Flag the company for Phase 3 follow-up.

---

## 7. Implementation Sequence

### Completed

1. ✅ ~~Scan all `applyUrls` in BigQuery for known ATS domain patterns~~ — **Done** (identified 74 companies)
2. ✅ ~~Validate Greenhouse, Lever, Ashby tokens via public APIs~~ — **Done**
3. ✅ ~~Document findings in Master Roster~~ — **Done** (`ideas/ats-identification-findings.md`)

### Next steps

4. ⬜ **Build automated careers page scanner** (Phase A) — Script to visit each NONE company's careers page and identify ATS markers in the HTML/redirects. Add to `scripts/ats/discover_ats.sh`.
5. ⬜ **Run scanner against all 108 NONE companies** and update findings file with results
6. ⬜ **Manual investigation** (Phase B) — For any companies that remain unidentified after the scanner, manually inspect their careers pages
7. ⬜ **Update findings file** with each new identification as it's made

### Medium-term

8. ⬜ **Workday integration investigation** — Workday is 9.3% of companies; determine if their job data API is feasible
9. ⬜ **SmartRecruiters integration investigation** — 2.7% of companies but 4.9% of jobs
10. ⬜ **Seed `company_ats_configs` in BigQuery** once identifiers are validated

---

## 8. Relationship to Existing Infrastructure

The codebase already has:
- `CompanyAtsConfig` model (`persistence/model/CompanyAtsConfig.kt`)
- `AtsConfigBigQueryRepository` for persistence (`persistence/ats/AtsConfigBigQueryRepository.kt`)
- `AtsConfigMapper` for reading rows (`persistence/ats/AtsConfigMapper.kt`)
- `GreenhouseClient` for fetching jobs (`sync/ats/greenhouse/GreenhouseClient.kt`)
- `GreenhouseNormalizer` for data normalization (`sync/ats/greenhouse/GreenhouseNormalizer.kt`)
- `AtsProvider` enum and `SyncStatus` enum (`sync/ats/`)
- `company_ats_configs` BigQuery table definition

Once the roster is built from this discovery process, the identifiers flow directly into `CompanyAtsConfig.identifier` and the sync pipeline can begin polling.

---

## 9. Documents & References

| Document | Purpose |
|:---|:---|
| `ideas/ats-identification-findings.md` | Master Roster with all 182 companies, ATS provider, identifier, and validation status |
| `ideas/ats-integration.md` | Full ATS integration strategy (API details, architecture, data mapping) |
| `scripts/ats/` | Validation and data conversion scripts |
