# Staging Environment Setup

## Overview

This document describes how to set up a staging environment for development and testing before deploying to production.

**Approach**: Use the same GCP project (`tech-market-insights`) with separate, suffixed resources for staging. This avoids the overhead of managing a second GCP project and billing account while still providing clear isolation between environments.

| Component | Production | Staging |
|-----------|-----------|---------|
| GCP Project | `tech-market-insights` | `tech-market-insights` (same) |
| Backend (Cloud Run) | `tech-market-backend` | `tech-market-backend-staging` |
| Crawler (Cloud Run) | `tech-market-crawler` | `tech-market-crawler-staging` |
| BigQuery dataset | `techmarket` | `techmarket_staging` |
| GCS bucket | `techmarket-bronze-ingestions` | `techmarket-bronze-ingestions-staging` |
| Cloud Tasks queue | `techmarket-sync-queue` | `techmarket-sync-queue-staging` |
| Frontend (Vercel) | `main` branch | `staging` branch |

---

## 1. BigQuery — Staging Dataset

Create a dedicated BigQuery dataset for staging so you can test data pipeline changes without touching production data.

```bash
bq mk \
  --dataset \
  --location=australia-southeast1 \
  --description="Staging dataset for development and testing" \
  tech-market-insights:techmarket_staging
```

Then create the required tables. The easiest approach is to copy the production schema and optionally seed with a sample of production data:

```bash
# Copy schema (empty table) from production
bq cp --no_clobber \
  tech-market-insights:techmarket.raw_jobs \
  tech-market-insights:techmarket_staging.raw_jobs

bq cp --no_clobber \
  tech-market-insights:techmarket.raw_companies \
  tech-market-insights:techmarket_staging.raw_companies

bq cp --no_clobber \
  tech-market-insights:techmarket.raw_ingestions \
  tech-market-insights:techmarket_staging.raw_ingestions
```

To seed with a sample of production data (optional but useful for realistic testing):

```bash
bq query --use_legacy_sql=false \
  'INSERT INTO `tech-market-insights.techmarket_staging.raw_jobs`
   SELECT * FROM `tech-market-insights.techmarket.raw_jobs`
   WHERE DATE(ingestedAt) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
   LIMIT 5000'
```

---

## 2. Cloud Storage — Staging Bucket

```bash
gcloud storage buckets create gs://techmarket-bronze-ingestions-staging \
  --project=tech-market-insights \
  --location=australia-southeast1 \
  --uniform-bucket-level-access
```

---

## 3. Cloud Tasks — Staging Queue

```bash
gcloud tasks queues create techmarket-sync-queue-staging \
  --project=tech-market-insights \
  --location=australia-southeast1 \
  --max-attempts=3 \
  --min-backoff=10s \
  --max-backoff=300s \
  --max-doublings=5
```

> You can also add the staging queue to `terraform/gcp/cloud_tasks.tf` so it is managed by Terraform alongside the production queue.

---

## 4. Backend — Staging Cloud Run Service

### Environment Variables

Create a `.env.staging` file at the project root (do not commit this — add to `.gitignore`):

```bash
# .env.staging
GCP_PROJECT_ID=tech-market-insights
GCP_REGION=australia-southeast1
SERVICE_ACCOUNT_EMAIL=<your-service-account>@tech-market-insights.iam.gserviceaccount.com

# Staging-specific overrides
BIGQUERY_DATASET=techmarket_staging
GCS_BUCKET_NAME=techmarket-bronze-ingestions-staging
CLOUD_TASKS_QUEUE=techmarket-sync-queue-staging

# Apify — you can reuse prod credentials or create a separate account/token
APIFY_API_TOKEN=<apify-token>
APIFY_ACTOR_ID=<apify-actor-id>

# Backend URL (set after first deploy)
BACKEND_URL=https://tech-market-backend-staging-<hash>-ts.a.run.app
```

### Deployment Script

