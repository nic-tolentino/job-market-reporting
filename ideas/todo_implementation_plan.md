# Comprehensive Strategic Roadmap & Analysis

Providing a prioritized plan to evolve DevAssembly from a LinkedIn scraper into a high-trust, multi-channel market intelligence platform.

## Effort vs. Benefit Matrix

This matrix identifies the Return on Investment (ROI) for major categories of tasks identified in the `ideas` files and `README`.

| Benefit \ Effort | Low Effort | Medium Effort | High Effort |
| :--- | :--- | :--- | :--- |
| **High Benefit** | **Quick Wins**: <br>• Bug: City naming duplicates <br>• PII filter audit <br>• Sitemap.xml for SEO <br>• Data source transparency | **Strategic Core**: <br>• Salary Normalization <br>• Background Processing (Cloud Tasks) <br>• Stale Job/Dead Link Detection <br>• Visa Sponsorship tags | **Market Moat**: <br>• Tech Domains / Hubs <br>• Multi-channel Integration (Seek/TradeMe) <br>• Mobile UX Overhaul |
| **Medium Benefit** | **Maintenance**: <br>• Old job (2025) archival <br>• Third-party URL fallbacks | **Operational**: <br>• Trending Lists <br>• Tech record pre-computation <br>• Unit Test expansion | **Future-Proofing**: <br>• Public Company Repo <br>• Resource Data Modeling (DB-driven) |
| **Low Benefit** | **Polish**: <br>• Class/Package renaming <br>• "Other" country selector | **Cleanup**: <br>• Nullable field reduction | **Experimental**: <br>• User accounts (for saved jobs) <br>• Interview prep content |

---

## Proposed Implementation Plan

### Phase 1: High-ROI "Quick Wins" & Trust Factors
Focus on visible polish and transparency to build user trust.
*   **[MODIFY]** `RawJobDataParser.kt`: Fix the "Auckland, Auckland" duplication bug.
*   **[MODIFY]** UI/Frontend: Explicitly label data sources and sync frequencies.
*   **[NEW]** `sitemap.xml` generation for search engine visibility.
*   **[VERIFY]** Audit PII filter for edge cases (e.g., recruiter phone numbers in body).

### Phase 2: Core Data Engine & Reliability
Infrastructure changes to ensure the system can scale beyond current volumes.
*   **[NEW]** `CloudTasksService`: Decouple ingestion from processing to avoid Cloud Run timeouts.
*   **[MODIFY]** `RawJobParser`: Implement Robust Salary Normalization (detecting "per hour", "per month", and currency).
*   **[NEW]** "Dead Link" Worker: Daily health checks on `applyUrl` to detect filled roles.
*   **[MODIFY]** `CompanyDataStrategy`: Automate the AI enrichment loop for "Ghost" companies.

### Phase 3: Market Moat (Multi-Source & Domains)
Expanding beyond LinkedIn to capture the true ANZ market.
*   **[NEW]** Seek & TradeMe Scrapers: Integrate via Apify to capture the 50%+ of roles missing from LinkedIn.
*   **[NEW]** Tech Domains / Hubs: Implement top-level categories (Cloud, Mobile, Web) and dedicated landing pages.
*   **[NEW]** Visa Sponsorship Matrix: Implement structured tracking for sponsorship availability.

### Phase 4: Platform Maturation & UX
Refining the experience for high engagement.
*   **[MODIFY]** Mobile UX: Complete redesign of job rows and headers for smaller screens.
*   **[MODIFY]** `ResourceDataModeling`: Migrate from static `techResources.ts` to a managed database/JSON structure.
*   **[NEW]** Advanced Charts: Jobs per capita, salary trends over time, and market seniority distribution.

### Phase 5: Scalability & Premium Features
*   **[NEW]** User Accounts: Allow users to save companies/technologies and receive email notifications.
*   **[NEW]** Public Company Repository: Allow the community to contribute to company metadata (B-Corp status, Remote policy).

---

## Verification Plan

### Automated Tests
*   `RawJobDataParserTest`: Add cases for salary normalization and city deduplication.
*   `JobDataSyncServiceTest`: Test Cloud Tasks queuing and error handling.
*   `CrawlHealthTest`: Verify the dead-link detector correctly flags 404s.

### Manual Verification
*   **UI Review**: Verify sitemap generation and data source labels in the frontend.
*   **Mobile Testing**: Physical device/emulator review of the redesigned job list.
*   **Sync Audit**: Run a Seek/TradeMe ingestion and verify no duplicates appear in BigQuery.
