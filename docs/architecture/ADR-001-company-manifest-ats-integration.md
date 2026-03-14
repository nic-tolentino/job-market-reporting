# Architecture Decision Record: Company Manifest & ATS Integration System

**ADR-001** | **March 10, 2026** | **Status: Implemented**

---

## Executive Summary

This document records the architecture decisions for the company manifest system and ATS (Applicant Tracking System) integration. The system evolved from a single JSON file to a directory-based manifest with declarative ATS configuration, enabling scalable company data management and direct ATS API integration.

---

## Table of Contents

1. [Company Manifest Structure](#1-company-manifest-structure)
2. [ATS Integration Architecture](#2-ats-integration-architecture)
3. [Validation & Quality Assurance](#3-validation--quality-assurance)
4. [Data Synchronization Pipeline](#4-data-synchronization-pipeline)
5. [Future Enhancements](#5-future-enhancements)
6. [Lessons Learned](#6-lessons-learned)
7. [References](#7-references)
8. [Crawler Data Persistence & Backend Integration](#8-crawler-data-persistence--backend-integration)

---

## 1. Company Manifest Structure

### 1.1 Problem Statement

**Context (February 2026):**
- Company data stored in single `data/companies.json` file (2,500+ lines, 180+ companies)
- Growing merge conflicts with multiple contributors
- Large diffs making PR review difficult
- Single point of failure (syntax error breaks entire manifest)
- ATS configuration stored separately in BigQuery only

**Requirements:**
- Support multiple contributors without merge conflicts
- Enable granular PR reviews (one company per PR)
- Isolate errors to individual companies
- Support declarative ATS configuration
- Maintain data quality and validation

### 1.2 Approaches Considered

#### Option 1: Single File with Feature Branches
**Description:** Keep single `companies.json` but use feature branches for each company edit.

**Pros:**
- Simple structure
- No migration needed

**Cons:**
- Merge conflicts inevitable with parallel work
- Large diffs hard to review
- Single syntax error breaks everything
- ❌ **Rejected**

#### Option 2: Directory-Based Manifest (Selected)
**Description:** Split into individual files: `data/companies/{company-id}.json`

**Pros:**
- Zero merge conflicts for parallel work
- Clean, reviewable diffs
- Error isolation (one file = one company)
- Git provides audit trail
- Scales to thousands of companies
- ✅ **Selected**

**Cons:**
- Migration effort required
- Slightly more complex backend loading

#### Option 3: Database-Only Configuration
**Description:** Store all company data in BigQuery, no Git manifests.

**Pros:**
- No file management
- Direct database queries

**Cons:**
- No Git audit trail
- No PR review process
- Environment drift risk
- ❌ **Rejected**

### 1.3 Decision & Implementation

**Decision:** Directory-based manifest with individual JSON files.

**Implementation (March 2026):**
```
data/companies/
├── schema.json              # JSON Schema validation
├── README.md                # Documentation
├── xero.json               # Individual company (108 files)
├── canva.json
├── rocket-lab.json
└── ...
```

**Schema Evolution:**
```json
{
  "id": "xero",
  "name": "Xero",
  "verification_level": "verified",
  "hq_country": "NZ",
  "updated_at": "2026-03-10T00:00:00Z",
  "ats": {                    // NEW: Declarative ATS config
    "provider": "GREENHOUSE",
    "identifier": "xero"
  }
}
```

**Backend Loading:**
```kotlin
// CompanySyncService.kt
fun syncFromManifest() {
    val jsonFiles = dir.listFiles { file ->
        file.isFile && file.extension == "json"
    }
    
    val companies = jsonFiles.mapNotNull { file ->
        objectMapper.readValue(file, CompanyJsonDto::class.java)
    }
    
    // Sync companies and ATS configs to BigQuery
}
```

### 1.4 Results & Metrics

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Files | 1 | 108 | Granular edits |
| Merge conflicts | Frequent | None | ✅ Eliminated |
| PR review time | 30+ min | 5 min | 6x faster |
| Error isolation | None | Per-file | ✅ Isolated |
| ATS coverage | 0 in Git | 60 in Git | ✅ Declarative |

### 1.5 Future Enhancements

- **Per-company supplemental files:** `xero.logo.svg`, `xero.bio.md`
- **CDN hosting:** Stable logo URLs (Vercel Blob, S3)
- **Parallel parsing:** Coroutines for faster loading (5-7x improvement)

---

## 2. ATS Integration Architecture

### 2.1 Problem Statement

**Context (February 2026):**
- 50.5% of companies use identifiable ATS (Greenhouse, Lever, Ashby, Workday, etc.)
- ATS configuration stored only in BigQuery (operational data)
- No Git audit trail for ATS changes
- Manual BigQuery inserts required for new ATS integrations
- LinkedIn Easy Apply masks ATS for 82% of companies

**Requirements:**
- Declarative ATS configuration in Git
- Preserve operational state (enabled, sync status) in database
- Support 10+ ATS providers
- Validate ATS configurations before merge
- Enable direct API polling for real-time job data

### 2.2 Approaches Considered

#### Option 1: BigQuery-Only Configuration
**Description:** Keep ATS configs only in BigQuery `ats_configs` table.

**Pros:**
- Simple architecture
- Direct database access

**Cons:**
- No Git audit trail
- No PR review for ATS changes
- Environment drift
- Manual seeding required
- ❌ **Rejected**

#### Option 2: Declarative Git Configuration (Selected)
**Description:** Store ATS provider + identifier in company manifest files.

**Pros:**
- Git audit trail for all changes
- PR-based workflow
- Declarative configuration
- Environment consistency
- Easy to review and validate

**Cons:**
- Requires backend sync logic
- Need to preserve operational state separately
- ✅ **Selected**

#### Option 3: Hybrid with External Config Service
**Description:** Use external configuration service (e.g., LaunchDarkly, ConfigCat).

**Pros:**
- Feature flag capabilities
- Runtime configuration changes

**Cons:**
- Additional infrastructure cost
- Over-engineered for use case
- ❌ **Rejected**

### 2.3 Decision & Implementation

**Decision:** Declarative ATS configuration in company manifest files with operational state in BigQuery.

**Data Architecture:**
```
Git (Definition)                    BigQuery (Operational State)
┌─────────────────────┐            ┌─────────────────────────┐
│ "ats": {            │  sync      │ company_id              │
│   "provider":       │ ─────────▶ │ provider                │
│     "GREENHOUSE",   │            │ identifier              │
│   "identifier":     │            │ enabled        (mutable)│
│     "xero"          │            │ last_synced_at (mutable)│
│ }                   │            │ sync_status    (mutable)│
└─────────────────────┘            │ error_message  (mutable)│
                                   └─────────────────────────┘
```

**Sync Logic:**
```kotlin
// CompanySyncService.kt
private fun syncAtsConfigurations(companies: List<CompanyJsonDto>) {
    companies.forEach { dto ->
        dto.atsConfig?.let { atsDto ->
            val existingConfig = atsConfigRepository.getConfig(dto.id)
            
            val config = CompanyAtsConfig(
                companyId = dto.id,
                atsProvider = AtProvider.valueOf(atsDto.provider.uppercase()),
                identifier = atsDto.identifier,
                enabled = existingConfig?.enabled ?: true,  // Preserve state
                lastSyncedAt = existingConfig?.lastSyncedAt,
                syncStatus = existingConfig?.syncStatus
            )
            
            atsConfigRepository.saveConfig(config)
        }
    }
}
```

**ATS Provider Support:**
| Provider | Companies | Integration Status | API Type |
|----------|-----------|-------------------|----------|
| Greenhouse | 9 | ✅ Ready | Public API |
| Lever | 12 | ✅ Ready | Public API |
| Ashby | 7 | ✅ Ready | Public API |
| Workday | 10 | 🟡 Identified | Domain-based |
| BambooHR | 6 | 🟡 Identified | Domain-based |
| SnapHire | 3 | 🟡 Identified | Domain-based |
| Teamtailor | 2 | 🟡 Identified | Domain-based |
| SmartRecruiters | 3 | 🟡 Identified | Domain-based |
| Workable | 4 | 🟡 Identified | Domain-based |
| SuccessFactors | 4 | 🟡 Identified | Domain-based |

### 2.4 Migration Strategy

**Migration Execution (March 2026):**
1. Created migration scripts (`add_from_findings.py`, `migrate_to_declarative.py`)
2. Migrated 60 ATS configurations from BigQuery to manifests
3. Validated all configurations
4. Deployed backend with sync logic
5. Synced to BigQuery

**Results:**
- 108 company manifests (from 79)
- 60 ATS configurations in Git
- 100% validation pass rate
- Zero data loss

### 2.5 Future Enhancements

- **Direct ATS API Polling:** Backend polls Greenhouse/Lever/Ashby APIs every 1-2 hours
- **Workday Integration:** Enterprise ATS (13.7% of companies)
- **OAuth Integration:** JobAdder, Employment Hero
- **Job Deduplication:** Cross-reference ATS + LinkedIn + SEEK data

---

## 3. Validation & Quality Assurance

### 3.1 Problem Statement

**Context:**
- Opening manifests to public contributions risks data quality issues
- Schema drift (contributors add/remove fields inconsistently)
- Invalid data formats (wrong country codes, URLs)
- Duplicate companies (same company, different IDs)
- Inconsistent formatting

**Requirements:**
- Enforce schema at file level
- Catch errors before commit
- Block invalid PRs at CI level
- Provide clear contributor guidelines
- Auto-fix formatting issues

### 3.2 Approaches Considered

#### Option 1: Manual Review Only
**Description:** Rely on maintainer review for quality assurance.

**Pros:**
- No tooling required
- Flexible

**Cons:**
- Human error
- Doesn't scale
- Inconsistent enforcement
- ❌ **Rejected**

#### Option 2: Multi-Layer Validation (Selected)
**Description:** JSON Schema + Pre-commit hooks + CI validation + Contributor docs.

**Pros:**
- Catches errors at multiple stages
- Automated enforcement
- Clear contributor guidance
- Scales with contributions

**Cons:**
- Initial setup effort
- Maintenance overhead
- ✅ **Selected**

#### Option 3: External Validation Service
**Description:** Use external service (e.g., JSON Schema validator API).

**Pros:**
- Centralized validation
- No local dependencies

**Cons:**
- Network dependency
- Latency
- Cost
- ❌ **Rejected**

### 3.3 Decision & Implementation

**Decision:** Multi-layer validation system.

**Validation Layers:**
```
┌─────────────────────────┐
│  Layer 1: JSON Schema   │ ← Enforces structure
│  (schema.json)          │
└──────────┬──────────────┘
           │
┌──────────▼──────────────┐
│  Layer 2: Pre-commit    │ ← Catches before commit
│  Hooks                  │
└──────────┬──────────────┘
           │
┌──────────▼──────────────┐
│  Layer 3: GitHub        │ ← Blocks invalid PRs
│  Actions CI             │
└──────────┬──────────────┘
           │
┌──────────▼──────────────┐
│  Layer 4: Manual        │ ← Final review
│  Review                 │
└─────────────────────────┘
```

**Implementation:**
```python
# validate_all.py
def validate_all():
    # 1. Load JSON Schema
    schema = load_schema()
    
    # 2. Validate each file
    for file in json_files:
        company = load_company(file)
        
        # Schema validation
        errors = validate_schema(company, schema, file)
        
        # Business rules
        errors += validate_business_rules(company, file)
        # - ID matches filename
        # - Lowercase enforcement
        # - Valid country codes
    
    # 3. Report results
    if errors:
        print("❌ Validation FAILED")
        return 1
    else:
        print("✅ All files valid")
        return 0
```

**Validation Rules:**
- Required fields: `id`, `name`, `verification_level`, `hq_country`, `updated_at`
- ID pattern: lowercase slug, matches filename exactly
- Country codes: ISO 3166-1 alpha-2 (NZ, AU, US)
- ATS provider: enum validation
- No additional properties (prevents schema drift)

### 3.4 Results & Metrics

| Validation Layer | Errors Caught | Prevention Rate |
|-----------------|---------------|-----------------|
| JSON Schema | 15 | 40% |
| Pre-commit Hooks | 10 | 27% |
| GitHub Actions CI | 8 | 22% |
| Manual Review | 4 | 11% |
| **Total** | **37** | **100%** |

### 3.5 Future Enhancements

- **Automated company discovery:** Clearbit/Apollo API integration
- **Duplicate detection enhancement:** Fuzzy matching for similar names
- **Link validation:** Check website URLs periodically

---

## 4. Data Synchronization Pipeline

### 4.1 Problem Statement

**Context:**
- Company data needs to sync from Git to BigQuery
- Operational state (enabled, sync status) must be preserved
- Two-phase sync: companies first, then jobs
- ATS configurations need separate handling

**Requirements:**
- Atomic sync operations
- Preserve operational state across deploys
- Handle errors gracefully
- Support rollback

### 4.2 Approaches Considered

#### Option 1: Direct Database Writes
**Description:** Scripts write directly to BigQuery from Git.

**Pros:**
- Simple architecture
- No backend changes

**Cons:**
- No business logic validation
- No error handling
- Security concerns
- ❌ **Rejected**

#### Option 2: Backend Sync Service (Selected)
**Description:** `CompanySyncService` reads Git and syncs to BigQuery.

**Pros:**
- Centralized logic
- Business rule validation
- Error handling
- Operational state preservation
- ✅ **Selected**

**Cons:**
- Backend deployment required
- Sync timing coordination

### 4.3 Decision & Implementation

**Decision:** Backend sync service with operational state preservation.

**Sync Flow:**
```
1. Read all JSON files from data/companies/
2. Parse company data
3. Delete all companies from BigQuery (atomic)
4. Insert all companies (atomic)
5. For each company with ATS config:
   a. Get existing config from BigQuery
   b. Preserve operational state (enabled, syncStatus)
   c. Upsert ATS config
6. Log results
```

**Implementation:**
```kotlin
fun syncFromManifest() {
    // Phase 1: Sync companies
    companyRepository.deleteAllCompanies()
    companyRepository.saveCompanies(records)
    
    // Phase 2: Sync ATS configs (preserve operational state)
    syncAtsConfigurations(companies)
}
```

### 4.4 Future Enhancements

- **Incremental sync:** Only sync changed companies
- **Parallel parsing:** Coroutines for faster loading
- **Sync scheduling:** Cloud Scheduler integration

---

## 5. Future Enhancements

### 5.1 Short-Term (Q2 2026)

#### 5.1.1 Direct ATS API Polling
**Priority:** HIGH  
**Effort:** 12-16 hours

**Description:** Backend polls ATS APIs (Greenhouse, Lever, Ashby) every 1-2 hours for real-time job data.

**Benefits:**
- Real-time job data (before LinkedIn)
- Structured data (salary, benefits, requirements)
- 50%+ more jobs from direct sources

**Implementation:**
- `GreenhouseApiClient`, `LeverApiClient`, `AshbyApiClient`
- Scheduled polling via Cloud Scheduler
- Deduplication with LinkedIn/SEEK data

#### 5.1.2 CDN for Logos
**Priority:** MEDIUM  
**Effort:** 4-6 hours

**Description:** Host company logos on CDN (Vercel Blob, S3) instead of LinkedIn URLs.

**Benefits:**
- Stable URLs (no expiration)
- Faster loading
- Better control over image quality

**Implementation:**
- Logo fetcher service
- Upload to CDN on company creation
- Store CDN URL in manifest

### 5.2 Medium-Term (Q3 2026)

#### 5.2.1 Workday Integration
**Priority:** MEDIUM  
**Effort:** 16-20 hours

**Description:** Integrate with Workday API (13.7% of companies).

**Benefits:**
- Enterprise ATS coverage
- Structured job data

**Challenges:**
- Workday API complexity
- Authentication requirements

#### 5.2.2 Parallel Parsing
**Priority:** LOW  
**Effort:** 4-6 hours

**Description:** Use Kotlin coroutines for parallel JSON parsing.

**Benefits:**
- 5-7x faster loading
- Better scalability (1000+ companies)

**Implementation:**
```kotlin
withContext(IO) {
    jsonFiles.map { file ->
        async { parseFile(file) }
    }.awaitAll()
}
```

### 5.3 Long-Term (H2 2026)

#### 5.3.1 Community Contributions
**Priority:** MEDIUM  
**Effort:** 20-30 hours

**Description:** Enable community contributions via public GitHub repo.

**Benefits:**
- Community-driven data quality
- Reduced maintainer burden
- Faster updates

**Requirements:**
- Contribution guidelines
- Automated validation
- Moderator review process

#### 5.3.2 Automated Company Discovery
**Priority:** LOW  
**Effort:** 16-20 hours

**Description:** Integrate with Clearbit/Apollo APIs for automatic company profiling.

**Benefits:**
- Automatic company enrichment
- Reduced manual work

**Implementation:**
- API integration
- LLM-assisted profiling
- Human review workflow

---

## 6. Lessons Learned

### 6.1 What Worked Well

1. **Directory-based manifest:** Zero merge conflicts, clean diffs
2. **Declarative ATS config:** Git audit trail, PR workflow
3. **Multi-layer validation:** Caught 37 errors before production
4. **Operational state preservation:** No data loss during sync

### 6.2 What Could Be Improved

1. **Parallel parsing:** Currently sequential (acceptable for now)
2. **Logo hosting:** LinkedIn URLs expire (CDN needed)
3. **Documentation:** Multiple docs created, needed consolidation (this ADR)

### 6.3 Recommendations for Future Projects

1. **Start with directory structure:** Don't wait for scale issues
2. **Implement validation early:** Catch errors before they multiply
3. **Separate definition from operational state:** Git for config, DB for state
4. **Document decisions as you go:** ADRs prevent knowledge loss

---

## 7. References

### 7.1 Related Documents

- `docs/data/company-data-strategy.md` - Company data strategy
- `docs/data/ats/ats-identification-plan.md` - ATS identification strategy
- `docs/implementation-plans/ats-migration-guide.md` - Migration instructions
- `docs/implementation-plans/NEXT_STEPS.md` - Next steps guide

### 7.2 Implementation Files

**Backend:**
- `backend/src/main/kotlin/com/techmarket/sync/CompanySyncService.kt`
- `backend/src/main/kotlin/com/techmarket/sync/model/CompanyJsonDto.kt`
- `backend/src/main/kotlin/com/techmarket/persistence/ats/AtsConfigRepository.kt`

**Scripts:**
- `scripts/companies/validate_all.py` - Validation
- `scripts/companies/extract_missing_from_db.py` - DB extraction
- `scripts/ats/validate_ats_configs.py` - ATS validation
- `scripts/ats/discover_missing_ats.py` - ATS discovery

**Configuration:**
- `data/companies/schema.json` - JSON Schema
- `.pre-commit-config.yaml` - Pre-commit hooks
- `.github/workflows/validate-companies.yml` - GitHub Actions

---

## 8. Crawler Data Persistence & Backend Integration

### 8.1 Architectural Decision: Kotlin Backend Owns BigQuery Write-Back

**March 14, 2026** | **Status: Decided**

#### 8.1.1 Decision
The Kotlin backend — not the crawler service — is responsible for persisting crawl results to the `crawler_seeds` BigQuery table. The crawler service is a stateless computation service: it crawls, extracts, and returns a typed `CrawlMeta` response. It has no BigQuery client and no opinion about persistence.

#### 8.1.2 Rationale
1. **Single BigQuery client** — schema definition, validation, and access patterns live in one place. When `crawler_seeds` changes, one codebase changes.
2. **Type safety** — Kotlin data classes + `CrawlerSeedRepository` provide compile-time schema validation. TypeScript has no entity model and weak guarantees around what it writes to BigQuery.
3. **Business logic belongs here** — deciding what to do with a `PAGINATION_GROWTH` signal (schedule a re-crawl, mark a seed STALE, alert) is orchestration logic that belongs alongside other scheduling code in the backend.
4. **Consistent error handling** — the backend already handles partial failures, retries, and BQ write errors for other tables. Crawl result persistence follows the same patterns.

### 8.2 Implementation Recommendations

#### 8.2.1 Treat CrawlMeta as a Formal Contract
The crawler's `CrawlMeta` response is the interface between the two services. Define a matching Kotlin DTO immediately and treat it as a versioned contract — not an ad-hoc JSON blob.

```kotlin
data class CrawlMetaDto(
    val companyId: String,
    val seedUrl: String?,
    val pagesVisited: Int,
    val totalJobsFound: Int,
    val durationMs: Long,
    val exitState: CrawlExitState, // ACTIVE, BLOCKED, TIMEOUT
    val errorMessage: String?,
    val paginationSignal: PaginationSignal?,
    val jobYieldSignal: JobYieldSignal?,
    val paginationPattern: String?
)

enum class CrawlExitState { ACTIVE, BLOCKED, TIMEOUT }

data class PaginationSignal(
    val type: SignalType, // GROWTH, CONTRACTION
    val previousPages: Int,
    val newPages: Int
)

data class JobYieldSignal(
    val type: SignalType,
    val previousJobs: Int,
    val newJobs: Int,
    val delta: Int
)
```

#### 8.2.2 Log Raw Response Immediately on Receipt
Before any processing or BQ write, log the raw `CrawlMeta` payload. This is the primary failure recovery mechanism — if the BQ write fails, the data isn't lost.

```kotlin
fun handleCrawlResult(meta: CrawlMetaDto) {
    logger.info("CRAWL_RESULT_RECEIVED company={} seed={} payload={}",
        meta.companyId, meta.seedUrl, objectMapper.writeValueAsString(meta))

    persistToDatabase(meta)   // can fail — log is already written
    processSignals(meta)      // separate concern
}
```

#### 8.2.3 Separate Persistence from Signal Processing
Two distinct responsibilities — keep them in separate methods/classes:

```kotlin
// 1. Always runs first — raw state update
crawlerSeedRepository.upsert(CrawlerSeedRecord(
    companyId = meta.companyId,
    url = meta.seedUrl ?: return,
    lastKnownPageCount = meta.pagesVisited,
    lastKnownJobCount = meta.totalJobsFound,
    lastCrawledAt = Instant.now(),
    lastDurationMs = meta.durationMs,
    status = meta.exitState.toSeedStatus(),
    errorMessage = meta.errorMessage?.take(500),
    paginationPattern = meta.paginationPattern,
    consecutiveZeroYieldCount = if (meta.totalJobsFound == 0) (existing?.consecutiveZeroYieldCount ?: 0) + 1 else 0
))

// 2. Runs after — signal-driven side effects
signalProcessor.handle(meta)
```

#### 8.2.4 CrawlerSeedRepository — Upsert Pattern
BigQuery doesn't support true upserts natively. The standard pattern for `crawler_seeds`:
- **Option A (simplest)**: Use a `MERGE` statement in a scheduled backend job that deduplicates by `(company_id, url)` on `last_crawled_at`.
- **Option B (recommended)**: Use BigQuery's `INSERT` with `ignoreUnknownValues` and run periodic `MERGE` to compact. This works well for append-heavy operational tables.

#### 8.2.5 STALE Lifecycle — Backend Maintenance Job
`STALE` is set by age (>30 days) and yield (0 jobs for 3 consecutive runs). This needs a scheduled job, not inline crawl processing. 

Note: yield-based staleness requires adding `consecutive_zero_yield_count (INTEGER)` to the schema to track runs without querying history.

#### 8.2.6 Failure Mitigation — Tiered Approach
1. **Tier 1**: Structured log on receipt (immediate) — zero cost, implement now.
2. **Tier 2**: Retry with exponential backoff on BQ write failure.
3. **Tier 3**: Dead-letter queue (Cloud Pub/Sub or Cloud Tasks).
4. **Tier 4**: Idempotent re-crawl (Last resort).

---

**Last Updated:** March 14, 2026
