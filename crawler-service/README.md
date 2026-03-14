# Crawler Service

Self-hosted Crawlee + Gemini Flash crawler for extracting job data from company career pages.

## Overview

This service crawls company career pages, extracts job listings using AI, and returns structured data in a standardized format. It's designed to run on Google Cloud Run and integrates with the Kotlin backend via HTTP API.

## Architecture

```
┌─────────────────────────────────────────┐
│  Crawler Service                        │
│  ┌─────────────────────────────────┐    │
│  │  POST /crawl                    │    │
│  │  1. PlaywrightCrawler fetches   │    │
│  │  2. ContentExtractor cleans     │    │
│  │  3. AtsDetector identifies ATS  │    │
│  │  4. Gemini Flash extracts jobs  │    │
│  │  5. Validator checks quality    │    │
│  │  6. Returns NormalizedJob[]     │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

## Quick Start

### Local Development

```bash
# Install dependencies
npm install

# Set environment variables
export GEMINI_API_KEY=your-api-key

# Run in development mode
npm run dev

# Or build and run
npm run build
npm start
```

### Testing

```bash
# Run unit tests
npm test

# Run with coverage
npm run test:coverage

# Run E2E tests (requires running server)
npm run test:e2e
```

### Docker

```bash
# Build image
docker build -t crawler-service:latest .

# Run container
docker run -p 8081:8081 -e GEMINI_API_KEY=your-key crawler-service:latest
```

### Deploy to Cloud Run

```bash
# Build and push
gcloud builds submit --tag gcr.io/PROJECT_ID/crawler-service

# Deploy
gcloud run deploy crawler-service \
  --image gcr.io/PROJECT_ID/crawler-service \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GEMINI_API_KEY=your-key
```

## API

### Health Check

```
GET /health
```

Response:
```json
{
  "status": "ok",
  "timestamp": "2026-03-10T12:00:00.000Z"
}
```

### Crawl Single Company

```
POST /crawl
Content-Type: application/json

{
  "companyId": "airwallex",
  "url": "https://www.airwallex.com/careers",
  "seedData": {
    "url": "https://careers.airwallex.com/jobs",
    "category": "tech-filtered",
    "lastKnownJobCount": 42,
    "lastKnownPageCount": 2
  },
  "crawlConfig": {
    "maxPages": 15,
    "followJobLinks": true,
    "extractionPrompt": null,
    "timeout": 60000
  }
}
```

Response:
```json
{
  "companyId": "airwallex",
  "crawlMeta": {
    "status": "ACTIVE",
    "pagesVisited": 3,
    "totalJobsFound": 47,
    "detectedAtsProvider": "ASHBY",
    "detectedAtsIdentifier": "airwallex",
    "crawlDurationMs": 8200,
    "extractionModel": "gemini-2.5-flash-lite",
    "extractionConfidence": 0.92,
    "lastCrawledAt": "2026-03-14T12:00:00.000Z",
    "pagination_pattern": "query:p",
    "errorMessage": null,
    "paginationSignal": null,
    "jobYieldSignal": {
      "type": "GROWTH",
      "previousJobs": 42,
      "newJobs": 47,
      "delta": 5
    }
  },
  "jobs": [
    {
      "platformId": "crawl-airwallex-senior-engineer-2026-03-10",
      "source": "Crawler",
      "title": "Senior Software Engineer",
      "companyName": "Airwallex",
      "location": "Melbourne, AU",
      "applyUrl": "https://jobs.ashbyhq.com/airwallex/abc123"
    }
  ]
}
```

## Crawl Modes

The service automatically switches behavior based on the presence of `seedData.url`:

| Mode | Trigger | maxPages (default) | Behavior |
|:---|:---|:---|:---|
| **Discovery** | No `seedData.url` | 10 | Crawls from `url`, prioritizes career/job links via glob matching. |
| **Targeted** | `seedData.url` present | 60 | Jumps directly to `seedData.url`, stays on path, depth for pagination. |

## Manifest Structure

Company seeds are defined in `data/companies/**/*.json` manifests.

```json
{
  "crawler": {
    "seeds": [
      {
        "url": "https://careers.canva.com/engineering/",
        "category": "tech-filtered"
      }
    ]
  }
}
```

- `url`: The deepest known career board URL.
- `category`: 
  - `general`: Broad board, applies tech-filtering to drop non-tech roles.
  - `tech-filtered`: Targeted board, applies insurance-only filtering.

### Batch Crawl

```
POST /crawl/batch
Content-Type: application/json

{
  "requests": [
    {"companyId": "airwallex", "url": "https://..."},
    {"companyId": "canva", "url": "https://..."}
  ]
}
```

## Configuration

| Environment Variable | Default | Description |
|:---|:---|:---|
| `PORT` | `8081` | Port to listen on |
| `GEMINI_API_KEY` | - | Google Gemini API key (required) |
| `NODE_ENV` | `production` | Environment mode |

## Project Structure

```
crawler-service/
├── src/
│   ├── api/
│   │   ├── types.ts           # API type definitions
│   │   ├── server.ts          # Express server setup
│   │   └── CrawlerService.ts  # Main crawler orchestration
│   ├── detector/
│   │   └── AtsDetector.ts     # ATS provider detection
│   ├── extractor/
│   │   └── ContentExtractor.ts # HTML cleaning & sanitization
│   ├── extraction/
│   │   └── GeminiExtractionService.ts  # LLM extraction
│   ├── validator/
│   │   └── JobValidator.ts    # Job validation rules
│   └── index.ts               # Entry point
├── tests/
│   ├── AtsDetector.test.ts
│   ├── ContentExtractor.test.ts
│   ├── JobValidator.test.ts
│   └── crawler.e2e.test.ts
├── package.json
├── tsconfig.json
├── jest.config.js
└── Dockerfile
```

## Security

### Prompt Injection Defense

The service implements multiple layers of defense against prompt injection:

1. **HTML Sanitization** - Removes `<script>`, `<iframe>`, event handlers, `javascript:` URLs
2. **XML Wrapping** - Content wrapped in `<page-content>` tags for clear delimitation
3. **Output Validation** - Strict schema validation of LLM output
4. **Logging** - Suspicious patterns logged for review

### Robots.txt Compliance

The service respects `robots.txt` rules:
- Checks `robots.txt` before crawling
- Respects `Crawl-delay` directives
- Default rate limit: 2 requests/second per domain

## Monitoring

### Metrics to Track

- Crawl success rate (% companies completed)
- Average extraction confidence
- Jobs extracted per company
- LLM token usage + cost
- Cloud Run instance count + latency
- Error breakdown by type

### Alert Thresholds

- Crawl failure rate > 20%
- Average confidence < 0.7
- Gemini API error rate > 5%
- Cloud Run latency p95 > 30s

## Troubleshooting

### Crawler returns no jobs

1. Check if the career page requires JavaScript (Playwright should handle this)
2. Verify the page isn't behind a login/CAPTCHA
3. Check extraction confidence in `crawlMeta`
4. Review raw HTML in logs for debugging

### High error rate

1. Check Cloud Run logs for error patterns
2. Verify Gemini API quota and status
3. Check if target sites are rate-limiting
4. Review robots.txt compliance

### Slow crawl times

1. Increase `maxConcurrency` (default: 10)
2. Reduce `maxPages` per crawl (default: 5)
3. Check network latency to target sites
4. Consider caching unchanged pages

## License

Proprietary - DevAssembly
