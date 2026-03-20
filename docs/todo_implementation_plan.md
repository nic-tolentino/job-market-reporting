# Comprehensive Strategic Roadmap & Analysis

Providing a prioritized plan to evolve DevAssembly from a LinkedIn scraper into a high-trust, multi-channel market intelligence platform.

**Last Updated:** March 17, 2026

---

## 🎯 Next Best Opportunities (March 17, 2026)

Ordered by value delivered vs. effort. Doing these in sequence gets the crawler pipeline producing visible results, scales ATS coverage to 527 companies, and gives operational visibility without rebuilding production.

### Tier 1 — Fix First (must-do, low-medium effort, high impact)

| # | Feature | Why now | Effort | Doc |
|---|---------|---------|--------|-----|
| ✅ **1** | **7.1 Crawler Job Persistence** | **Done** — `CrawlerJobMapper.kt`, `CrawlerJobPersistenceService.kt`, `NormalizedJobDto.kt` all exist. | — | `docs/implementation-plans/7.1-crawler-job-persistence.md` |
| ✅ **2** | **Activate ATS pipeline (GH/LV/AS)** | **Done** — Lever and Ashby wired into `AtsClientFactory` as `@Component` beans alongside Greenhouse. | — | `docs/implementation-plans/crawler-extraction-improvement-plan.md` |
| **3** | **Staging environment** | Currently testing in production causes breakage. The plan is fully documented — ~2h of infra work to get a `staging` branch + Cloud Run service + `techmarket_staging` BigQuery dataset. Blocks safe delivery of everything below. | 2h | `docs/deployment/staging-environment.md` |

### Tier 2 — ATS Coverage Expansion (medium effort, big coverage gains)

| # | Feature | Why now | Effort | Doc |
|---|---------|---------|--------|-----|
| ✅ **4** | **TeamTailor client (99 companies)** | **Done** — `TeamTailorClient.kt` exists. | — | `docs/implementation-plans/crawler-extraction-improvement-plan.md` §1.5 |
| ✅ **5** | **SmartRecruiters client (157 companies)** | **Done** — `SmartRecruitersClient.kt` exists. | — | `docs/implementation-plans/crawler-extraction-improvement-plan.md` §1.5 |
| ✅ **6** | **Workable client (28 companies)** | **Done** — `WorkableClient.kt` exists. | — | `docs/implementation-plans/crawler-extraction-improvement-plan.md` §1.5 |
| **7** | **autechjobs historical data load (ID-12–14)** | Script (`--export-jsonl` flag) is fully implemented and SQL dump exists at `data/third-party/autechjobs/`. BigQuery load (ID-13) and trend backfill (ID-14) still need to be run. | 1-2h | `docs/data/autechjobs-historical-data.md` |

### Tier 3 — Admin Panel + Operational Visibility (medium-high effort, high long-term value)

| # | Feature | Why now | Effort | Doc |
|---|---------|---------|--------|-----|
| ✅ **8** | **Admin Panel Phase 1 (foundation + company table)** | **Done** — `frontend/src/admin/` exists with `AdminLayout.tsx`, `CompaniesPage.tsx`, `DashboardPage.tsx`, `CrawlsPage.tsx`, `PipelinePage.tsx`, `AnalyticsPage.tsx`, `LoginPage.tsx`. | — | `docs/admin-panel-plan.md` |
| 🔄 **9** | **Admin Panel Phase 2 (pipeline observability)** | `PipelinePage.tsx` exists but backend only has 6 basic endpoints (no dead-letters browser, no jobs browser). Backend admin API gap remains. | ~1 week | `docs/admin-panel-plan.md` |
| ✅ **10** | **Per-company extraction hints (self-improving crawler)** | **Done** — `extractionHints` referenced in `GeminiExtractionService.ts` lines 35, 97-98. | — | `docs/implementation-plans/crawler-extraction-improvement-plan.md` §2.1-2.2 |

### Tier 4 — Data Quality & UX (when operational pipeline is stable)

| # | Feature | Why now | Effort | Doc |
|---|---------|---------|--------|-----|
| ✅ **11** | **Admin Panel Phase 3 (analytics)** | **Done** — `AnalyticsPage.tsx` exists in the admin frontend. | — | `docs/admin-panel-plan.md` |
| **12** | **Snapshot fixture test suite (20 snapshots)** | No `crawler-service/fixtures/` directory exists. Catches regressions when extraction logic changes. Low ongoing cost — small HTML files in Git. | 4-6h | `docs/implementation-plans/crawler-extraction-improvement-plan.md` §2.3 |
| ✅ **13** | **GCS Bronze cold storage** | **Done** — `BronzeGcsRepository.kt`, `BronzeIngestionManifest.kt`, `BronzeRepository.kt`, and `terraform/gcp/storage_bucket.tf` all exist. | — | `docs/implementation-plans/6.1-file-based-cold-storage-plan.md` |
| ✅ **14** | **Visa Sponsorship UI** | **Done** — Full implementation in `CompanyProfilePage.tsx` and `CompaniesPage.tsx` including filter and badge. | — | `docs/phase2/2.5-visa-sponsorship.md` |
| **14b** | **Dead link UI** | `urlStatus` field exists in admin types but no dedicated public-facing dead link UI component. Low effort frontend work. | 2-3h | `docs/phase2/2.3-dead-link-detection.md` |
| **15** | **Mobile UX overhaul** | Known weakness. Significant effort but affects all mobile users. | 16-20h | `docs/implementation-plans/4.1-mobile-ux-overhaul-plan.md` |

---

## 📚 Documentation Structure

**Architecture Decision Records:**
- `docs/architecture/ADR-001-company-manifest-ats-integration.md` - **Master ADR** for company manifest & ATS system

**Completed Features** (`docs/phase2/`):
- Detailed technical specifications
- Implementation guides with code examples
- Testing strategies
- Success metrics
- Known issues and future enhancements

**Upcoming Features** (`docs/implementation-plans/`):
- Executive summaries
- Step-by-step implementation guides
- Testing strategies
- Effort estimates

| Location | Purpose | Examples |
|----------|---------|----------|
| `docs/architecture/` | **Architecture decisions & rationale** | ADR-001 (company manifest, ATS integration) |
| `docs/phase2/` | Completed feature specs | 2.1 Salary, 2.2 Cloud Tasks, 2.3 Dead Links, 2.4 ATS, 2.5 Visa |
| `docs/implementation-plans/` | Upcoming feature plans | 2.3 Dead Links, 2.4 ATS, 3.x Market Expansion, 4.x UX |
| `docs/data/` | Data strategy & pipeline | company-data-strategy, ats-identification-plan |
| `docs/todo_implementation_plan.md` | **This file** - Strategic roadmap | Prioritized backlog, progress tracking |

---

## 📋 Implementation Plans

Detailed implementation plans have been created for all major features. Each plan includes:
- Executive summary
- Technical specification
- Step-by-step implementation guide
- Testing strategy
- Success metrics
- Files to create/modify

**Location:** 
- Phase 2.1-2.2: `docs/phase2/` (completed features with full specs)
- Phase 2.3+: `docs/implementation-plans/` (upcoming features)

