# ADR 006: File-Based Cold Storage for Dataset Ingestions

**Date:** 2026-03-10  
**Status:** Accepted  
**Deciders:** Tech Market Team  
**Tags:** architecture, data-layer, cost-optimization, scalability

---

## Context and Problem Statement

### Current State
- Raw JSON payloads from Apify scrapers and ATS integrations stored directly in BigQuery `raw_ingestions` table
- Each ingestion writes full JSON strings to BigQuery rows
- Cost: ~$0.50/GB/month for active storage plus query costs
- No compression optimization for historical data
- BigQuery row limits and streaming quotas become bottlenecks at high ingestion rates

### Problem
With daily Apify scrapes and ATS integrations for 50+ companies, we expect data volume to scale from ~10GB/month to ~500GB/month:
- **Current cost:** ~$5/month → **Scaled cost:** ~$250/month (BigQuery-native)
- BigQuery streaming quotas limit ingestion frequency
- No cost-effective archival strategy for historical data

### Decision Drivers
- **Cost reduction:** Target 90%+ storage cost savings
- **Scalability:** Support unlimited ingestion frequency
- **Data lifecycle:** Automatic archival of cold data
- **Query performance:** Maintain ability to query historical data
- **Audit trail:** Preserve immutable raw data for reprocessing

---

## Considered Options

### Option 1: BigQuery-Native Storage (Current)
Store all raw JSON payloads directly in BigQuery tables.

**Pros:**
- Simple implementation
- Fast queries with clustering
- Built-in backup and versioning

**Cons:**
- Expensive at scale (~$250/month for 500GB)
- Streaming quotas limit ingestion rate
- No automatic cost optimization for cold data

### Option 2: GCS File-Based Storage with BigQuery Metadata ✅
Store compressed JSON files in GCS with BigQuery metadata index.

**Pros:**
- 96% cost reduction (~$10/month for 500GB)
- No practical storage limits
- Automatic lifecycle policies (STANDARD → COLDLINE → ARCHIVE)
- Gzip compression (~80% size reduction)
- External table support for direct querying

**Cons:**
- More complex implementation
- Slightly slower queries on external tables
- Requires managing two storage systems

### Option 3: Parquet Format on GCS
Store data in Parquet format instead of NDJSON+gzip.

**Pros:**
- 60-80% better compression than NDJSON+gzip
- Predicate pushdown for faster queries
- Schema enforcement and type safety

**Cons:**
- More complex serialization logic
- Requires schema evolution management
- Not implemented in initial phase (future optimization)

### Option 4: Cloud Storage + Dataflow
Use Cloud Dataflow for processing instead of backend.

**Pros:**
- Fully managed processing
- Automatic scaling
- Better for very large datasets

**Cons:**
- Higher operational complexity
- Increased cost for small/medium workloads
- Overkill for current scale (< 1M records/month)

---

## Decision

**We choose Option 2: GCS File-Based Storage with BigQuery Metadata**

This option provides the best balance of cost savings (96%), scalability, and maintainability. It aligns with our current architecture and team expertise while providing a clear migration path to Parquet format (Option 3) in the future if needed.

---

## Architecture

### Storage Structure
```
gs://techmarket-bronze-ingestions/
├── apify/
│   ├── 2026/
│   │   ├── 03/
│   │   │   ├── 10/
│   │   │   │   ├── dataset-{id}/
│   │   │   │   │   ├── jobs-0001.json.gz
│   │   │   │   │   └── jobs-0002.json.gz
│   │   │   │   └── dataset-{id}/
│   │   └── dataset-{id}/
│   └── dataset-{id}/
└── ats/
    ├── workday/
    │   └── {company-id}/
    │       └── 2026-03-10.json.gz
    ├── greenhouse/
    └── lever/
```

### Data Flow
```
┌─────────────┐     ┌──────────────────────────────────────┐
│  Apify/ATS  │────▶│       JobDataSyncService             │
│  Scrapers   │     │       AtsJobDataSyncService          │
└─────────────┘     └─────────────────┬────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    │                                   │
                    ▼                                   ▼
        ┌───────────────────┐              ┌──────────────────┐
        │  GCS Bucket       │              │  BigQuery        │
        │  (Bronze Files)   │              │  (Metadata)      │
        │  - Compressed     │              │  - Manifest      │
        │  - Partitioned    │              │  - Index         │
        └─────────┬─────────┘              └────────┬─────────┘
                  │                                 │
                  └─────────────┬───────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │   Silver Layer        │
                    │   (raw_jobs,          │
                    │    raw_companies)     │
                    └───────────────────────┘
```

### Key Components

