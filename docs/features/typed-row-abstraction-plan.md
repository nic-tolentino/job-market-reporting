# Typed Row Abstraction for Job Data

**Status:** ✅ COMPLETE
**Created:** 2026-03-10  
**Owner:** Backend Team

## Problem Statement

Raw `FieldValueList` from BigQuery is accessed directly in 3 mapper files, creating a large surface area for null-safety bugs. The BigQuery Java SDK returns Java "platform types" that bypass Kotlin's null checker, meaning any `.stringValue` call on a null field throws at runtime instead of compile time.

### Current State

```
BigQuery FieldValueList
├── JobMapper ❌ raw access
├── TechMapper ❌ raw access
├── JobBigQueryRepository ❌ raw access
└── CompanyMapper ✅ via JobRow/CompanyRow
```

### Target State

```
BigQuery FieldValueList
└── QueryRows.kt (single hydration point)
    ├── JobMapper ✅ typed
    ├── TechMapper ✅ typed
    ├── JobBigQueryRepository ✅ typed
    └── CompanyMapper ✅ typed
```

## Goals

1. **Consolidate null-safety logic**: All raw `FieldValueList` → typed data conversion happens in one place (`QueryRows.kt`)
2. **Eliminate FieldValueList from mappers**: Mappers operate only on typed row objects
3. **Compile-time null safety**: Kotlin's type system prevents null pointer exceptions
4. **Simpler tests**: Tests construct typed rows directly instead of mocking BigQuery types

## Implementation Plan

### Phase 1: Expand QueryRows.kt

#### 1.1 Harden JobRow null-safety

**File:** `backend/src/main/kotlin/com/techmarket/models/QueryRows.kt`

Replace unsafe `getString()` calls with safe `getStringOrDefault()` for fields that must never be null:
- `jobId`
- `title`
- `seniorityLevel`
- `source`
- `postedDate`

#### 1.2 Add missing fields to JobRow

Add fields currently accessed via raw `FieldValueList` in `JobMapper`:
- `companyId: String`
- `companyName: String`
- `description: String?`
- `employmentType: String?`
- `jobFunction: String?`

#### 1.3 Add CompanyInfoRow

New data class for JOINed company data in job details queries:

```kotlin
data class CompanyInfoRow(
    val companyId: String,
    val name: String,
    val logoUrl: String,
    val description: String,
    val website: String,
    val hiringLocations: List<String>,
    val hqCountry: String?,
    val verificationLevel: String
) {
    companion object {
        fun fromJoinedRow(field: FieldValueList): CompanyInfoRow { ... }
    }
}
```

#### 1.4 Add JobDetailsRow

Wrapper for job details page query results (job + joined company):

```kotlin
data class JobDetailsRow(
    val job: JobRow,
    val company: CompanyInfoRow
) {
    companion object {
        fun fromJoinedRow(field: FieldValueList): JobDetailsRow { ... }
    }
}
```

#### 1.5 Add SeniorityRow and CompanyLeaderboardRow

Small typed rows for TechMapper's aggregation queries:

```kotlin
data class SeniorityRow(
    val name: String,
    val value: Int
) {
    companion object {
        fun fromAggregationRow(field: FieldValueList): SeniorityRow { ... }
    }
}

data class CompanyLeaderboardRow(
    val id: String,
    val name: String,
    val logo: String,
    val activeRoles: Int
) {
    companion object {
        fun fromAggregationRow(field: FieldValueList): CompanyLeaderboardRow { ... }
    }
}
```

### Phase 2: Refactor Mappers

#### 2.1 JobMapper.kt

**Changes:**
- `mapJobDetails()`: Accept `JobDetailsRow` instead of `FieldValueList`
- `mapJobDetailsDto()`: Accept `JobRow` instead of `FieldValueList`
- `mapJobLocations()`: Accept `JobRow` instead of `FieldValueList`
- `mapJobCompanyDto()`: Accept `CompanyInfoRow` instead of `FieldValueList`
- `mapJobRole()`: Accept `JobRow` instead of `FieldValueList`
- `mapToJobRecord()`: Accept `JobRow` instead of `FieldValueList`
- Remove duplicated `parseTimestampSafe()` / `parseLocalDateSafe()` helpers
- Remove all `FieldValueList` imports

