# Company Level Tech Stack Fix

## Problem Statement

Company level tech stacks is currently broken. The `techStack` field displayed on company profile pages is not correctly aggregating technologies from the company's active job postings.

---

## Current Implementation Analysis

### Data Flow

1. **Job Ingestion** (`RawJobDataMapper.kt`):
   - Technologies are extracted from job descriptions via `parser.extractTechnologies()`
   - Each `JobRecord` contains a `technologies: List<String>` field
   - Non-manifest companies get their technologies aggregated during sync

2. **Company Storage** (`CompanyRecord.kt`):
   - Companies have a `technologies: List<String>` field
   - For manifest companies (from `companies.json`), this is set to `emptyList()` during sync
   - For "ghost" companies (discovered during job sync), technologies are aggregated from jobs

3. **API Response** (`CompanyMapper.kt` → `CompanyProfilePageDto.kt`):
   - `techStack` is populated from `CompanyFields.TECHNOLOGIES` in BigQuery
   - **Critical Issue**: Only reads technologies stored directly on the company record
   - **Does NOT aggregate** technologies from the company's active job postings

### Root Cause

In `CompanyMapper.mapCompanyProfile()` (line 150-155):

```kotlin
val allTechs =
    if (detRow?.get(CompanyFields.TECHNOLOGIES)?.isNull == false)
        detRow.get(CompanyFields.TECHNOLOGIES).repeatedValue.map {
            TechFormatter.format(it.stringValue)
        }
    else emptyList()
```

The `techStack` is populated **only** from the company record's `technologies` field in BigQuery. However:

1. **Manifest companies** (from `companies.json`) have `technologies = emptyList()` set explicitly in `CompanySyncService.syncFromManifest()` (line 54)
2. **Ghost companies** get technologies from their initial job parsing, but these are never updated as new jobs arrive
3. There is **no dynamic aggregation** from active job postings at query time

---

## Solution Design

### Option 1: Query-Time Aggregation (Recommended)

**Approach**: Aggregate technologies from active job postings at query time in the `getCompanyProfile()` method.

**Pros**:
- Always reflects current active roles
- No data duplication
- Works for both manifest and ghost companies
- Automatically handles job expiration (old jobs removed from aggregation)

**Cons**:
- Slightly more complex BigQuery query
- Minor performance impact (acceptable for company profile pages)

**Implementation**:

```kotlin
// In CompanyMapper.mapCompanyProfile()
val techFromJobs = jobsResult.values
    .flatMap { r ->
        if (r.get(JobFields.TECHNOLOGIES).isNull) emptyList()
        else r.get(JobFields.TECHNOLOGIES).repeatedValue.map { 
            TechFormatter.format(it.stringValue) 
        }
    }
    .distinct()
    .sortedByDescending { tech -> 
        // Optional: sort by frequency to show most common first
        jobsResult.values.count { r -> 
            !r.get(JobFields.TECHNOLOGIES).isNull && 
            r.get(JobFields.TECHNOLOGIES).repeatedValue.any { it.stringValue == tech }
        }
    }

// Merge with company-level technologies (for manual curation)
val companyTechs = if (detRow?.get(CompanyFields.TECHNOLOGIES)?.isNull == false)
    detRow.get(CompanyFields.TECHNOLOGIES).repeatedValue.map { TechFormatter.format(it.stringValue) }
else emptyList()

val allTechs = (companyTechs + techFromJobs).distinct().sorted()
```

---

### Option 2: Background Aggregation Job

**Approach**: Create a scheduled job that aggregates technologies from jobs and updates company records periodically.

**Pros**:
- Faster query response time
- Can apply more sophisticated logic (e.g., tech trending, decay over time)

**Cons**:
- Data staleness between runs
- More complex infrastructure
- Requires additional scheduling mechanism

**Not recommended** for initial implementation due to complexity.

---

### Option 3: Hybrid Approach (Future Enhancement)

Combine Option 1 with a `technologies_last_aggregated` timestamp field:
- Query-time aggregation as primary source
- Cache the result in company record for fallback
- Show "Last updated" timestamp on UI

---

## Implementation Plan

### Phase 1: Core Fix (Query-Time Aggregation)

**Files to Modify**:
1. `backend/src/main/kotlin/com/techmarket/persistence/company/CompanyMapper.kt`
   - Update `mapCompanyProfile()` to aggregate technologies from `jobsResult`
   - Merge with company-level technologies (for manual curation)
   - Apply consistent formatting via `TechFormatter.format()`

