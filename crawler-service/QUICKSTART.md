# Crawler Service Implementation - Quick Start Guide

## Overview

This guide helps you get the Crawler Service up and running for the first time.

## Prerequisites

1. **Node.js 18+** - Check with `node --version`
2. **Google Cloud Project** - With Vertex AI API enabled
3. **Gemini API Key** - From Google Cloud Console
4. **Docker** (optional) - For containerized deployment

## Step 1: Install Dependencies

```bash
cd crawler-service
npm install
```

## Step 2: Configure Environment

```bash
# Copy example env file
cp .env.example .env

# Edit .env and add your Gemini API key
# Get key from: https://console.cloud.google.com/vertex-ai
```

## Step 3: Run Locally

```bash
# Development mode (auto-reload)
npm run dev

# Or build and run
npm run build
npm start
```

The service will start on `http://localhost:8080`

## Step 4: Test the Service

### Health Check

```bash
curl http://localhost:8080/health
```

Expected response:
```json
{"status":"ok","timestamp":"2026-03-10T12:00:00.000Z"}
```

### Test Crawl

```bash
curl -X POST http://localhost:8080/crawl \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "test-company",
    "url": "https://example.com/careers"
  }'
```

## Step 5: Run Tests

```bash
# Unit tests
npm test

# With coverage
npm run test:coverage

# E2E tests (requires running server)
npm run test:e2e
```

## Step 6: Deploy to Cloud Run

### Build and Push

```bash
# From project root
gcloud builds submit crawler-service --tag gcr.io/PROJECT_ID/crawler-service
```

### Deploy

```bash
gcloud run deploy crawler-service \
  --image gcr.io/PROJECT_ID/crawler-service \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GEMINI_API_KEY=your-key-here \
  --memory 2Gi \
  --cpu 1 \
  --timeout 300 \
  --concurrency 10
```

### Get Service URL

```bash
gcloud run services describe crawler-service \
  --platform managed \
  --region us-central1 \
  --format 'value(status.url)'
```

## Step 7: Configure Backend

Add to `backend/src/main/resources/application.yml`:

```yaml
crawler:
  service:
    url: https://crawler-service-xxxxx.a.run.app  # Your Cloud Run URL
```

## Step 8: Verify Integration

```bash
# From backend directory
./gradlew test --tests "*Crawler*"

# Run a test sync
# (Implement a test endpoint or use existing sync endpoint)
```

## Troubleshooting

### "GEMINI_API_KEY not set"

Ensure your `.env` file has the correct key:
```
GEMINI_API_KEY=AIzaSy...
```

### "Cannot find module '@google/generative-ai'"

Run `npm install` to install dependencies.

### "Playwright browsers not found"

Install Playwright browsers:
```bash
npx playwright install chromium
```

### "Crawler returns no jobs"

1. Check if the career page requires JavaScript
2. Verify the page isn't behind login/CAPTCHA
3. Check extraction confidence in response
4. Review logs for errors

### "Rate limited by target site"

1. Reduce `maxConcurrency` in `CrawlerService.ts`
2. Add delay between requests
3. Check robots.txt compliance

## Monitoring

### View Logs (Cloud Run)

```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=crawler-service" \
  --limit 50 \
  --format "table(timestamp,textPayload)"
```

### Check Metrics

```bash
gcloud monitoring metrics-descriptors list \
  --filter='metric.type="run.googleapis.com/request/count"'
```

## Next Steps

1. **Configure 50 test companies** - Update company manifests with career URLs
2. **Run MVP crawl** - Test against 50 companies first
3. **Review extraction quality** - Check confidence scores
4. **Tune prompts** - Add company-specific extraction hints
5. **Scale to all companies** - Enable nightly full crawl

## Support

- [README.md](./README.md) - Full API documentation
- [ADR 008](../docs/architecture/adr-008-crawler-service.md) - Architecture decision record
- [Integration Plan](../docs/data/ats/ats-integration-plan.md) - Full implementation plan