**Benefit:** This file should no longer touch BigQuery types.

#### 2.2 TechMapper.kt

**Changes:**
- `mapJobRole()`: Accept `JobRow` instead of `FieldValueList`
- `mapSeniorityLine()`: Accept `SeniorityRow` instead of `FieldValueList`
- `mapCompanyLine()`: Accept `CompanyLeaderboardRow` instead of `FieldValueList`
- Remove duplicated `parseTimestampSafe()` helper
- Remove all `FieldValueList` imports

### Phase 3: Update Repositories

#### 3.1 JobBigQueryRepository.kt

**Changes:**
- `getJobDetails()`: Hydrate `JobDetailsRow.fromJoinedRow(r)` and pass to `JobMapper.mapJobDetails()`
- `getJobsByIds()`: Hydrate via `JobRow.fromJobRow(row)` and pass to `JobMapper.mapToJobRecord()`
- `getAllJobs()`: Hydrate via `JobRow.fromJobRow(row)` and pass to `JobMapper.mapToJobRecord()`
- `getJobById()`: Hydrate via `JobRow.fromJobRow(row)` and pass to `JobMapper.mapToJobRecord()`
- `getJobsNeedingHealthCheck()`: Hydrate via `JobRow.fromJobRow(row)`

#### 3.2 TechBigQueryRepository.kt

**Changes:**
- `getTechDetails()`: Hydrate `SeniorityRow`, `CompanyLeaderboardRow`, and `JobRow` before passing to `TechMapper`

### Phase 4: Tests

#### 4.1 JobMapperTest.kt

**Changes:**
- Refactor tests to construct `JobRow` / `JobDetailsRow` directly instead of mocking `FieldValueList`
- Tests become simpler and more readable

#### 4.2 QueryRowsTest.kt (NEW)

**New test file:** `backend/src/test/kotlin/com/techmarket/models/QueryRowsTest.kt`

**Null-safety regression test:**
- Construct a mock `FieldValueList` with ALL fields null
- Call `JobRow.fromJobRow()` and assert it doesn't throw + returns sensible defaults
- Same test for `CompanyInfoRow.fromJoinedRow()`
- This prevents the original class of bug from ever recurring

## Verification Plan

### Automated Tests

```bash
./gradlew test
```

All existing tests must pass. New `QueryRowsTest` provides null-safety coverage.

### Manual Verification

Deploy and verify the previously-broken job page:
```
https://www.devassembly.org/api/job/crossing-hurdles.es.software-engineer-remote.2026-03-06
```

## Files to Modify

| File | Status | Changes |
|------|--------|---------|
| `QueryRows.kt` | ✅ Done | Expand `JobRow`, add `CompanyInfoRow`, `JobDetailsRow`, `SeniorityRow`, `CompanyLeaderboardRow` |
| `JobMapper.kt` | ✅ Done | Refactor to use typed rows |
| `TechMapper.kt` | ✅ Done | Refactor to use typed rows |
| `JobBigQueryRepository.kt` | ✅ Done | Hydrate rows before passing to mappers |
| `TechBigQueryRepository.kt` | ✅ Done | Hydrate rows before passing to mappers |
| `JobMapperTest.kt` | ✅ Done | Refactor to use typed rows |
| `QueryRowsTest.kt` | ✅ Done | Create new null-safety regression tests |

## Rollback Plan

If issues arise:
1. Revert commits in reverse order (Phase 4 → Phase 3 → Phase 2 → Phase 1)
2. No database schema changes required
3. No API contract changes - DTOs remain unchanged

## Success Metrics

- [x] All tests pass
- [x] Zero `FieldValueList` imports in mapper files
- [x] Null-safety regression test in place
- [x] Job details page loads without errors
- [x] Tech details page loads without errors
