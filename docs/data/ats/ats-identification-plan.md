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
| Companies with ATS identified | 92 | 50.5% |
| Companies with NONE (unidentified) | 90 | 49.5% |
| Total jobs in database | 449 | — |
| Jobs from identified-ATS companies | 305 | 68.0% |
| Jobs from NONE companies | 144 | 32.0% |

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

### Phase B: Advanced Discovery for Remaining Companies (Optional / Low ROI)

After automated scanning, ~90 companies (49.5%) remain unidentified. These companies almost certainly fall into one of three buckets:
1. They use **LinkedIn Easy Apply** exclusively and don't maintain a public ATS board.
2. They use a **custom/proprietary job board** (like EY or Microsoft) that we cannot easily integrate with via a standard API.
3. They use an ATS that we haven't mapped yet, or their ATS is heavily masked behind a corporate proxy.

**Recommendation:** Do not aggressively pursue identifying these remaining 90 companies right now. The ROI is low because even if we find an ATS, if they force candidates through LinkedIn Easy Apply, their public ATS board might not be populated with the jobs we actually want. We must continue to rely on Apify scraping for these companies.

If we *need* to identify a specific high-value company, we can use these advanced manual methods:

1. **Google Dorking for hidden boards:**
   - Search: `site:boards.greenhouse.io "{Company Name}"` or `site:jobs.lever.co "{Company Name}"`.
   - Often, companies have an active public ATS board that just isn't linked from their main careers page because they prefer routing traffic through LinkedIn.
2. **Wayback Machine / Historical Data:**
   - Check the company's careers page on the Internet Archive from 1-2 years ago. They may have switched to Easy Apply recently but still use the same ATS internally (which might still have an active API).
3. **LinkedIn Network Interception (Manual):**
   - Attempt an "Easy Apply" on LinkedIn with browser dev tools open. Look at network requests — sometimes the LinkedIn integration pings the underlying ATS API directly, revealing the provider.

**Remaining high-value targets** (if we choose to investigate):
- Spark New Zealand
- Microsoft (known proprietary)
- Kiwibank
- Fisher & Paykel Healthcare
- IAG
- Orion Health

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
3. ✅ ~~Document findings in Master Roster~~ — **Done** (`docs/data/ats/ats-identification-findings.md`)
4. ✅ ~~Build automated careers page scanner (Phase A)~~ — **Done** (`discover_ats.py`)
5. ✅ ~~Run scanner against all 108 NONE companies~~ — **Done** (Identified 18 additional companies, bringing total to 92 identified)

### Next steps

6. ⬜ **Migrate ATS configs to manifest** — Run migration script to add ATS configs to company JSON files:
   ```bash
   # Option A: From BigQuery (if configs already in BQ)
   python3 scripts/ats/migrate_to_declarative.py
   
   # Option B: From findings document (hardcoded data)
   python3 scripts/ats/add_from_findings.py
   ```
7. ⬜ **Validate migrated configs** — Run validation script:
   ```bash
   python3 scripts/ats/validate_ats_configs.py
   ```
8. ⬜ **Commit and deploy** — Commit changes and deploy:
   ```bash
   git diff data/companies/
   git commit -m "Add ATS configs from identification findings"
   # Deploy - CompanySyncService will sync to BigQuery on next run
   ```
9. ⬜ **Full Validation Sweep** — Run `validate_ats.sh` across all identified companies to ensure tokens are valid and actively returning jobs.
10. ⬜ **Enable ATS polling** — Update backend to poll ATS APIs directly for companies with configs.

### Medium-term

11. ⬜ **Workday integration investigation** — Workday is now 13.7% of companies; determine if their job data API is feasible.
12. ⬜ **SmartRecruiters / Workable integration** — Assess the viability of pulling data from these platforms which make up a combined 8.2% of companies.

---

## 8. Migration Scripts

### Script 1: Migrate from BigQuery

**File:** `scripts/ats/migrate_to_declarative.py`

Use this if ATS configs are already in BigQuery `ats_configs` table.

```bash
# Prerequisites
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json

# Run migration
python3 scripts/ats/migrate_to_declarative.py

# Validate
python3 scripts/ats/validate_ats_configs.py
```

**What it does:**
1. Exports all `ats_configs` from BigQuery
2. Updates corresponding company JSON files
3. Preserves operational state (`enabled`, `last_synced_at`) in BigQuery
4. After deploy, `CompanySyncService` syncs manifest definitions to BigQuery

### Script 2: Add from Findings Document

**File:** `scripts/ats/add_from_findings.py`

Use this to add ATS data from the identification findings document.

```bash
# Run migration
python3 scripts/ats/add_from_findings.py

# Validate
python3 scripts/ats/validate_ats_configs.py
```

**What it does:**
1. Reads hardcoded ATS identification data from findings
2. Adds ATS configs to company JSON files
3. Skips companies without identifiers
4. Validates provider/identifier format

**Data included:**
- 21 companies with verified API tokens (Greenhouse, Lever, Ashby)
- 30+ companies with identified ATS (Workday, BambooHR, etc.)
- Total: ~50 companies with ATS configs

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

