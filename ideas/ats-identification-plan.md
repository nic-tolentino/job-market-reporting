# ATS Identification Plan — Company-by-Company Discovery

This document outlines a systematic approach for identifying which ATS (Applicant Tracking System) each company in our BigQuery `raw_companies` table uses, and extracting the tokens/slugs/IDs required for direct API integration.

> [!NOTE]
> This plan covers the **Phase 1 discovery work** referenced in `ats-integration.md` — specifically the "👤 Your tasks" items: research company ATS identifiers, build the initial company roster, and validate test companies.

---

## 1. Goal

For every company currently tracked via Apify (LinkedIn scraping), determine:

1. **Which ATS provider they use** (Greenhouse, Lever, Ashby, JobAdder, Employment Hero, SnapHire, or none)
2. **Their ATS identifier** (board token, company slug, org ID, etc.)
3. **Whether the ATS public API returns job data** (validate the identifier works)

The output is a roster that seeds the `company_ats_configs` BigQuery table, enabling direct ATS polling.

---

## 2. How to Identify a Company's ATS

### 2.1 Careers Page URL Patterns

The fastest identification method is checking where a company's careers page points. Each ATS has a distinctive URL pattern:

| ATS Provider | Careers Page URL Pattern | What to Extract |
|:---|:---|:---|
| **Greenhouse** | `boards.greenhouse.io/{board_token}` or redirects to it | `board_token` |
| **Greenhouse** (embedded) | Company's own domain, but loads content from `boards-api.greenhouse.io` | `board_token` (found in page source / network requests) |
| **Lever** | `jobs.lever.co/{company_slug}` or redirects to it | `company_slug` |
| **Lever** (embedded) | Company's own domain, but loads content from `api.lever.co` | `company_slug` (found in network requests) |
| **Ashby** | `jobs.ashbyhq.com/{company_slug}` | `company_slug` |
| **Ashby** (embedded) | Company's own domain, but loads from `api.ashbyhq.com` | `company_slug` (found in network requests) |
| **JobAdder** | Usually embedded; look for `jobadder.com` in network calls | `board_id` (requires OAuth — see Section 4) |
| **Employment Hero** | Embedded widget or `employmenthero.com` subdomain | `org_id` (in the widget URL or page source) |
| **SnapHire** | `{company}.snaphire.com` or Talent App Store URL | TAS identifier |
| **None / Other** | Company's own careers page (Workday, Taleo, BambooHR, custom, etc.) | N/A — Apify remains sole source |

### 2.2 Step-by-Step Identification Process

For each company:

1. **Find their careers/jobs page**
   - Check the `website` field in `raw_companies` if available
   - Google: `"{company name}" careers` or `"{company name}" jobs`
   - Check the `applyUrls` and `platformLinks` from their `raw_jobs` entries — these often reveal the ATS directly

2. **Inspect the URL**
   - If the URL contains `greenhouse.io`, `lever.co`, `ashbyhq.com`, etc. → immediate match
   - The slug/token is the path segment right after the domain

3. **If embedded on their own domain — check the page source**
   - Open the careers page in a browser
   - Right-click → View Page Source (or open DevTools → Network tab)
   - Search for these strings in the HTML or network requests:
     - `greenhouse` → Greenhouse
     - `lever.co` or `api.lever.co` → Lever
     - `ashbyhq.com` or `ashby` → Ashby
     - `jobadder` → JobAdder
     - `employmenthero` → Employment Hero
     - `snaphire` → SnapHire
   - The API URL in the request will contain the identifier (board token, slug, etc.)