Create `scripts/deployment/deploy-staging.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

source .env.staging

IMAGE="australia-southeast1-docker.pkg.dev/tech-market-insights/tech-market-repo/tech-market-backend-staging:latest"

echo "Building and deploying staging backend..."

gcloud builds submit \
  --project=tech-market-insights \
  --tag="$IMAGE" \
  ./backend

gcloud run deploy tech-market-backend-staging \
  --project=tech-market-insights \
  --region=australia-southeast1 \
  --image="$IMAGE" \
  --platform=managed \
  --allow-unauthenticated \
  --set-env-vars="SPRING_PROFILES_ACTIVE=staging" \
  --set-env-vars="GCP_PROJECT_ID=$GCP_PROJECT_ID" \
  --set-env-vars="BIGQUERY_DATASET=$BIGQUERY_DATASET" \
  --set-env-vars="GCS_BUCKET_NAME=$GCS_BUCKET_NAME" \
  --set-env-vars="CLOUD_TASKS_QUEUE=$CLOUD_TASKS_QUEUE" \
  --set-env-vars="BACKEND_URL=$BACKEND_URL" \
  --set-env-vars="APIFY_API_TOKEN=$APIFY_API_TOKEN" \
  --set-env-vars="APIFY_ACTOR_ID=$APIFY_ACTOR_ID" \
  --min-instances=0 \
  --max-instances=2

echo "Staging backend deployed."
gcloud run services describe tech-market-backend-staging \
  --project=tech-market-insights \
  --region=australia-southeast1 \
  --format="value(status.url)"
```

Make it executable: `chmod +x scripts/deployment/deploy-staging.sh`

### Spring Boot Staging Profile

Add a staging profile override to the backend. Create `backend/src/main/resources/application-staging.yml`:

```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=100,expireAfterWrite=5m   # Short cache for testing

logging:
  level:
    com.yourpackage: DEBUG

# BigQuery / GCS values are injected via env vars set in Cloud Run
```

The `SPRING_PROFILES_ACTIVE=staging` env var above activates this profile automatically.

---

## 5. Crawler — Staging Cloud Run Service

The crawler already has a deploy script. Add a staging variant:

Create `scripts/deployment/deploy-crawler-staging.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

source .env.staging

IMAGE="australia-southeast1-docker.pkg.dev/tech-market-insights/tech-market-repo/tech-market-crawler-staging:latest"

gcloud builds submit \
  --project=tech-market-insights \
  --tag="$IMAGE" \
  ./crawler-service

gcloud run deploy tech-market-crawler-staging \
  --project=tech-market-insights \
  --region=australia-southeast1 \
  --image="$IMAGE" \
  --platform=managed \
  --no-allow-unauthenticated \
  --set-env-vars="BACKEND_URL=$BACKEND_URL" \
  --set-env-vars="GCP_PROJECT_ID=$GCP_PROJECT_ID" \
  --min-instances=0 \
  --max-instances=1 \
  --memory=2Gi \
  --cpu=2

echo "Staging crawler deployed."
```

---

## 6. Frontend — Staging on Vercel

Vercel's branch-based deployments make this straightforward.

### One-time setup