#### 1. BronzeRepository Interface
```kotlin
interface BronzeRepository {
    fun saveIngestion(manifest: BronzeIngestionManifest, dataChunks: List<ByteArray>): BronzeIngestionManifest
    fun getManifest(datasetId: String): BronzeIngestionManifest?
    fun isDatasetIngested(datasetId: String): Boolean
    fun listManifests(source: String? = null, fromDate: Instant? = null, toDate: Instant? = null): List<BronzeIngestionManifest>
    fun readFile(filePath: String): InputStream
    fun updateProcessingStatus(datasetId: String, status: ProcessingStatus): Boolean
}
```

#### 2. BronzeIngestionManifest
```kotlin
data class BronzeIngestionManifest(
    val datasetId: String,
    val source: String,
    val ingestedAt: Instant,
    val targetCountry: String?,
    val schemaVersion: String = "1.0",
    val recordCount: Int,
    val fileCount: Int,
    val uncompressedSizeBytes: Long,
    val compressedSizeBytes: Long,
    val compressionRatio: Double,
    val processingStatus: ProcessingStatus,
    val files: List<String>,  // GCS paths
    val metadataId: String? = null
)

enum class ProcessingStatus {
    PENDING,      // Uploaded to GCS, not yet processed
    COMPLETED,    // Successfully mapped to Silver layer
    FAILED        // Silver layer mapping failed
}
```

#### 3. Chunking Strategy
| Source | Chunk Size | Rationale |
|--------|------------|-----------|
| Apify (large crawls) | 5,000 records | Fewer, larger files for better BigQuery performance |
| ATS integrations | 500 records | Smaller chunks for frequent, small ingestions |
| Mixed/unknown | 2,000 records | Balanced default |

#### 4. File Format
- **Format:** NDJSON (newline-delimited JSON)
- **Compression:** GZIP (~80% size reduction)
- **Encoding:** UTF-8

---

## Implementation Details

### Terraform Infrastructure
- **GCS Bucket:** `techmarket-bronze-ingestions`
- **Lifecycle Policies:**
  - 0-90 days: STANDARD (frequent access)
  - 90-365 days: COLDLINE (infrequent access)
  - 365+ days: ARCHIVE (long-term archival)
- **BigQuery Tables:**
  - `raw_ingestions_external`: External table for querying GCS files
  - `ingestion_metadata`: Metadata index with clustering on `source` and `ingested_at`
- **IAM:** Single `roles/storage.objectAdmin` role for backend service account

### Backend Implementation
- **New Packages:**
  - `com.techmarket.persistence.ingestion` - Repository layer
  - `com.techmarket.persistence.model.BronzeIngestionManifest` - Data model
- **Updated Services:**
  - `JobDataSyncService` - Migrated to use BronzeRepository
  - `AtsJobDataSyncService` - Migrated to use BronzeRepository
- **Configuration:**
  - `GcsConfig` - GCS bucket configuration
  - `application.yml` - GCS settings

### Error Handling
- **Orphaned File Cleanup:** Automatic deletion of GCS files if metadata save fails
- **Status Tracking:** Processing status updated to COMPLETED or FAILED after Silver layer mapping
- **Batch Processing:** Incremental persistence to avoid OOM errors during reprocessing

---

## Consequences and Trade-offs

### Positive Consequences
- **Cost Savings:** 96% reduction in storage costs (~$240/month at 500GB scale)
- **Scalability:** No practical limits on ingestion frequency or data volume
- **Automatic Archival:** Lifecycle policies manage cost optimization automatically
- **Audit Trail:** Immutable raw data preserved for compliance and reprocessing
- **Flexibility:** Can query GCS files directly via BigQuery external tables

### Negative Consequences
- **Complexity:** More complex than BigQuery-native storage
- **Query Performance:** External table queries slower than native BigQuery tables
- **Operational Overhead:** Managing two storage systems (GCS + BigQuery)
- **Testing:** Requires integration tests with GCS/BigQuery emulators

### Trade-offs Accepted
- **Chose NDJSON over Parquet:** Simpler implementation, can migrate later if needed
- **Chose backend processing over Dataflow:** Lower cost for current scale, can migrate if needed
- **Chose relaxed consistency over strong consistency:** Metadata updates may lag behind file uploads

---

## Implementation Notes

### Critical: BigQuery TIMESTAMP Serialization
**Issue:** BigQuery Storage Write API rejects ISO-8601 timestamp strings for TIMESTAMP fields.

**Solution:** Use microsecond timestamps (not ISO-8601 strings):

```kotlin
// ✅ Correct - Microseconds since epoch
IngestionMetadataFields.INGESTED_AT to QueryParameterValue.timestamp(
    instant.toEpochMilli() * 1000
)

// ❌ Incorrect - ISO-8601 string (will fail with 403 Forbidden)
IngestionMetadataFields.INGESTED_AT to QueryParameterValue.string(instant.toString())
```

