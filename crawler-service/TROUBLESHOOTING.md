# 🔧 Crawler Service Troubleshooting Guide

**Common Issues and Solutions**

---

## 🚨 Gemini API Errors (Most Common)

### Error: "API key not valid"

**Symptoms:**
```json
{
  "error": "Crawl failed",
  "message": "API key not valid. Please pass a valid API key."
}
```

**Health Check Shows:**
```bash
curl https://crawler-service-xxx.a.run.app/health
# Response:
{
  "status": "degraded",
  "geminiApiKey": "invalid",
  "geminiApiError": "Invalid Gemini API key..."
}
```

**Causes & Solutions:**

| Cause | Solution |
|:---|:---|
| **Billing not enabled** | Go to https://console.cloud.google.com/billing and enable billing |
| **Wrong API key** | Verify API key in Secret Manager: `gcloud secrets versions access GEMINI_API_KEY` |
| **API not enabled** | Enable Gemini API: https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com |
| **Key permissions** | Ensure API key has "Generative Language API" permissions |

**Quick Fix:**
```bash
# 1. Check current API key
gcloud secrets versions access GEMINI_API_KEY --project=tech-market-insights

# 2. Update if needed
echo "AIzaSy...YOUR_NEW_KEY" | gcloud secrets versions add GEMINI_API_KEY \
  --project=tech-market-insights \
  --data-file=-

# 3. Force Cloud Run to use new secret
gcloud run services update-traffic crawler-service \
  --region australia-southeast1 \
  --to-latest
```

---

### Error: "Quota exceeded"

**Symptoms:**
```json
{
  "error": "Gemini API Error: Quota exceeded",
  "message": "Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests"
}
```

**Causes & Solutions:**

| Cause | Solution |
|:---|:---|
| **Free tier limit reached** | Wait ~60 seconds for quota to reset |
| **No billing enabled** | Enable billing to get higher quotas |
| **Rate limiting** | Implement request batching with delays |

**Quick Fix:**
```bash
# Wait 1 minute and retry
sleep 60
curl -X POST https://crawler-service-xxx.a.run.app/crawl ...

# OR enable billing for higher quotas
# https://console.cloud.google.com/billing
```

---

### Error: "Billing not enabled"

**Symptoms:**
```json
{
  "error": "Gemini API Error: Billing not enabled",
  "message": "Please enable billing to continue using this API"
}
```

**Solution:**
1. Go to: https://console.cloud.google.com/billing
2. Link your project to a billing account
3. If no billing account exists, create one (requires credit card)

**Cost Estimate:**
- Development (100 crawls/day): ~$0.60-0.90/month
- Production (1,257 companies weekly): ~$1-2/month

---

## 🕷️ Playwright/Browser Errors

### Error: "Failed to launch browser"

**Symptoms:**
```json
{
  "error": "Crawl failed",
  "message": "Failed to launch browser. Executable doesn't exist..."
}
```

**Causes & Solutions:**

| Cause | Solution |
|:---|:---|
| **Playwright browsers not installed** | Redeploy with updated Dockerfile |
| **Wrong PLAYWRIGHT_BROWSERS_PATH** | Check environment variable in Cloud Run |
| **Missing system dependencies** | Ensure Dockerfile installs required libraries |

**Quick Fix:**
```bash
# Check if browsers are installed
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --limit 10

# Redeploy if needed
cd crawler-service
gcloud run deploy crawler-service --source . --region australia-southeast1
```

---

## 🤖 robots.txt Errors

### Error: "Crawl blocked by robots.txt"

**Symptoms:**
```json
{
  "crawlMeta": {
    "pagesVisited": 0,
    "totalJobsFound": 0
  }
}
```

**Causes & Solutions:**

| Cause | Solution |
|:---|:---|
| **Website blocks crawlers** | Respect robots.txt (by design) |
| **Wrong user-agent** | Check if specific user-agent is blocked |
| **Crawl-delay too aggressive** | Adjust crawl delay in RobotsChecker |

**Note:** This is **expected behavior** - the crawler respects robots.txt rules.

---

## 📊 Low Quality Scores

**Symptoms:**
```json
{
  "crawlMeta": {
    "extractionConfidence": 0.3,
    "qualityTier": "POOR"
  }
}
```

