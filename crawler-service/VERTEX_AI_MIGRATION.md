# Vertex AI Migration Guide

**Migrating from AI Studio API Key to Vertex AI Service Account**

---

## 🎯 Why Migrate?

**AI Studio API Key Issues:**
- Free tier quota limits (often 0 without billing)
- API key management complexity
- Limited enterprise features

**Vertex AI Benefits:**
- Service account authentication (more secure)
- Better quota management
- Enterprise-grade reliability
- Integrated with GCP billing
- No API key management needed

---

## 📋 Prerequisites

### 1. Service Account

Your backend service account should already exist:
```
181692518949-compute@developer.gserviceaccount.com
```

### 2. Enable Vertex AI API

```bash
gcloud services enable aiplatform.googleapis.com \
  --project=tech-market-insights
```

### 3. Grant Vertex AI Permissions

```bash
# Grant Vertex AI User role to service account
gcloud projects add-iam-policy-binding tech-market-insights \
  --member="serviceAccount:181692518949-compute@developer.gserviceaccount.com" \
  --role="roles/aiplatform.user"
```

### 4. Check Quotas

Go to: https://console.cloud.google.com/vertex-ai/quotas

Request increases if needed (default quotas are usually sufficient).

---

## 🚀 Deployment

### Using the Deployment Script

```bash
cd /Users/nic/Projects/job-market-gemini
chmod +x scripts/deployment/deploy-crawler-vertex.sh
./scripts/deployment/deploy-crawler-vertex.sh
```

### Manual Deployment

```bash
cd crawler-service

gcloud run deploy crawler-service \
  --source . \
  --project=tech-market-insights \
  --region=australia-southeast1 \
  --allow-unauthenticated \
  --min-instances=0 \
  --max-instances=3 \
  --cpu=1 \
  --memory=2Gi \
  --timeout=300 \
  --port=8080 \
  --set-env-vars="NODE_ENV=production,GCP_PROJECT_ID=tech-market-insights,GCP_REGION=australia-southeast1" \
  --service-account="181692518949-compute@developer.gserviceaccount.com"
```

---

## 🔧 Configuration Changes

### Environment Variables

**Before (AI Studio):**
```bash
GEMINI_API_KEY=AIzaSy...
```

**After (Vertex AI):**
```bash
GCP_PROJECT_ID=tech-market-insights
GCP_REGION=australia-southeast1
GEMINI_MODEL=gemini-2.0-flash  # Optional
```

### Code Changes

**Before:**
```typescript
const crawlerService = new CrawlerService(process.env.GEMINI_API_KEY);
```

**After:**
```typescript
const crawlerService = new CrawlerService(
  process.env.GCP_PROJECT_ID,
  process.env.GCP_REGION,
  process.env.GEMINI_MODEL
);
```

---

## 🧪 Testing

### Health Check

```bash
curl https://crawler-service-xxx.a.run.app/health | jq .
```

**Expected Response:**
```json
{
  "status": "ok",
  "timestamp": "2026-03-11T12:00:00.000Z",
  "vertexAI": "configured"
}
```

### Test Crawl

```bash
curl -X POST https://crawler-service-xxx.a.run.app/crawl \
  -H "Content-Type: application/json" \
  -d '{"companyId": "xero", "url": "https://www.xero.com/au/about/careers/"}' | jq .
```

---

## 📊 Cost Comparison

### AI Studio (Free Tier)
- 60 requests/minute
- 1,500 requests/day
- **Problem**: Often shows "limit: 0" without billing

### Vertex AI (Pay-as-you-go)
- **Gemini 2.0 Flash**:
  - Input: $0.075 per 1M tokens
  - Output: $0.30 per 1M tokens
- **Average crawl**: ~$0.0002-0.0003
- **Monthly estimate** (1,257 companies weekly): ~$1-2

**Note**: Vertex AI charges your GCP billing account directly - no separate API key billing needed.

---

## 🔍 Troubleshooting

### Error: "Permission Denied"

**Cause**: Service account lacks Vertex AI permissions

**Solution**:
```bash
gcloud projects add-iam-policy-binding tech-market-insights \
  --member="serviceAccount:181692518949-compute@developer.gserviceaccount.com" \
  --role="roles/aiplatform.user"
```

### Error: "API not enabled"

**Cause**: Vertex AI API not enabled

**Solution**:
```bash
gcloud services enable aiplatform.googleapis.com \
  --project=tech-market-insights
```

### Error: "Quota exceeded"

**Cause**: Vertex AI quota limit reached

**Solution**:
1. Wait for quota reset (usually 1 minute for rate limits)
2. Request quota increase: https://console.cloud.google.com/vertex-ai/quotas

---

## 📚 Migration Checklist

- [ ] Enable Vertex AI API
- [ ] Grant service account "Vertex AI User" role
- [ ] Check quotas at https://console.cloud.google.com/vertex-ai/quotas
- [ ] Update deployment script
- [ ] Deploy new version
- [ ] Test health endpoint
- [ ] Test crawl with real company
- [ ] Remove old GEMINI_API_KEY secret (optional)
- [ ] Update documentation

---

## 🎯 Post-Migration

### Monitoring

```bash
# View logs
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --follow

# Check Vertex AI usage
https://console.cloud.google.com/vertex-ai/usage
```

### Billing

Vertex AI charges appear in your GCP billing:
```bash
gcloud billing accounts list
```

---

## 📞 Support

### Documentation
- [Vertex AI Documentation](https://cloud.google.com/vertex-ai/docs)
- [Gemini API Reference](https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/gemini)
- [Quotas](https://cloud.google.com/vertex-ai/docs/quotas)

### Console
- Vertex AI: https://console.cloud.google.com/vertex-ai
- Quotas: https://console.cloud.google.com/vertex-ai/quotas
- Usage: https://console.cloud.google.com/vertex-ai/usage

---

**Migration Date**: March 11, 2026  
**Status**: Ready for deployment  
**Next Step**: Enable Vertex AI API and deploy