**Why:** The BigQuery Storage Write API (used by `bigQueryTemplate.writeJsonStream()`) requires TIMESTAMP fields as `Long` values representing microseconds since Unix epoch, not ISO-8601 strings.

**Reference:** See [Deployment Guide](006-file-based-cold-storage-DEPLOYMENT.md#key-deployment-fix-bigquery-serialization) for full details.

---

## Migration Plan

### Phase 1: Infrastructure Setup (Week 1)
```bash
cd terraform/gcp
terraform init
terraform plan -var="gcp_project_id=your-project" -var="gcp_service_account=your-sa"
terraform apply
```

### Phase 2: Backend Deployment (Week 2)
1. Deploy updated backend with BronzeRepository
2. Update environment variables:
   ```bash
   GCS_BRONZE_BUCKET=techmarket-bronze-ingestions
   ```
3. New ingestions automatically use GCS storage

### Phase 3: Historical Data (Optional)
1. Old `raw_ingestions` table remains intact
2. Run `reprocessHistoricalData()` to refresh Silver layer from new storage
3. Consider migrating historical data to GCS if cost savings justify effort

---

## Monitoring and Success Metrics

### Cost Metrics
- **Storage cost per GB:** Target <$0.02/GB/month (down from $0.50)
- **Total monthly cost:** Target <$10/month for 500GB (down from $250)
- **Compression ratio:** Target ~0.2 (80% reduction)

### Performance Metrics
- **Ingestion latency:** < 5 seconds per dataset
- **Query latency:** < 30 seconds for external table queries
- **Reprocessing time:** < 1 hour for full historical reprocessing

### Reliability Metrics
- **Data loss:** 0% (all ingested data preserved)
- **Status tracking:** 100% of manifests transition to COMPLETED or FAILED
- **Cleanup success:** 100% of orphaned files deleted on failure

---

## Future Considerations

### Potential Optimizations
1. **Parquet Format:** Migrate from NDJSON+gzip to Parquet for 60-80% better compression
2. **Partitioning:** Add date-based partitioning to external table for faster queries
3. **Streaming Reads:** Implement streaming for large files (>10MB) to reduce JVM memory pressure
4. **Cloud Dataflow:** Migrate reprocessing to Dataflow for very large datasets (>100M records)

### Deprecation Plan
Once GCS-based storage is validated in production:
1. Deprecate `IngestionRepository` interface
2. Remove `IngestionBigQueryRepository` implementation
3. Delete old `raw_ingestions` table after migration period

---

## Compliance and Security

### Data Retention
- **Retention period:** Indefinite (immutable audit trail)
- **Deletion policy:** Manual deletion only, with approval
- **Backup:** GCS provides automatic redundancy across zones

### Access Control
- **IAM:** Service account with minimal required permissions
- **Network:** No public access, backend-only via service account
- **Encryption:** GCS default encryption at rest, TLS in transit

### Audit Trail
- **Immutable storage:** Raw data cannot be modified after ingestion
- **Metadata tracking:** All ingestions logged with timestamp and source
- **Status tracking:** Full audit trail from Bronze to Silver layer

---

## References

### Related ADRs
- ADR 002: Apify Integration Pattern
- ADR 003: ATS Integration Strategy
- ADR 004: Silver Layer Data Model

### Documentation
- [Implementation Plan](docs/implementation-plans/6.1-file-based-cold-storage-plan.md)
- [Terraform Configuration](terraform/gcp/storage_bucket.tf)
- [Backend Implementation](backend/src/main/kotlin/com/techmarket/persistence/ingestion/)

### External Resources
- [GCS Storage Classes](https://cloud.google.com/storage/docs/storage-classes)
- [BigQuery External Tables](https://cloud.google.com/bigquery/external-data-sources)
- [GCS Lifecycle Management](https://cloud.google.com/storage/docs/lifecycle)

---

## Review and Update History

| Date | Reviewer | Changes |
|------|----------|---------|
| 2026-03-10 | Tech Market Team | Initial ADR creation |
| 2026-03-10 | Antigravity (Claude) | Round 1 review - 7 critical issues fixed |
| 2026-03-10 | Antigravity (Claude) | Round 2 review - 6 clean-up items fixed |
| 2026-03-10 | Antigravity (Claude) | Round 3 review - test mock setup fixed |
| 2026-03-10 | Tech Market Team | Final approval for production deployment |

---

**Status:** ✅ PRODUCTION READY  
**Next Review:** After 30 days of production operation  
**Owner:** Tech Market Backend Team
