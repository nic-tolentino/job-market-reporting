# Comprehensive Strategic Roadmap & Analysis

Providing a prioritized plan to evolve DevAssembly from a LinkedIn scraper into a high-trust, multi-channel market intelligence platform.

**Last Updated:** March 9, 2026

---

## 📋 Implementation Plans

Detailed implementation plans have been created for all major features. Each plan includes:
- Executive summary
- Technical specification
- Step-by-step implementation guide
- Testing strategy
- Success metrics
- Files to create/modify

**Location:** `docs/implementation-plans/`

| Plan | Priority | Effort | Status |
|------|----------|--------|--------|
| [2.1 Salary Normalization](./implementation-plans/2.1-salary-normalization-plan.md) | HIGH | 8-10 hours | ✅ Ready |
| [2.2 Cloud Tasks](./implementation-plans/2.2-cloud-tasks-plan.md) | HIGH | 8-10 hours | ✅ Ready |
| [2.3 Dead Link Detection](./implementation-plans/2.3-dead-link-detection-plan.md) | HIGH | 6-8 hours | ✅ Ready |
| [2.4 ATS Integrations](./implementation-plans/2.4-ats-integrations-plan.md) | HIGH | 12-16 hours | ✅ Ready |
| [2.5 Visa Sponsorship](./implementation-plans/2.5-visa-sponsorship-plan.md) | MEDIUM | 3-4 hours | ✅ Ready |
| [3.1 SEEK & TradeMe](./implementation-plans/3.1-seek-trademe-integration-plan.md) | HIGH | 10-14 hours | ✅ Ready |
| [3.2 Domain Hubs](./implementation-plans/3.2-technology-domain-hubs-plan.md) | HIGH | 12-16 hours | ✅ Ready |
| [3.3 Tech Exclusion Filters](./implementation-plans/3.3-technology-exclusion-filters-plan.md) | MEDIUM | 4-6 hours | ✅ Ready |
| [3.4 Trending Lists](./implementation-plans/3.4-trending-lists-plan.md) | MEDIUM | 6-8 hours | ✅ Ready |
| [4.1 Mobile UX Overhaul](./implementation-plans/4.1-mobile-ux-overhaul-plan.md) | HIGH | 16-20 hours | ✅ Ready |

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

### Phase 2: Data Quality & Reliability Engine (Week 3-5)
**Goal:** Make the data pipeline more robust, scalable, and comprehensive.

> **📄 Implementation Plans Available:**
> - [2.1 Salary Normalization](./implementation-plans/2.1-salary-normalization-plan.md) (8-10 hours)
> - [2.2 Background Processing](./implementation-plans/2.2-cloud-tasks-plan.md) (8-10 hours)
> - [2.3 Dead Link Detection](./implementation-plans/2.3-dead-link-detection-plan.md) (6-8 hours)
> - [2.4 ATS Integrations](./implementation-plans/2.4-ats-integrations-plan.md) (12-16 hours)
> - [2.5 Visa Sponsorship](./implementation-plans/2.5-visa-sponsorship-plan.md) (3-4 hours)

#### 2.1 Salary Normalization Engine [HIGH - User Value]
**Problem:** Salary data is messy - different currencies, periods (hourly/monthly/yearly), and formats.

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/sync/RawJobDataParser.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/model/JobRecord.kt`

**Implementation:**
```kotlin
data class NormalizedSalary(
    val amount: Long,
    val currency: String, // NZD, AUD, USD, EUR
    val period: String,   // HOUR, DAY, MONTH, YEAR
    val source: String    // JOB_POSTING (explicit), ATS_API (structured), MARKET_DATA (inferred)
)
```
**Note:** Confidence level is inferred from the `source` field - no separate field needed.
- `JOB_POSTING`: HIGH confidence (explicitly stated in job description)
- `ATS_API`: HIGH confidence (structured data from ATS provider)
- `MARKET_DATA`: MEDIUM confidence (inferred from market benchmarks)

**Effort:** 6-8 hours | **Impact:** Enables salary comparison features

---

#### 2.2 Background Processing with Cloud Tasks [HIGH - Scalability]
**Problem:** Webhook processing runs synchronously, risking Cloud Run timeouts as data volume grows.

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/config/CloudTasksConfig.kt`
- `backend/src/main/kotlin/com/techmarket/service/CloudTasksService.kt`

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/webhook/ApifyWebhookController.kt`
- `backend/src/main/kotlin/com/techmarket/api/AdminController.kt`

**Implementation:**
1. Create Cloud Tasks queue via Terraform/gcloud
2. Webhook pushes task → responds 202 immediately
3. Separate internal endpoint processes the task
4. Add retry policies and dead-letter queue

**Effort:** 8-10 hours | **Impact:** Prevents timeouts, enables scaling

---

#### 2.3 Dead Link Detection Worker [HIGH - Data Freshness]
**Problem:** "Ghost jobs" - filled roles that remain visible because apply URLs go stale.

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/service/JobHealthCheckService.kt`
- `backend/src/main/kotlin/com/techmarket/scheduler/HealthCheckScheduler.kt`

