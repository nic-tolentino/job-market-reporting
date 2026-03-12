# Model Selection: Gemini 2.0 Flash Lite

**Decision**: Use `gemini-2.0-flash-lite` as the default model for job extraction.

---

## 🎯 Why Flash Lite?

### Cost Savings (at 3,000 crawls/day)

| Model | Daily Cost | Monthly Cost | Annual Cost |
|:---|:---:|:---:|:---:|
| Gemini 2.0 Flash | $0.68 | $20.25 | $246 |
| **Gemini 2.0 Flash Lite** | **$0.33** | **$9.90** | **$120** |
| Qwen 2.5 72B | $0.15 | $4.50 | $55 |

**Savings with Flash Lite: 50% (~$126/year)**

---

## 📊 Model Comparison

### Gemini 2.0 Flash Lite (Selected)

**Pros:**
- ✅ 50% cheaper than standard Flash
- ✅ Same reliability and consistency
- ✅ Faster inference (lower latency)
- ✅ Well-tested on structured extraction
- ✅ Same API, easy to switch
- ✅ Good balance of cost/quality

**Cons:**
- ⚠️ Slightly less nuanced than full Flash (minimal impact for job extraction)

**Best for:** Production workloads at scale

---

### Gemini 2.0 Flash (Previous Default)

**Pros:**
- ✅ Excellent extraction quality
- ✅ Very reliable output format
- ✅ Well-tested

**Cons:**
- ❌ 2x more expensive than Lite
- ❌ Slightly slower inference

**Best for:** Complex extraction tasks, high-stakes scenarios

---

### Qwen 2.5 72B (Alternative)

**Pros:**
- ✅ 78% cheaper than Flash
- ✅ Fast inference
- ✅ Good general capabilities

**Cons:**
- ⚠️ Less tested on AU/NZ job formats
- ⚠️ May need prompt tuning
- ⚠️ Output format may be less consistent
- ⚠️ Primarily trained on English + Chinese data

**Best for:** Cost-optimized workloads after testing

---

## 🔧 Configuration

### Default Model (Code)

All defaults updated to `gemini-2.0-flash-lite`:

```typescript
// src/index.ts
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-2.0-flash-lite';

// src/api/CrawlerService.ts
constructor(gcpProjectId?: string, gcpRegion: string = 'us-central1', geminiModel: string = 'gemini-2.0-flash-lite')

// src/extraction/VertexAIClient.ts
constructor(project: string, location: string = 'us-central1', model: string = 'gemini-2.0-flash-lite')
```

### Override via Environment

```bash
# Use standard Flash for testing
export GEMINI_MODEL=gemini-2.0-flash

# Use Qwen for cost testing
export GEMINI_MODEL=qwen-2.5-72b-instruct

# Use Flash Lite (default)
export GEMINI_MODEL=gemini-2.0-flash-lite
```

### Deployment

```bash
# Deploy with Flash Lite (default)
./scripts/deployment/deploy-crawler-vertex.sh

# Or specify explicitly
gcloud run deploy crawler-service \
  ...
  --set-env-vars="GEMINI_MODEL=gemini-2.0-flash-lite"
```

---

## 🧪 Testing Plan

### Phase 1: Flash Lite Validation (Week 1)

```bash
export GEMINI_MODEL=gemini-2.0-flash-lite
```

**Test with 200 companies:**
- Verify extraction quality ≥ 95% of standard Flash
- Check output format consistency
- Monitor error rates
- Measure latency

**Success criteria:**
- Quality score ≥ 0.8 (same as Flash)
- Error rate < 5%
- Latency < 30s per crawl

---

### Phase 2: Qwen A/B Test (Week 2)

```bash
# Deploy test service with Qwen
export GEMINI_MODEL=qwen-2.5-72b-instruct
```

**Test with 100 companies:**
- Compare output quality vs Flash Lite
- Check format consistency
- Measure cost savings

