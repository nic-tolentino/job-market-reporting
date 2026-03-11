# 🚀 Crawler Service Deployment Guide

**Quick Start Guide for Deploying the Self-Hosted AI Crawler**

---

## ⚡ Quick Start (5 Minutes)

### Step 1: Get Gemini API Key

1. Go to: https://aistudio.google.com/app/apikey
2. Sign in with Google account
3. Click "Create API Key"
4. Copy the key (starts with `AIza...`)

### Step 2: Add to Secret Manager

```bash
# Create secret
gcloud secrets create GEMINI_API_KEY \
    --project=tech-market-insights \
    --replication-policy="automatic"

# Add your key (paste when prompted)
gcloud secrets versions add GEMINI_API_KEY \
    --project=tech-market-insights \
    --data-file=-
```

### Step 3: Deploy

```bash
cd /Users/nic/Projects/job-market-gemini
./scripts/deployment/deploy-crawler.sh
```

### Step 4: Test

```bash
export CRAWLER_SERVICE_URL=https://crawler-service-xxx.a.run.app
./scripts/testing/test-crawler.sh
```

---

## 📁 What Was Created

### Deployment Scripts
- `scripts/deployment/deploy-crawler.sh` - Deploys to Cloud Run
- `scripts/testing/test-crawler.sh` - Test suite

### Documentation
- `crawler-service/DEPLOYMENT_PLAN.md` - Full deployment guide
- `crawler-service/GET_GEMINI_API_KEY.md` - How to get API key
- `crawler-service/README.md` - API documentation
- `crawler-service/QUICKSTART.md` - Getting started

### Code Ready
- ✅ TypeScript compiles successfully
- ✅ Kotlin backend integration complete
- ✅ 95/95 tests passing
- ✅ Dockerfile configured
- ✅ BigQuery logging ready

---

## 🔧 Configuration

### Environment Variables (.env)

```bash
# Google Cloud
GCP_PROJECT_ID=tech-market-insights
GCP_REGION=australia-southeast1

# Crawler Service
CRAWLER_SERVICE_URL=https://crawler-service-xxx.a.run.app  # After deployment

# Gemini API (required)
GEMINI_API_KEY=AIzaSy...  # Store in Secret Manager
```

### Backend Integration

The Kotlin backend is already configured to use the crawler:

```kotlin
// CrawlerClient.kt
class CrawlerClient(
    private val crawlerServiceUrl: String,
    private val httpClient: HttpClient
)
```

Just set `CRAWLER_SERVICE_URL` in backend `.env` after deployment.

---

## 📊 Architecture

```
┌─────────────────┐
│   Backend       │
│   (Kotlin)      │
└────────┬────────┘
         │ HTTP
         │
         ▼
┌─────────────────┐
│  Crawler        │
│  Service        │
│  (Node.js)      │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌───────┐  ┌──────────┐
│Gemini │  │  Target  │
│  API  │  │  Website │
└───────┘  └──────────┘
```

---

## 🧪 Testing

### Manual Test

```bash
# Health check
curl https://crawler-service-xxx.a.run.app/health

# Single crawl
curl -X POST https://crawler-service-xxx.a.run.app/crawl \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "airwallex",
    "url": "https://airwallex.com/careers"
  }'

# Batch crawl
curl -X POST https://crawler-service-xxx.a.run.app/crawl/batch \
  -H "Content-Type: application/json" \
  -d '{
    "requests": [
      {"companyId": "canva", "url": "https://canva.com/careers"},
      {"companyId": "atlassian", "url": "https://atlassian.com/careers"}
    ]
  }'
```

### Automated Tests

```bash
# Run test suite
export CRAWLER_SERVICE_URL=https://crawler-service-xxx.a.run.app
./scripts/testing/test-crawler.sh
```

---

## 📈 Monitoring

### Check Logs

```bash
# Real-time logs
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --follow
```

### BigQuery Analytics

```sql
-- Recent crawls
SELECT 
  company_id,
  crawl_date,
  pages_visited,
  jobs_extracted,
  quality_score,
  quality_tier,
  detected_ats_provider
FROM `tech-market-insights.crawler_analytics.crawl_results`
ORDER BY crawl_date DESC
LIMIT 20;

-- Success rate
SELECT 
  COUNT(*) as total_crawls,
  SUM(IF(success, 1, 0)) as successful,
  ROUND(SUM(IF(success, 1, 0)) * 100.0 / COUNT(*), 2) as success_rate_pct
FROM `tech-market-insights.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 24 HOUR);
```

---

## 💰 Cost Estimate

### Development (Free Tier)
- 60 requests/minute
- 1,500 requests/day
- **Cost: $0**

### Production (1,257 companies)
- Full crawl: ~$0.13-0.63
- Weekly refresh: ~$0.50-2.50/week
- **Monthly: ~$2-10**

---

## ⚠️ Common Issues

### "GEMINI_API_KEY not found"
```bash
# Check secret
gcloud secrets describe GEMINI_API_KEY --project=tech-market-insights

# Add if missing (see Step 2)
```

### "Service unavailable"
```bash
# Check deployment
gcloud run services describe crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights
```

### "Timeout error"
- Increase timeout: `--timeout 600`
- Check if page loads slowly
- Reduce maxPages in config

---

## 🎯 Next Steps After Deployment

1. ✅ Deploy crawler service
2. ✅ Run test suite
3. ✅ Test with 5-10 real companies
4. ✅ Check BigQuery logging
5. ✅ Update backend `.env` with `CRAWLER_SERVICE_URL`
6. ✅ Deploy backend (if needed)
7. ✅ Crawl all 1,257 companies (batch by 50)

---

## 📚 Full Documentation

- **Deployment**: `crawler-service/DEPLOYMENT_PLAN.md`
- **API Reference**: `crawler-service/README.md`
- **Getting Started**: `crawler-service/QUICKSTART.md`
- **API Key Guide**: `crawler-service/GET_GEMINI_API_KEY.md`
- **Integration Plan**: `docs/data/ats/ats-integration-plan.md`
- **Monitoring**: `docs/monitoring/crawler-dashboard-queries.sql`
- **Alerting**: `docs/monitoring/crawler-alerting.md`

---

## 🆘 Need Help?

### Check Logs
```bash
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --limit 50
```

### Service Status
```bash
gcloud run services describe crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights
```

### Rollback
```bash
# List revisions
gcloud run revisions list --service crawler-service

# Rollback
gcloud run services update-traffic crawler-service \
  --to-revisions=PREVIOUS_REVISION=100
```

---

## ✅ Deployment Checklist

- [ ] Gemini API key obtained
- [ ] Secret created in Secret Manager
- [ ] Crawler service deployed
- [ ] Health check passing
- [ ] Test suite passing
- [ ] BigQuery logging verified
- [ ] Backend configured with URL
- [ ] Test crawl completed successfully

---

**Ready to deploy?** Run: `./scripts/deployment/deploy-crawler.sh`
