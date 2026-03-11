# Production Deployment Plan

**Date**: March 11, 2026  
**Service**: Self-Hosted AI Crawler  
**Status**: Ready for Deployment (pending Gemini API key)

---

## 📋 Pre-Deployment Checklist

### 1. Environment Setup
- [ ] Obtain Gemini API key from https://aistudio.google.com/app/apikey
- [ ] Add to `.env` file: `GEMINI_API_KEY=AIzaSy...`
- [ ] Add to Secret Manager (see below)
- [ ] Verify gcloud authentication: `gcloud auth login`
- [ ] Verify project access: `gcloud config set project tech-market-insights`

### 2. Secret Manager Setup
```bash
# Create secret (if not exists)
gcloud secrets create GEMINI_API_KEY \
    --project=tech-market-insights \
    --replication-policy="automatic"

# Add version
echo "YOUR_API_KEY_HERE" | gcloud secrets versions add GEMINI_API_KEY \
    --project=tech-market-insights \
    --data-file=-
```

### 3. Code Readiness
- [x] TypeScript compiles successfully
- [x] Kotlin compiles successfully
- [x] All tests passing (95/95 unit tests)
- [x] Merge conflicts resolved
- [x] Documentation updated

---

## 🚀 Deployment Steps

### Step 1: Deploy Crawler Service

```bash
cd /Users/nic/Projects/job-market-gemini
./scripts/deployment/deploy-crawler.sh
```

**Expected Output:**
```
🚀 Deploying Crawler Service to Cloud Run
==========================================
Project: tech-market-insights
Region: australia-southeast1
Service: crawler-service

✅ Deployment complete!

📡 Service URL: https://crawler-service-xxx.a.run.app
```

**Note**: Save the service URL for Step 2.

---

### Step 2: Update Backend Configuration

Add crawler service URL to `.env`:

```bash
# Crawler Service Configuration
CRAWLER_SERVICE_URL=https://crawler-service-xxx.a.run.app
```

---

### Step 3: Deploy Backend (if needed)

```bash
cd /Users/nic/Projects/job-market-gemini
./scripts/deployment/deploy.sh
```

---

### Step 4: Verify Deployment

#### 4.1 Health Check
```bash
curl https://crawler-service-xxx.a.run.app/health
```

**Expected Response:**
```json
{
  "status": "ok",
  "timestamp": "2026-03-11T12:00:00.000Z",
  "version": "1.0.0"
}
```

#### 4.2 Test Single Crawl
```bash
curl -X POST https://crawler-service-xxx.a.run.app/crawl \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "test-company",
    "url": "https://example.com/careers"
  }'
```

**Expected Response:**
```json
{
  "companyId": "test-company",
  "crawlMeta": {
    "pagesVisited": 1,
    "totalJobsFound": 0,
    "detectedAtsProvider": null,
    "crawlDurationMs": 5000
  },
  "jobs": []
}
```

#### 4.3 Check Logs
```bash
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --limit 20
```

---

## 🧪 Testing Plan

### Test 1: Company with No ATS
**Company**: A small company without identifiable ATS  
**Expected**: Crawler extracts jobs from HTML  
**Success Criteria**: Jobs extracted with confidence > 0.5

### Test 2: Company with Greenhouse
**Company**: Airwallex (uses Greenhouse)  
**Expected**: ATS detected, jobs extracted  
**Success Criteria**: `detectedAtsProvider: "GREENHOUSE"`

### Test 3: Company with Lever
**Company**: Canva (uses Lever)  
**Expected**: ATS detected, jobs extracted  
**Success Criteria**: `detectedAtsProvider: "LEVER"`

### Test 4: Batch Crawl
**Companies**: 5-10 companies  
**Expected**: All crawled successfully  
**Success Criteria**: > 90% success rate

### Test 5: Quality Scoring
**Expected**: Quality scores assigned to extractions  
**Success Criteria**: Scores range 0.0-1.0, tier assigned

---

## 📊 Success Metrics

| Metric | Target | Measurement |
|:---|:---|:---|
| **Crawl Success Rate** | > 90% | `success_count / total_count` |
| **ATS Detection Rate** | > 50% | Companies with detected ATS |
| **Avg Quality Score** | > 0.6 | Mean quality score |
| **Avg Crawl Duration** | < 30s | Mean crawl time |
| **Error Rate** | < 5% | Failed crawls / total |

---

## 🔍 Monitoring

### BigQuery Queries

After crawling, check results:

