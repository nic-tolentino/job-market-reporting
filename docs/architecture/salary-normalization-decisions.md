# Salary Normalization Architecture Decisions

## Open Questions from Code Review

### 1. Always Provide Salary Range (AI Estimates)?

**Current State:**
- `parseSalary()` returns `NormalizedSalary?` (nullable)
- Jobs can have `null` salaries if not found in posting
- `salaryMin` and `salaryMax` are independent fields

**Proposed Change:**
- `parseSalary()` always returns `NormalizedSalary` (non-null)
- When no salary is found, generate AI estimate based on:
  - Job title/seniority
  - Location (country/city)
  - Technologies required
  - Market data from similar roles

**Implementation Requirements:**

```kotlin
// New service needed
class SalaryEstimationService(
    private val marketDataRepository: MarketDataRepository,
    private val mlModel: SalaryPredictionModel
) {
    fun estimateSalary(
        title: String,
        location: String,
        seniority: String?,
        technologies: List<String>
    ): NormalizedSalaryRange {
        // Query market data for similar roles
        // Apply ML model for prediction
        // Return range with SOURCE_AI_ESTIMATE and LOW confidence
    }
}
```

**Pros:**
- Users always see salary information (better UX)
- Enables salary comparisons across all jobs
- Clear confidence indicators (JOB_POSTING vs AI_ESTIMATE)

**Cons:**
- Requires market data infrastructure
- ML model training/maintenance overhead
- Risk of inaccurate estimates damaging trust
- More complex testing (need to validate estimates)

**Recommendation:** 
**Defer for now.** Focus on accurate parsing of explicit salaries first. Add estimation once we have:
1. Sufficient market data in BigQuery
2. Clear UI indicators for estimated vs actual salaries
3. User feedback mechanism for estimate accuracy

---

### 2. ATS System Scalability

**Current Architecture:**
```
NormalizedJob (generic) → AtsJobDataMapper → JobRecord
                          ↑
                    RawJobDataParser (reused)
```

**Question:** Can `AtsJobDataMapper` handle all ATS systems, or do we need per-ATS handlers?

**Analysis:**

| ATS System | Complexity | Unique Fields | Decision |
|------------|-----------|---------------|----------|
| Greenhouse | Low | Standard fields | ✅ Generic mapper works |
| Lever | Low | Standard fields | ✅ Generic mapper works |
| Workday | Medium | Custom dept codes | ⚠️ May need custom normalizer |
| SuccessFactors | High | Complex hierarchies | ⚠️ May need custom normalizer |

**Recommendation:**
**Keep generic mapper for now, but design for extension:**

```kotlin
// Current (keep)
@Service
class AtsJobDataMapper(private val parser: RawJobDataParser)

// Future extension point (if needed)
interface AtsCustomMapper {
    fun canHandle(provider: AtsProvider): Boolean
    fun customize(job: JobRecord): JobRecord
}

// AtsJobDataMapper would apply custom mappers:
fun map(job: NormalizedJob): JobRecord {
    val base = mapToJobRecord(job)
    return customMappers
        .find { it.canHandle(job.provider) }
        ?.customize(base) 
        ?: base
}
```

**Document for later:** If we encounter ATS systems with fundamentally different data models (not just field variations), we'll add the extension point above.

---

### 3. RawJobDataParser Splitting for Multiple Apify Sources

**Current State:**
```
ApifyJobDto → RawJobDataMapper → JobRecord
               ↑
         RawJobDataParser (shared)
```

**Future Sources:**
- Apify LinkedIn (current)
- Apify Seek (Australia-specific)
- Apify TradeMe (NZ-specific)
- Direct ATS integrations

**Question:** Should we split `RawJobDataParser` per source?

**Analysis:**

**Parser responsibilities:**
1. Location parsing → Same logic for all sources
2. Country detection → Same logic for all sources  
3. Technology extraction → Same logic for all sources
4. Seniority extraction → Same logic for all sources
5. **Salary parsing** → ⚠️ May differ by source/country
6. **Date parsing** → ⚠️ May differ by source format

**Recommendation:**
**Keep monolithic parser for now, but prepare for splitting:**

```kotlin
// Current (keep)
@Component
class RawJobDataParser

// Future structure (if needed)
@Component
class RawJobDataParser(
    val locationParser: LocationParser,
    val techExtractor: TechnologyExtractor,
    val salaryParser: SalaryParser,  // ← Swap per source
    val dateParser: DateParser        // ← Swap per source
)

// Source-specific configurations
class SeekSalaryParser : SalaryParser { ... }
class TradeMeDateParser : DateParser { ... }
```

**Document for later:** When we add Seek/TradeMe:
1. First, try to handle differences in `RawJobDataMapper` per source
2. If parser logic diverges significantly, extract interfaces as shown above
3. Use dependency injection to swap implementations per source

---

## Summary: Build for Now, Design for Later

| Concern | Action Now | Document for Later |
|---------|-----------|-------------------|
| **AI Salary Estimates** | ❌ Don't implement | ✅ Add when we have market data + UI indicators |
| **Per-ATS Custom Mappers** | ❌ Don't implement | ✅ Add extension point if Workday/SuccessFactors need it |
| **Parser Component Splitting** | ❌ Don't implement | ✅ Extract interfaces if Seek/TradeMe have different formats |
| **Static Imports** | ✅ Done | - |
| **Country-based Defaults** | ✅ Done | ⚠️ Plan BigQuery STRUCT migration |

---

## BigQuery Schema Migration (Action Item)

**Current Schema:**
```sql
salaryMin INT64,      -- No currency/period metadata
salaryMax INT64,      -- No currency/period metadata
```

**Target Schema:**
```sql
salaryMin STRUCT<
    amount INT64,      -- In cents
    currency STRING,   -- ISO 4217 (NZD, AUD, USD, EUR)
    period STRING,     -- HOUR, DAY, MONTH, YEAR
    source STRING      -- JOB_POSTING, ATS_API, MARKET_DATA, AI_ESTIMATE
>,
salaryMax STRUCT<...>,
```

**Migration Plan:**
1. Add new STRUCT columns (nullable)
2. Update writers to populate STRUCT fields
3. Backfill legacy data with country-based defaults
4. Remove old INT64 columns after validation

**Tracking Issue:** Create GitHub issue for BigQuery schema migration
