# Custom Ingestion Time Feature

**Date:** 2026-03-10  
**Status:** ✅ Implemented  
**Purpose:** Support historical data ingestion with accurate scrape timestamps  

---

## Overview

The file-based cold storage system now supports custom ingestion times, allowing you to ingest historical Apify datasets with accurate timestamps reflecting when the data was actually scraped, not when it's being ingested into the system.

---

## Use Cases

### 1. Historical Data Backfill
You've scraped data in the past but are ingesting it now. Use the original scrape time for accurate temporal analysis.

### 2. Data Recovery
Re-ingesting data from a backup with the original ingestion timestamp preserved.

### 3. Testing
Testing with known timestamps to verify time-based queries and aggregations.

---

## Usage

### API Endpoint

```bash
POST /api/admin/trigger-sync?datasetId={datasetId}&ingestedAt={ISO-8601-timestamp}
```

### Parameters

| Parameter | Required | Format | Description |
|-----------|----------|--------|-------------|
| `datasetId` | Yes | String | Apify dataset ID to ingest |
| `ingestedAt` | No | ISO-8601 | Custom ingestion time (e.g., `2026-03-09T10:30:00Z`) |
| `x-apify-signature` | Yes | String | Webhook secret for authentication |

### Examples

#### 1. Ingest with Current Time (Default Behavior)
```bash
curl -X POST "https://tech-market-backend.a.run.app/api/admin/trigger-sync?datasetId=Xwh8CqHp6RiRr0div" \
  -H "x-apify-signature: your-secret"
```

#### 2. Ingest with Custom Timestamp
```bash
# Ingest dataset as if it was scraped on March 9, 2026 at 10:30 AM UTC
curl -X POST "https://tech-market-backend.a.run.app/api/admin/trigger-sync?datasetId=Xwh8CqHp6RiRr0div&ingestedAt=2026-03-09T10:30:00Z" \
  -H "x-apify-signature: your-secret"
```

#### 3. Ingest with Different Timezones
```bash
# ISO-8601 supports various timezone formats
# UTC
ingestedAt=2026-03-09T10:30:00Z

# With timezone offset
ingestedAt=2026-03-09T11:30:00+01:00

# With milliseconds
ingestedAt=2026-03-09T10:30:00.000Z
```

---

## Response

### Success (200 OK)
```json
{
  "status": "queued",
  "taskName": "projects/tech-market-insights/locations/australia-southeast1/queues/tech-market-sync-queue/tasks/abc123",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Sync task queued for background processing with custom ingestion time"
}
```

### Error Responses

#### Invalid Timestamp Format (400 Bad Request)
```json
{
  "error": "Invalid ingestedAt format. Expected ISO-8601 format (e.g., 2026-03-10T10:30:00Z)"
}
```

#### Unauthorized (401 Unauthorized)
```json
{
  "error": "Unauthorized"
}
```

#### Missing Dataset ID (400 Bad Request)
```json
{
  "error": "Error: Provide a datasetId as a query param (?datasetId=...) or configure apify.dataset-id in application.yml"
}
```

---

## Implementation Details

### Data Flow

```
Admin API → Parse ingestedAt → Cloud Tasks → SyncTaskHandler → JobDataSyncService
                                              ↓
                                    Use custom time or Instant.now()
                                              ↓
                                    BronzeIngestionManifest.ingestedAt
```

### Code Changes

#### 1. JobDataSyncService
```kotlin
fun runDataSync(
    datasetId: String, 
    targetCountry: String? = null, 
    ingestedAt: Instant? = null  // NEW parameter
) {
    // Use custom ingestion time if provided, otherwise use current time
    val syncTime = ingestedAt ?: Instant.now()
    
    // ... rest of sync logic uses syncTime
}
```

#### 2. AdminController
```kotlin
@PostMapping("/trigger-sync")
fun triggerSync(
    @RequestParam datasetId: String?,
    @RequestParam ingestedAt: String?,  // NEW parameter (ISO-8601 string)
    @RequestHeader("x-apify-signature") providedSecret: String?
): ResponseEntity<Any> {
    // Parse ISO-8601 string to Instant
    val customIngestionTime = ingestedAt?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            log.warn("Invalid ingestedAt format: $it")
            null
        }
    }
    
    // Pass to Cloud Tasks payload
    val taskPayload = CloudTasksService.SyncTaskPayload(
        datasetId = effectiveId,
        ingestedAt = customIngestionTime?.toString()  // Serialize to ISO-8601
    )
}
```

#### 3. SyncTaskHandler
```kotlin
@PostMapping("/process-sync")
fun processSync(@RequestBody taskPayload: CloudTasksService.SyncTaskPayload) {
    // Parse ISO-8601 string back to Instant
    val customIngestionTime = taskPayload.ingestedAt?.let {
        try {
            Instant.parse(it)
        } catch (e: Exception) {
            log.warn("Invalid ingestedAt format in task payload: $it")
            null
        }
    }
    
    // Pass to sync service
    jobDataSyncService.runDataSync(
        taskPayload.datasetId, 
        taskPayload.country, 
        customIngestionTime
    )
}
```

---

## BigQuery Impact

### ingestion_metadata Table

