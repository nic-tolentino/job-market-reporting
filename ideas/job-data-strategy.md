# Job Sourcing & Data Quality Strategy

To establish DevAssembly as the **most reliable and comprehensive source of tech job data in Australia and New Zealand**, we must excel in two distinct pillars:
1. **Sourcing (Knowing about the jobs):** Capturing the maximum addressable market across all relevant platforms.
2. **Quality (Having the best data):** Ensuring listings are real-time, richly categorized, and immediately purged when filled.

This strategy is informed by our recent ATS identification sweep (March 2026), which revealed that relying solely on LinkedIn and URL pattern matching leaves a massive ~50-60% blind spot in the ANZ tech market.

---

## Pillar 1: Multi-Channel Sourcing Strategy
*Goal: Expand our reach to capture 95%+ of the active tech job market in ANZ.*

### The Problem with LinkedIn Exclusivity
Currently, we rely heavily on LinkedIn (scraped via Apify). While LinkedIn is strong for tech and corporate roles, it does not hold a monopoly in ANZ:
*   **SEEK** dominates the overall ANZ job board market, capturing ~90% of total job seeker time.
*   **Trade Me Jobs** holds a highly significant share of the New Zealand market, particularly for domestic SMEs and government roles.
*   **LinkedIn Easy Apply** masks the underlying ATS for ~82% of companies, preventing us from leveraging direct API integrations if we only discover companies via LinkedIn.

By ignoring SEEK and Trade Me, we are missing an estimated **50-60% of the total addressable tech job market** (e.g., government organizations, established domestic enterprises, and non-tech-first companies that post exclusively on local boards).

### The Four-Tier Sourcing Model

**Tier 1: Direct ATS API Integration (The "Real-Time Moat")**
*   **Target:** The 50.5% of companies we have already identified as using Greenhouse, Lever, Ashby, or Workday.
*   **Action:** Bypass job boards entirely. Connect our backend directly to their public APIs.
*   **Advantage:** We get 100% of their active jobs instantly, often before they are even cross-posted to LinkedIn or SEEK.

**Tier 2: Primary Dominant Boards (SEEK & Trade Me)**
*   **Target:** The massive segment of the market that does not use LinkedIn or uses proprietary ATS systems (the 49.5% "NONE" group).
*   **Action:** Develop dedicated Apify scrapers (or direct API integrations if accessible) for SEEK (AU/NZ) and Trade Me Jobs (NZ).
*   **Advantage:** Captures the "hidden" 50-60% of jobs we are currently missing. Crucial for volume and market completeness.

**Tier 3: Tech & Startup Niche Boards**
*   **Target:** Tech-first startups, recruiters, and international companies.
*   **Key Platforms:** 
    *   **LinkedIn**: Unparalleled for mid-to-senior corporate tech roles and active recruiter outreach.
    *   **Wellfound (formerly AngelList) & Built In**: Essential for early-stage and high-growth startups globally, including AU/NZ.
    *   **Matchstiq (NZ/AU) & Hatch (AU)**: Highly prominent regional boards dedicated strictly to ANZ tech and startup ecosystems.
*   **Action:** Maintain the existing LinkedIn scraper. Evaluate targeted scrapers for Wellfound and Matchstiq to capture the early-stage tech market that SEEK often misses.

**Tier 4: Government & Public Sector Portals**
*   **Target:** Stable, high-paying tech roles within local and federal government agencies.
*   **Key Platforms:** **Jobs.govt.nz** (NZ), **APSJobs** (AU Federal), and state-level specific boards (e.g., Careers.vic, SmartJobs QLD).
*   **Action:** Government IT roles are frequently not posted on LinkedIn or even SEEK to save costs. Developing a lightweight scraper for the localized government portals ensures comprehensive public sector coverage.

**Tier 5: Aggregators (The Catch-All/Validation Layer)**
*   **Target:** Jobs that slip through the cracks of direct integrations and primary boards.
*   **Key Platforms:** **Indeed**, **Jora** (owned by SEEK, strong local presence), **Google for Jobs**.
*   **Action:** These aggregators are highly engineered to block scraping and contain massive duplication. We should *not* use them as primary data sources, but rather as a validation layer to discover *companies* we are missing, which we can then target via Tiers 1-4.

