# Model Configuration Guide

**Centralized Model Management**

---

## 📍 Single Source of Truth

All model references are now centralized in:
```
src/config/model-config.ts
```

**To change the default model, edit ONE file:**
```typescript
// src/config/model-config.ts - Line 54
export const DEFAULT_MODEL = 'gemini-2.0-flash-lite';  // Change this line only
```

---

## 🎛️ Available Models

### Production (Recommended)
```typescript
'gemini-2.0-flash-lite'  // Default - 50% cost savings
```

### Testing (Higher Quality)
```typescript
'gemini-2.0-flash'  // 2x cost, slightly better quality
```

### Budget (Test First)
```typescript
'qwen-2.5-72b-instruct'  // 78% savings, needs validation
```

### Premium (Complex Tasks)
```typescript
'gemini-1.5-pro'  // Best quality, highest cost
```

---

## 🔧 How to Change the Model

### Option 1: Change Default (Permanent)

**Edit `src/config/model-config.ts`:**
```typescript
export const DEFAULT_MODEL = 'gemini-2.0-flash';  // New default
```

Then redeploy:
```bash
./scripts/deployment/deploy-crawler-vertex.sh
```

### Option 2: Environment Variable (Temporary/Per-Deployment)

```bash
# Use standard Flash for this deployment
export GEMINI_MODEL=gemini-2.0-flash
./scripts/deployment/deploy-crawler-vertex.sh

# Or specify directly
gcloud run deploy crawler-service \
  --set-env-vars="GEMINI_MODEL=gemini-2.0-flash"
```

### Option 3: Runtime Configuration

```bash
# In .env file
GEMINI_MODEL=qwen-2.5-72b-instruct
```

---

## 📊 Model Comparison

| Model | Input/1M | Output/1M | Daily (3K) | Monthly | Quality |
|:---|:---:|:---:|:---:|:---:|:---:|
| **gemini-2.0-flash-lite** | $0.0375 | $0.15 | $0.33 | $9.90 | ⭐⭐⭐⭐ |
| gemini-2.0-flash | $0.075 | $0.30 | $0.68 | $20.25 | ⭐⭐⭐⭐⭐ |
| qwen-2.5-72b-instruct | $0.02 | $0.06 | $0.15 | $4.50 | ⭐⭐⭐ |
| gemini-1.5-pro | $0.125 | $0.50 | $1.13 | $33.75 | ⭐⭐⭐⭐⭐ |

---

## 📈 Cost Calculator

Use the built-in calculator:

```typescript
import { estimateCost } from './config/model-config';

// Calculate cost for a crawl
const cost = estimateCost(
  'gemini-2.0-flash-lite',
  1000,  // input tokens
  500    // output tokens
);
// Returns: ~$0.00011
```

---

## 🔍 View Available Models

```typescript
import { getAvailableModelIds, getModelConfig } from './config/model-config';

// List all models
const models = getAvailableModelIds();
// ['gemini-2.0-flash-lite', 'gemini-2.0-flash', ...]

// Get model details
const config = getModelConfig('gemini-2.0-flash-lite');
// { displayName: 'Gemini 2.0 Flash Lite', inputCostPerMillion: 0.0375, ... }
```

---

## 🎯 Recommended Models by Use Case

```typescript
import { getRecommendedModel } from './config/model-config';

// Production deployment
const prodModel = getRecommendedModel('production');
// Returns: 'gemini-2.0-flash-lite'

// Testing/development
const testModel = getRecommendedModel('testing');
// Returns: 'gemini-2.0-flash'

// Budget-optimized
const budgetModel = getRecommendedModel('budget');
// Returns: 'qwen-2.5-72b-instruct'

// Premium/high-stakes
const premiumModel = getRecommendedModel('premium');
// Returns: 'gemini-1.5-pro'
```

---

## 📝 Files Using Centralized Config

All these files now import from `model-config.ts`:

| File | Usage |
|:---|:---|
| `src/index.ts` | Default model for service initialization |
| `src/api/CrawlerService.ts` | Constructor default parameter |
| `src/extraction/VertexAIClient.ts` | Constructor default parameter |
| `src/extraction/GeminiExtractionService.ts` | Constructor default parameter |
| `scripts/deployment/deploy-crawler-vertex.sh` | Default env variable |

**Zero hardcoded model strings** in the codebase (except the central config).

---

## ✅ Changing Models - Checklist

### For Production Deployment

- [ ] Update `DEFAULT_MODEL` in `src/config/model-config.ts`
- [ ] Test locally: `npm run dev`
- [ ] Verify health endpoint shows correct model
- [ ] Deploy: `./scripts/deployment/deploy-crawler-vertex.sh`
- [ ] Monitor quality scores in BigQuery
- [ ] Update `MODEL_SELECTION.md` with decision date

### For Testing/Trials

- [ ] Set environment variable: `export GEMINI_MODEL=<model>`
- [ ] Deploy with env var
- [ ] Run A/B test with 50-100 companies
- [ ] Compare quality scores
- [ ] Document results in `MODEL_SELECTION.md`

---

## 🔍 Monitoring Model Performance

### BigQuery Queries

```sql
-- Quality by model
SELECT 
  extraction_model,
  COUNT(*) as crawls,
  AVG(quality_score) as avg_quality,
  AVG(extraction_confidence) as avg_confidence
FROM `crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
GROUP BY extraction_model
ORDER BY avg_quality DESC;

-- Cost estimation
SELECT 
  extraction_model,
  SUM(token_usage_input) as total_input_tokens,
  SUM(token_usage_output) as total_output_tokens,
  COUNT(*) as total_crawls
FROM `crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
GROUP BY extraction_model;
```

---

## 📚 Related Documentation

- `MODEL_SELECTION.md` - Model selection rationale and cost analysis
- `VERTEX_AI_MIGRATION.md` - Migration guide from AI Studio
- `src/config/model-config.ts` - Centralized configuration file

---

**Last Updated**: March 11, 2026  
**Current Default**: `gemini-2.0-flash-lite`  
**Next Review**: April 11, 2026