4. **If neither URL nor source reveals anything**
   - Check the page for `<iframe>` tags — many ATS systems embed via iframes
   - Check for `<script>` tags referencing ATS CDN/JS files
   - Look at job listing links — where do individual "Apply" buttons point?
   - As a last resort: check [BuiltWith](https://builtwith.com/) or [Wappalyzer](https://www.wappalyzer.com/) for the company's domain

---

## 3. Extraction & Validation by Provider

### 3.1 Greenhouse

**Extract:** `board_token` from URL `boards.greenhouse.io/{board_token}`

**Validate:**
```
curl -s "https://boards-api.greenhouse.io/v1/boards/{board_token}/jobs" | head -c 200
```
A valid response returns JSON with a `jobs` array. An invalid token returns a 404.

**Common NZ tech companies on Greenhouse:** Xero, Rocket Lab, Datacom, Vista Group

---

### 3.2 Lever

**Extract:** `company_slug` from URL `jobs.lever.co/{company_slug}`

**Validate:**
```
curl -s "https://api.lever.co/v0/postings/{company_slug}?limit=1" | head -c 200
```
A valid response returns a JSON array of postings. An invalid slug returns an empty array `[]`.

**Common NZ tech companies on Lever:** Often used by US-origin companies with NZ offices

---

### 3.3 Ashby

**Extract:** `company_slug` from URL `jobs.ashbyhq.com/{company_slug}`

**Validate:**
```
curl -s "https://api.ashbyhq.com/posting-api/job-board/{company_slug}" | head -c 200
```
A valid response returns JSON with a `jobs` array. Invalid slugs return an error.

**Tip:** Add `?includeCompensation=true` to also get salary data.

---

### 3.4 JobAdder

**Identification:** Look for `jobadder.com` in careers page source or network requests.

**Note:** JobAdder requires **OAuth 2.0** — we can't just test with curl. Identification only at this stage; actual integration requires partner registration (email `api@jobadder.com`).

**Record:** Flag the company as `JOBADDER` with a placeholder identifier. The `board_id` is discovered after OAuth setup.

---

### 3.5 Employment Hero

**Identification:** Look for `employmenthero.com` in careers page source or widget URLs.

**Extract:** `org_id` from the widget URL pattern: `api.employmenthero.com/api/v1/organisations/{org_id}/jobs`

**Note:** Requires Platinum subscription for API access. Flag the company and verify their tier later.

---

### 3.6 SnapHire

**Identification:** Look for `snaphire.com` in the careers page URL or source.

**Note:** API access requires TAS developer registration. Flag the company for Phase 3 follow-up.

---

## 4. Recommended Workflow

### Step 1: Export Company List from BigQuery

Run this query in BigQuery Console to get all companies with their websites and a sample apply URL:

```sql
SELECT
  c.companyId,
  c.name,
  c.website,
  (SELECT applyUrls[SAFE_OFFSET(0)] FROM `techmarket.raw_jobs` j WHERE j.companyId = c.companyId LIMIT 1) AS sampleApplyUrl,
  (SELECT platformLinks[SAFE_OFFSET(0)] FROM `techmarket.raw_jobs` j WHERE j.companyId = c.companyId LIMIT 1) AS samplePlatformLink
FROM `techmarket.raw_companies` c
ORDER BY c.name
```

Export the results to a spreadsheet/CSV.

### Step 2: Bulk URL Analysis (Quick Wins)

Many identifiers can be found just from the `sampleApplyUrl` and `samplePlatformLink` columns:

- **Contains `greenhouse.io`** → Extract board token from URL path
- **Contains `lever.co`** → Extract company slug from URL path
- **Contains `ashbyhq.com`** → Extract company slug from URL path
- **Contains `jobadder`** → Flag as JobAdder
- **Contains `employmenthero`** → Flag as Employment Hero
- **Contains `snaphire`** → Flag as SnapHire

This can be done with a simple script or spreadsheet formula.

### Step 3: Manual Inspection (Remaining Companies)

For companies not identified in Step 2, manually visit their careers pages and check the source (see Section 2.2).

### Step 4: Validate Identifiers

For each Greenhouse, Lever, and Ashby company identified, run the curl validation commands from Section 3 to confirm the identifier works and returns job data.

### Step 5: Build the Roster

Compile the results into a JSON file or spreadsheet with this structure:

```json
[
  {
    "companyId": "xero",
    "companyName": "Xero",
    "atsProvider": "GREENHOUSE",
    "identifier": "xero",
    "validated": true,
    "jobCount": 45,
    "notes": ""
  },
  {
    "companyId": "rocket-lab",
    "companyName": "Rocket Lab",
    "atsProvider": "GREENHOUSE",
    "identifier": "rocketlab",
    "validated": true,
    "jobCount": 23,
    "notes": ""
  },
  {
    "companyId": "some-company",
    "companyName": "Some Company",
    "atsProvider": "NONE",
    "identifier": "",
    "validated": false,
    "jobCount": 0,
    "notes": "Uses Workday — not supported in Phase 1"
  }
]
```

### Step 6: Document Findings
Record all identification results, identifiers, and validation status in a new findings file. This serves as the source of truth before any database seeding occurs.

> [!IMPORTANT]
> **No database changes or data seeding** will be performed during this discovery phase. The goal is documentation only.

---

## 5. Automated Discovery Tooling

To accelerate the identification and validation process, the following tools have been developed and are located in `scripts/ats/`:

| Script | Purpose | Usage |
|:---|:---|:---|
| `validate_ats.sh` | Main validation engine. Checks Greenhouse, Lever, Ashby APIs and Workday domains. | `./scripts/ats/validate_ats.sh` |
| `csv_to_md.py` | Utility to convert BigQuery CSV exports into clean Markdown tables. | `python3 scripts/ats/csv_to_md.py` |
| `update_findings.py` | Injects validated data results directly into `ideas/ats-identification-findings.md`. | `python3 scripts/ats/update_findings.py` |

> [!TIP]
> These scripts allow for rapid re-validation of the entire 182-company roster as new job data is scraped or company ATS systems change.

---

## 6. Expected Outcomes

Based on the NZ tech market, a rough distribution might be:

| ATS Provider | Expected % | Effort Level |
|:---|:---|:---|
| Greenhouse | 25–35% | ✅ Low — public API, just need board token |
| Lever | 10–15% | ✅ Low — public API, just need slug |
| Ashby | 5–10% | ✅ Low — public API, just need slug |
| JobAdder | 5–10% | ⚠️ Medium — requires OAuth partner registration |
| Employment Hero | 3–5% | ⚠️ Medium — requires Platinum tier verification |
| SnapHire | 2–3% | 🔴 High — sparse API docs, TAS registration |
| Other (Workday, Taleo, BambooHR, custom) | 20–35% | ❌ Not supported — Apify remains sole source |

> [!IMPORTANT]
> Companies using unsupported ATS platforms (Workday, Taleo, SmartRecruiters, etc.) should still be recorded with `atsProvider: "NONE"` so we have a clear picture of coverage gaps and can prioritise future integrations.

---

## 7. Tracking Spreadsheet Template

Use this column structure for tracking progress:

| Column | Description |
|:---|:---|
| `companyId` | From BigQuery `raw_companies` |
| `companyName` | Human-readable name |
| `website` | Company website |
| `careersPageUrl` | Direct link to their jobs/careers page |
| `atsProvider` | GREENHOUSE / LEVER / ASHBY / JOBADDER / EMPLOYMENT_HERO / SNAPHIRE / NONE / UNKNOWN |
| `identifier` | Board token, slug, org ID, etc. |
| `validated` | YES / NO — did the curl test return jobs? |
| `jobCount` | Number of jobs returned by validation |
| `notes` | Any relevant observations |
| `status` | TODO / IN_PROGRESS / DONE |

---

## 8. Full Coverage Strategy

The objective is to identify the ATS for **every company** in our database, with no exceptions.

1. **Breadth-First Identification** — Go through the entire company list and attempt to identify the ATS, including those outside the core 6 (e.g. Workday, Taleo, SmartRecruiters, BambooHR).
2. **Deep-Dive for Identifiers** — For companies using our core 6 ATS providers (Greenhouse, Lever, Ashby, JobAdder, Employment Hero, SnapHire), extract the necessary identifiers and validate them via public API checks.
3. **Capture "Other" ATS** — If a company uses a different ATS, record its name. This helps map the "long tail" of the market for future integration planning.
4. **Identify "No ATS"** — If a company truly posts jobs directly or uses a proprietary system without clear ATS markers, record this as `NONE`.

---

## 9. Relationship to Existing Infrastructure

The codebase already has:
- `CompanyAtsConfig` model (`persistence/model/CompanyAtsConfig.kt`)
- `AtsConfigBigQueryRepository` for persistence (`persistence/ats/AtsConfigBigQueryRepository.kt`)
- `AtsConfigMapper` for reading rows (`persistence/ats/AtsConfigMapper.kt`)
- `GreenhouseClient` for fetching jobs (`sync/ats/greenhouse/GreenhouseClient.kt`)
- `GreenhouseNormalizer` for data normalization (`sync/ats/greenhouse/GreenhouseNormalizer.kt`)
- `AtsProvider` enum and `SyncStatus` enum (`sync/ats/`)
- `company_ats_configs` BigQuery table definition

Once the roster is built from this discovery process, the identifiers flow directly into `CompanyAtsConfig.identifier` and the sync pipeline can begin polling.
