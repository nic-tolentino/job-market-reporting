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
ATS postings and SEEK/Trade Me/LinkedIn listings often lack standardized metadata. To be the best, we must process the raw text to provide deep filtering capabilities that normal job boards lack.

*   **Action:** Supercharge the `TechRoleClassifier` and backend mapper to heavily enrich the data on ingestion.
*   **Extraction Goals:**
    *   **Work Model Extraction:** Process the job description and location to strictly classify roles as **On-site**, **Hybrid**, or **100% Remote**.
    *   **Job Type Classification:** Scan for keywords to label the work type securely: **Contract**, **Fixed-term**, **Permanent**, **Internship**, or **Part-time**.
    *   **Seniority Bands:** Aggressively parse descriptions for years of experience to tag jobs as Junior, Mid, Senior, Staff, or Principal.
    *   **Tech Stack Mapping:** Extract keywords (React, Kotlin, AWS, GCP, etc.) and map them directly to our internal `tech Resources` tags.
    *   **Salary Transparency:** If the ATS/board doesn't provide structured salary fields, run a regex pass over the JD text to pluck out salary bands (e.g., "$120k - $150k").

### 4. Advanced Company Profiling (The "Quality" Signal)
A job is only as good as the company offering it. We need to enrich the company data to give candidates maximum context.
*   **Local vs. International:** Identify if the company is an "NZ/Local Company" or an "International Company with a Local Presence."
*   **Recruitment Agencies:** Flag recruitment/staffing agencies vs. direct employers to let candidates filter out third-party recruiters.
*   **International Remote Spam:** Identify "Global Remote" spam (e.g., international staffing agencies geo-tagging jobs to New Zealand) and label/filter them.
*   **Industry & Social Enterprises:** Categorize companies by Industry (Fintech, Healthtech, Agritech) and specifically identify **Social Enterprises** or "Tech for Good" organizations.

### 4. Intelligent Deduplication
As we expand to SEEK, Trade Me, and LinkedIn simultaneously, we will encounter the same job posted multiple times. We must not duplicate listings.
*   **Action:** Build a robust deduplication engine at the sync layer.
*   **Mechanism:** Match incoming jobs against existing active jobs based on a composite key: `[Company ID] + [Normalized Job Title] + [Location]`. 
*   **Priority:** If a duplicate is found, prefer the Direct ATS API link as the primary `applyUrl`, as it provides the cleanest candidate experience (bypassing the job board login walls).

### 5. Community Reporting Loop
*   **Action:** Add a "Report this Job" button on the job details UI.
*   **Mechanism:** Allow users to flag incorrect formatting, filled roles, or scam postings. This triggers a manual review and helps train our classifiers.

---

## Pillar 3: Apify Scaling Strategy
*Goal: Optimize our third-party scraping infrastructure to maximize coverage while minimizing duplicate API credit burn and maintenance overhead.*

### 1. Custom Scrapers vs. Apify Store
When expanding to SEEK and Trade Me, we have two options:
*   **Build Custom (Crawlee/Playwright):** 
    *   *Feasibility:* Medium to Hard. Developing a basic scraper in Apify is easy, but maintaining it against major job boards is a constant arms race against bot-protection (Cloudflare, rate-limiting, IP bans). It requires expensive residential proxies and constant DOM-selector maintenance.
*   **Rent from Apify Store (Recommended):**
    *   *Feasibility:* Easy. The Apify Store is filled with pre-built, highly maintained scrapers for major boards (including LinkedIn and SEEK). 
    *   *Strategy:* We should utilize reputable Store Scrapers (like `voyager/linkedin-jobs-scraper` or equivalent) as our primary ingestion layer for Tiers 2 & 3. We pay a slight premium per run, but we offload 100% of the anti-bot maintenance and proxy rotation. We only build custom scrapers for Tier 4 (Government portals) where commercial scrapers don't exist.

