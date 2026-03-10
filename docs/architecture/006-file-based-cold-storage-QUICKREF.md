# File-Based Cold Storage - Quick Reference

**Status:** ✅ Production Deployed (2026-03-10)  
**Project:** tech-market-insights  
**Region:** australia-southeast1  

---

## Documentation Index

| Document | Purpose | Location |
|----------|---------|----------|
| **ADR 006** | Architecture decision record - why we chose this approach | [006-file-based-cold-storage.md](006-file-based-cold-storage.md) |
| **Deployment Guide** | Step-by-step deployment instructions and troubleshooting | [006-file-based-cold-storage-DEPLOYMENT.md](006-file-based-cold-storage-DEPLOYMENT.md) |
| **Implementation Plan** | Original detailed implementation plan | [../../implementation-plans/6.1-file-based-cold-storage-plan.md](../../implementation-plans/6.1-file-based-cold-storage-plan.md) |

---

## Quick Start

### Deploy Infrastructure
```bash
cd terraform/gcp
terraform init
terraform apply -var="gcp_project_id=tech-market-insights" \
                -var="gcp_service_account=tech-market-backend@tech-market-insights.iam.gserviceaccount.com"
```

### Deploy Backend
```bash
# Set environment variable
export GCS_BRONZE_BUCKET=techmarket-bronze-ingestions

# Deploy to Cloud Run
gcloud run deploy tech-market-backend \
  --image gcr.io/tech-market-insights/backend:latest \
  --set-env-vars GCS_BRONZE_BUCKET=techmarket-bronze-ingestions
```

### Verify Deployment
```bash
# Trigger sync
curl -X POST https://tech-market-backend.a.run.app/api/admin/sync-jobs?datasetId=test123

# Check BigQuery
bq query --use_legacy_sql=false \
  "SELECT dataset_id, processing_status FROM techmarket.ingestion_metadata ORDER BY ingested_at DESC LIMIT 5"
```

---

## Critical Gotcha: BigQuery TIMESTAMP Serialization

**⚠️ DO NOT use ISO-8601 strings for BigQuery TIMESTAMP fields!**

```kotlin
// ❌ WRONG - Will fail with 403 Forbidden
mapOf("ingested_at" to instant.toString())

// ✅ CORRECT - Use microseconds
mapOf("ingested_at" to QueryParameterValue.timestamp(instant.toEpochMilli() * 1000))
```

**Why:** BigQuery Storage Write API requires microseconds (Long), not ISO-8601 strings.

**See:** [Deployment Guide - Key Fixes](006-file-based-cold-storage-DEPLOYMENT.md#key-deployment-fix-bigquery-serialization)

---

## Architecture Overview

```
Apify/ATS → BronzeRepository → GCS (compressed files)
                              → BigQuery (metadata index)
                              → Silver Layer (structured data)
```

**Storage:**
- GCS: `gs://techmarket-bronze-ingestions/{source}/{date}/dataset-{id}/jobs-0001.json.gz`
- BigQuery: `techmarket.ingestion_metadata` (manifest index)
- Format: NDJSON + GZIP (~80% compression)

**Cost:** ~$10/month for 500GB (96% savings vs BigQuery-native)

---

## Key Files

### Backend (Kotlin)
| File | Purpose |
|------|---------|
| `BronzeRepository.kt` | Repository interface |
| `BronzeGcsRepository.kt` | GCS implementation |
| `IngestionMetadataRepository.kt` | BigQuery metadata |
| `BronzeIngestionManifest.kt` | Data model |
| `JobDataSyncService.kt` | Apify sync orchestration |
| `AtsJobDataSyncService.kt` | ATS sync orchestration |

### Infrastructure (Terraform)
| File | Purpose |
|------|---------|
| `storage_bucket.tf` | GCS bucket + lifecycle policies |
| `bigquery_external_table.tf` | BigQuery external table + metadata table |

---

## Monitoring Queries

### Daily Ingestion Volume
```sql
SELECT DATE(ingested_at) as date, source, SUM(record_count) as records
FROM `techmarket.ingestion_metadata`
WHERE processing_status = 'COMPLETED'
GROUP BY 1, 2
ORDER BY 1 DESC;
```

### Stuck Manifests (PENDING > 1 hour)
```sql
SELECT dataset_id, source, ingested_at
FROM `techmarket.ingestion_metadata`
WHERE processing_status = 'PENDING'
  AND ingested_at < TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 1 HOUR);
```

### Compression Efficiency
```sql
SELECT source, AVG(compression_ratio) as avg_ratio, COUNT(*) as datasets
FROM `techmarket.ingestion_metadata`
GROUP BY source;
```

---

## Troubleshooting Quick Reference

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| 403 Forbidden on BigQuery write | ISO-8601 timestamps | Use microsecond timestamps |
| GCS 404 Not Found | Wrong bucket name | Check `GCS_BRONZE_BUCKET` env var |
| Status stuck in PENDING | Silver layer failure | Check logs for mapping errors |
| GZIP decompression error | Wrong decompression library | Use `GZIPInputStream`, not `InflaterInputStream` |

**Full troubleshooting:** [Deployment Guide - Troubleshooting](006-file-based-cold-storage-DEPLOYMENT.md#troubleshooting)

---

## Production Metrics

| Metric | Target | Current |
|--------|--------|---------|
| Storage cost (500GB) | <$10/month | ~$10/month ✅ |
| Compression ratio | ~0.2 | ~0.2 ✅ |
| Processing status | 100% COMPLETED | 100% ✅ |
| Ingestion latency | <5s | <5s ✅ |

**Last verified:** 2026-03-10

---

## Support

- **Documentation:** See index above
- **Code:** `backend/src/main/kotlin/com/techmarket/persistence/ingestion/`
- **Infrastructure:** `terraform/gcp/`
- **Owner:** Tech Market Backend Team

---

**For detailed information, always refer to the full [ADR](006-file-based-cold-storage.md) and [Deployment Guide](006-file-based-cold-storage-DEPLOYMENT.md).**
