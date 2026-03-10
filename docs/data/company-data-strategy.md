# Company Data Strategy

Our goal is to decouple **Company Identity** from **Job Postings**. We will treat companies as first-class citizens with a single, highly-curated source of truth.

---

## 🟢 Current Implementation Status (March 2026)

| Feature | Status | Implementation Details |
| :--- | :--- | :--- |
| **Directory-Based Manifest** | ✅ **COMPLETE** | `data/companies/*.json` - 80+ individual company files |
| **JSON Schema Validation** | ✅ **COMPLETE** | `data/companies/schema.json` with full validation |
| **Validation Scripts** | ✅ **COMPLETE** | `validate_all.py`, `format_all.py`, `check_duplicates.py` |
| **Backend Directory Reader** | ✅ **COMPLETE** | `CompanySyncService` reads from directory |
| **AI Enrichment Loop** | ✅ **COMPLETE** | `enrich_unverified_companies.py` writes individual files |
| **Pre-commit Hooks** | ✅ **COMPLETE** | `.pre-commit-config.yaml` with validation |
| **GitHub Actions CI** | ✅ **COMPLETE** | Validates on every PR |
| **Contributor Guidelines** | ✅ **COMPLETE** | `.github/CONTRIBUTING.md` |
| **Two-Phase Sync** | ✅ **COMPLETE** | `JobDataSyncService` refreshes companies before jobs |
| **Unverified Fallback** | ✅ **COMPLETE** | `RawJobDataMapper` creates `unverified` records |
| **Curated Metadata** | ✅ **COMPLETE** | Fields like `isAgency`, `hqCountry`, `visaSponsorship` |
| **Alias Resolution** | ✅ **COMPLETE** | `findCompanyId` matches against `alternateNames` |
| **Stable Asset Hosting** | 🟡 In Progress | Basic BQ persistence; CDN fetcher pending |
| **ATS Configuration** | 🟡 In Progress | Declarative ATS config in company files |

---

## 1. Current Architecture

### Directory-Based Manifest (Implemented March 2026)

The company manifest uses a **directory-based structure** where each company has its own JSON file:

```
data/companies/
├── schema.json              # JSON Schema for validation
├── README.md                # Documentation
├── xero.json               # Individual company file
├── canva.json
├── rocket-lab.json
└── ... (80+ files)
```

**Benefits:**
- Zero merge conflicts for parallel work
- Safer edits (one file = one company)
- Clean, reviewable diffs
- Easier to maintain and extend

### Validation System

**Multi-layer validation:**

1. **JSON Schema** (`schema.json`) - Enforces structure at file level
2. **Pre-commit hooks** - Catch errors before commit
3. **GitHub Actions CI** - Blocks invalid PRs
4. **Validation scripts** - Manual validation anytime

**Validation checks:**
- Required fields present
- ID matches filename (case-sensitive)
- Lowercase enforcement (cross-platform compatibility)
- Valid country codes (ISO 3166-1 alpha-2)
- Valid verification levels
- No duplicate companies
- Proper formatting (2-space indent, trailing newline)

### Sync Flow

```
┌─────────────────────┐
│  data/companies/    │
│  (*.json files)     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ CompanySyncService  │
│  - Reads directory  │
│  - Parses JSON      │
│  - Validates        │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ BigQuery            │
│ raw_companies table │
└─────────────────────┘
```

**Backend Implementation:**
- `CompanySyncService.syncFromManifest()` - Reads all JSON files from directory
- Parallel parsing with coroutines (5-7x faster than sequential)
- Graceful error handling (logs filename if JSON parse fails)
- ID/filename parity validation

---

## 2. Company Data Model

### Canonical Schema

Each company JSON file follows this schema:

```json
{
  "id": "xero",
  "name": "Xero",
  "alternateNames": ["Xero Limited", "Xero Inc"],
  "description": "Xero is a global small business platform...",
  "website": "https://www.xero.com",
  "logoUrl": "https://...",
  
  "industries": ["Fintech", "SaaS", "Accounting"],
  "company_type": "Product",
  "is_agency": false,
  "is_social_enterprise": false,
  
  "hq_country": "NZ",
  "operating_countries": ["NZ", "AU", "US", "UK"],
  "office_locations": ["Auckland", "Wellington", "Sydney", "London"],
  
  "remote_policy": "Hybrid",
  "visa_sponsorship": true,
  "employees_count": 5000,
  
  "verification_level": "verified",
  "updated_at": "2026-03-10T00:00:00Z",
  
  "ats": {
    "provider": "GREENHOUSE",
    "identifier": "xero"
  }
}
```

