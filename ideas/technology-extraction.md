# Robust Technology Extraction Pipeline (Ideas)

This document outlines a phased approach to moving from a hardcoded list of technologies to a robust, continuously learning pipeline for identifying and extracting technologies from job descriptions.

## Phase 2: Architecture for Dynamic Dictionary
- **Action:** Externalize the `techKeywords` dictionary.
- **Details:** 
  - Create a new BigQuery table (e.g., `tech_dictionary`) to store the canonical list of technologies.
  - Update `JobDataMapper.kt` to load this dictionary from BigQuery on startup (and cache it, refreshing periodically) instead of using a hardcoded list.
- **Outcome:** Technologies can be added or removed without requiring a code change and deployment.

## Phase 3: Historical Backfill
- **Action:** Retroactively apply the new dictionary to existing jobs.
- **Details:** Create a one-off SQL script or a dedicated API endpoint in the backend that reads all records from `raw_ingestions`, re-runs the `extractTechnologies` logic using the updated dictionary, and updates the `raw_jobs` table.
- **Outcome:** Historical parity for new technologies.

## Phase 4: Automated Discovery Pipeline (The "Brain")
- **Action:** Implement an LLM-assisted discovery loop to find *new* technologies.
- **Details:**
  - Create a scheduled job (e.g., via Cloud Scheduler triggering a new endpoint `/api/admin/discover-tech`).
  - The job samples recent job descriptions from BigQuery and sends them to an LLM (e.g., Google Gemini Vertex API) to extract technical terms.
  - The extracted terms are compared against the existing `tech_dictionary`.
  - Novel terms are inserted into a new BigQuery table: `tech_review_queue`.
- **Outcome:** The system automatically discovers emerging technologies.

## Phase 5: Human-in-the-Loop Review
- **Action:** Create a mechanism to approve/reject discovered technologies.
- **Details:**
  - Build a simple admin UI or API endpoints to view the `tech_review_queue`.
  - Approved terms are moved to the `tech_dictionary` and immediately become active for new jobs.
  - Rejected terms are flagged so the LLM pipeline ignores them in the future.
- **Outcome:** Ensures data quality and prevents generic terms from polluting the dataset.