**Causes & Solutions:**

| Cause | Solution |
|:---|:---|
| **Few fields extracted** | Check if career page has actual job listings |
| **Missing required fields** | Verify page structure (title, location, etc.) |
| **ATS detection failed** | Manually specify ATS provider in crawlConfig |
| **Poor page structure** | Try different career page URL |

**Quick Fix:**
```bash
# Try with explicit ATS provider hint
curl -X POST https://crawler-service-xxx.a.run.app/crawl \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "company",
    "url": "https://company.com/careers",
    "crawlConfig": {
      "knownAtsProvider": "greenhouse"
    }
  }'
```

---

## 🔍 Diagnostic Commands

### Check Service Status
```bash
gcloud run services describe crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights
```

### View Recent Logs
```bash
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --limit 50
```

### Follow Logs in Real-time
```bash
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --follow
```

### Check Secret Manager
```bash
# List secret versions
gcloud secrets versions list GEMINI_API_KEY --project=tech-market-insights

# Access latest version
gcloud secrets versions access latest --secret=GEMINI_API_KEY \
  --project=tech-market-insights
```

### Test Health Endpoint
```bash
curl https://crawler-service-xxx.a.run.app/health | jq .
```

### Test Crawl
```bash
curl -X POST https://crawler-service-xxx.a.run.app/crawl \
  -H "Content-Type: application/json" \
  -d '{"companyId": "test", "url": "https://example.com/careers"}' | jq .
```

---

## 📋 Error Message Quick Reference

| Error Message | Cause | Solution |
|:---|:---|:---|
| "API key not valid" | Invalid/expired key, billing not enabled | Check key, enable billing |
| "Quota exceeded" | Rate limit reached | Wait 60s or enable billing |
| "Billing not enabled" | No billing account | Enable billing |
| "Failed to launch browser" | Playwright not installed | Redeploy service |
| "Crawl blocked by robots.txt" | Website blocks crawlers | Respect robots.txt |
| "Gemini API returned empty response" | API error or quota | Check billing/quota |
| "Gemini API blocked request" | Safety filter or policy | Review content/policy |

---

## 🆘 Getting Help

### 1. Check Logs First
```bash
gcloud run services logs read crawler-service \
  --region australia-southeast1 \
  --project tech-market-insights \
  --limit 100
```

### 2. Test Health Endpoint
```bash
curl https://crawler-service-xxx.a.run.app/health | jq .
```

### 3. Verify API Key
```bash
gcloud secrets versions access latest --secret=GEMINI_API_KEY
```

### 4. Check Billing Status
https://console.cloud.google.com/billing

### 5. Review Documentation
- `crawler-service/README.md` - API documentation
- `crawler-service/DEPLOYMENT_GUIDE.md` - Deployment guide
- `crawler-service/DEPLOYMENT_STATUS.md` - Current status

---

## ✅ Health Check Response Codes

| Status | Meaning | Action |
|:---|:---|:---|
| `"status": "ok"` | All systems operational | Ready to crawl |
| `"status": "degraded"` | API key issue | Check `geminiApiError` field |
| HTTP 500 | Service error | Check logs |
| HTTP 503 | Service unavailable | Check Cloud Run status |

---

## 🎯 Proactive Monitoring

### Set Up Alerts
See `docs/monitoring/crawler-alerting.md` for:
- API key failure alerts
- High error rate alerts
- Quality score degradation alerts
- Crawl duration alerts

### Monitor BigQuery
```sql
-- Check recent crawl errors
SELECT 
  crawl_id,
  company_id,
  crawl_date,
  success,
  error_message
FROM `tech-market-insights.crawler_analytics.crawl_results`
WHERE error_message IS NOT NULL
ORDER BY crawl_date DESC
LIMIT 20;

-- Check API key errors specifically
SELECT 
  COUNT(*) as api_errors,
  DATE(crawl_date) as date
FROM `tech-market-insights.crawler_analytics.crawl_results`
WHERE error_message LIKE '%API key%'
   OR error_message LIKE '%billing%'
   OR error_message LIKE '%quota%'
GROUP BY DATE(crawl_date)
ORDER BY date DESC;
```

---

**Last Updated**: March 11, 2026  
**Service URL**: https://crawler-service-181692518949.australia-southeast1.run.app