**Success criteria:**
- Quality score ≥ 0.75 (90% of Flash Lite)
- Output format matches schema
- Cost savings confirmed

---

### Phase 3: Production Decision

**If Qwen passes tests:**
- Deploy Qwen for production
- Keep Flash Lite as fallback
- Monitor quality weekly

**If Qwen fails tests:**
- Stick with Flash Lite
- Re-evaluate in 3 months (model improvements)

---

## 📈 Cost Projections

### At 2,000 Companies/Day

| Model | Daily | Monthly | Annual |
|:---|:---:|:---:|:---:|
| Flash | $0.45 | $13.50 | $164 |
| **Flash Lite** | **$0.22** | **$6.60** | **$80** |
| Qwen | $0.10 | $3.00 | $36 |

### At 3,000 Companies/Day

| Model | Daily | Monthly | Annual |
|:---|:---:|:---:|:---:|
| Flash | $0.68 | $20.25 | $246 |
| **Flash Lite** | **$0.33** | **$9.90** | **$120** |
| Qwen | $0.15 | $4.50 | $55 |

### At 5,000 Companies/Day

| Model | Daily | Monthly | Annual |
|:---|:---:|:---:|:---:|
| Flash | $1.13 | $33.75 | $410 |
| **Flash Lite** | **$0.55** | **$16.50** | **$200** |
| Qwen | $0.25 | $7.50 | $91 |

---

## 🎛️ Model Switching Guide

### When to Upgrade to Standard Flash

- Quality scores consistently < 0.7 with Flash Lite
- Complex career pages (heavy JavaScript, unusual formats)
- High-value targets where extraction quality is critical

### When to Downgrade to Qwen

- Flash Lite quality scores consistently > 0.9
- Cost optimization is top priority
- Willing to invest time in prompt tuning

### When to Try Other Models

- **Claude 3.5 Haiku**: Complex pages, higher budget
- **Gemini 1.5 Flash**: Larger context windows needed
- **Custom fine-tuned model**: Very specific extraction needs

---

## 📊 Monitoring Quality

### Key Metrics

```sql
-- Average quality score by model
SELECT 
  extraction_model,
  COUNT(*) as crawls,
  AVG(quality_score) as avg_quality,
  AVG(crawl_duration_ms) as avg_latency
FROM `crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
GROUP BY extraction_model
ORDER BY avg_quality DESC;

-- Error rate by model
SELECT 
  extraction_model,
  COUNT(*) as total,
  SUM(IF(success, 0, 1)) as errors,
  ROUND(SUM(IF(success, 0, 1)) * 100.0 / COUNT(*), 2) as error_rate_pct
FROM `crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
GROUP BY extraction_model;
```

### Alert Thresholds

| Metric | Warning | Critical |
|:---|:---:|:---:|
| Quality Score | < 0.7 | < 0.5 |
| Error Rate | > 10% | > 20% |
| Latency (p95) | > 45s | > 60s |

---

## ✅ Implementation Checklist

- [x] Update default model in `src/index.ts`
- [x] Update default in `src/api/CrawlerService.ts`
- [x] Update default in `src/extraction/VertexAIClient.ts`
- [x] Update deployment script
- [ ] Deploy with Flash Lite
- [ ] Test with 100 companies
- [ ] Verify quality scores ≥ 0.8
- [ ] Monitor cost savings
- [ ] Document results

---

## 📚 References

- [Gemini 2.0 Flash Lite Documentation](https://cloud.google.com/vertex-ai/docs/generative-ai/model-reference/gemini#gemini-2.0-flash-lite)
- [Vertex AI Pricing](https://cloud.google.com/vertex-ai/pricing)
- [Model Comparison](https://cloud.google.com/vertex-ai/docs/generative-ai/learn/models)

---

**Decision Date**: March 11, 2026  
**Default Model**: `gemini-2.0-flash-lite`  
**Expected Savings**: 50% (~$126/year at 3K crawls/day)  
**Review Date**: April 11, 2026 (after 1 month of production data)
