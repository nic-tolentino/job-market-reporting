Our goal is to decouple **Company Identity** from **Job Postings**. We will treat companies as first-class citizens with a single, highly-curated source of truth.

---

## 🟢 Current Implementation Status (March 2026)

| Feature | Status | Implementation Details |
| :--- | :--- | :--- |
| **Master JSON Manifest** | ✅ Implemented | `data/companies.json` contains curated "Gold" and "Silver" companies. |
| **Two-Phase Sync** | ✅ Implemented | `JobDataSyncService` refreshes companies from Master Manifest before processing jobs. |
| **Ghost Fallback** | ✅ Implemented | `RawJobDataMapper` creates `ghost-` IDs for unmapped companies. |
| **Curated Metadata** | ✅ Implemented | Fields like `isAgency`, `hqCountry`, and `visaSponsorship` are live. |
| **Alias Resolution** | ✅ Implemented | `findCompanyId` matches against `alternateNames` in the manifest. |
| **Stable Asset Hosting** | 🟡 In Progress | Basic BQ persistence is live; local CDN fetcher is pending. |
| **AI Enrichment Loop** | 🟡 In Progress | Bootstrapping script exists; automated ghost enrichment is next. |

---

---

## 1. Ensuring Completeness and High Quality

### The "Master Manifest" vs. The "Scraped" Layer
Currently, our `RawJobDataMapper` creates company records based on whatever data it can extract from a job posting. While this is great for discovery, it is noisy and represents the **Bronze Layer**.
We have introduced a **Master Manifest** (`companies.json`) that provides structured metadata which overrides scraped data, moving companies into the **Silver** or **Gold** layers.

*   **Bronze Layer (Scraped):** Discovers new companies ("Ghosts") and provides temporary data (name, logo, description) to get them on the board immediately.
*   **Silver Layer (AI Enriched):** AI-enriched data (via `enrich_ghosts.py`) that adds depth to Ghost records. These records live in the Master Manifest but pending human audit.
*   **Gold Layer (Human Curated):** Definitive data that locks in official names, high-res logos, and specific tags. These are the "Verified" records in the Master Manifest.

### Verification Levels (Medallion Mapping)
To manage trust at scale, we assign every company a `verification_level`:
1.  **`verified` (Gold):** Manually reviewed by the core team. Clean, perfect data.
2.  **`community_verified` (Gold):** Validated by a trusted community member.
3.  **`silver` (Silver):** AI-enriched data based on job descriptions and external APIs.
4.  **`ghost` (Bronze):** Minimal data extracted directly from a job board or ATS. Low confidence.
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

#### 1. Seeding the Initial Master List (One-Off Process)
We already have hundreds of unique companies residing in the BigQuery `jobs` data. We will extract them to establish our baseline:
*   **The BigQuery Extract:** We will run a one-off query (e.g., `SELECT DISTINCT companyName, logoUrl FROM techmarket.jobs`) to dump every company we've encountered so far.
*   **The Bootstrapping Script:** We will build a temporary local script (`scripts/companies/bootstrap-companies.ts` or a Kotlin equivalent). This script will loop through the CSV extract and frame it into our new JSON schema. It will slugify the name for the `id` and seed the `alternateNames` array.
*   **LLM First-Pass Enrichment:** To save hours of manual data entry, the bootstrapping script will ping a local LLM or API (like Gemini or Claude) with each company name, prompting it to intelligently guess the `industries`, `company_type`, `is_agency` flag, and `hq_country`.
*   **Manual Review:** We will manually review the generated `data/companies.json` file, correct any LLM hallucinations, and commit it to the repository as part of the Master Manifest.

#### 2. Handling Unmapped Discoveries (The Real-Time Fallback)
Once the system is live, the Apify scrapers will eventually ingest a job from a brand new startup that does not yet exist in `data/companies.json`. The pipeline must gracefully handle this without crashing or dropping the job:
1.  **The "Ghost" Company Creation:** During Phase 2 (Job Processing) of the sync, if `JobMapper` cannot find a match for the scraped company name in the BigQuery `companies` table (which represents our master JSON), it will automatically generate a transitional "Ghost" company record.
2.  **Ghost Characteristics:** A Ghost record is sparse. It has an auto-generated ID (e.g., `ghost-scraped-name`), the scraped name, and whatever `logoUrl` was present in the API payload. These are inserted into BigQuery on the fly.
3.  **Job Linking:** The new jobs are immediately linked to this Ghost company. This ensures that job postings are instantly visible to candidates, even if the parent company lacks an industry classification.

#### 3. The Continuous Enhancement Loop (Ongoing Updates)
We do not want to hand-write JSON entries every time a new startup appears. We will automate the curation loop.
*   **The Discovery Worker:** We have created a local utility script `scripts/companies/enrich_ghosts.py` that runs asynchronously from the core application. You can execute this script on an ad-hoc basis whenever there are many ghost companies.
*   **Execution Requires API Key:** Running the script locally requires you to provide the `GEMINI_API_KEY` environment variable. (e.g. `export GEMINI_API_KEY="your-key" && python3 scripts/companies/enrich_ghosts.py`).
*   **The Workflow:**
    1.  **Query:** The script queries our BigQuery production database for all "Ghost" companies (`verificationLevel = 'ghost'`).
    2.  **Context Building:** The script pulls recent job descriptions associated with each Ghost company to provide context to the LLM.
    3.  **AI Enrichment:** It prompts the Google Gemini API to analyze the company metadata, explicitly extracting: `name`, `alternateNames`, `description`, `company_type`, `is_agency`, `hq_country`, `industries`, and `employees_count`.
    4.  **Append:** The script appends completed structured `silver` tier JSON objects for these new companies to the local `data/companies.json` manifest and automatically sorts the file alphabetically.
*   **The Merge:** A developer opens a Pull Request with the updated JSON file. Because it's just JSON, it's incredibly easy to review the diff to catch LLM mistakes. Once merged, the backend loads the new file on startup (or via sync payload), and the sparse "Ghost" records in production are overwritten with definitive "Silver" records.

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
