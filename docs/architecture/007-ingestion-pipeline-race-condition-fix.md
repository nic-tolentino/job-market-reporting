# Ingestion Pipeline Race Condition Fix

**Date:** 2026-03-10  
**Severity:** 🔴 Critical  
**Status:** ✅ **Deployed** (2026-03-10)  

---

## Summary

Fixed a critical race condition in the company data ingestion pipeline that was causing:
1. **Data loss** - `raw_companies` table wiped during concurrent dataset ingestions
2. **NOT_FOUND errors** - BigQuery Storage Write API references to dropped tables
3. **Silent failures** - Invalid dataset IDs not properly reported

---

## Root Causes

### Issue 1: Company Table Drop Race Condition (Critical)

**Problem:** `CompanyBigQueryRepository.deleteAllCompanies()` was **dropping and recreating** the entire BigQuery table on every dataset sync.

**Impact:**
- When multiple datasets sync concurrently, Sync B drops the table while Sync A is mid-write
- BigQuery Storage Write API holds internal references to the old table
- Writes fail with `NOT_FOUND: Requested entity was not found`
- All company data lost

**Code Flow:**
```
JobDataSyncService.runDataSync()
  └─> companySyncService.syncFromManifest()
      └─> CompanyBigQueryRepository.deleteAllCompanies()
          └─> bigQuery.delete(tableId)  // DROPS TABLE!
          └─> ensureTable()             // Recreates empty
```

**Fix:** Changed `deleteAllCompanies()` to use DML `DELETE FROM` instead of `DROP TABLE`:

```kotlin
// BEFORE (Destructive)
override fun deleteAllCompanies() {
    val tableId = TableId.of(datasetName, companiesTableName)
    bigQuery.delete(tableId)  // ❌ Drops entire table
    ensureTable()
}

// AFTER (Safe)
override fun deleteAllCompanies() {
    ensureTable()
    val sql = "DELETE FROM `$datasetName.$companiesTableName` WHERE true"
    bigQuery.query(QueryJobConfiguration.newBuilder(sql).build())  // ✅ Deletes rows only
}
```

### Issue 2: Unnecessary Company Sync Per Dataset

**Problem:** Every dataset ingestion triggered a full company manifest sync, which called `deleteAllCompanies()`.

**Impact:**
- 8 datasets = 8 table drop/recreate cycles
- High probability of race conditions
- Company data constantly being wiped

**Fix:** Removed `companySyncService.syncFromManifest()` from `JobDataSyncService.runDataSync()`.

Company manifest sync should now be run:
- Separately via dedicated admin endpoint (to be added)
- Or on application startup
- NOT per-dataset ingestion

### Issue 3: Silent Error Swallowing in ApifyClient

**Problem:** All HTTP errors (including 404 Not Found) were caught and returned as empty lists.

**Impact:**
- Invalid dataset IDs silently skipped
- No FAILED status in metadata
- No visibility into data ingestion failures

**Fix:** Re-throw HTTP 4xx errors to propagate to caller:

```kotlin
// BEFORE (Silent failure)
} catch (e: Exception) {
    log.error("Failed to fetch: {}", e.message)
    emptyList()  // ❌ Swallows all errors
}

// AFTER (Proper error handling)
} catch (e: HttpClientErrorException) {
    log.error("HTTP {} error: {}", e.statusCode.value(), e.message)
    throw e  // ✅ Permanent error - mark as FAILED
} catch (e: Exception) {
    log.error("Transient error: {}", e.message)
    emptyList()  // Transient errors only
}
```

---

## Files Modified

| File | Change | Impact |
|------|--------|--------|
| `CompanyBigQueryRepository.kt` | `deleteAllCompanies()` uses DML DELETE | Prevents table drops |
| `JobDataSyncService.kt` | Removed `syncFromManifest()` call | Prevents race condition trigger |
| `ApifyClient.kt` | Re-throw HTTP 4xx errors | Proper error visibility |

---

## Deployment Steps

### 1. Deploy Backend Fixes

```bash
cd backend
./gradlew clean build -x test
./gradlew bootBuildImage
gcloud run deploy tech-market-backend \
  --image gcr.io/tech-market-insights/tech-market-backend:latest \
  --region australia-southeast1
```

**Or use the automated script:**
```bash
chmod +x scripts/deploy-backend-fixes.sh
./scripts/deploy-backend-fixes.sh
```

### 2. Verify Deployment

```bash
# Check backend is running
curl -s https://tech-market-backend.a.run.app/actuator/health

# Check logs for errors
gcloud run services logs read tech-market-backend \
  --region=australia-southeast1 \
  --limit=20
```

### 3. Recover Failed Dataset

```bash
chmod +x scripts/recover-failed-dataset.sh
./scripts/recover-failed-dataset.sh
```