| Plan | Priority | Effort | Status | Location |
|------|----------|--------|--------|----------|
| [2.1 Salary Normalization](./phase2/2.1-salary-normalization.md) | HIGH | 8-10 hours | ✅ **COMPLETE** | `docs/phase2/` |
| [2.2 Background Processing](./phase2/2.2-background-processing.md) | HIGH | 8-10 hours | ✅ **COMPLETE** | `docs/phase2/` |
| [2.3 Dead Link Detection](./phase2/2.3-dead-link-detection.md) | HIGH | 6-8 hours | 🔄 **Backend Complete** | `docs/phase2/` |
| [2.4 ATS Integrations](./phase2/2.4-ats-integrations.md) | HIGH | 12-16 hours | 🔄 **Backend Complete** | `docs/phase2/` |
| [2.5 Visa Sponsorship](./phase2/2.5-visa-sponsorship.md) | MEDIUM | 4-6 hours | 🔄 **Backend Complete** | `docs/phase2/` |
| [2.5 Type-Safe Query Results](./features/typed-row-abstraction-plan.md) | MEDIUM | 12-16 hours | ✅ **COMPLETE** | `docs/features/` |
| [2.6 Automated Contract Validation](./phase2/2.6-automated-contract-validation.md) | HIGH | 8-10 hours | ✅ **COMPLETE** | `docs/phase2/` |
| [3.1 SEEK & TradeMe](./implementation-plans/3.1-seek-trademe-integration-plan.md) | HIGH | 10-14 hours | ⏳ Pending | `docs/implementation-plans/` |
| [3.2 Domain Hubs](./phase3/3.2-technology-domain-hubs.md) | HIGH | 12-16 hours | ✅ **COMPLETE** | `docs/phase3/` |
| [3.3 Tech Exclusion Filters](./implementation-plans/3.3-technology-exclusion-filters-plan.md) | MEDIUM | 4-6 hours | ⏳ Pending | `docs/implementation-plans/` |
| [3.4 Trending Lists](./implementation-plans/3.4-trending-lists-plan.md) | MEDIUM | 6-8 hours | ⏳ Pending | `docs/implementation-plans/` |
| [4.1 Mobile UX Overhaul](./implementation-plans/4.1-mobile-ux-overhaul-plan.md) | HIGH | 16-20 hours | ⏳ Pending | `docs/implementation-plans/` |
| [5.1 Directory Manifest Migration](./implementation-plans/5.1-directory-based-manifest-migration.md) | MEDIUM | 6-8 hours | ✅ **COMPLETE** | `docs/implementation-plans/` |
| [5.2 Manifest Validation System](./implementation-plans/5.2-company-manifest-validation.md) | HIGH | 8-10 hours | ✅ **COMPLETE** | `docs/implementation-plans/` |
| [5.3 Manifest Improvements](./implementation-plans/5.3-manifest-improvements.md) | HIGH | Included | ✅ **COMPLETE** | `docs/implementation-plans/` |
| [6.1 GCS Bronze Cold Storage](./implementation-plans/6.1-file-based-cold-storage-plan.md) | MEDIUM | 2-3 weeks | ⏳ Pending | `docs/implementation-plans/` |
| [7.1 Crawler Job Persistence](./implementation-plans/7.1-crawler-job-persistence.md) | **CRITICAL** | 4-6 hours | ⏳ Pending | `docs/implementation-plans/` |
| [ATS Extraction Improvements](./implementation-plans/crawler-extraction-improvement-plan.md) | HIGH | Ongoing | 🔄 **Sprint 2 active** | `docs/implementation-plans/` |
| [Admin Panel](./admin-panel-plan.md) | HIGH | 4 weeks (phased) | ⏳ Pending | `docs/` |
| [Staging Environment](./deployment/staging-environment.md) | HIGH | 2 hours | ⏳ Pending | `docs/deployment/` |

---

## ✅ Completed: Phase 2.1 & 2.2

### Phase 2.1: Salary Normalization Engine ✅ COMPLETE

**Status:** Implementation complete, documentation in `docs/phase2/2.1-salary-normalization.md`

**What Was Built:**
- `NormalizedSalary` data class with currency, period, source, and isGross fields
- Comprehensive salary parsing for NZ, AU, EUR formats (including European number formats)
- Support for "plus super" extraction (base salary only)
- Spanish "Bruto/Neto" (gross/net) detection
- Confidence model based on source (JOB_POSTING, ATS_API, MARKET_DATA, AI_ESTIMATE)
- BigQuery STRUCT schema for clean salary storage
- Frontend salary formatting with locale-aware display
- 25+ unit tests covering real-world salary formats

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/model/NormalizedSalary.kt` (new)
- `backend/src/main/kotlin/com/techmarket/sync/RawJobDataParser.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/model/JobRecord.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/job/JobBigQueryRepository.kt`
- `frontend/src/lib/salary.ts` (new)
- Frontend components: `JobCard.tsx`, `CompanyProfilePage.tsx`, `MarketTab.tsx`

**Success Metrics:**
- ✅ 90%+ of explicit salary data parsed correctly
- ✅ Salary display shows currency and period
- ✅ Confidence badges display on UI

**Documentation:** `docs/phase2/2.1-salary-normalization.md` (788 lines)

---

### Phase 2.2: Background Processing with Cloud Tasks ✅ COMPLETE

**Status:** Implementation complete, documentation in `docs/phase2/2.2-background-processing.md`

**What Was Built:**
- Cloud Tasks queue infrastructure (primary + DLQ)
- `CloudTasksService` for queuing sync tasks
- `SyncTaskHandler` internal endpoint with intelligent retry logic
- Transient vs permanent error classification
- `X-Cloud-Tasks` header validation + IAM security
- Correlation ID tracing for end-to-end debugging
- Daily health check scheduler (Phase 2.3 prep)
- Automated deployment script
- 18 new unit tests

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/service/CloudTasksService.kt` (new)
- `backend/src/main/kotlin/com/techmarket/api/internal/SyncTaskHandler.kt` (new)
- `backend/src/main/kotlin/com/techmarket/config/CloudTasksConfig.kt` (new)
- `backend/src/main/kotlin/com/techmarket/config/SecurityConfig.kt`
- `backend/src/main/kotlin/com/techmarket/config/WebhookBypassFilter.kt` (new)
- `backend/src/main/kotlin/com/techmarket/webhook/ApifyWebhookController.kt`
- `backend/src/main/kotlin/com/techmarket/api/AdminController.kt`
- `backend/src/main/kotlin/com/techmarket/scheduler/HealthCheckScheduler.kt` (new)
- `backend/src/main/kotlin/com/techmarket/util/CloudTasksConstants.kt` (new)
- `backend/src/main/kotlin/com/techmarket/util/HealthCheckConstants.kt` (new)
- `terraform/gcp/cloud_tasks.tf` (new)
- `scripts/deployment/setup-cloud-tasks.sh` (new)

**Success Metrics:**
| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Webhook response time | <500ms | <100ms | ✅ Exceeded |
| Zero timeout errors | Yes | Yes | ✅ Met |
| Failed tasks properly retried | Max 5 attempts | Configured | ✅ Met |
| DLQ alerts trigger | <5 minutes | Log immediate | ✅ Met |
| Sync completion rate | >99% | 99.2% | ✅ Met |

**Security Notes:**
- ✅ Cloud Run IAM restricts access to Cloud Tasks service account
- ✅ `allUsers` access removed
- ✅ `X-Cloud-Tasks` header validation
- ⚠️ OIDC tokens deferred (documented in Known Issues)

**Documentation:** `docs/phase2/2.2-background-processing.md` (854 lines)

**Deployment:**
```bash
./scripts/deployment/setup-cloud-tasks.sh
```

---

## 🎯 Completed Major Features

### ✅ ATS Identification Sprint 2 — autechjobs + Domain-Slug Expansion (March 17, 2026)

**Result:** 579/1,371 companies (42%) now have ATS configs — up from 260 (20.5%). 240 companies have free-API configs ready to activate.

**What was done:**
- Parsed `data/third-party/autechjobs/Cloud_SQL_Export_2026-03-17.sql` (149 companies, 161k jobs) — extracted ATS slugs from historical URLs, applied to matching manifests
- Fixed false positives (`"ai"` suffix strip, Macquarie group mismatch)
- Created 104 new manifest stubs for unmatched SQL companies (manifest total: 1,267 → 1,371)
- Extended `probe_vanity_domains.py` with SR + TT probing; fixed critical SR false-positive (postings API returns 200 for any slug — correct check is board redirect)
- Applied 142 SmartRecruiters + 114 TeamTailor slug hits from domain probing

**Free-API breakdown:** Greenhouse ×94, Lever ×105, Ashby ×44 = **243 ready to activate**

**Identification scripts now available:** `probe_free_ats.py`, `extract_ats_from_apply_urls.py`, `scan_careers_pages.py`, `probe_vanity_domains.py`, `parse_autechjobs.py`, `create_autechjobs_manifests.py`, `validate_ats_configs.py`

---

### ✅ ATS Integration — Sprint 1 Lever + Ashby Clients (March 2026)

**Result:** `LeverClient.kt` + `LeverNormalizer.kt` (8 tests), `AshbyClient.kt` + `AshbyNormalizer.kt` (9 tests). All wired into `AtsClientFactory` and `AtsNormalizerFactory`.

---

### ✅ ATS Integration — Self-Hosted AI Crawler (March 11, 2026)

**Problem:** 87.8% of 1,257 target companies have no identified ATS. Building specific ATS integrations first would be premature optimization.

**Solution:** Built a self-hosted Crawlee + Gemini Flash crawler that can crawl any company's career page and extract structured job data.

**Key Features:**
- **Crawler Service** (Node.js/TypeScript) - Separate microservice on Cloud Run
- **ATS Detection** - Automatically identifies ATS providers (Greenhouse, Lever, Ashby, Workday, etc.)
- **AI Extraction** - Gemini 2.0 Flash for job data extraction (~$0.0001/page)
- **Quality Scoring** - Automated extraction quality evaluation
- **Prompt Injection Defense** - HTML sanitization, XML wrapping, strict validation
- **robots.txt Compliance** - Respects crawl rules and rate limits
- **BigQuery Logging** - Full audit trail for monitoring and analytics

**Files Created:**
- `crawler-service/` - Complete Node.js/TypeScript microservice (2,500+ lines)
- `backend/.../CrawlerClient.kt` - Kotlin backend integration
- `backend/.../CrawlerNormalizer.kt` - Job normalization
- `backend/.../CrawlerBatchSyncService.kt` - Batch sync orchestration
- `backend/.../CrawlConfigService.kt` - Crawl configuration management
- `backend/.../ExtractionQualityScorer.kt` - Quality scoring engine
- `backend/.../CrawlMetadataLogger.kt` - BigQuery metadata logging
- `docs/data/ats/ats-integration-plan.md` - Full implementation plan
- `docs/architecture/adr-008-crawler-service.md` - Architecture decision record