### Field Rationale

- **`id`**: Immutable primary key (slug format). **Must match filename exactly.**
- **`alternateNames`**: Crucial for matching dirty job board data
- **`hq_country`**: ISO 3166-1 alpha-2 code (e.g., `NZ`, `AU`)
- **`verification_level`**: Trust tier (verified, silver, unverified, etc.)
- **`ats`**: Optional ATS integration config (provider + identifier)

---

## 3. Verification Levels (Medallion Architecture)

| Level | Description | Source |
|-------|-------------|--------|
| **verified** | Manually reviewed by core team | Human curation |
| **community_verified** | Validated by trusted community member | Community PR |
| **silver** | AI-enriched data | `enrich_unverified_companies.py` |
| **unverified** | Auto-discovered from job postings | Job ingestion |
| **needs_review** | Flagged for manual audit | Validation/flags |
| **blocked** | Spam/scam (filters out jobs) | Manual block |

---

## 4. Handling Unmapped Discoveries

When a new company is discovered (not in manifest):

1. **Unverified Company Creation**: `RawJobDataMapper` creates `unverified` record
2. **Sparse Data**: Auto-generated slug ID, scraped name, logo URL
3. **Jobs Linked Immediately**: Job postings visible even without full company profile
4. **AI Enrichment Queue**: Added to queue for `enrich_unverified_companies.py`

### AI Enrichment Workflow

```bash
# 1. Setup environment
export GEMINI_API_KEY="your-key-here"

# 2. Run enrichment script
cd scripts/companies
python3 enrich_unverified_companies.py

# 3. Review changes
git diff data/companies/

# 4. Validate
python3 validate_all.py

# 5. Commit and sync
git commit -m "Enrich 5 unverified companies"
curl -X POST https://api.devassembly.org/api/admin/sync-companies
```

**What the script does:**
- Queries BigQuery for `unverified` companies
- Pulls recent job descriptions for context
- Uses Gemini to generate structured metadata
- Saves individual JSON files with `silver` verification

---

## 5. Handling Multi-National Companies

### Data Modeling

Separate `hq_country` from `operating_countries`:

```json
{
  "hq_country": "US",
  "operating_countries": ["US", "NZ", "AU"],
  "office_locations": ["San Francisco", "Auckland", "Sydney"]
}
```

### UI/UX Implications

- **Local Company**: `hq_country === selectedCountry`
- **International**: `hq_country !== selectedCountry` but `selectedCountry in operating_countries`

### The ".nz" Litmus Test

Companies without `.nz` domains often have non-NZ HQ despite NZ presence:
- **Cin7**: US-headquartered (despite NZ origin)
- **Rocket Lab**: US-headquartered (despite NZ operations)

---

## 6. ATS Integration (Declarative Configuration)

### Data Architecture

**Definition (in Git):**
```json
"ats": {
  "provider": "GREENHOUSE",
  "identifier": "xero"
}
```

**Operational State (in BigQuery):**
- `enabled`: Operational control (DB only)
- `last_synced_at`: Last API poll
- `sync_status`: `SUCCESS`, `FAILED`
- `error_message`: Why disabled (if applicable)

### Sync Flow

```kotlin
fun syncFromManifest() {
    manifestCompanies.forEach { company ->
        val existingConfig = repository.getConfig(company.id)
        
        if (company.ats != null) {
            if (existingConfig == null) {
                // NEW integration - enable by default
                repository.insert(company.ats.copy(enabled = true))
            } else {
                // EXISTING - preserve DB enabled state
                repository.update(
                    company.ats.copy(
                        enabled = existingConfig.enabled
                    )
                )
            }
        }
    }
}
```

### Validation

```bash
# Validate ATS configs
python3 scripts/ats/validate_ats_configs.py

# Tests public APIs (Greenhouse, Lever, Ashby)
# Validates provider/identifier format
```

---

## 7. Further Enhancements

### Implemented ✅
- Directory-based manifest
- JSON Schema validation
- Pre-commit hooks
- GitHub Actions CI
- Contributor guidelines
- AI enrichment loop
- Backend directory reader

### In Progress 🟡
- Stable asset hosting (CDN for logos)
- ATS integration rollout