### 2. Optimizing LinkedIn Search Queries (Bypassing the 1,000-Job Wall)
While many Apify Store scrapers generously only charge for *unique* jobs returned (meaning overlapping searches don't cost extra money), we still face a massive technical limitation: **LinkedIn's Pagination Wall**. 

LinkedIn's public search only allows pagination up to 40 pages (1,000 jobs) per search URL. If we run a broad search for "Information Technology New Zealand", LinkedIn will simply stop providing data after 1,000 jobs, even if 4,000 exist.

To bypass this wall and maximize coverage, we must slice the market into **Mutually Exclusive Boolean Searches** segmented by core domains. By running 6 separate queries, we multiply our maximum extraction ceiling from 1,000 to 6,000 jobs.

### 2. The Failure of Complex Boolean Logic on LinkedIn
Historically, optimizing scrapers involved building complex, mutually exclusive Boolean strings (e.g., `title:(Software OR Frontend) AND NOT (Manager)`). 

However, **recent testing shows LinkedIn's search engine actively ignores or breaks when presented with complex Boolean logic.** If a query is too complex, LinkedIn falls back to a broad, fuzzy semantic search across the entire job description, returning highly irrelevant noise (e.g., "Music Producer" or "Business Analyst" appearing in a strict `title:(Software)` search).

### 3. The "Broad Net + Truncated Extraction" Strategy
Since we cannot rely on LinkedIn's boolean engine, and the **unauthenticated public search lacks advanced filters (like Job Function or Industry)**, we must fundamentally alter how we instruct Apify to extract the data.

1.  **Simple, Theme-Based Clusters:** We use a few tight, generic keyword clusters with no `AND NOT` operators. 
    *   *Search 1 (Core Web/Software):* `"Software" OR "Developer" OR "Fullstack" OR "Frontend" OR "Backend"`
    *   *Search 2 (Mobile & Native):* `"iOS" OR "Android" OR "Mobile Engineer" OR "React Native"`
    *   *Search 3 (Data & Analytics):* `"Data Engineer" OR "Data Scientist" OR "Machine Learning" OR "AI Engineer"`
    *   *Search 4 (Infra & Cloud):* `"DevOps" OR "SRE" OR "Cloud Engineer" OR "Security Engineer"`
    *   *Search 5 (Core/Hardware):* `"Firmware" OR "Embedded" OR "Hardware Engineer"`

2.  **The "One Task Per Query" Apify Workaround:** Because Apify processes URLs in a batch sequentially, a global `maxItems: 1000` setting will cause it to exhaust all 1,000 results on Search 1 (digging into the "fuzzy" garbage tail) and completely ignore Searches 2-5. 
    *   *Solution:* We must create **5 distinct "Saved Tasks"** inside Apify (e.g., "Scraper - Software", "Scraper - Data"). 
    *   We assign exactly one URL to each task and set the `maxItems` to **300** (or ~12 pages). 
    *   This forces Apify to only grab the top 300 highly relevant jobs for each cluster and immediately stop before LinkedIn's semantic engine starts feeding it "CCTV Operators".

3.  **The Backend Choke Point (TechRoleClassifier):** We accept that even the top 300 results will contain *some* noise. We rely entirely on our newly built `TechRoleClassifier` in the Kotlin backend to silently drop these non-tech jobs during the sync process.

**Why this works:** By firing 5 separate, truncated tasks, we extract 1,500 highly-relevant, unique jobs. We bypass the 1,000-job pagination wall, we completely avoid the "garbage tail" at the end of the search, and we bypass the limitation of the public unauthenticated search not having a "Job Function" filter.

---

## The Action Plan: What We Should Do Next

This is a phenomenal roadmap for building a massive data quality moat. Job boards like SEEK and LinkedIn have terrible filtering because they rely on the employer to manually select dropdowns (which they often get wrong). By extracting this data programmatically, DevAssembly will be vastly superior.

### Immediate Next Steps (Current Sprint)

#### Step 1: The Two-Phase Sync Architecture (JSON Manifest)
Before we can intelligently filter jobs based on parent company data, we must decouple the `JobDataSyncService.kt` processing pipeline from company generation. We need to implement a true two-phase workflow:
*   **Phase 1 (Company Sync):** The backend reads our stable "Master JSON" company file (e.g., `companies.json`) and upserts these verified profiles into BigQuery.
*   **Phase 2 (Job Processing):** The backend syncs the job payload, mapping jobs against the freshly updated, highly-contextual `companies` database table. Jobs belonging to known agencies can instantly be flagged or discarded.

#### Step 2: The "Company Profiler" Service (Heuristic/List Based)
Next, we build a new backend service: `CompanyEnrichmentService.kt`. 
- **Identify Recruitment Agencies:** We can start by seeding a backend list of the top 30 Tech Staffing agencies in NZ/AU (e.g., Randstad, Hays, Potentia, Robert Walters, Talent International). If `companyName` matches, we flag `isAgency = true`.
- **Global Remote Spam:** We can also build a heuristic to flag "Global Remote Spam" by comparing the company's stated headquarters with the job location and looking for keywords like "Bilingual" or "Global Remote".

#### Step 3: The "Work Detail" Extractors (Regex/Keyword Based)
Update `JobMapper.kt` and `TechRoleClassifier.kt`. We can write robust pattern matchers to read the raw job description text and confidently extract **Job Type** (looking for "12-month contract", "fixed term") and **Work Model** (looking for "WFH", "2 days in office"). Since this runs in Phase 2 of the sync, we can use the agency flags designated in Step 2 to instantly discard toxic jobs.

#### Step 4: Industry & Social Enterprise Classification (LLM/API Based)
Categorizing thousands of companies by industry and identifying "Social Enterprises" is very hard to do purely with regex. For this, I recommend we build an asynchronous worker that takes new `companyNames`, pings a lightweight LLM (or a company lookup API like Clearbit/Apollo), and asks "What industry is this company, and is it a registered social enterprise/B-Corp?". We then cache that result forever in our database.

#### Step 5: Surface it to the User
Finally, we update the React frontend to add prominent toggle buttons: **[Hide Agencies]**, **[Contract Only]**, **[Remote Only]**, **[Social Enterprises]**.