**Test Coverage:** 95 tests passing (90% pass rate)
- AtsDetector: 100% (11/11)
- ContentExtractor: 100% (11/11)
- JobValidator: 100% (24/24)
- RobotsChecker: 100% (10/10)
- GeminiExtractionService: 100% (11/11)

**Cost:** ~$15/month (vs ~$100/month for Apify equivalent)
**Coverage:** 100% of companies (vs 30% with Apify-only)

**Status:** ✅ **READY FOR DEPLOYMENT**
- TypeScript compiles successfully
- Kotlin compiles successfully
- All core functionality tests passing
- E2E tests ready for post-deployment verification

**Documentation:**
- `crawler-service/README.md` - Full API documentation
- `crawler-service/QUICKSTART.md` - Getting started guide
- `docs/monitoring/crawler-dashboard-queries.sql` - BigQuery monitoring queries
- `docs/monitoring/crawler-alerting.md` - Alerting configuration
- `docs/data/ats/phase-3-cloud-tasks-plan.md` - Phase 3 distributed crawling plan

---

---

## Effort vs. Benefit Matrix

This matrix identifies the Return on Investment (ROI) for major categories of tasks identified in the `ideas` files and `README`.

| Benefit \ Effort | Low Effort | Medium Effort | High Effort |
| :--- | :--- | :--- | :--- |
| **High Benefit** | **Quick Wins**: <br>• ✅ **FIX**: Company tech stack aggregation (#1 user-facing bug) - **COMPLETED**<br>• ✅ **FIX**: City naming duplicates (Auckland, Auckland) - **COMPLETED**<br>• ✅ **FIX**: PII filter audit for phone numbers/emails - **COMPLETED**<br>• ✅ **NEW**: Data source transparency labels - **COMPLETED**<br>• ✅ **NEW**: Sitemap.xml for SEO - **COMPLETED** | **Strategic Core**: <br>• **NEW**: Salary normalization engine (currency, period detection)<br>• **NEW**: Background processing (Cloud Tasks)<br>• **NEW**: Dead link detection worker<br>• **NEW**: ATS direct integrations (Greenhouse/Lever/Ashby) | **Market Moat**: <br>• **NEW**: SEEK & TradeMe integration (50-60% market coverage)<br>• **NEW**: Tech Domain Hubs (Web, Mobile, Cloud, Data)<br>• **NEW**: Mobile UX complete overhaul |
| **Medium Benefit** | **Maintenance**: <br>• **FIX**: Stale job (mid-2025) archival<br>• **FIX**: Third-party URL fallbacks<br>• **NEW**: Visa sponsorship tags | **Operational**: <br>• **NEW**: Trending jobs/companies/tech lists<br>• **NEW**: Tech record pre-computation<br>• **NEW**: Unit test expansion (critical paths)<br>• **NEW**: Technology exclusion filters (Native-only toggle) | **Future-Proofing**: <br>• **NEW**: Public company data repo (community contributions)<br>• **NEW**: Resource data modeling (DB-driven)<br>• **NEW**: Company logo CDN hosting |
| **Low Benefit** | **Polish**: <br>• **RENAME**: Analytics → Insights (clarity)<br>• **FIX**: Tooltip for company verification status<br>• **FIX**: Log unparsed locations | **Cleanup**: <br>• **REFACTOR**: Nullable field reduction<br>• **FIX**: Multiple seniority levels per job | **Experimental**: <br>• **NEW**: User accounts (saved jobs)<br>• **NEW**: Interview prep content<br>• **NEW**: "Other" country selector |

---

## Prioritized Implementation Roadmap

### Phase 1: Critical Bug Fixes & Trust Foundation (Week 1-2) ✅ COMPLETED
**Goal:** Fix the most visible user-facing issues and establish data transparency. **Status: All items completed on March 8, 2026.**

> **Note:** All Phase 1 items have been completed. See implementation details in the codebase. Detailed implementation plans for Phase 2+ features are available in `docs/implementation-plans/`.

#### 1.1 Company Tech Stack Aggregation [CRITICAL - HIGH IMPACT] ✅ COMPLETED
**Problem:** Company profile pages show empty or stale tech stacks because technologies are not aggregated from active job postings at query time.

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/persistence/company/CompanyMapper.kt`

**Implementation:**
```kotlin
// Aggregate technologies from active jobs at query time (lines 86-93)
val techFromJobs = jobsResult.values
    .flatMap { r ->
        if (r.get(JobFields.TECHNOLOGIES).isNull) emptyList()
        else r.get(JobFields.TECHNOLOGIES).repeatedValue.map { it.stringValue }
    }
    .map { TechFormatter.format(it) }
    .distinct()
    .sorted()

// Merge with company-level technologies (line 120)
val allTechs = (companyTechs + techFromJobs).distinct().sorted()
```

**Effort:** 2-3 hours | **Impact:** Fixes broken feature on all company pages

---

#### 1.2 Location Duplication Bug [HIGH] ✅ COMPLETED
**Problem:** Locations display as "Auckland, Auckland" or "Sydney, New South Wales, New South Wales"

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/sync/RawJobDataParser.kt` - `parseLocation()` with knownLocations map
- `backend/src/main/kotlin/com/techmarket/util/LocationFormatter.kt` - Dedicated deduplication utility

**Implementation:**
- `LocationFormatter.format()` removes adjacent duplicate parts (e.g., "Auckland, Auckland" → "Auckland")
- `RawJobDataParser.knownLocations` map provides pre-defined city/state/country tuples for major AU/NZ/ES cities
- Inline deduplication in `CompanyMapper.kt` (lines 121-128) for hiring locations

**Effort:** 1-2 hours | **Impact:** Fixes UX across all location displays

---

#### 1.3 PII Filter Audit [HIGH - Privacy/Compliance] ✅ COMPLETED
**Problem:** PII sanitizer may not catch all edge cases like recruiter phone numbers in job descriptions.

**Test Case:** "If you would like to find out more, call Bob Blob on 021 999 111"

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/util/PiiSanitizer.kt`
- `backend/src/test/kotlin/com/techmarket/util/PiiSanitizerTest.kt`

**Implementation:**
- Enhanced `PHONE_REGEX` to capture AU/NZ formats:
  - Australian: +61 4 1234 5678, 0412 345 678, (02) 1234 5678
  - New Zealand: 021 123 4567, 09 123 4567, +64 21 123 4567
- Added comprehensive unit tests (25+ test cases) covering:
  - Regional TLDs (.co.nz, .com.au)
  - AU mobile/landline formats
  - NZ mobile/landline formats
  - Real-world recruiter contact scenarios

**Effort:** 2-3 hours | **Impact:** Privacy compliance, user trust

---

#### 1.4 Data Source Transparency [MEDIUM - Trust Signal] ✅ COMPLETED
**Problem:** Users don't know where job data comes from or how fresh it is.

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/api/model/CommonDto.kt` - Added `source` and `lastSeenAt` fields to `JobRoleDto`
- `backend/src/main/kotlin/com/techmarket/persistence/company/CompanyMapper.kt` - Populates source/lastSeenAt
- `backend/src/main/kotlin/com/techmarket/persistence/job/JobMapper.kt` - Populates source/lastSeenAt for similar roles
- `backend/src/main/kotlin/com/techmarket/persistence/tech/TechMapper.kt` - Populates source/lastSeenAt for tech pages
- `frontend/src/lib/api.ts` - Updated `JobRoleDto` TypeScript interface
- `frontend/src/pages/CompanyProfilePage.tsx` - Added source badge and "last seen" timestamp
- `frontend/src/components/tech/MarketTab.tsx` - Added source badge and "last seen" timestamp

**Implementation:**
- Backend DTOs now include `source: String` and `lastSeenAt: Instant` fields
- Frontend displays:
  - Source badge with shield icon (e.g., "LinkedIn", "Company Website")
  - Relative timestamp (e.g., "Seen 2h ago", "Seen yesterday")
- `formatLastSeen()` helper function provides human-readable relative times

**Effort:** 3-4 hours | **Impact:** Significant trust increase

---

#### 1.5 Sitemap Generation [MEDIUM - SEO] ✅ COMPLETED
**Problem:** Search engines can't discover all tech/company pages.

**Files Created:**
- `backend/src/main/kotlin/com/techmarket/api/SitemapController.kt`
- `backend/src/test/kotlin/com/techmarket/api/SitemapControllerTest.kt`

**Implementation:**
- Dynamic sitemap endpoint at `/api/sitemap.xml`
- Includes:
  - Static pages (home, contact, transparency, privacy, terms)
  - Technology detail pages (`/tech/{name}`) with weekly changefreq
  - Company profile pages (`/company/{id}`) with weekly changefreq
- Proper `<lastmod>` timestamps from BigQuery `last_updated_at` fields
- Sitemap conforms to sitemaps.org protocol
- 6 unit tests covering XML structure, URL encoding, and well-formedness

**Effort:** 2-3 hours | **Impact:** Organic traffic growth

---

## ✅ Recently Completed: Background Jobs & Company File Separation

### Background Processing with Cloud Tasks ✅ COMPLETE (March 2026)

**Summary:** Migrated from synchronous webhook processing to asynchronous background jobs using Google Cloud Tasks.

**Impact:**
- Webhook response time: 30-120s → <100ms
- Zero timeout errors
- Intelligent retry logic with dead-letter queue
- 99.2% sync completion rate

**Key Files:**
- `backend/src/main/kotlin/com/techmarket/service/CloudTasksService.kt`
- `backend/src/main/kotlin/com/techmarket/api/internal/SyncTaskHandler.kt`
- `terraform/gcp/cloud_tasks.tf`

**Documentation:** [`docs/phase2/2.2-background-processing.md`](./phase2/2.2-background-processing.md)

---

### Company Manifest Migration ✅ COMPLETE (March 2026)

**Summary:** Migrated from single `companies.json` file (2,500+ lines) to directory-based structure with individual files per company.

**Impact:**
- Zero merge conflicts for parallel contributions
- PR review time: 30+ min → 5 min
- Error isolation (one file = one company)
- 108 company files migrated successfully

**Key Files:**
- `data/companies/*.json` (108 files)
- `backend/src/main/kotlin/com/techmarket/sync/CompanySyncService.kt`
- `scripts/companies/` (validation and enrichment scripts)

**Documentation:** [`docs/implementation-plans/5.1-directory-based-manifest-migration.md`](./implementation-plans/5.1-directory-based-manifest-migration.md)

---

### Company Manifest Validation System ✅ COMPLETE (March 2026)

**Summary:** Implemented multi-layer validation for safe public contributions to company data.

**Impact:**
- JSON Schema validation at file level
- Pre-commit hooks catch errors before commit
- CI validation blocks invalid PRs
- Automated formatting for consistent style

**Key Files:**
- `data/companies/schema.json`
- `scripts/companies/validate_all.py`
- `.github/workflows/validate-companies.yml`

**Documentation:** [`docs/implementation-plans/5.2-company-manifest-validation.md`](./implementation-plans/5.2-company-manifest-validation.md)

---

### Automated Contract Validation ✅ COMPLETE (March 2026)

**Summary:** Implemented automatic extraction of field requirements from mapper source code to catch query/mapper mismatches.

**Impact:**
- Zero field-mismatch bugs in production
- No manual `requiredFields` maintenance
- Contract tests auto-update with mapper changes

**Key Files:**
- Contract test infrastructure
- Field extractor utilities
- Source code parser

**Documentation:** [`docs/phase2/2.6-automated-contract-validation.md`](./phase2/2.6-automated-contract-validation.md)

---

### Phase 2: Data Quality & Reliability Engine (Week 3-5)

**Goal:** Make the data pipeline more robust, scalable, and comprehensive.

**Status:** 6/8 features complete or in progress. See detailed documentation in `docs/phase2/`.

> **✅ Completed:**
> - [2.0 Self-Hosted AI Crawler](../crawler-service/README.md) - COMPLETE (3 days)
> - [2.1 Salary Normalization](./phase2/2.1-salary-normalization.md) - COMPLETE (8-10 hours)
> - [2.2 Background Processing](./phase2/2.2-background-processing.md) - COMPLETE (14 hours)
> - [2.6 Automated Contract Validation](./phase2/2.6-automated-contract-validation.md) - COMPLETE (8-10 hours)
> - [2.5 Type-Safe Query Results](./features/typed-row-abstraction-plan.md) - COMPLETE (12-16 hours)
>
> **🔄 Backend Complete (UI/Integration Pending):**
> - [2.3 Dead Link Detection](./phase2/2.3-dead-link-detection.md) - Backend complete, UI pending (6-8 hours)
> - [2.4 ATS Integrations](./phase2/2.4-ats-integrations.md) - Backend complete, integration pending (12-16 hours)
> - [2.5 Visa Sponsorship](./phase2/2.5-visa-sponsorship.md) - Backend complete, UI pending (4-6 hours)

---

#### 2.1 Salary Normalization Engine [HIGH - User Value] ✅ COMPLETE

**See full documentation:** `docs/phase2/2.1-salary-normalization.md`

**What Was Built:**
- `NormalizedSalary` data class with amount, currency, period, source, isGross fields
- Comprehensive parsing for NZD, AUD, USD, EUR formats
- European number format support (35.000€ = 35k, not 35)
- Spanish "Bruto/Neto" (gross/net) detection
- "Plus super/benefits" extraction (base salary only)
- Confidence model: JOB_POSTING (HIGH), ATS_API (HIGH), MARKET_DATA (MEDIUM), AI_ESTIMATE (LOW)
- BigQuery STRUCT schema for clean storage
- Frontend locale-aware formatting with confidence badges

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/model/NormalizedSalary.kt` (new)
- `backend/src/main/kotlin/com/techmarket/sync/RawJobDataParser.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/model/JobRecord.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/job/JobBigQueryRepository.kt`
- `frontend/src/lib/salary.ts` (new)
- Frontend components updated

**Testing:** 25+ unit tests covering real-world salary formats

**Success Metrics:**
- ✅ 90%+ of explicit salary data parsed correctly
- ✅ Salary display shows currency and period
- ✅ Confidence badges display on UI

---

#### 2.2 Background Processing with Cloud Tasks [HIGH - Scalability] ✅ COMPLETE

**See full documentation:** `docs/phase2/2.2-background-processing.md`

**What Was Built:**
- Cloud Tasks queue infrastructure (primary + DLQ)
- `CloudTasksService` for queuing sync tasks
- `SyncTaskHandler` internal endpoint with intelligent retry logic
- Transient vs permanent error classification
- `X-Cloud-Tasks` header validation + IAM security
- Correlation ID tracing for end-to-end debugging
- Daily health check scheduler (Phase 2.3 prep)
- Automated deployment script (`setup-cloud-tasks.sh`)

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/service/CloudTasksService.kt` (new)
- `backend/src/main/kotlin/com/techmarket/api/internal/SyncTaskHandler.kt` (new)
- `backend/src/main/kotlin/com/techmarket/config/CloudTasksConfig.kt` (new)
- `backend/src/main/kotlin/com/techmarket/config/SecurityConfig.kt`
- `backend/src/main/kotlin/com/techmarket/config/WebhookBypassFilter.kt` (new)
- `backend/src/main/kotlin/com/techmarket/webhook/ApifyWebhookController.kt`
- `backend/src/main/kotlin/com/techmarket/api/AdminController.kt`
- `backend/src/main/kotlin/com/techmarket/scheduler/HealthCheckScheduler.kt` (new)
- `terraform/gcp/cloud_tasks.tf` (new)
- `scripts/deployment/setup-cloud-tasks.sh` (new)

**Testing:** 18 new unit tests, 324 total tests passing

**Performance Metrics:**
| Metric | Before | After | Target |
|--------|--------|-------|--------|
| Webhook response time | 30-120s | <100ms | <500ms ✅ |
| Timeout errors | ~5% | 0% | 0% ✅ |
| Max concurrent syncs | 1 | 5 | 5 ✅ |
| Retry success rate | N/A | 92% | >90% ✅ |

**Security:**
- ✅ Cloud Run IAM restricts access to Cloud Tasks service account
- ✅ `allUsers` access removed
- ✅ `X-Cloud-Tasks` header validation
- ⚠️ OIDC tokens deferred (documented in Known Issues)

**Deployment:**
```bash
./scripts/deployment/setup-cloud-tasks.sh
```

---

#### 2.3 Dead Link Detection Worker [HIGH - Data Freshness] 🔄 Backend Complete

**Status:** Backend implementation complete, UI/monitoring pending

**See full documentation:** `docs/phase2/2.3-dead-link-detection.md`

**Problem:** "Ghost jobs" - filled roles that remain visible because apply URLs go stale.

**Files Created:**
- `backend/src/main/kotlin/com/techmarket/service/JobHealthCheckService.kt` ✅
- `backend/src/main/kotlin/com/techmarket/service/HttpHealthChecker.kt` ✅
- `backend/src/main/kotlin/com/techmarket/scheduler/HealthCheckScheduler.kt` ✅
- `backend/src/main/kotlin/com/techmarket/util/HealthCheckConstants.kt` ✅

**Implementation:**
- Daily scheduled job (Cloud Scheduler)
- Concurrent HEAD requests to all active `applyUrls`
- Mark jobs as `CLOSED` if 404/redirect to generic careers page
- Add `url_last_checked` and `url_status` fields to jobs table
- Alerting on high failure rates

**Remaining Work:**
- [ ] Add URL health status to admin dashboard
- [ ] Configure Cloud Scheduler trigger
- [ ] Set up alerting (Slack/email)

**Effort:** 6-8 hours | **Impact:** Zero ghost jobs, high user trust

---

#### 2.4 ATS Direct Integrations [HIGH - Market Coverage] 🔄 Active

**Status:** GH/LV/AS built and tested (38 unit tests). 240 companies ready to activate. Tier-2 providers (SR/TT/Workable) planned — see Phase 6.

**See full documentation:** `docs/phase2/2.4-ats-integrations.md` + `docs/implementation-plans/crawler-extraction-improvement-plan.md`

**Current ATS coverage (as of March 17, 2026):**

| Tier | Provider | Companies | Status |
|------|----------|-----------|--------|
| 1 | Greenhouse | 94 | ✅ Client built — ready to activate |
| 1 | Lever | 105 | ✅ Client built — ready to activate |
| 1 | Ashby | 44 | ✅ Client built — ready to activate |
| 2 | SmartRecruiters | 157 | ⏳ Slugs identified, client not built |
| 2 | TeamTailor | 99 | ⏳ Slugs identified, client not built |
| 2 | Workable | 28 | ⏳ Slugs identified, client not built |
| 3 | Workday + others | ~89 | Crawler fallback (no public API) |
| — | Unidentified | 792 | Crawler |

**Files Created:**
- `AtsProvider.kt`, `AtsClient.kt`, `AtsClientFactory.kt`, `AtsNormalizer.kt`, `AtsNormalizerFactory.kt` ✅
- `GreenhouseClient.kt` + `GreenhouseNormalizer.kt` ✅
- `LeverClient.kt` + `LeverNormalizer.kt` ✅ (added March 2026)
- `AshbyClient.kt` + `AshbyNormalizer.kt` ✅ (added March 2026)
- `NormalizedJob.kt`, `SyncStatus.kt`, `AtsJobDataSyncService.kt`, `AtsSyncController.kt` ✅

**To activate GH/LV/AS (no code needed):**
```bash
POST /api/admin/pipeline/sync-companies  # seeds 240 configs to BigQuery
POST /api/internal/ats-sync-all          # triggers first extraction
```

**Remaining Work (Phase 6):**
- [x] **7.1** Crawler job persistence — Done (`CrawlerJobMapper.kt`, `CrawlerJobPersistenceService.kt`)
- [x] **P1.8** TeamTailorClient.kt + normalizer — Done
- [x] **P1.9** SmartRecruitersClient.kt + normalizer — Done
- [x] **P1.10** WorkableClient.kt + normalizer — Done
- [ ] ATS sync monitoring in admin dashboard analytics (Phase 7.3)

**Identification tools available:**
- `scripts/ats/probe_free_ats.py` — slug probing (A)
- `scripts/ats/extract_ats_from_apply_urls.py` — BigQuery URL extraction (B)
- `scripts/ats/scan_careers_pages.py` — seed page scan (C)
- `scripts/ats/probe_vanity_domains.py` — domain probing incl. SR + TT (D)
- `scripts/ats/parse_autechjobs.py` — cross-reference autechjobs historical data
- `scripts/ats/validate_ats_configs.py` — live API validation

**Impact:** 527 companies with native ATS extraction once tier-2 providers built

---

#### 2.5 Visa Sponsorship Tracking [MEDIUM - High User Value] ✅ COMPLETE

**Status:** ✅ Done — full implementation including frontend. Badge and filter in `CompaniesPage.tsx` and `CompanyProfilePage.tsx`.

**See full documentation:** `docs/phase2/2.5-visa-sponsorship.md`

**Problem:** Migrant workers can't filter for companies that sponsor visas.

**Files Modified:**
- `backend/src/main/kotlin/com/techmarket/sync/model/CompanyJsonDto.kt` - Added `visaSponsorship` field ✅
- `backend/src/main/kotlin/com/techmarket/persistence/model/CompanyRecord.kt` - Added to BigQuery model ✅
- `backend/src/main/kotlin/com/techmarket/persistence/company/CompanyBigQueryRepository.kt` - Persistence layer ✅
- `backend/src/main/kotlin/com/techmarket/persistence/company/CompanyMapper.kt` - Mapping logic ✅
- `backend/src/main/kotlin/com/techmarket/api/model/CompanyProfilePageDto.kt` - API DTO ✅
- `backend/src/main/kotlin/com/techmarket/sync/CompanySyncService.kt` - Sync from manifest ✅
- `backend/src/main/kotlin/com/techmarket/sync/SilverDataMerger.kt` - Data merging ✅
- `backend/src/main/kotlin/com/techmarket/models/QueryRows.kt` - Type-safe query results ✅

**Implementation:**
- `visaSponsorship` field added to company data model (boolean)
- Data flows from company manifest → BigQuery → API response
- Backend ready for filtering and display
- Field populated from `data/companies/*.json` files

**Remaining Work:**
- [x] Add visa sponsorship badge to company cards (frontend)
- [x] Add visa sponsorship filter to company search (frontend)
- [x] Display visa info on company profile page (frontend)
- [ ] Audit and update visa sponsorship data in company manifest files (data quality — ongoing)

**Effort:** 4-6 hours | **Impact:** Critical for migrant job seekers

---

#### 2.5 Type-Safe Query Results [MEDIUM - Code Quality & Safety] ✅ COMPLETE

**Status:** Implementation complete.

**See full documentation:** `docs/features/typed-row-abstraction-plan.md`

**Problem:** String-based field access between queries and mappers causes runtime errors when fields are missing.

**Proposed Solution:**
- Introduce type-safe data classes for query results
- Compile-time verification of field access
- Eliminate runtime `IllegalArgumentException` errors

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/models/QueryRows.kt` (already exists with partial implementation)
- Type-safe row classes for each query type

**Files to Modify:**
- All `*Queries.kt` files - Return type-safe query objects
- All `*Mapper.kt` files - Use type-safe field access
- Contract tests - Auto-extract required fields from mapper source

**Effort:** 12-16 hours | **Impact:** Eliminate field-mismatch bugs, improve code safety

---

#### 2.6 Automated Contract Validation [HIGH - Reliability] ✅ COMPLETE

**Status:** Implementation complete

**See full documentation:** `docs/phase2/2.6-automated-contract-validation.md`

**Problem:** Manual `requiredFields` lists in contract tests can become stale, allowing bugs to slip through.

**Solution:**
- Automatically extract field requirements from mapper source code
- Parse mapper Kotlin files to detect which fields are accessed
- Generate contract tests dynamically based on actual usage

**Implementation:**
- Contract tests that parse mapper source code
- Auto-extract field requirements (no manual maintenance)
- Catch query/mapper mismatches at test time

**Files Modified:**
- Contract test infrastructure
- Field extractor utilities
- Source code parser for field detection

**Success Metrics:**
- ✅ Zero field-mismatch bugs in production
- ✅ Contract tests auto-update with mapper changes
- ✅ No manual `requiredFields` maintenance

**Effort:** 8-10 hours | **Impact:** Catch bugs before production, eliminate manual maintenance

---

### Phase 3: Market Expansion & Discovery (Week 6-8)
**Goal:** Expand beyond LinkedIn and improve content discoverability.

> **📄 Implementation Plans Available:**
> - [3.1 SEEK & TradeMe Integration](./implementation-plans/3.1-seek-trademe-integration-plan.md) (10-14 hours)
> - [3.2 Technology Domain Hubs](./implementation-plans/3.2-technology-domain-hubs-plan.md) (12-16 hours)
> - [3.3 Technology Exclusion Filters](./implementation-plans/3.3-technology-exclusion-filters-plan.md) (4-6 hours)
> - [3.4 Trending Lists](./implementation-plans/3.4-trending-lists-plan.md) (6-8 hours)

#### 3.1 SEEK & TradeMe Integration [HIGH - Market Coverage]
**Problem:** LinkedIn-only sourcing misses 50-60% of ANZ tech jobs.

**Approach:** Use Apify Store scrapers (don't build custom)
- Create separate Apify tasks for each job board
- Implement deduplication logic (same job on LinkedIn + SEEK)
- Prefer direct ATS links over job board links

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/sync/JobDataSyncService.kt`
- `backend/src/main/kotlin/com/techmarket/sync/model/ApifyJobDto.kt`

**Effort:** 10-14 hours | **Impact:** Doubles job coverage

---

#### 3.2 Technology Domain Hubs ✅ COMPLETE (March 2026)

**Summary:** Implemented specialized career-stage and technology-category hubs to help users discover high-level market trends.

**Impact:**
- 8 specialized hubs (Web, Mobile, Cloud, Data, Security, etc.)
- Batch query optimization: Consolidated 8 subqueries into a single optimized `GROUP BY` pass
- Market share and growth trend computation for whole categories
- High-level SEO landing pages for technology domains

**Key Files:**
- `backend/src/main/kotlin/com/techmarket/model/TechCategory.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/analytics/HubQueries.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/analytics/InsightsBigQueryRepositoryImpl.kt`
- `frontend/src/pages/HubPage.tsx`
- `frontend/src/components/landing/LandingPage.tsx`

**Documentation:** [`docs/phase3/3.2-technology-domain-hubs.md`](./phase3/3.2-technology-domain-hubs.md)

---

#### 3.3 Technology Exclusion Filters [MEDIUM - UX]
**Problem:** Native iOS developers see Xamarin/React Native jobs when filtering for iOS.

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/util/TechFormatter.kt` - Add metadata
- `backend/src/main/kotlin/com/techmarket/persistence/job/JobQueries.kt`

**Implementation:**
```kotlin
// API: GET /api/tech/iOS?exclude=Xamarin,ReactNative,Flutter
val exclusions = mapOf(
    "iOS" to listOf("Xamarin", "React Native", "Flutter"),
    "Android" to listOf("Xamarin", "React Native", "Flutter")
)
```

**Effort:** 4-6 hours | **Impact:** Cleaner search results

---

#### 3.4 Trending Lists [MEDIUM - Engagement]
**Problem:** No visibility into what's hot (most viewed jobs/companies/tech).

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/service/TrendingService.kt`
- BigQuery table: `page_views` (track via Vercel Analytics)

**Implementation:**
- Track page views via Vercel Analytics API
- Compute trending (7-day rolling window)
- Add "Trending This Week" sections to landing page

**Effort:** 6-8 hours | **Impact:** User engagement, FOMO

---

#### 3.5 Stale Job Archival [LOW - Maintenance]
**Problem:** Jobs from mid-2025 still appear in database.

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/sync/RawJobDataMapper.kt`

**Implementation:**
- Current cutoff is 6 months - verify this is working
- Add `archived_at` field instead of hard delete (for audit)
- Add admin endpoint to view archived jobs

**Effort:** 2-3 hours | **Impact:** Data cleanliness

---

### Phase 4: UX Polish & Mobile Excellence (Week 9-11)
**Goal:** Deliver a best-in-class mobile experience and visual polish.

> **📄 Implementation Plans Available:**
> - [4.1 Mobile UX Overhaul](./implementation-plans/4.1-mobile-ux-overhaul-plan.md) (16-20 hours)

#### 4.1 Mobile UX Overhaul [HIGH - User Retention]
**Problem:** Mobile design is "meh" - job rows, headers, and navigation need work.

**Areas to Address:**
- [ ] Job list cards: Better hierarchy, larger tap targets
- [ ] Page headers: Responsive typography, compact layout
- ✅ Navigation: Smooth scroll show/hide for navbar
- [ ] Company page refresh bug (404 on mobile)

**Files to Modify:**
- `frontend/src/components/job/JobCard.tsx`
- `frontend/src/components/layout/PageHeader.tsx`
- `frontend/src/components/layout/Navbar.tsx`

**Effort:** 16-20 hours | **Impact:** Mobile user retention

---

#### 4.2 Company Logo CDN Hosting [MEDIUM - Reliability]
**Problem:** LinkedIn logo URLs expire, causing broken images.

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/service/LogoFetcherService.kt`

**Implementation:**
- On company discovery, download logo to Vercel Blob/S3
- Store stable CDN URL in company record
- Fallback to placeholder if no logo

**Effort:** 4-6 hours | **Impact:** Professional appearance

---

#### 4.3 Technology Icons & Brand Colors [LOW - Polish]
**Problem:** Tech icons lack visual consistency.

**Files to Modify:**
- `frontend/src/constants/techBrandColors.ts` (exists)
- `frontend/public/icons/` - Download all tech logos

**Implementation:**
- Use `download_all_icons.mjs` script
- Apply brand colors consistently
- Add fallback icons

**Effort:** 3-4 hours | **Impact:** Visual polish

---

#### 4.4 Dark Theme [LOW - User Preference]
**Status:** Partially implemented via `next-themes`

**Files to Modify:**
- Frontend Tailwind config
- Component-level dark mode variants

**Effort:** 8-10 hours | **Impact:** User preference support

---

### Phase 5: Advanced Features & Community (Week 12+)
**Goal:** Build features that create network effects and community ownership.

#### 5.1 Resource Data Modeling [MEDIUM - Maintainability]
**Problem:** `techResources.ts` is hard-coded and doesn't scale.

**Files to Modify:**
- See `docs/data/resources/resource-data-modeling.md`

**Implementation:**
- Phase 1: Managed JSON in S3/Supabase Storage
- Phase 2: Supabase PostgreSQL with real-time updates
- Many-to-many tech-resource mapping

**Effort:** 8-12 hours | **Impact:** Easier content updates

---

#### 5.2 Public Company Data Repository [MEDIUM - Community]
**Problem:** Company metadata requires manual updates.

**Implementation:**
- Create public GitHub repo for `companies.json` contributions
- Add contribution guidelines and schema validation
- Backend pulls from repo on sync

**Effort:** 6-8 hours | **Impact:** Community-driven data quality

---

#### 5.3 Advanced Analytics Charts [MEDIUM - Insights]
**Problem:** Limited market insights visualizations.

**Charts to Add:**
- Jobs per capita comparison (by country)
- Salary trends over time
- Seniority distribution (Junior/Mid/Senior split)
- Company posting frequency timeline

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/persistence/analytics/AnalyticsBigQueryRepository.kt`
- Frontend chart components

**Effort:** 12-16 hours | **Impact:** Market intelligence value

---

#### 5.4 User Accounts & Saved Items [LOW - Future]
**Problem:** No way to track favorite companies/tech.

**Implementation:**
- Add authentication (NextAuth or Clerk)
- Saved companies/technologies
- Email notifications for new jobs

**Effort:** 20-30 hours | **Impact:** User retention (but high effort)

---

### Phase 6: Crawler Pipeline Data Quality (Active — March 2026)

**Goal:** Make every crawl produce jobs that actually appear on the public site, and scale native ATS coverage from 240 → 527 companies.

**Status:** ATS identification sprint 2 complete (579/1,371 companies identified). Clients for GH/LV/AS built and passing CI. Sprint 2 work begins now.

#### 6.0 Crawler Job Persistence ✅ COMPLETE

**Status:** ✅ Done — `CrawlerJobMapper.kt`, `CrawlerJobPersistenceService.kt`, `NormalizedJobDto.kt` all exist.

**See full plan:** `docs/implementation-plans/7.1-crawler-job-persistence.md`

**What to build:**
- `NormalizedJobDto.kt` — Kotlin mirror of the TypeScript `NormalizedJob` response type
- `CrawlerJobMapper.kt` — maps `NormalizedJob` → `JobRecord` using existing `RawJobDataParser`
- `CrawlerJobPersistenceService.kt` — map → fetch existing → merge → save
- Wire into `CrawlerAdminController.triggerCrawl()` after the existing seed upsert
- Add `findByCompanyId()` to `JobBigQueryRepository`

**Effort:** 4-6 hours | **Impact:** Crawler jobs appear on the site for the first time

---

#### 6.1 Activate GH/LV/AS ATS Pipeline (240 companies) ✅ COMPLETE

**Status:** ✅ Done — Lever and Ashby clients wired into `AtsClientFactory` alongside Greenhouse. All three are `@Component` beans in the factory.

**Action:**
```bash
POST /api/admin/pipeline/sync-companies   # seeds 240 ATS configs to BigQuery
POST /api/internal/ats-sync-all           # first extraction run
```

**Effort:** ~30 minutes | **Impact:** ~240 companies get structured job data immediately

---

#### 6.2 TeamTailor Client (99 companies) [P1.8] ✅ COMPLETE

**Status:** ✅ Done — `TeamTailorClient.kt` exists in `backend/src/main/kotlin/com/techmarket/sync/ats/teamtailor/`.

**API:** `GET https://{slug}.teamtailor.com/feed/jobs.json`
**Fields:** id, title, apply-url, locations (array), department, tags. No auth required.

**Files to create:**
- `backend/src/main/kotlin/com/techmarket/sync/ats/teamtailor/TeamTailorClient.kt`
- `backend/src/main/kotlin/com/techmarket/sync/ats/teamtailor/TeamTailorNormalizer.kt`
- Wire into `AtsClientFactory`, `AtsNormalizerFactory`, `application.yml`

**Effort:** 3-4 hours | **Impact:** 99 more companies, lowest effort of the tier-2 providers

---

#### 6.3 SmartRecruiters Client (157 companies) [P1.9] ✅ COMPLETE

**Status:** ✅ Done — `SmartRecruitersClient.kt` exists in `backend/src/main/kotlin/com/techmarket/sync/ats/smartrecruiters/`.

**API:** `GET https://api.smartrecruiters.com/v1/companies/{slug}/postings?status=PUBLISHED&limit=100`
**Pagination:** offset-based. Full job detail via separate `GET .../postings/{jobId}`.
**Important:** Do not use the postings API as an existence check — it returns HTTP 200 for any slug including made-up ones. Board existence is validated by redirect check.

**Files to create:**
- `SmartRecruitersClient.kt` + `SmartRecruitersNormalizer.kt`
- Wire into factories and `application.yml`

**Effort:** 5-6 hours | **Impact:** 157 more companies — largest gain of the tier-2 providers

---

#### 6.4 Workable Client (28 companies) [P1.10] ✅ COMPLETE

**Status:** ✅ Done — `WorkableClient.kt` exists in `backend/src/main/kotlin/com/techmarket/sync/ats/workable/`.

**API:** `POST https://apply.workable.com/api/v3/accounts/{slug}/jobs` — no auth, returns `{total, results[]}`. No JSON detail endpoint; description will require crawl fallback or be omitted.

**Effort:** 4-5 hours | **Impact:** 28 more companies; completes tier-2 ATS sweep (total coverage: 527 companies)

---

#### 6.5 autechjobs Historical Data Load [ID-12–14]

**Status:** 🔄 Script ready, execution pending. SQL dump exists at `data/third-party/autechjobs/Cloud_SQL_Export_2026-03-17.sql` (294 MB). `--export-jsonl` flag fully implemented in `parse_autechjobs.py`. BigQuery load (ID-13) and trend backfill (ID-14) still need to be run manually.

**Steps:**
```bash
# ID-12: Export to JSONL
python3 scripts/ats/parse_autechjobs.py --export-jsonl --output data/third-party/autechjobs/historical_jobs.jsonl

# ID-13: Load to BigQuery
bq load --source_format=NEWLINE_DELIMITED_JSON techmarket.autechjobs_historical historical_jobs.jsonl

# ID-14: Derive monthly tech_job_counts back to 2020
# (BigQuery transformation query — see docs/data/autechjobs-historical-data.md)
```

**Effort:** 2-3 hours | **Impact:** 5+ years of trend data for market insights charts

---

#### 6.6 Self-Improving Crawler: Extraction Hints [P2.5–2.8] ✅ COMPLETE

**Status:** ✅ Done — `extractionHints` is referenced and consumed in `GeminiExtractionService.ts` (lines 35, 97-98), fed into the Gemini prompt for companies with prior crawl history.

**What to build:**
- `ExtractionHints` schema (TypeScript interface in crawler-service) — stores pagination type, job container selector, last successful titles, etc.
- `hints` JSON column on `crawler_seeds` BigQuery table
- `CrawlerSeedRepository.updateHints()` method
- Pass hints into Gemini extraction prompt in `ContentExtractor.ts`
- Track `lastSuccessfulJobCount` + emit `[REGRESSION_WARNING]` on zero-yield for previously-productive seeds

**Effort:** 4-6 hours | **Impact:** Crawler improves quality run-over-run; regressions become detectable

---

#### 6.7 Snapshot Fixture Test Suite [P2.7]

**Status:** ⏳ Pending. No `crawler-service/fixtures/` directory exists. Design complete in §2.3 of crawler extraction plan.

**Goal:** 20+ HTML snapshots of real career pages (post-render, captured by Playwright) with expected job counts + field assertions. Runs as a local test suite without hitting the live internet.

**Priority companies:** Datacom, Xero, Trade Me, Westpac, Weta FX, Alan (Ashby), Halter

**Effort:** 4-6 hours | **Impact:** Catch extraction regressions before they hit production

---

### Phase 7: Admin Panel

**Goal:** Operational visibility and control over the crawler/ingestion pipeline without touching raw JSON files or running API calls manually.

**See full design:** `docs/admin-panel-plan.md`

**Auth prerequisite:** Bearer token (env-var secret) before shipping to production. Long-term: Google IAP.

**Current state:** Frontend admin UI is **substantially built** — `frontend/src/admin/` contains `AdminLayout.tsx`, `DashboardPage.tsx`, `CompaniesPage.tsx`, `CrawlsPage.tsx`, `PipelinePage.tsx`, `AnalyticsPage.tsx`, `LoginPage.tsx`. Backend `AdminController` has 6 endpoints but is missing the dead-letters browser and jobs browser APIs.

#### Phase 7.1 — Foundation ✅ COMPLETE

- [x] `/admin` route family in React Router with token auth screen (`LoginPage.tsx`)
- [x] `AdminLayout` (sidebar nav, toast system)
- [x] Company table page (`CompaniesPage.tsx`)
- [x] Dashboard page (`DashboardPage.tsx`)
- [x] Crawl operations page (`CrawlsPage.tsx`)

#### Phase 7.2 — Pipeline Observability 🔄 Frontend Done, Backend Partial

- [x] Pipeline page frontend (`PipelinePage.tsx`)
- [x] Backend: `POST /api/admin/trigger-sync`, `POST /api/admin/sync-companies`, `POST /api/admin/reprocess-jobs`
- [ ] Backend: `GET /admin/pipeline/ingestions` — paginated ingestion history
- [ ] Backend: dead letter queue read + retry + discard endpoints
- [ ] Backend: `GET /admin/jobs` — paginated job browser with filters

**Remaining:** ~1 week of backend work to expose the pipeline data the frontend is ready to consume.

#### Phase 7.3 — Analytics ✅ COMPLETE (Frontend)

- [x] Analytics page frontend (`AnalyticsPage.tsx`)
- [x] Backend: `GET /api/admin/health-check/stats`

#### Phase 7.4 — Job Browser + Quality Tools

- [ ] Backend: `GET /admin/jobs` — paginated job browser with filters (company, source, status, country, tech)
- [ ] Job detail modal with raw JSON + flag/review actions
- [ ] URL health check trigger per company from admin UI
- [ ] Discovery success rates by ATS provider chart

---

### Phase 8: Staging Environment

**Status:** ⏳ Pending. **Plan is fully documented — `docs/deployment/staging-environment.md` — ~2 hours of setup.** No deploy scripts or `application-staging.yml` exist yet.

**See:** `docs/deployment/staging-environment.md`

**Why now:** Every change currently ships directly to production. This is the root cause of the "keep breaking in production" problem noted in TODO.md.

**Setup checklist:**
- [ ] `bq mk --dataset techmarket_staging` + copy table schemas
- [ ] `gcloud storage buckets create gs://techmarket-bronze-ingestions-staging`
- [ ] `gcloud tasks queues create techmarket-sync-queue-staging`
- [ ] Create `application-staging.yml` in backend
- [ ] Create `.env.staging` (gitignored) + deploy scripts
- [ ] Set Vercel env vars scoped to `staging` branch
- [ ] Create `staging` git branch

**Estimated cost:** ~$0–$2/month (all resources scale to zero).

---

### Phase 9: GCS Bronze Cold Storage ✅ COMPLETE

**Status:** ✅ Done — `BronzeGcsRepository.kt`, `BronzeIngestionManifest.kt`, `BronzeRepository.kt` interface, and `terraform/gcp/storage_bucket.tf` all exist.

**See full plan:** `docs/implementation-plans/6.1-file-based-cold-storage-plan.md`

**Trigger to start:** When monthly ingestion storage cost exceeds ~$20/month, or before enabling daily Apify + 500+ ATS companies.

**Expected savings:** 93% storage cost reduction at scale ($300/month → $20/month for 500GB).

**Key design decisions (already resolved):**
- NDJSON + gzip files in GCS (Parquet as future optimization)
- 90-day STANDARD → COLDLINE → 1-year ARCHIVE lifecycle
- `ingestion_metadata` BigQuery table as queryable index (not raw storage)
- Dual-write migration strategy (2 weeks overlap before cutover)

---

## Technical Debt & Refactoring

### T1: Rename Analytics to Insights [LOW]
**Rationale:** Avoid confusion with actual analytics (page views, etc.)

**Files to Rename:**
- `com.techmarket.persistence.analytics` → `com.techmarket.persistence.insights`
- `AnalyticsBigQueryRepository` → `InsightsBigQueryRepository`
- `AnalyticsMapper` → `InsightsMapper`

**Effort:** 2-3 hours (find/replace) | **Impact:** Code clarity

---

### T2: Nullable Field Reduction [MEDIUM]
**Problem:** Many fields are nullable that could be non-nullable with defaults.

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/persistence/model/JobRecord.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/model/CompanyRecord.kt`

**Effort:** 4-6 hours | **Impact:** Fewer null checks, cleaner code

---

### T3: Test Coverage Expansion [MEDIUM]
**Critical Test Gaps:**
- `RawJobDataParserTest`: Salary parsing, location edge cases
- `JobDataSyncServiceTest`: Cloud Tasks integration, error scenarios
- `CompanyMapperTest`: Tech aggregation scenarios
- `CrawlHealthTest`: Dead link detection

**Effort:** 8-12 hours | **Impact:** Confidence in refactoring

---

## Success Metrics

| Metric | Current | Target (3 months) | Measurement |
|--------|---------|-------------------|-------------|
| Job Coverage (ANZ tech market) | ~40% | 90%+ | Compare to SEEK/LinkedIn total |
| Ghost Jobs (filled but visible) | Unknown | <5% | Dead link detection rate |
| Company Tech Stack Accuracy | Broken | 100% | Manual audit |
| Mobile Bounce Rate | Unknown | <40% | Vercel Analytics |
| Organic Search Traffic | Unknown | +50% | Google Search Console |
| API Response Time (p95) | Unknown | <500ms | Cloud Run metrics |

---

## Verification Plan

### Automated Tests
- `RawJobDataParserTest`: Salary parsing, location deduplication, phone number PII
- `JobDataSyncServiceTest`: Cloud Tasks queuing, error handling, retry logic
- `CrawlHealthTest`: Dead link detector flags 404s correctly
- `CompanyMapperTest`: Tech aggregation from jobs, frequency sorting

### Manual Verification
- **UI Review**: Data source labels, sitemap.xml structure
- **Mobile Testing**: Physical device review of job cards, headers, navigation
- **Sync Audit**: Run SEEK/TradeMe ingestion, verify no BigQuery duplicates
- **Company Pages**: Verify tech stacks match active job postings

### Monitoring Dashboards
- Cloud Run: Request latency, error rates, CPU utilization
- BigQuery: Query costs, scan bytes, job duration
- Vercel Analytics: Page views, bounce rates, core web vitals

---

## Cost Analysis

| Service | Current | Projected (Post-Phase 3) |
|---------|---------|--------------------------|
| Apify | $2.50/mo | $10/mo (SEEK + TradeMe scrapers) |
| Domain | $7/mo | $7/mo |
| GCP (Cloud Run + BigQuery) | ~$1/mo (free tier) | ~$5/mo |
| Vercel | $0/mo (hobby) | $0/mo (hobby) |
| **Total** | **~$10.50/mo** | **~$22/mo** |

**Note:** Still well within budget for the value provided. Consider sponsorship/donations to offset costs.

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Apify scraper breaks (SEEK/TradeMe) | Medium | High | Use store-bought scrapers, monitor runs |
| BigQuery costs spike | Low | Medium | Set budget alerts, use materialized views |
| Cloud Run timeouts during sync | Medium | Medium | Implement Cloud Tasks (Phase 2.2) |
| Legal issues with scraped data | Low | High | Add disclaimers, robots.txt compliance, terms of service |
| ATS API changes break integration | Medium | Medium | Abstract API clients, add health checks |

---

## Appendix: Quick Reference

### Files Requiring Most Changes
1. `backend/src/main/kotlin/com/techmarket/sync/RawJobDataParser.kt` - Salary, location, PII
2. `backend/src/main/kotlin/com/techmarket/persistence/company/CompanyMapper.kt` - Tech aggregation
3. `backend/src/main/kotlin/com/techmarket/webhook/ApifyWebhookController.kt` - Cloud Tasks
4. `frontend/src/components/` - Mobile UX, domain hubs

### Key Documentation
- `docs/data-pipeline-flowchart.md` - Full data flow visualization
- `docs/data/company-data-strategy.md` - Master manifest system
- `docs/data/job-data-strategy.md` - Multi-channel sourcing
- `docs/data/ats/ats-identification-findings.md` - ATS integration targets
- `docs/implementation-plans/crawler-extraction-improvement-plan.md` - **ATS identification + self-improving crawler (active)**
- `docs/implementation-plans/7.1-crawler-job-persistence.md` - **Critical bug fix (jobs not saved)**
- `docs/admin-panel-plan.md` - **Admin panel full design (phases 1-5)**
- `docs/deployment/staging-environment.md` - Staging environment setup
- `docs/implementation-plans/6.1-file-based-cold-storage-plan.md` - GCS Bronze migration
- `docs/crawler_efficiency_strategy.md` - Two-tier discovery/harvesting strategy
- `docs/data/autechjobs-historical-data.md` - 161k historical jobs (staged, pending BigQuery load)
- `docs/implementation-plans/` - **Detailed implementation plans for all major features**

### External Dependencies
- **Apify Store**: SEEK and TradeMe scrapers
- **Google Cloud Tasks**: Background processing
- **Vercel Blob/S3**: Logo hosting
- **Supabase** (optional): Resource database
- **autechjobs.com.au**: Historical AU tech job data (SQL dump, 161k records, 2010–2026)

---

## 📊 Summary & Next Steps

### Total Implementation Effort (Updated March 17, 2026)

| Phase | Features | Total Effort | Priority Features |
|-------|----------|--------------|-------------------|
| **Phase 1** | 5 bug fixes | ✅ COMPLETED | All done |
| **Phase 2** | 8 features | 58-78 hours | Salary ✅, Cloud Tasks ✅, Contract Validation ✅, Query Rows ✅, Dead Links 🔄, ATS 🔄, Visa 🔄 |
| **Phase 3** | 4 features | 36-44 hours | SEEK/TradeMe ⏳, Domain Hubs ✅, Tech Exclusion ⏳, Trending ⏳ |
| **Phase 4** | 1 feature | 16-20 hours | Mobile UX ⏳ |
| **Phase 5** | 3 features | ✅ COMPLETED | Directory Manifest ✅, Validation ✅, Improvements ✅ |
| **Phase 6** | 8 features | mostly ✅ | Job persistence ✅, ATS activation ✅, TT/SR/WL ✅, extraction hints ✅, GCS ✅; autechjobs BQ load ⏳, snapshots ⏳ |
| **Phase 7** | 4 phases | Frontend ✅ partial | Admin frontend done; backend dead-letters + jobs browser missing |
| **Phase 8** | 1 feature | 2 hours | Staging environment — not yet set up |
| **Phase 9** | 1 feature | ✅ | GCS Bronze cold storage — Done |
| **Total remaining** | **~10 items** | **~30-50 hours** | **See Tier 1-4 in Next Opportunities (most done)** |

### Recommended Implementation Order

**Sprint 1 (Week 1-2): Foundation** ✅ COMPLETE
1. **2.2 Cloud Tasks** - Enables all background processing ✅
2. **2.1 Salary Normalization** - High user value ✅
3. **2.6 Contract Validation** - Catch bugs early ✅
4. **2.5 Type-Safe Query Results** - Code safety ✅

**Sprint 2 (Week 3-4): Data Quality** 🔄 In Progress
1. **2.3 Dead Link Detection** - Zero ghost jobs (backend done, UI pending)
2. **2.4 ATS Integrations** - GH/LV/AS built + 240 companies ready; SR/TT/Workable pending
3. **2.5 Visa Sponsorship** - Quick win (backend done, UI pending)

**Sprint 6 (Current — most things already done!)** ✅
1. ✅ **7.1 Crawler Job Persistence** — Done
2. ✅ **Activate GH/LV/AS** — Done (wired in factory)
3. ✅ **TeamTailor / SmartRecruiters / Workable clients** — Done
4. ✅ **GCS Bronze cold storage** — Done
5. ✅ **Admin Panel frontend** — Done (6 pages)
6. ✅ **Extraction hints** — Done
7. **8.1 Staging Environment** — Still pending (~2h setup)
8. **autechjobs historical load** — Script ready, BQ load pending (~1h)

**Sprint 7 (Next): Admin Panel Backend + UX Gaps**
1. Backend dead-letter queue + ingestion history endpoints (admin panel Phase 7.2 remaining work)
2. Backend jobs browser endpoint (`GET /admin/jobs`)
3. **Dead link UI** (public-facing url_status display) — 2-3h
4. **Snapshot fixture test suite** (20 crawl pages) — 4-6h

**Sprint 8: UX + Market Expansion**
1. **3.3 Tech Exclusion Filters** - Cleaner results
2. **3.4 Trending Lists** - Engagement
3. **3.1 SEEK & TradeMe** - Double job coverage (if still relevant with 527 ATS companies)
4. **4.1 Mobile UX** - Retention

### Success Criteria

All implementation plans include specific, measurable success criteria. Key metrics to track:

**Data Pipeline:**
- Crawler jobs appear on site after crawl (currently 0%) → **100%** (crawler job persistence fix)
- Companies with native ATS: 0 active → **527** (GH/LV/AS + TT + SR + Workable)
- Historical trend data: 0 years → **5+ years** (autechjobs load)

**Data Quality:**
- Job coverage: 40% → 90%+ (ATS + SEEK + TradeMe)
- Ghost jobs: Unknown → <5% (dead link detection)
- Salary data: Messy → Normalized with confidence indicators ✅

**User Experience:**
- Mobile bounce rate: Unknown → <40%
- API response time: Unknown → <500ms (p95) ✅
- Organic search traffic: Unknown → +50% (sitemap + domain hubs) ✅

**Technical:**
- Zero timeout errors (Cloud Tasks) ✅
- 99%+ sync success rate ✅
- All unit tests passing (90%+ coverage) ✅
- Zero field-mismatch bugs (contract validation) ✅
- Staging environment operational (currently none)

---

**Document Status:** ✅ Complete - All major features have detailed implementation plans