---

## 10. Strategy for Market Reliability (Next Steps)

To ensure we remain the **most reliable and accurate source of job information on the market**, we must shift focus from *discovery* to *data quality and freshness*.

### 1. High-Frequency Real-Time Syncing (The "Moat")
Traditional job boards lag because they rely on daily XML feeds or slow scrapers. By integrating directly with the ATS APIs (Greenhouse, Lever, Ashby, Workday), we bypass the delay.
**Action:** Configure the backend `JobSyncTimer` to poll these Tier 1 APIs every 1–2 hours. When a company posts a job, it should appear on our platform before it even hits LinkedIn.

### 2. "Ghost Job" & Dead Link Detection
One of the biggest frustrations for candidates is applying to a job that was already filled. 
**Action:** Implement a daily health-check worker that sends a `HEAD` request to every active `applyUrl` in our database. If an ATS returns a 404 (or redirects to a generic careers page), immediately mark the job as `CLOSED` in our system.

### 3. Deep Data Enrichment (AI Classification)
Simply having the job is not enough; the data must be highly filterable. Many ATS postings lack structured metadata for "Seniority" or "Tech Stack".
**Action:** Expand the `TechRoleClassifier` (and potentially introduce an LLM pass) to aggressively extract:
- **Seniority:** Junior, Mid, Senior, Staff, Principal.
- **Tech Stack:** (e.g., React, Kotlin, AWS) mapping to our internal `tech Resources` tags.
- **Salary:** Pluck salary bands directly from the job description if the ATS doesn't provide structured fields (like Ashby does).

### 4. Robust Fallback for "NONE" Companies
For the 49.5% of companies relying on LinkedIn Easy Apply, we cannot use direct APIs.
**Action:** 
- Keep the Apify/LinkedIn scraper highly maintained. Set up alerting if the number of scraped jobs drops by >20% in a week, indicating a LinkedIn DOM change.
- Run the scraper daily to ensure parity with LinkedIn.

### 5. Community Sourcing & Reporting
No automated system is perfect. Startups appear overnight, and companies change ATS providers.
**Action:** 
- Add a "Submit a Job" feature to the UI so users can manually link a careers page we missed.
- Add a "Report this Job" button on the job details UI so users can flag incorrect formatting, filled roles, or scam postings, triggering a manual review.

### 6. Multi-Channel Sourcing (Seek & Trade Me)
Relying solely on LinkedIn and direct ATS integrations leaves a significant blind spot. While LinkedIn is highly competitive for tech and corporate roles, **SEEK** completely dominates the overall ANZ job board market (capturing ~90% of job seeker time), and **Trade Me Jobs** holds a massive share of the New Zealand market. 
*   **Market Share Gap:** We estimate that we are currently missing **50-60%** of the total addressable tech job market. Many government organizations, established domestic enterprises, and non-tech-first companies post *exclusively* to Seek or Trade Me and do not use LinkedIn. If we have ~450 tech jobs now, extending to Seek and Trade Me could rapidly expand our database to 1,000+ active roles.
**Action:**
- **Phase C Sourcing:** Develop dedicated Apify scrapers (or direct API integrations if accessible) for both Seek (AU/NZ) and Trade Me Jobs (NZ), filtering strictly for our defined Tech/IT categories.
- Deduplicate these listings against our existing LinkedIn/ATS data using company name, job title, and location to avoid double-counting.

---

## 11. Declarative ATS Configuration (New Strategy)

With the migration to a directory-based manifest (`data/companies/*.json`), we are moving ATS technical configuration into the company manifest files. This allows us to track "Definition" in version control while keeping "Operational State" in the database.

### Data Architecture

1.  **Definition (in `data/companies/{id}.json`)**:
    Store the immutable technical identifier and provider.
    ```json
    "ats": {
      "provider": "GREENHOUSE",
      "identifier": "xero"
    }
    ```
    
    **Note:** The `enabled` field is intentionally NOT stored in the manifest. This allows operational control (e.g., disabling a broken integration) without requiring a code commit.

2.  **Operational State (in BigQuery `ats_configs`)**:
    The database remains the source of truth for dynamic metadata:
    - `provider`: Synced from manifest (immutable)
    - `identifier`: Synced from manifest (immutable)
    - `enabled`: Operational control (DB only)
    - `last_synced_at`: When the last API poll occurred
    - `sync_status`: Current health of the integration (`SUCCESS`, `FAILED`)
    - `error_message`: Track why disabled (if applicable)

### The Sync Flow

The `CompanySyncService` performs "Declarative Provisioning". Every time it syncs a company from the manifest:

```kotlin
fun syncFromManifest() {
    manifestCompanies.forEach { company ->
        val existingConfig = repository.getConfig(company.id)
        
        if (company.ats != null) {
            if (existingConfig == null) {
                // NEW integration - create enabled by default
                repository.insert(company.ats.copy(enabled = true))
            } else {
                // EXISTING integration - update config, preserve DB enabled state
                repository.update(
                    company.ats.copy(
                        enabled = existingConfig.enabled,  // Preserve operational state
                        lastSyncedAt = existingConfig.lastSyncedAt,
                        syncStatus = existingConfig.syncStatus
                    )
                )
            }
        }
    }
}
```