```sql
-- Recent crawls
SELECT 
  company_id,
  crawl_date,
  pages_visited,
  jobs_extracted,
  quality_score,
  quality_tier
FROM `tech-market-insights.crawler_analytics.crawl_results`
ORDER BY crawl_date DESC
LIMIT 20;

-- Success rate by company
SELECT 
  company_id,
  COUNT(*) as total_crawls,
  SUM(IF(success, 1, 0)) as successful_crawls,
  ROUND(SUM(IF(success, 1, 0)) * 100.0 / COUNT(*), 2) as success_rate
FROM `tech-market-insights.crawler_analytics.crawl_results`
GROUP BY company_id
ORDER BY success_rate ASC
LIMIT 10;

-- Quality score distribution
SELECT 
  quality_tier,
  COUNT(*) as count,
  ROUND(AVG(quality_score), 2) as avg_score
FROM `tech-market-insights.crawler_analytics.crawl_results`
WHERE quality_score IS NOT NULL
GROUP BY quality_tier
ORDER BY avg_score DESC;
```

### Cloud Run Dashboard

1. Go to: https://console.cloud.google.com/run
2. Select `crawler-service`
3. View metrics:
   - Request count
   - Response latency
   - Error rate
   - Instance count

---

## ⚠️ Troubleshooting

### Issue: "GEMINI_API_KEY not found"
**Solution**: 
```bash
# Check secret exists
gcloud secrets describe GEMINI_API_KEY --project=tech-market-insights

# If missing, create it (see Pre-Deployment Checklist)
```

### Issue: "Service unavailable"
**Solution**:
```bash
# Check service status
gcloud run services describe crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights

# Check logs
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights
```

### Issue: "Timeout error"
**Solution**:
- Increase timeout in Cloud Run: `--timeout 600`
- Check if page is slow to load
- Reduce `maxPages` in crawl config

### Issue: "Low quality scores"
**Solution**:
- Check HTML extraction (is content being sanitized too aggressively?)
- Review Gemini prompt (may need tuning)
- Verify career page has actual job listings

---

## 🎯 Post-Deployment Actions

### Immediate (Day 1)
- [ ] Deploy crawler service
- [ ] Run health check
- [ ] Test with 5-10 companies
- [ ] Review logs for errors
- [ ] Check BigQuery logging

### Short-term (Week 1)
- [ ] Crawl all 1,257 companies (batch by 50)
- [ ] Establish baseline metrics
- [ ] Identify companies with low quality scores
- [ ] Tune extraction prompt if needed

### Medium-term (Month 1)
- [ ] Set up automated weekly crawls
- [ ] Configure alerting (see `docs/monitoring/crawler-alerting.md`)
- [ ] Create Looker Studio dashboard
- [ ] Plan Phase 3 (Cloud Tasks integration)

---

## 💰 Cost Tracking

### Daily Cost Estimate
```
Crawls per day: 50
Cost per crawl: ~$0.0003
Daily cost: ~$0.015
Monthly cost: ~$0.45
```

### Monitor Costs
```bash
# View current month billing
gcloud billing accounts list

# View project costs
gcloud billing budgets list --billing-account=YOUR_BILLING_ACCOUNT
```

---

## 🔐 Security Checklist

- [x] API key stored in Secret Manager (not in code)
- [x] Cloud Run service has minimal IAM permissions
- [x] No public access to internal endpoints
- [x] robots.txt compliance implemented
- [x] Rate limiting enabled (2 req/sec per domain)

---

## 📝 Rollback Plan

If deployment fails:

```bash
# List revisions
gcloud run revisions list --service crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights

# Rollback to previous revision
gcloud run services update-traffic crawler-service \
  --to-revisions=PREVIOUS_REVISION=100 \
  --region australia-southeast1 \
  --project tech-market-insights
```

---

## ✅ Deployment Sign-Off

- [ ] Pre-deployment checklist complete
- [ ] Gemini API key obtained and configured
- [ ] Crawler service deployed
- [ ] Health check passing
- [ ] Test crawls successful
- [ ] BigQuery logging verified
- [ ] Monitoring dashboard created
- [ ] Alerting configured

**Deployment Date**: _______________  
**Deployed By**: _______________  
**Service URL**: _______________  
**Notes**: _______________

---

## 📚 Related Documentation

- `crawler-service/README.md` - API documentation
- `crawler-service/QUICKSTART.md` - Getting started
- `docs/data/ats/ats-integration-plan.md` - Full implementation plan
- `docs/architecture/adr-008-crawler-service.md` - Architecture decision
- `docs/monitoring/crawler-dashboard-queries.sql` - Monitoring queries
- `docs/monitoring/crawler-alerting.md` - Alerting configuration
- `crawler-service/GET_GEMINI_API_KEY.md` - How to get API key