**Tier 6: Community & Company-Direct Sourcing**
*   **Target:** Newly funded startups or stealth companies not yet on major boards.
*   **Action:** Add a "Submit a Job" or "Add My Company" feature to the UI. Monitor major tech parks (e.g., GridAKL, Stone & Chalk, Fishburners) for new tenant announcements to add to our company tracking list.

---

## Pillar 2: Data Quality & Freshness Strategy
*Goal: Provide a candidate experience that is vastly superior to traditional job boards by eliminating "ghost jobs" and offering deeper filtering capability.*

Knowing about the jobs is only half the battle. Our data must be cleaner, richer, and fresher than the source material.

### 1. High-Frequency Real-Time Syncing
Traditional job boards lag because they rely on daily XML XML feeds or slow web scrapers.
*   **Action:** Configure the backend `JobSyncTimer` to poll Tier 1 ATS APIs **every 1–2 hours**.
*   **Impact:** When a company posts a job, it appears on DevAssembly immediately, giving our users a first-mover advantage.

### 2. "Ghost Job" & Dead Link Detection (Crucial)
One of the biggest frustrations for candidates is spending time on an application only to find the role was already filled or the URL is dead. Job boards are notoriously bad at cleaning these up.
*   **Action:** Implement a daily health-check worker.
*   **Mechanism:** Send a simple `HEAD` request to every active `applyUrl` in our database. If an ATS returns a 404, or the page redirects back to a generic `/careers` homepage, **immediately mark the job as `CLOSED` in our system**.
*   **Impact:** Zero ghost jobs. High candidate trust.

### 3. Deep Data Enrichment & Classification
ATS postings and SEEK/Trade Me listings often lack standardized metadata for "Seniority" or "Tech Stack". If a job title is just "Software Engineer", it's hard to filter.
*   **Action:** Supercharge the `TechRoleClassifier`.
*   **Extraction Goals:**
    *   **Seniority Bands:** Aggressively parse descriptions for years of experience to tag jobs as Junior, Mid, Senior, Staff, or Principal.
    *   **Tech Stack Mapping:** Extract keywords (React, Kotlin, AWS, GCP, etc.) and map them directly to our internal `tech Resources` tags.
    *   **Salary Transparency:** If the ATS/board doesn't provide structured salary fields, run a regex pass over the JD text to pluck out salary bands (e.g., "$120k - $150k").
    *   **Work Model:** Strictly classify On-site, Hybrid, or 100% Remote.

### 4. Intelligent Deduplication
As we expand to SEEK, Trade Me, and LinkedIn simultaneously, we will encounter the same job posted multiple times. We must not duplicate listings.
*   **Action:** Build a robust deduplication engine at the sync layer.
*   **Mechanism:** Match incoming jobs against existing active jobs based on a composite key: `[Company ID] + [Normalized Job Title] + [Location]`. 
*   **Priority:** If a duplicate is found, prefer the Direct ATS API link as the primary `applyUrl`, as it provides the cleanest candidate experience (bypassing the job board login walls).

### 5. Community Reporting Loop
*   **Action:** Add a "Report this Job" button on the job details UI.
*   **Mechanism:** Allow users to flag incorrect formatting, filled roles, or scam postings. This triggers a manual review and helps train our classifiers.

---

## Execution Roadmap

1.  **Phase 1: The API Moat (Current Focus)**
    *   Seed internal database with the 92 validated Greenhouse, Lever, Ashby, and Workday identifiers.
    *   Build out the automated hourly polling connectors for these Tier 1 APIs.
    *   *Result: We immediately have the fastest, most reliable data for 50.5% of the companies.*

2.  **Phase 2: Quality Control**
    *   Implement the `applyUrl` health-checker to eliminate ghost jobs.
    *   Upgrade the `TechRoleClassifier` for deep enrichment.
    *   *Result: Our existing data becomes the highest quality on the market.*

3.  **Phase 3: The Volume Expansion (SEEK, Trade Me, & Startups)**
    *   Develop and deploy scrapers for SEEK and Trade Me Jobs to capture the missing SME and enterprise market.
    *   Integrate highly-relevant startup boards (Matchstiq, Wellfound).
    *   Implement the cross-platform deduplication engine.
    *   *Result: We capture the "missing 50-60%" and achieve near-total market coverage.*

4.  **Phase 4: Public Sector & Community Features**
    *   Launch user submission and reporting features.
    *   Index government portals (Jobs.govt.nz, APSJobs).
    *   *Result: The platform captures the final non-traditional segments and becomes self-healing.*