The `ingested_at` field will reflect the custom timestamp:

```sql
SELECT dataset_id, source, ingested_at, processing_status
FROM `techmarket.ingestion_metadata`
WHERE dataset_id = 'Xwh8CqHp6RiRr0div';

-- Result with custom ingestion time:
-- dataset_id: Xwh8CqHp6RiRr0div
-- source: LinkedIn-Apify
-- ingested_at: 2026-03-09 10:30:00 UTC  ← Custom time
-- processing_status: COMPLETED
```

### Time-Based Queries

Your time-based queries will now use the accurate scrape time:

```sql
-- Get all datasets ingested (scraped) on March 9
SELECT * FROM `techmarket.ingestion_metadata`
WHERE DATE(ingested_at, 'Australia/Sydney') = '2026-03-09';

-- Trend analysis by scrape date (not ingestion date)
SELECT 
  DATE(ingested_at) as scrape_date,
  source,
  SUM(record_count) as jobs_scraped
FROM `techmarket.ingestion_metadata`
GROUP BY 1, 2
ORDER BY 1 DESC;
```

---

## Validation

### Format Requirements

The `ingestedAt` parameter must be in ISO-8601 format:

✅ **Valid Formats:**
- `2026-03-09T10:30:00Z` (UTC)
- `2026-03-09T11:30:00+01:00` (with offset)
- `2026-03-09T10:30:00.000Z` (with milliseconds)

❌ **Invalid Formats:**
- `2026-03-09 10:30:00` (missing T separator)
- `03/09/2026 10:30:00` (wrong format)
- `March 9, 2026` (not ISO-8601)
- `1678356600` (Unix timestamp - not supported)

### Automatic Validation

The system automatically validates the format:
- Invalid formats are logged as warnings
- Falls back to current time if invalid
- Processing continues (doesn't fail the entire request)

---

## Best Practices

### 1. Use UTC Timestamps
Always use UTC (`Z` suffix) to avoid timezone confusion:
```bash
# ✅ Good
ingestedAt=2026-03-09T10:30:00Z

# ⚠️ Avoid (unless you have specific timezone requirements)
ingestedAt=2026-03-09T11:30:00+01:00
```

### 2. Document Your Backfills
Keep a log of historical data ingestions:
```markdown
## Historical Data Backfill Log

| Date Ingested | Dataset ID | ingestedAt Used | Notes |
|---------------|------------|-----------------|-------|
| 2026-03-10 | Xwh8CqHp6RiRr0div | 2026-03-09T10:30:00Z | March 9 scrape |
| 2026-03-10 | abc123DEF456 | 2026-03-08T09:00:00Z | March 8 scrape |
```

### 3. Verify After Ingestion
Always verify the ingestion metadata:
```sql
SELECT dataset_id, ingested_at, processing_status
FROM `techmarket.ingestion_metadata`
WHERE dataset_id = '{your-dataset-id}';
```

### 4. Don't Mix Timezones
If you're backfilling multiple datasets from the same day, use consistent timestamps:
```bash
# ✅ Consistent - all use UTC
dataset1: ingestedAt=2026-03-09T09:00:00Z
dataset2: ingestedAt=2026-03-09T10:00:00Z

# ⚠️ Inconsistent - confusing for queries
dataset1: ingestedAt=2026-03-09T09:00:00Z
dataset2: ingestedAt=2026-03-09T10:00:00+01:00
```

---

## Troubleshooting

### Issue: Timestamp Not Applied
**Symptom:** `ingested_at` in BigQuery shows current time instead of custom time

**Causes:**
1. Invalid ISO-8601 format
2. Timezone parsing issue
3. Cloud Tasks payload serialization issue

**Debug Steps:**
```bash
# 1. Check backend logs for parsing errors
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=tech-market-backend" \
  --filter="Invalid ingestedAt" \
  --limit=10

# 2. Verify BigQuery metadata
bq query --use_legacy_sql=false \
  "SELECT dataset_id, ingested_at FROM techmarket.ingestion_metadata ORDER BY ingested_at DESC LIMIT 5"
```

**Fix:**
- Ensure ISO-8601 format with `Z` suffix
- Use quotes around the timestamp in curl commands
- Check that Cloud Tasks payload includes `ingestedAt` field

### Issue: Duplicate Dataset Error
**Symptom:** "Dataset X has already been ingested"

**Cause:** Idempotency guard uses `datasetId`, not `ingestedAt`

**Solution:**
```sql
-- Check if dataset exists
SELECT dataset_id, ingested_at, processing_status
FROM `techmarket.ingestion_metadata`
WHERE dataset_id = '{datasetId}';

-- If you need to re-ingest, delete the existing record first
DELETE FROM `techmarket.ingestion_metadata`
WHERE dataset_id = '{datasetId}';
```

---

## Related Documentation

- [Deployment Guide](architecture/006-file-based-cold-storage-DEPLOYMENT.md)
- [Quick Reference](architecture/006-file-based-cold-storage-QUICKREF.md)
- [ADR 006](architecture/006-file-based-cold-storage.md)

---

**Feature Status:** ✅ Production Ready  
**Implemented:** 2026-03-10  
**Owner:** Tech Market Backend Team