**Implementation:**
- Daily scheduled job (Cloud Scheduler)
- HEAD request to all active `applyUrls`
- Mark jobs as `CLOSED` if 404/redirect to generic careers page
- Add `url_last_checked` and `url_status` fields to jobs table

**Effort:** 6-8 hours | **Impact:** Zero ghost jobs, high user trust

---

#### 2.4 ATS Direct Integrations [HIGH - Market Coverage]
**Problem:** 50.5% of companies use identifiable ATS systems (Greenhouse, Lever, Ashby, Workday) that we can query directly.

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/sync/ats/greenhouse/GreenhouseApiClient.kt`
- `backend/src/main/kotlin/com/techmarket/sync/ats/lever/LeverApiClient.kt`
- `backend/src/main/kotlin/com/techmarket/sync/ats/ashby/AshbyApiClient.kt`

**Implementation:**
- Use findings from `docs/data/ats/ats-identification-findings.md`
- 92 companies (50.5%) have identifiable ATS
- Start with Greenhouse (9 companies) and Lever (12 companies) - easiest APIs
- Schedule daily syncs for active companies

**Effort:** 12-16 hours | **Impact:** 50%+ more jobs, real-time data

---

#### 2.5 Visa Sponsorship Tracking [MEDIUM - High User Value]
**Problem:** Migrant workers can't filter for companies that sponsor visas.

**Files to Modify:**
- `data/companies.json` - Add `visa_sponsorship` field (already exists!)
- `backend/src/main/kotlin/com/techmarket/persistence/model/CompanyRecord.kt`
- Frontend company filters

**Implementation:**
- Audit existing `companies.json` for accuracy
- Add visa sponsorship filter to company search
- Add badge to company cards

**Effort:** 3-4 hours | **Impact:** Critical for migrant job seekers

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

#### 3.2 Technology Domain Hubs [HIGH - UX/Discovery]
**Problem:** Users can't browse jobs by high-level categories (Web, Mobile, Cloud, Data).

**Files to Create:**
- `backend/src/main/kotlin/com/techmarket/model/TechCategory.kt` (enum)
- `backend/src/main/kotlin/com/techmarket/api/DomainHubController.kt`

**Files to Modify:**
- `backend/src/main/kotlin/com/techmarket/util/TechFormatter.kt`
- Frontend: New `/hubs/{category}` pages

**Implementation:**
See `docs/features/technology-grouping-plan.md` and `docs/features/hubs-and-career-stages.md`

**Effort:** 12-16 hours | **Impact:** Better discovery, SEO landing pages

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
- Job list cards: Better hierarchy, larger tap targets
- Page headers: Responsive typography, compact layout
- Navigation: Smooth scroll show/hide for navbar
- Company page refresh bug (404 on mobile)

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
- `docs/implementation-plans/` - **Detailed implementation plans for all major features**

### External Dependencies
- **Apify Store**: SEEK and TradeMe scrapers
- **Google Cloud Tasks**: Background processing
- **Vercel Blob/S3**: Logo hosting
- **Supabase** (optional): Resource database

---

## 📊 Summary & Next Steps

### Total Implementation Effort

| Phase | Features | Total Effort | Priority Features |
|-------|----------|--------------|-------------------|
| **Phase 1** | 5 bug fixes | ✅ COMPLETED | All done |
| **Phase 2** | 5 features | 38-48 hours | Salary, Cloud Tasks, Dead Links, ATS |
| **Phase 3** | 4 features | 36-44 hours | SEEK/TradeMe, Domain Hubs |
| **Phase 4** | 4 features | 20-24 hours | Mobile UX |
| **Phase 5** | 4 features | TBD | Future enhancements |
| **Total** | **13 new features** | **~100 hours** | **8 high-priority** |

### Recommended Implementation Order

**Sprint 1 (Week 1-2): Foundation**
1. **2.2 Cloud Tasks** - Enables all background processing
2. **2.1 Salary Normalization** - High user value
3. **2.5 Visa Sponsorship** - Quick win (3-4 hours)

**Sprint 2 (Week 3-4): Data Quality**
1. **2.3 Dead Link Detection** - Zero ghost jobs
2. **2.4 ATS Integrations** - 50% more jobs

**Sprint 3 (Week 5-6): Market Expansion**
1. **3.1 SEEK & TradeMe** - Double job coverage
2. **3.2 Domain Hubs** - Better discovery

**Sprint 4 (Week 7-8): UX Polish**
1. **3.3 Tech Exclusion Filters** - Cleaner results
2. **3.4 Trending Lists** - Engagement
3. **4.1 Mobile UX** - Retention

### Success Criteria

All implementation plans include specific, measurable success criteria. Key metrics to track:

**Data Quality:**
- Job coverage: 40% → 90%+ (SEEK + TradeMe + ATS)
- Ghost jobs: Unknown → <5% (dead link detection)
- Salary data: Messy → Normalized with confidence indicators

**User Experience:**
- Mobile bounce rate: Unknown → <40%
- API response time: Unknown → <500ms (p95)
- Organic search traffic: Unknown → +50% (sitemap + domain hubs)

**Technical:**
- Zero timeout errors (Cloud Tasks)
- 99%+ sync success rate
- All unit tests passing (90%+ coverage)

---

**Document Status:** ✅ Complete - All major features have detailed implementation plans
