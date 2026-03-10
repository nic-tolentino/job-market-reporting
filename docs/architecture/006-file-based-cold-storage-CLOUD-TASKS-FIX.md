# Cloud Tasks Authentication Fix

**Date:** 2026-03-10  
**Issue:** `StatusRuntimeException: NOT_FOUND` when Cloud Tasks tries to call backend  
**Status:** 🔧 In Progress  

---

## Problem

Cloud Tasks is failing to authenticate with Cloud Run backend:
```
StatusRuntimeException: NOT_FOUND: Requested entity was not found
```

This happens because:
1. Cloud Tasks queue doesn't have OIDC authentication configured
2. Cloud Run rejects unauthenticated requests
3. Tasks can't be processed, so datasets never get ingested

---

## Solution

### 1. Add OIDC Authentication to Cloud Tasks Queue ✅

**File:** `terraform/gcp/cloud_tasks.tf`

Added `oidc_access_token_config` block:
```hcl
# OIDC authentication for Cloud Run
oidc_access_token_config {
  service_account_email = var.gcp_service_account
  audience             = "https://tech-market-backend.a.run.app"
}
```

**What this does:**
- Cloud Tasks will generate OIDC tokens for each task
- Tokens are signed by the service account
- Cloud Run validates the token and allows the request
- Audience must match the Cloud Run service URL exactly

### 2. Apply Terraform Changes ⏳

```bash
cd terraform/gcp
terraform apply
```

**Expected output:**
```
google_cloud_tasks_queue.sync_queue will be updated
  ~ oidc_access_token_config {
      + audience             = "https://tech-market-backend.a.run.app"
      + service_account_email = "tech-market-backend@tech-market-insights.iam.gserviceaccount.com"
    }
```

### 3. Verify Cloud Tasks Configuration

After Terraform apply completes:
```bash
gcloud tasks queues describe tech-market-sync-queue \
  --location=australia-southeast1 \
  --format="yaml(oidcAccess_token_config)"
```

**Expected output:**
```yaml
oidcAccessTokenConfig:
  audience: https://tech-market-backend.a.run.app
  serviceAccountEmail: tech-market-backend@tech-market-insights.iam.gserviceaccount.com
```

---

## Testing

### Test Cloud Tasks Integration

1. **Trigger a test dataset:**
   ```bash
   curl -X POST "https://tech-market-backend.a.run.app/api/admin/trigger-sync?datasetId=test123" \
     -H "x-apify-signature: YOUR_WEBHOOK_SECRET"
   ```

2. **Check Cloud Tasks logs:**
   ```bash
   gcloud tasks queues list-operations \
     projects/tech-market-insights/locations/australia-southeast1/queues/tech-market-sync-queue \
     --filter="responseCode=200"
   ```

3. **Check backend logs:**
   ```bash
   gcloud run services logs read tech-market-backend \
     --region=australia-southeast1 \
     --limit=20 \
     --filter="process-sync"
   ```

4. **Verify dataset was processed:**
   ```bash
   bq query --use_legacy_sql=false \
     "SELECT dataset_id, processing_status FROM techmarket.ingestion_metadata ORDER BY ingested_at DESC LIMIT 5"
   ```

---

## Troubleshooting

### Issue: Terraform Apply Fails

**Error:** `Permission denied on Cloud Tasks queue`

**Fix:** Ensure service account has Cloud Tasks admin role:
```bash
gcloud projects add-iam-policy-binding tech-market-insights \
  --member="serviceAccount:terraform@tech-market-insights.iam.gserviceaccount.com" \
  --role="roles/cloudtasks.admin"
```

### Issue: Still Getting NOT_FOUND After Apply

**Possible causes:**
1. **Wrong audience URL** - Must match Cloud Run service URL exactly
2. **Service account permissions** - Must have Cloud Run invoker role
3. **Cloud Run not allowing unauthenticated invocations**

**Fix permissions:**
```bash
# Grant Cloud Run invoker to service account
gcloud run services add-iam-policy-binding tech-market-backend \
  --region=australia-southeast1 \
  --member="serviceAccount:tech-market-backend@tech-market-insights.iam.gserviceaccount.com" \
  --role="roles/run.invoker"
```

### Issue: Backend Still Returns 404

**Check if backend is running:**
```bash
curl -v https://tech-market-backend.a.run.app/actuator/health
```

If 404, backend may need redeployment:
```bash
cd backend
./gradlew bootBuildImage
docker push gcr.io/tech-market-insights/tech-market-backend:latest
gcloud run deploy tech-market-backend \
  --image gcr.io/tech-market-insights/tech-market-backend:latest \
  --region=australia-southeast1
```

---

## Next Steps After Fix

Once Cloud Tasks authentication is working:

1. **Trigger missing datasets:**
   ```bash
   # Dataset 1
   curl -X POST "https://tech-market-backend.a.run.app/api/admin/trigger-sync?datasetId=q1lYhL1JW43Z3eaTO&ingestedAt=2026-03-06T04:25:18Z" \
     -H "x-apify-signature: YOUR_WEBHOOK_SECRET"
   
   # Wait 90 seconds, then verify
   bq query --use_legacy_sql=false \
     "SELECT dataset_id, processing_status FROM techmarket.ingestion_metadata WHERE dataset_id='q1lYhL1JW43Z3eaTO'"
   
   # Repeat for remaining datasets...
   ```

2. **Verify all 8 datasets:**
   ```bash
   bq query --use_legacy_sql=false \
     "SELECT dataset_id, record_count, processing_status, ingested_at FROM techmarket.ingestion_metadata ORDER BY ingested_at DESC"
   ```

3. **Expected final state:**
   - 8 datasets in `ingestion_metadata` (all COMPLETED)
   - ~4,824 jobs in `raw_jobs`
   - All unique companies in `raw_companies`

---

## Related Issues

- [Custom Ingestion Time Feature](006-file-based-cold-storage-CUSTOM-INGESTION-TIME.md)
- [Deployment Guide](006-file-based-cold-storage-DEPLOYMENT.md)
- [Quick Reference](006-file-based-cold-storage-QUICKREF.md)

---

**Status:** Terraform apply in progress  
**Next:** Verify OIDC config, then trigger missing datasets  
**Owner:** Tech Market Backend Team
