# Job Market Reporting - Backend (BFF)

This project is a Spring Boot application designed as a Backend-for-Frontend (BFF) to provide parsed and structured job market insights from LinkedIn web-scrapes.

## 🚀 Architecture: Bronze to Silver

The system follows a simplified Medallion Architecture to ensure data integrity and reprocessability.

### 1. Bronze Layer (`raw_ingestions` table)
*   **Source of Truth**: Stores the exact JSON payloads received from the Apify LinkedIn scraper.
*   **Immutable**: Data in this layer is never modified or deleted. 
*   **Reprocessable**: If we change our technology extraction logic or deduplication rules, we can wipe the Silver layer and re-run the entire pipeline from these raw records.

### 2. Silver Layer (`raw_jobs`, `raw_companies` tables)
*   **Structured & Cleaned**: Data is parsed into typed Kotlin objects (`JobRecord`, `CompanyRecord`).
*   **Deduplicated**: Multiple postings for the same logical role (e.g., the same title at the same company across different cities) are collapsed into a single entity.
*   **Enriched**: Locations are normalized, and technologies are extracted from the free-text descriptions.

---

## 🏗️ Main Data Flow

### Data Sync Pipeline (`JobDataSyncService.runDataSync`)
Triggered via webhook or manual admin call.
1.  **Fetch**: Pulls recent results from Apify.
2.  **Bronze Load**: Saves the full JSON to the `raw_ingestions` table.
3.  **Phase 1 (Company Sync)**: Refreshes the `raw_companies` table from the local `companies.json` Master Manifest.
4.  **Phase 2 (Job Mapping)**: Uses `RawJobDataMapper` and `RawJobDataParser` to normalize job fields against the verified company data and determine target country (e.g. AU or NZ).
5.  **Silver Load**: Updates the `raw_jobs` table and upserts any unmapped "Ghost" companies.

### Reprocessing Pipeline (`JobDataSyncService.reprocessHistoricalData`)
Triggered when schema or mapping logic changes.
1.  **Extract**: Reads all records from the Bronze `raw_ingestions` table.
2.  **Transform**: Re-parses the data using the latest `JobDataMapper` logic.
3.  **Load**: Wipes the Silver tables and re-populates them with the fresh data.

---

## 🧩 Key Components

- **`JobDataSyncService`**: The orchestrator. manages the transitions between layers, executes the Two-Phase Sync, and ensures cache eviction.
- **`RawJobDataMapper`**: Handles the heavy lifting of deduplication and company linking. It groups jobs by a composite key of `(companyId, title, seniority)` to provide a "canonical" view of a role.
- **`RawJobDataParser`**: Contains the regex-heavy logic for:
    - Extracting 100+ technologies from descriptions.
    - Normalizing messy location strings into `(city, state, country)` to determine `targetCountry` routing.
    - Extracting seniority levels and work models (Remote/Hybrid/On-site).
- **`persistence.*Repository`**: Interfaces for BigQuery. These repositories handle the streaming of JSON records into BigQuery tables and manage the SQL queries for the frontend.
- **`persistence.*Mapper`**: Presentation-layer mappers that transform the database `Records` into `DTOs` optimized for the frontend UI.

---

## 🛠️ Key Technical Features

### 1. Multi-Pass Deduplication
The `JobDataMapper` handles high-frequency duplicates gracefully. It groups multiple postings for the same role into a single "canonical" role with a list of historical `jobIds` and all unique `locations`.

### 2. Automated Data Freshness
To keep the UI responsive and relevant, the system automatically filters out "stale" jobs (older than 6 months) during the Silver layer mapping. 
*   **Active Maintenance**: Old jobs are physically removed from the Silver layer (`raw_jobs`) during each reprocessing cycle.
*   **Archive Intact**: The Bronze layer (`raw_ingestions`) always retains the full historical JSON for all jobs ever ingested.

### 3. BF (Backend-for-Frontend) Mapping
Unlike a traditional REST API, this backend serves specialized DTOs (e.g., `JobPageDto`) that aggregate data from multiple tables (Jobs, Companies, Similar Roles) in a single round-trip, minimizing frontend complexity.

---

## 🚀 Deployment

For detailed instructions on deploying to Google Cloud, see **[DEPLOY.md](./DEPLOY.md)**.

We provide several utility scripts in the project root to manage the production environment:
- **`./scripts/deployment/deploy.sh`**: Deploys the current backend to GCP Cloud Run.
- **`./scripts/deployment/db-drop-silver.sh`**: Safely drops the Silver layer tables for recreation.
- **`./scripts/deployment/db-reprocess.sh`**: Triggers a full historical re-parse of all raw data.
- **`./scripts/deployment/ingest-dataset.sh`**: Manually triggers ingestion for a specific Apify dataset.

See the **[GCP Deployment Guide](./DEPLOY.md)** for more details.