1. In your Vercel project dashboard, go to **Settings → Git**
2. Add `staging` as a **Production Branch alias** (or just use Vercel's automatic preview deployments for any non-main branch)
3. Create a `staging` git branch:
   ```bash
   git checkout -b staging
   git push -u origin staging
   ```

### Environment Variables

In the Vercel dashboard under **Settings → Environment Variables**, scope the following to the `staging` branch/environment:

| Variable | Value |
|----------|-------|
| `VITE_API_URL` | `https://tech-market-backend-staging-<hash>-ts.a.run.app/api` |
| `VITE_FORCE_MOCK_DATA` | `false` |

### Workflow

- Push to `staging` branch → Vercel auto-deploys frontend to staging URL
- Push to `main` branch → Vercel deploys to production

You can also use `vercel --prod=false` from the CLI to manually deploy a preview.

---

## 7. Day-to-Day Development Workflow

```
feature branch
      │
      ▼
   staging branch  ──► Vercel staging URL + Cloud Run staging backend
      │
      │  (test & verify)
      ▼
    main branch  ──► Vercel prod + Cloud Run prod backend
```

1. Develop on a feature branch
2. Merge into `staging` (or deploy directly from your branch)
3. Run smoke tests against staging
4. Merge `staging` → `main` to promote to production

### Quick deploy to staging (backend + crawler)

```bash
# From project root
./scripts/deployment/deploy-staging.sh
./scripts/deployment/deploy-crawler-staging.sh
```

### Resetting staging data

```bash
# Drop and recreate staging silver tables (mirrors db-drop-silver.sh for staging)
bq rm -f tech-market-insights:techmarket_staging.raw_jobs
bq rm -f tech-market-insights:techmarket_staging.raw_companies

# Re-copy schema from production
bq cp tech-market-insights:techmarket.raw_jobs tech-market-insights:techmarket_staging.raw_jobs
bq cp tech-market-insights:techmarket.raw_companies tech-market-insights:techmarket_staging.raw_companies
```

---

## 8. Cost Considerations

All staging resources are configured for minimal cost:

- **Cloud Run**: `--min-instances=0` means services scale to zero when idle. You only pay for actual invocations.
- **BigQuery**: Queries on a small staging dataset will stay well within the 1 TB/month free tier.
- **Cloud Storage**: A staging bucket with light usage costs fractions of a cent.
- **Cloud Tasks**: Free tier covers 1M tasks/month.

Estimated additional cost: **~$0–$2/month** depending on how actively you use the staging environment.

---

## 9. Optional: Automate with GitHub Actions

Add a workflow that auto-deploys the backend to staging on pushes to the `staging` branch:

Create `.github/workflows/deploy-staging.yml`:

```yaml
name: Deploy to Staging

on:
  push:
    branches: [staging]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.GCP_SA_KEY }}

      - uses: google-github-actions/setup-gcloud@v2

      - name: Build and deploy staging backend
        run: |
          IMAGE="australia-southeast1-docker.pkg.dev/tech-market-insights/tech-market-repo/tech-market-backend-staging:${{ github.sha }}"
          gcloud builds submit --tag="$IMAGE" ./backend
          gcloud run deploy tech-market-backend-staging \
            --project=tech-market-insights \
            --region=australia-southeast1 \
            --image="$IMAGE" \
            --platform=managed \
            --allow-unauthenticated \
            --set-env-vars="SPRING_PROFILES_ACTIVE=staging,BIGQUERY_DATASET=techmarket_staging,GCS_BUCKET_NAME=techmarket-bronze-ingestions-staging,CLOUD_TASKS_QUEUE=techmarket-sync-queue-staging" \
            --set-env-vars="GCP_PROJECT_ID=tech-market-insights" \
            --set-secrets="APIFY_API_TOKEN=apify-api-token:latest"
```

> Store the GCP service account key as a GitHub Actions secret named `GCP_SA_KEY`. For secrets like the Apify token, consider using [GCP Secret Manager](https://cloud.google.com/secret-manager) and `--set-secrets` instead of plain env vars.

---

## 10. Initial Setup Checklist

- [ ] Run BigQuery dataset and table creation commands (Section 1)
- [ ] Create staging GCS bucket (Section 2)
- [ ] Create staging Cloud Tasks queue (Section 3)
- [ ] Create `application-staging.yml` in the backend (Section 4)
- [ ] Create `.env.staging` (do not commit — add to `.gitignore`)
- [ ] Create `deploy-staging.sh` and `deploy-crawler-staging.sh` scripts
- [ ] Run `./scripts/deployment/deploy-staging.sh` to get the staging backend URL
- [ ] Update `BACKEND_URL` in `.env.staging` with the URL from the previous step
- [ ] Set Vercel environment variables scoped to the `staging` environment
- [ ] Create `staging` git branch and push to Vercel
- [ ] (Optional) Add GitHub Actions workflow for automated staging deploys