This ensures that:
- Adding a new ATS integration is as simple as adding the `ats` block to a company's JSON file
- Disabling a broken integration in the DB persists across manifest syncs
- Git provides the audit trail for who added/changed configurations

### JSON Schema Validation

To prevent invalid configurations from being merged, add ATS validation to `data/companies/schema.json`:

```json
"ats": {
  "type": "object",
  "description": "ATS integration configuration",
  "required": ["provider", "identifier"],
  "properties": {
    "provider": {
      "type": "string",
      "enum": [
        "GREENHOUSE",
        "LEVER",
        "ASHBY",
        "WORKDAY",
        "SNAPHIRE",
        "BAMBOOHR",
        "TEAMTAILOR",
        "WORKABLE",
        "SMARTRECRUITERS",
        "JOBADDER",
        "EMPLOYMENT_HERO",
        "SUCCESSFACTORS"
      ]
    },
    "identifier": {
      "type": "string",
      "minLength": 1,
      "maxLength": 100,
      "pattern": "^[a-zA-Z0-9_-]+$"
    }
  },
  "additionalProperties": false
}
```

### Validation Script

Create `scripts/ats/validate_ats_configs.py` to validate ATS configurations before merging:

```python
#!/usr/bin/env python3
"""
Validate ATS configurations in company manifest files.

Checks:
- Provider is from allowed list
- Identifier matches expected pattern
- Provider/identifier combination is valid (via API for public endpoints)
"""

import json
from pathlib import Path
import requests

MANIFEST_DIR = Path('data/companies')

PROVIDER_API_ENDPOINTS = {
    'GREENHOUSE': 'https://boards-api.greenhouse.io/v1/boards/{id}/jobs',
    'LEVER': 'https://api.lever.co/v0/postings/{id}?limit=1',
    'ASHBY': 'https://api.ashbyhq.com/posting-api/job-board/{id}'
}

def validate_ats_config(company_file: Path) -> list[str]:
    """Validate ATS config for a single company."""
    errors = []
    
    with open(company_file, 'r') as f:
        company = json.load(f)
    
    ats = company.get('ats')
    if not ats:
        return errors  # ATS config is optional
    
    # Validate provider
    provider = ats.get('provider')
    if provider not in PROVIDER_API_ENDPOINTS:
        errors.append(f"{company_file.name}: Unknown ATS provider '{provider}'")
    
    # Validate identifier format
    identifier = ats.get('identifier')
    if not identifier or len(identifier) > 100:
        errors.append(f"{company_file.name}: Invalid identifier '{identifier}'")
    
    # Test API (for providers with public APIs)
    if provider in PROVIDER_API_ENDPOINTS:
        url = PROVIDER_API_ENDPOINTS[provider].format(id=identifier)
        try:
            response = requests.get(url, timeout=5)
            if response.status_code == 404:
                errors.append(f"{company_file.name}: Invalid {provider} identifier '{identifier}' (404)")
            elif response.status_code != 200:
                errors.append(f"{company_file.name}: API error for {provider} '{identifier}': {response.status_code}")
        except requests.Timeout:
            errors.append(f"{company_file.name}: Timeout testing {provider} identifier '{identifier}'")
    
    return errors
```

**Usage:**
```bash
# Validate all ATS configs
python3 scripts/ats/validate_ats_configs.py

# Add to CI/CD (GitHub Actions)
- name: Validate ATS configs
  run: python3 scripts/ats/validate_ats_configs.py
```

### Migration from Existing Data

For existing ATS configurations in BigQuery, migrate them to the manifest:

```bash
# Run migration script
python3 scripts/ats/migrate_to_declarative.py

# Review git diff
git diff data/companies/

# Validate migrated configs
python3 scripts/ats/validate_ats_configs.py

# Commit and deploy
git commit -m "Migrate ATS configs to declarative manifest"
```

The migration script:
1. Exports all `ats_configs` from BigQuery
2. Updates corresponding company JSON files
3. Preserves operational state (`enabled`, `last_synced_at`) in BigQuery
4. After deploy, `CompanySyncService` syncs manifest definitions to BigQuery

### Security Note: API Credentials

Some ATS providers (JobAdder, Employment Hero, SnapHire) require OAuth credentials. These must **NEVER** be stored in Git.

**Recommended approach (future enhancement):**
- Store credentials in GCP Secret Manager
- Backend loads credentials at runtime based on provider/company
- Manifest contains only public identifier (safe for Git)

For now, focus on providers with public APIs (Greenhouse, Lever, Ashby) that don't require credentials.

---

## 12. Documents & References

| Document | Purpose |
|:---|:---|
| `ideas/ats-identification-findings.md` | Master Roster with all 182 companies, ATS provider, identifier, and validation status |
| `ideas/ats-integration.md` | Full ATS integration strategy (API details, architecture, data mapping) |
| `scripts/ats/` | Validation and data conversion scripts |