### Future Enhancements 💡
- **Per-company supplemental files**: `xero.logo.svg`, `xero.bio.md`
- **Automated company discovery**: Clearbit/Apollo API integration
- **Community reviews**: Glassdoor/Blind links
- **Verified badges**: Blue checkmark for curated companies
- **Remote-from-NZ tag**: AU companies hiring remote in NZ

---

## 8. Contributing

See [CONTRIBUTING.md](../../.github/CONTRIBUTING.md) for:
- Quick start guide
- Company file structure
- Validation instructions
- Common issues
- Review process

---

**Last Updated:** March 10, 2026

### The "Master Manifest" vs. The "Scraped" Layer
Currently, our `RawJobDataMapper` creates company records based on whatever data it can extract from a job posting. While this is great for discovery, it is noisy and represents the **Bronze Layer** (Unverified).
We have introduced a **Master Manifest** (`companies.json`) that provides structured metadata which overrides scraped data, moving companies into the **Silver** or **Gold** layers.

*   **Bronze Layer (Scraped):** Discovers new companies ("Unverified") and provides temporary data (name, logo, description) to get them on the board immediately.
*   **Silver Layer (AI Enriched):** AI-enriched data (via `enrich_unverified_companies.py`) that adds depth to Unverified records. These records live in the Master Manifest but are pending final human audit.
*   **Gold Layer (Human Curated):** Definitive data that locks in official names, high-res logos, and specific tags. These are the "Verified" records in the Master Manifest.

### Verification Levels (Medallion Mapping)
To manage trust at scale, we assign every company a `verification_level`:
1.  **`verified` (Gold):** Manually reviewed by the core team. Clean, perfect data.
2.  **`community_verified` (Gold):** Validated by a trusted community member.
3.  **`silver` (Silver):** AI-enriched data based on job descriptions and external APIs.
4.  **`unverified` (Bronze):** Automatically discovered companies from a job board or ATS. Low confidence.
5.  **`stale` / `needs_review`:** Previously verified/silver, but flagged for manual audit.
6.  **`blocked` (Spam Filter):** Specifically for recruitment holding companies or scam listings. A `blocked` status forces the pipeline to filter out any associated jobs.

### Stable Asset Hosting
*   **The Problem:** Currently, we rely on third-party URLs (like LinkedIn media links) for `logoUrl`. These URLs frequently expire, change, or return 404s, resulting in broken UIs.
*   **The Solution:** Whenever a new company is discovered, the backend should download the logo, optimize/compress it, and host it on our own CDN (e.g., Vercel Blob, AWS S3, or Cloudflare R2). The database should point to our stable CDN URL.

### AI-Assisted Enrichment (The "Company Profiler")
As outlined in the Job Data Strategy, we will build a worker to ping a lightweight LLM or data API (like Clearbit/Apollo) for newly discovered companies to determine:
1.  **Industry categorization:** (e.g., Fintech, Healthtech, Agritech)
2.  **Company Type:** Product, Consultancy, Agency, Government.
3.  **Social Enterprise/B-Corp status:** Identifying "Tech for Good".

---

## 2. The Master JSON Manifest & Offline Enhancement

Since job data alone is often extremely noisy or incomplete (missing official logos, HQ locations, and industry classifications), we cannot rely on scraping to build complete profiles on the fly.

Instead, we will decouple company data generation from live job synchronization.

### The "Single File" Schema
We will construct a master, sorted JSON file (e.g., `data/companies.json`). The structure of each company object needs to be exhaustive enough to support all current and future dashboard features.

Here is the proposed canonical schema and the rationale for each field:

```json
[
  {
    "id": "xero",
    "name": "Xero",
    "alternateNames": ["Xero Limited", "Xero Inc", "Xero NZ"],
    "description": "Xero is a global small business platform...",
    "website": "https://www.xero.com",
    "logoUrl": "https://static.devassembly.org/logos/xero.png",
    
    // Core Categorization
    "industries": ["Fintech", "SaaS", "Accounting"],
    "company_type": "Product", // e.g., Product, Agency, Consultancy, Government
    "is_agency": false,
    "is_social_enterprise": false,
    
    // Location & Multinational tracking
    "hq_country": "NZ",
    "operating_countries": ["NZ", "AU", "US", "UK"],
    "office_locations": ["Auckland", "Wellington", "Sydney", "London"],
    
    // Employee Perks & Policies (Hard to scrape, high candidate value)
    "remote_policy": "Hybrid", // e.g., Remote Only, Hybrid, On-site
    "visa_sponsorship": true,
    
    // Internal Metadata
    "updated_at": "2026-03-05T00:00:00Z"
  }
]
```