**Testing**:
- Verify tech stack shows technologies from all active roles
- Test with manifest companies (previously empty)
- Test with ghost companies (should merge existing + new)
- Test country filtering (if country param provided, only aggregate from jobs in that country)

---

### Phase 2: Enhanced Features

#### 2.1: Technology Frequency/Confidence

Show technologies ordered by how many roles mention them:

```kotlin
val techCounts = jobsResult.values
    .flatMap { r ->
        if (r.get(JobFields.TECHNOLOGIES).isNull) emptyList()
        else r.get(JobFields.TECHNOLOGIES).repeatedValue.map { it.stringValue }
    }
    .groupingBy { it }
    .eachCount()

val sortedTechs = techCounts.entries
    .sortedByDescending { it.value }
    .map { TechFormatter.format(it.key) }
```

**UI Enhancement**: Optionally show count badges (e.g., "Kotlin (12 roles)")

---

#### 2.2: Technology Categories

Allow grouping technologies by category (backend, frontend, database, cloud, etc.):

**Backend Changes**:
- Add `TechnologyCategory` enum/classification
- Create mapping of technology → category
- Add category filter to API response

**Frontend Changes**:
- Add category filter pills on company page
- Group tech stack display by category

---

#### 2.3: Tech Stack Freshness Indicator

Add metadata to show how current the tech stack is:

```kotlin
data class CompanyInsightsDto(
    val workModel: String,
    val hiringLocations: List<String>,
    val commonBenefits: List<String>,
    val operatingCountries: List<String> = emptyList(),
    val officeLocations: List<String> = emptyList(),
    val techStackLastUpdated: Instant? = null,  // NEW
    val activeRolesWithTech: Int = 0            // NEW
)
```

---

### Phase 3: Data Quality Improvements

#### 3.1: Technology Exclusion Lists

Some jobs mention technologies they don't actually use (e.g., "familiar with" lists). Implement:
- Negative keyword detection
- Context-aware extraction (require minimum mention confidence)
- Manual curation override for manifest companies

#### 3.2: Technology Normalization

Ensure consistent naming:
- "React" vs "React.js" vs "ReactJS"
- "C#" vs "C Sharp"
- Use `TechFormatter` consistently across all extraction points

---

## Testing Strategy

### Unit Tests

**File**: `backend/src/test/kotlin/com/techmarket/persistence/company/CompanyMapperTest.kt`

Add test cases:
1. `mapCompanyProfile_aggregatesTechFromJobs()` - Verify job technologies are included
2. `mapCompanyProfile_mergesCompanyAndJobTech()` - Verify merge logic
3. `mapCompanyProfile_handlesEmptyJobTech()` - Handle jobs with no technologies
4. `mapCompanyProfile_appliesCountryFilter()` - Verify country filtering works
5. `mapCompanyProfile_sortsByFrequency()` - Verify frequency sorting (if implemented)

### Integration Tests

1. Run full sync pipeline with sample data
2. Verify company profile API returns expected tech stacks
3. Test with real production data in BigQuery

### Manual Testing

1. Visit company profiles with active roles
2. Verify tech stack matches job descriptions
3. Test country filter functionality
4. Compare across multiple companies (manifest vs ghost)

---

## Related Issues

This fix may also impact:
- **Landing page technology counts** - May need similar aggregation fix
- **Technology profile pages** - Should show companies using each tech
- **Search functionality** - Tech-based company search should use aggregated data

---

## Success Criteria

- [ ] Company profile pages show technologies from all active job postings
- [ ] Works correctly for both manifest and ghost companies
- [ ] Country filter correctly filters aggregated technologies
- [ ] Technologies are consistently formatted (via `TechFormatter`)
- [ ] Unit tests cover all aggregation scenarios
- [ ] No regression in API response time (>95th percentile < 500ms)

---

## Implementation Timeline

| Phase | Description | Estimated Effort |
|-------|-------------|------------------|
| Phase 1 | Core query-time aggregation | 2-3 hours |
| Phase 2.1 | Technology frequency sorting | 1 hour |
| Phase 2.2 | Technology categories | 4-6 hours |
| Phase 2.3 | Freshness indicators | 1-2 hours |
| Phase 3 | Data quality improvements | 4-8 hours |
| Testing | Unit + integration tests | 2-3 hours |

**Total**: 14-23 hours for full implementation

---

## Notes

- The current implementation stores `technologies` on company records but doesn't keep them synchronized with job data
- Manifest companies intentionally have empty technology lists in `companies.json` to avoid duplication
- The fix should respect the existing `TechFormatter.format()` pipeline for consistency
- Consider caching implications - company profile pages are cached via `@Cacheable`
