# ADR 008: Self-Hosted AI Crawler for Job Data Extraction

**Date**: 2026-03-10  
**Status**: Accepted  
**Authors**: DevAssembly Team

## Context

87.8% of our 1,257 target companies have **no identified ATS** (Applicant Tracking System). Our current approach of building specific ATS integrations first (Greenhouse, Lever, Ashby) and adding a generic fallback later is premature optimization—we're guessing at the distribution before we have data.

The existing Apify-based LinkedIn scraping covers only ~30% of companies directly, with the rest requiring fallback mechanisms. Building 12+ individual ATS clients would be high maintenance for uncertain coverage.

## Decision

Build a **self-hosted Crawlee + Gemini Flash crawler** as a separate microservice that can crawl any company's career page and extract structured job data. This provides:

- **Immediate broad coverage** across all 1,257 companies
- **Minimal cost** (~$15/month vs ~$80-100/month for Apify)
- **Data-driven optimization**—the crawler naturally reveals which ATS systems companies use, allowing us to build targeted integrations only where justified

### Architecture

```
┌─────────────────────────────────────────┐
│  Crawler Service (Node.js + TypeScript) │
│  - Crawlee (PlaywrightCrawler)          │
│  - Gemini 2.0 Flash for extraction      │
│  - Deployed on Cloud Run                │
└─────────────────────────────────────────┘
              │
              │ HTTP API
              ▼
┌─────────────────────────────────────────┐
│  Kotlin Backend (existing)              │
│  - CrawlerClient (implements AtsClient) │
│  - CrawlerNormalizer (passthrough)      │
│  - Bronze/Silver pipeline               │
└─────────────────────────────────────────┘
```

### Technology Choices

| Component | Choice | Rationale |
|:---|:---|:---|
| Crawler engine | Crawlee (`PlaywrightCrawler`) | Open-source, built by Apify team. Handles JS rendering, anti-bot, pagination. Runs in Docker on Cloud Run. |
| LLM extraction | Gemini 2.0 Flash | $0.075/1M input + $0.30/1M output. ~$0.0001/page. We already use GCP. |
| Hosting | Cloud Run (Docker) | Fits existing infrastructure. Scales to zero. Free tier covers our volume. |
| Language | TypeScript | Better Crawlee support than Python. Mature ecosystem. Type safety. |

## Consequences

### Positive

- **100% coverage** of target companies (vs 30% with Apify-only approach)
- **5-7x cost reduction** vs Apify (~$15/month vs ~$100/month)
- **Data-driven decisions**—we'll know exactly which ATS providers are most common before building integrations
- **Progressive improvement**—company-specific prompt customizations without writing custom scrapers
- **Clean separation**—crawler is a separate microservice, keeping Kotlin codebase clean

### Negative

- **Additional microservice** to maintain (Node.js/TypeScript stack)
- **LLM dependency**—Gemini API outages affect crawling
- **Prompt injection risk**—career pages could contain malicious content
- **Engineering effort**—Phase 1 requires ~10-15 days of development

### Mitigations

| Risk | Mitigation |
|:---|:---|
| Prompt injection | Sanitize HTML (remove scripts, iframes, event handlers). Wrap content in XML tags. Validate output schema strictly. |
| Gemini API outage | Circuit breaker pattern. Fallback to cached data or skip. |
| Rate limiting | Respect robots.txt. Rate limit to 2 requests/second per domain. |
| Maintenance burden | Use TypeScript for type safety. Keep service simple (1 endpoint). Leverage Crawlee community support. |

## Alternatives Considered

### 1. Apify Actors Only

**Pros:**
- Low maintenance (Apify maintains actors)
- Proven reliability

**Cons:**
- 5-7x more expensive (~$80-100/month)
- Only covers ~30% of companies directly
- Less control over extraction logic

**Decision**: Rejected due to cost and limited coverage.

### 2. Build 12+ ATS API Clients

**Pros:**
- Direct API access (more reliable than scraping)
- No LLM costs

**Cons:**
- High maintenance (12+ normalizers to maintain)
- Premature optimization (we don't know ATS distribution)
- Still need fallback for 87.8% of companies without identified ATS

**Decision**: Rejected as premature optimization. Will build selectively based on crawl data.

### 3. Merge.dev Middleware

**Pros:**
- Unified API for 50+ ATS providers
- Vendor-managed

**Cons:**
- Very expensive ($300-1000/month)
- Less control over data quality
- Vendor lock-in

**Decision**: Rejected due to cost and lock-in.

### 4. Python + Crawl4AI

**Pros:**
- Aligns with ML tooling
- Newer, modern library

**Cons:**
- Less mature ecosystem than Crawlee
- Smaller community support

**Decision**: Rejected in favor of Node.js + Crawlee for better support and maturity.

## Implementation Plan

### Phase 1 — MVP Crawler (2-3 weeks)
- Scaffold Node.js/TypeScript service
- Implement core components (detector, extractor, validator)
- Build HTTP API
- Write tests (85% coverage minimum)
- Deploy to Cloud Run
- Integrate with Kotlin backend

### Phase 2 — Scale & Feedback Loop (2-3 weeks)
- Crawl all 1,257 companies
- Implement ATS detection feedback loop
- Add caching layer (skip unchanged pages)
- Build monitoring dashboard

### Phase 3 — Optimise Based on Data (Ongoing)
- Build specific ATS integrations only where data justifies
- Company-specific prompt customizations
- Re-extraction pipeline for cached HTML

## Compliance Notes

- **GDPR**: Do not store personal data. 90-day retention for job data.
- **robots.txt**: Respect crawl-delay and disallow rules.
- **Rate limiting**: 2 requests/second per domain maximum.

## Success Metrics

| Metric | Target |
|:---|:---|
| Coverage | 100% of companies crawled nightly |
| Cost | < $20/month |
| Extraction quality | > 0.7 average confidence |
| Failure rate | < 20% |

## References

- [Crawlee Documentation](https://crawlee.dev/)
- [Gemini API Documentation](https://ai.google.dev/docs)
- [ADR 006: File-Based Cold Storage](./006-file-based-cold-storage.md)