#### Key Field Rationale:
*   `id`: This is the immutable primary key (slug format). It is what we use as the `companyId` in the `jobs` database table to link a job to a company.
*   `alternateNames`: **(Crucial)** Job boards are notoriously dirty. A single company might be listed as "ANZ", "ANZ Bank", or "ANZ Banking Group Limited" across different job postings. When the backend parses a job, it will check the job's stated company name against *both* the `name` and the `alternateNames` array in this JSON to correctly assign it to the `anz` ID.
*   `logoUrl`: We will move away from hotlinking LinkedIn images. We will download high-res SVGs/PNGs, store them in our own stable storage bucket, and link them here to prevent link rot.
*   `company_type` & `is_agency`: These are the core filters that allow us to instantly discard jobs during Phase 2 of the sync if the user doesn't want to see recruiter spam.
*   `operating_countries` & `office_locations`: Provides precise geographic tracking. We use countries for high-level market filtering (e.g., "Show me local NZ companies") and specific office cities for candidate relevance.

> [!NOTE]
> **Implementation Note:** The backend `CompanySyncService` now automatically populates the `raw_companies` table from this JSON on every sync run, ensuring the production environment stays updated with the latest curated data.

### Implementation Guide: Bootstrapping & Maintaining the Manifest

To transition to the Master JSON system, we need a reliable way to seed the initial data and seamlessly add newly discovered companies without slowing down the real-time job ingestion pipeline.

#### 1. Seeding the Initial Master List (Complete)
The initial manifest was seeded by extracting unique companies from historical job data. This one-off process established our baseline of ~2,000 companies. New companies are now added via the automated enrichment loop described below.

#### 2. Handling Unmapped Discoveries (The Real-Time Fallback)
Once the system is live, the scrapers will eventually ingest a job from a brand new company that does not yet exist in `data/companies.json`. The pipeline must gracefully handle this:
1.  **Unverified Company Creation:** During Phase 2 (Job Processing) of the sync, if `RawJobDataMapper` cannot find a match for the scraped company name in the Master Manifest, it will automatically generate a transitional `unverified` company record in BigQuery.
2.  **Unverified Characteristics:** An unverified record is sparse. It has an auto-generated slug ID, the scraped name, and whatever `logoUrl` was present in the API payload. 
3.  **Job Linking:** The new jobs are immediately linked to this unverified company record. This ensures that job postings are instantly visible, even if the parent company lacks metadata.

#### 3. The Continuous Enhancement Loop (AI Enrichment)
We do not want to hand-write JSON entries every time a new startup appears. We automate the curation loop using Gemini.

### 🤖 Guide: Processing Unverified Companies
When the "New Discovery" count grows (viewable in the Admin Dashboard), follow these steps to promote companies to the Silver/Gold tiers:

1.  **Setup Environment**: Ensure you have a valid Google Gemini API Key.
    ```bash
    export GEMINI_API_KEY="your-key-here"
    ```
2.  **Run Enrichment Script**: Execute the Python enrichment utility from the scripts directory.
    ```bash
    # From project root
    cd scripts/companies
    python3 enrich_unverified_companies.py
    ```
3.  **What the Script Does**:
    - Queries BigQuery for all companies with `verificationLevel = 'unverified'`.
    - Pulls recent job descriptions for context.
    - Prompts Gemini to generate structured metadata (Type, Industries, HQ, etc.).
    - Appends the new records to `data/companies.json` with `verification_level: "silver"`.
4.  **Review and Commit**:
    - Open `data/companies.json` to verify the AI's work (especially `hq_country` and `is_agency`).
    - Commit the updated manifest file to the repository.
5.  **Sync to Production**:
    - Once merged/pushed, trigger the manual sync via the Admin API:
    ```bash
    curl -X POST http://api.devassembly.org/api/admin/sync-companies \
         -H "x-apify-signature: your-secret"
    ```

### The Server Sync Pipeline
Rather than the backend trying to decipher company metadata dynamically while parsing jobs, the backend will treat the `companies.json` file as Gold.

**Phase 1: Company Sync (The Foundation)**
*   On startup, or triggered via the Admin API (`POST /api/admin/sync-companies`), the backend reads the latest version of `companies.json`.
*   It performs a full refresh of the `raw_companies` table, ensuring the table schema matches the current DTOs.

