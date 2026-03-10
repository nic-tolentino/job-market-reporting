# Custom Ingestion Time - Quick Guide

**For:** Historical data ingestion with accurate timestamps  
**Status:** ✅ Production Ready  

---

## Quick Usage

### Ingest with Current Time (Default)
```bash
curl -X POST "https://your-backend-url/api/admin/trigger-sync?datasetId=Xwh8CqHp6RiRr0div" \
  -H "x-apify-signature: your-secret"
```

### Ingest with Custom Timestamp
```bash
# Use when the data was actually scraped
curl -X POST "https://your-backend-url/api/admin/trigger-sync?datasetId=Xwh8CqHp6RiRr0div&ingestedAt=2026-03-09T10:30:00Z" \
  -H "x-apify-signature: your-secret"
```

---

## When to Use Custom Ingestion Time

| Scenario | Use Custom Time? | Example |
|----------|------------------|---------|
| Daily scraping | ❌ No (use current time) | `trigger-sync?datasetId=abc123` |
| Historical backfill | ✅ Yes | `&ingestedAt=2026-03-09T10:30:00Z` |
| Data recovery from backup | ✅ Yes | `&ingestedAt=2026-03-08T09:00:00Z` |
| Testing with known dates | ✅ Yes | `&ingestedAt=2026-01-01T00:00:00Z` |

---

## Timestamp Format

### ✅ Valid ISO-8601 Formats
```
2026-03-09T10:30:00Z          # UTC (recommended)
2026-03-09T11:30:00+01:00     # With timezone offset
2026-03-09T10:30:00.000Z      # With milliseconds
```

### ❌ Invalid Formats
```
2026-03-09 10:30:00           # Missing T separator
03/09/2026 10:30:00           # Wrong format
March 9, 2026                 # Not ISO-8601
1678356600                    # Unix timestamp (not supported)
```

---

## Verify Ingestion

### Check BigQuery Metadata
```sql
SELECT dataset_id, source, ingested_at, processing_status
FROM `techmarket.ingestion_metadata`
WHERE dataset_id = 'Xwh8CqHp6RiRr0div';
```

### Expected Result
```
dataset_id: Xwh8CqHp6RiRr0div
source: LinkedIn-Apify
ingested_at: 2026-03-09 10:30:00 UTC  ← Your custom time
processing_status: COMPLETED
```

---

## Common Issues

### Issue: "Invalid ingestedAt format"
**Fix:** Use ISO-8601 format with `T` separator and `Z` suffix
```bash
# ✅ Correct
ingestedAt=2026-03-09T10:30:00Z

# ❌ Wrong
ingestedAt=2026-03-09 10:30:00
```

### Issue: "Dataset already ingested"
**Fix:** Idempotency uses `datasetId`, not timestamp. Delete existing record first:
```sql
DELETE FROM `techmarket.ingestion_metadata`
WHERE dataset_id = 'Xwh8CqHp6RiRr0div';
```

---

## Full Documentation

- [Custom Ingestion Time Guide](006-file-based-cold-storage-CUSTOM-INGESTION-TIME.md) - Complete feature documentation
- [Deployment Guide](006-file-based-cold-storage-DEPLOYMENT.md) - Production deployment steps
- [Quick Reference](006-file-based-cold-storage-QUICKREF.md) - One-page cheat sheet

---

**Last Updated:** 2026-03-10  
**Owner:** Tech Market Backend Team