This script will:
1. Delete FAILED metadata row for `wEVaOoamb196P6g85`
2. Re-trigger ingestion with correct timestamp
3. Wait for Cloud Tasks processing
4. Verify COMPLETED status
5. Verify companies table is populated

### 4. Verify Final State

```bash
# All 8 datasets should be COMPLETED
bq query --use_legacy_sql=false \
  "SELECT dataset_id, processing_status, record_count, ingested_at 
   FROM techmarket.ingestion_metadata 
   ORDER BY ingested_at DESC"

# Expected: 8 rows, all COMPLETED

# Companies should be populated
bq query --use_legacy_sql=false \
  "SELECT COUNT(*) as company_count FROM techmarket.raw_companies"

# Expected: > 0 companies

# Total jobs
bq query --use_legacy_sql=false \
  "SELECT COUNT(*) as job_count FROM techmarket.raw_jobs"

# Expected: ~4,824 jobs (from 8 datasets)
```

---

## Testing

### Test 1: Concurrent Dataset Ingestion

```bash
# Trigger 2 datasets simultaneously
curl -X POST "https://backend/api/admin/trigger-sync?datasetId=dataset1" &
curl -X POST "https://backend/api/admin/trigger-sync?datasetId=dataset2" &
wait

# Verify both complete successfully
bq query "SELECT dataset_id, processing_status FROM techmarket.ingestion_metadata 
          WHERE dataset_id IN ('dataset1', 'dataset2')"

# Expected: Both COMPLETED, no data loss
```

### Test 2: Invalid Dataset ID

```bash
# Trigger with invalid dataset ID
curl -X POST "https://backend/api/admin/trigger-sync?datasetId=INVALID123"

# Wait for processing
sleep 90

# Check status - should be FAILED, not silently skipped
bq query "SELECT dataset_id, processing_status FROM techmarket.ingestion_metadata 
          WHERE dataset_id = 'INVALID123'"

# Expected: processing_status = FAILED
```

### Test 3: Company Table Stability

```bash
# Trigger multiple datasets sequentially
for id in dataset1 dataset2 dataset3; do
  curl -X POST "https://backend/api/admin/trigger-sync?datasetId=$id"
  sleep 90
done

# Check companies table still exists and has data
bq query "SELECT COUNT(*) FROM techmarket.raw_companies"

# Expected: > 0, table not dropped
```

---

## Expected Final State

After deployment and recovery:

| Metric | Before | After |
|--------|--------|-------|
| `ingestion_metadata` rows | 8 (1 FAILED) | 8 (all COMPLETED) |
| `raw_jobs` count | 2,515 | ~4,824 |
| `raw_companies` count | 0 | > 0 (populated) |
| Race condition | ❌ Present | ✅ Fixed |
| Error visibility | ❌ Silent failures | ✅ Proper FAILED status |

---

## Monitoring

### Key Metrics to Watch

1. **Company Table Stability**
   ```sql
   SELECT 
     COUNT(*) as company_count,
     MAX(lastUpdatedAt) as last_update
   FROM techmarket.raw_companies
   ```

2. **Dataset Processing Status**
   ```sql
   SELECT 
     processing_status,
     COUNT(*) as dataset_count
   FROM techmarket.ingestion_metadata
   GROUP BY processing_status
   ```

3. **Error Logs**
   ```bash
   gcloud run services logs read tech-market-backend \
     --filter="severity=ERROR" \
     --limit=50
   ```

### Alerting

Set up alerts for:
- `processing_status = 'FAILED'` in `ingestion_metadata`
- `raw_companies` count = 0
- HTTP 4xx errors in ApifyClient logs

---

## Rollback Plan

If issues arise after deployment:

1. **Revert code changes:**
   ```bash
   git revert <commit-hash>
   ```

2. **Redeploy previous version:**
   ```bash
   gcloud run deploy tech-market-backend \
     --image gcr.io/tech-market-insights/tech-market-backend:<previous-tag>
   ```

3. **Manual company table recovery:**
   ```bash
   # If companies table is corrupted, restore from backup
   bq cp techmarket.raw_companies_backup techmarket.raw_companies
   ```

---

## Related Documentation

- [File-Based Cold Storage ADR](006-file-based-cold-storage.md)
- [Deployment Summary](007-ingestion-pipeline-fix-SUMMARY.md)
- [Cloud Tasks Fix](006-file-based-cold-storage-CLOUD-TASKS-FIX.md) (if exists)
- [Custom Ingestion Time](006-file-based-cold-storage-CUSTOM-TIME-GUIDE.md)

---

**Deployment Status:** ✅ **Deployed**  
**Tested:** ✅ Code compiles, unit tests pass, production deployment successful  
**Owner:** Tech Market Backend Team  
**Next Review:** After 30 days of production operation