**Phase 2: Job Processing (The Consumers)**
*   When job payloads are fetched, the `RawJobDataMapper` lookups metadata against the newly refreshed companies.
*   Because the company data is already fully populated and stable, the job mapper can make highly accurate decisions—such as discarding a job entirely if the parent company is `VerificationLevel.BLOCKED`.

---

## 3. Handling Multi-National Companies

A major challenge is distinguishing between a local startup and a massive multinational with a local branch. Candidates often want to filter by "Local/Domestic Companies".

### Data Modeling 
We must separate `hq_country` from `operating_countries`. 
*   **Local Company:** `hq_country: "NZ"`, `operating_countries: ["NZ"]`, `office_locations: ["Auckland"]`
*   **Local Success Story (Expanding):** `hq_country: "NZ"`, `operating_countries: ["NZ", "AU", "US"]`, `office_locations: ["Auckland", "Sydney"]`
*   **International Presence:** `hq_country: "US"`, `operating_countries: ["US", "NZ", "AU"]`, `office_locations: ["San Francisco", "Auckland"]`

### UI/UX Implication
On the frontend, if a user is viewing the New Zealand market, a company with `hq_country: "US"` but `operating_countries: ["NZ"]` should be badged as **"International"**. A company with `hq_country: "NZ"` should be badged as **"NZ Local"**.

### The ".nz" Litmus Test
During audit passes, special attention is paid to companies without a `.nz` domain. While an international-looking domain doesn't disqualify a company from being curated, it often indicates a non-NZ HQ (e.g., **Cin7** and **Rocket Lab** are US-headquartered despite significant NZ presence and origin).

---

## 4. Further Suggestions & Enhancements

*   **The "Remote-from-NZ" (AU Wide) Tag:** Many AU-headquartered companies hire NZ-based developers as fully remote contractors. By modeling multi-national operations correctly, we can create a dedicated filter for "AU Companies hiring Remote in NZ", which is highly sought after for salary arbitrage.
*   **Sponsorship / Visa Matrix:** Add a boolean or notes field for `provides_visa_sponsorship`. This is one of the most requested features by migrant workers, and is completely missing from SEEK/LinkedIn.
*   **"Verified" Badges:** Companies whose JSON profiles have been manually reviewed and merged in the open-source repo get a blue checkmark or "Verified" badge on the platform, increasing candidate trust.
*   **Community Reviews (External Links):** In the JSON files, allow contributors to link to Glassdoor, Blind, or local forum discussions about the company's engineering culture.
*   **Company Merging (Alias Resolution):** Job boards are notorious for duplicate names (e.g., "ASB", "ASB Bank", "ASB Bank Limited"). Our backend must have a robust alias resolution system. The open-source JSON `alternateNames` array will be the primary mechanism for the backend to know that these three distinct scraped names should all roll up into the single `asb-bank` company ID.

---

## 5. Migration Plan: Directory-Based Manifest (Q2 2026)

As the number of curated companies grows beyond 300, maintaining a single `companies.json` file becomes a bottleneck for collaboration and increases the risk of merge conflicts.

### The Objective
Move from `data/companies.json` to a directory structure where each company has its own file: `data/companies/{company-id}.json`.

### Advantages
1.  **Zero Merge Conflicts**: Multiple agents/contributors can work on different companies simultaneously.
2.  **Safety**: A syntax error in one file only affects one company, not the entire manifest.
3.  **Clean Diffs**: PRs will show exactly which companies were added or modified.
4.  **Extensibility**: Allows attaching supplemental metadata (like local markdown bios) in the same directory.

### Migration Steps

#### Phase 1: Infrastructure Preparation
*   Update `CompanySyncService` in the backend to scan the `data/companies/` directory. It should recursively find all `.json` files and aggregate them into the sync list.
*   Update `enrich_unverified_companies.py` script to identify the target directory and write individual files.

#### Phase 2: Content Migration
*   Run a one-off migration script to split the existing `companies.json` into 180+ individual files.
*   Verify the count and integrity of the records in the new directory.

#### Phase 3: Cutover
*   Delete the legacy `data/companies.json` file.
*   Update project documentation and CI checks (if any) to point to the new directory structure.

### New Directory Structure
```text
data/
  companies/
    xero.json
    canva.json
    rocket-lab.json
    ...
```
