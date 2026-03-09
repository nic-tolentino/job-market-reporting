# Comprehensive GCP Deployment Guide

This guide takes you step-by-step from having **no Google Cloud account** to having your `Tech Market Backend` completely hosted on Google Cloud Run with a serverless BigQuery database.

---

## ⚡ Quick Start: Utility Scripts
We have provided utility scripts in the `scripts/deployment/` directory to automate common tasks.

### 1. Setup Environment
First, create your `.env` file from the template:
```bash
./scripts/deployment/setup-env.sh
```
Then edit the `.env` file in the root directory with your actual GCP and Apify credentials.

### 2. Common Deployment Commands
| Task | Script |
|------|--------|
| **Deploy to Production** | `./scripts/deployment/deploy.sh` |
| **Setup Cloud Tasks** | `./scripts/deployment/setup-cloud-tasks.sh` |
| **Drop Silver Tables** | `./scripts/deployment/db-drop-silver.sh` |
| **Reprocess Data** | `./scripts/deployment/db-reprocess.sh` |
| **Ingest Specific Dataset** | `./scripts/deployment/ingest-dataset.sh [dataset_id]` |

---

## Stage 1: Google Cloud Setup

### 1. Create a Google Cloud Account & Project
1. Go to [console.cloud.google.com](https://console.cloud.google.com/) and sign in with your Google Account.
2. If this is your first time, you will be prompted to sign up for a Free Trial and add a billing method (GCP requires a credit card to prevent spam, but what we are building fits within the Free Tier).
3. At the top left of the console, click the **Project Dropdown** and select **New Project**.
4. Name it something like `tech-market-insights` and click **Create**.
5. Once created, make sure your new project is actively selected in the top dropdown. 
6. Take note of your **Project ID** (e.g., `tech-market-insights-12345`).

### 2. Install the Google Cloud CLI (`gcloud`)
1. Download and install the Google Cloud CLI for macOS by following [the official guide](https://cloud.google.com/sdk/docs/install). 
   - *If you have Homebrew, you can simply run:* `brew install --cask google-cloud-sdk`
2. Open your terminal and initialize the CLI:
   ```bash
   gcloud init
   ```
3. Follow the prompts in the terminal to log in via your browser, and select the project you just created.

---

## Stage 2: Enable Services and Prepare the Database

### 1. Enable Required APIs
Before you can use Cloud Run or BigQuery, you need to turn their APIs "on" for your project. Run this in your terminal:
```bash
gcloud services enable \
    run.googleapis.com \
    artifactregistry.googleapis.com \
    cloudbuild.googleapis.com \
    bigquery.googleapis.com
```

### 2. Create the BigQuery Dataset
The backend expects a dataset named `techmarket` to exist to store your tables.
```bash
bq mk --location=australia-southeast1 techmarket
```
*(The tables themselves will be automatically managed/created by your application logic if they don't exist, but the dataset folder must be created manually).*

### 3. Create an Artifact Registry Repository
This acts as a folder to hold your compiled Docker container images.
```bash
gcloud artifacts repositories create tech-market-repo \
    --repository-format=docker \
    --location=australia-southeast1 \
    --description="Docker repository for Backend"
```

---

### Method A: Standard Build (Via Cloud Build)
This is the easiest method but takes around **4-5 minutes** since Cloud Build downloads all Gradle dependencies from scratch.

```bash
gcloud run deploy tech-market-backend \
    --source . \
    --region australia-southeast1 \
    --allow-unauthenticated \
    --min-instances 0 \
    --max-instances 2 \
    --cpu 1 \
    --memory 512Mi \
    --port 8080 \
    --set-env-vars="\
APIFY_TOKEN=your_real_apify_token,\
APIFY_DATASET_ID=your_real_apify_dataset_id,\
APIFY_WEBHOOK_SECRET=your_made_up_secure_password,\
SPRING_CLOUD_GCP_PROJECT_ID=your_gcp_project_id"
```

### Method B: Fast Local Build (90 Seconds) 🚀
Use this if you have Docker installed locally. It reuses your local Gradle cache and Docker layers, making redeployments extremely fast.

1. **One-time authentication for Docker:**
   ```bash
   gcloud auth configure-docker australia-southeast1-docker.pkg.dev
   ```

2. **Build and push the image locally:**
   ```bash
   # Build the image locally (fast because of your local gradle cache)
   # Note: --platform linux/amd64 is required for Mac M1/M2/M3 users for Cloud Run compatibility
   docker build --platform linux/amd64 -t australia-southeast1-docker.pkg.dev/tech-market-insights/tech-market-repo/tech-market-backend:latest .

   # Push the image to GCP (only pushes changed layers)
   docker push australia-southeast1-docker.pkg.dev/tech-market-insights/tech-market-repo/tech-market-backend:latest
   ```

3. **Deploy from the pre-built image:**
   ```bash
   gcloud run deploy tech-market-backend \
    --image australia-southeast1-docker.pkg.dev/tech-market-insights/tech-market-repo/tech-market-backend:latest \
    --region australia-southeast1
   ```
   *(Note: Subsequent image deploys don't need the `--set-env-vars` again as Cloud Run remembers the previous config).*


If prompted to "Allow unauthenticated invocations", type `y` and press Enter.

---

## Stage 4: Grant Permissions (IAM)

When Cloud Run deploys your app, it runs using a default "Compute Engine Service Account". This account looks something like: `12345678910-compute@developer.gserviceaccount.com`. 

For your app to save data to BigQuery, you must give this account permission to do so.

1. Go to the [IAM & Admin page in the GCP Console](https://console.cloud.google.com/iam-admin/iam).
2. Find the row for the **Compute Engine default service account**.
3. Click the pencil icon (Edit principal) on the right side of that row.
4. Click **Add Another Role** and add:
   - **BigQuery Data Editor**
   - **BigQuery Job User**
5. Click **Save**.

---

## Stage 5: Post-Deployment Wiring

Once the deployment finishes successfully, the terminal will print out a **Service URL** (e.g., `https://tech-market-backend-abc.a.run.app`).

### 1. Update the Frontend (Vercel)
1. Go to your existing Vercel dashboard: [tech-market-insights.vercel.app](https://tech-market-insights.vercel.app/).
2. Navigate to **Settings > Environment Variables**.
3. Add a new variable:
   - **Key**: `VITE_API_URL`
   - **Value**: Your new Cloud Run URL (e.g., `https://tech-market-backend-abc.a.run.app/api`)
4. Redeploy your frontend in Vercel for the changes to take effect.

### 2. Connect the Apify Webhook
1. Go to your [Apify Console](https://console.apify.com/).
2. Navigate to the Actor or Task you use to scrape job data.
3. Go to the **Integrations** tab and add an **HTTP Webhook**.
4. Set the **Webhook URL** to your new backend endpoint: `https://tech-market-backend-abc.a.run.app/api/webhook/apify/data-changed`
5. In the **Headers** section, add the security header to match what you deployed with:
   - **Key**: `X-Apify-Webhook-Secret`
   - **Value**: `your_made_up_secure_password`

You're done! Your completely serverless pipeline is now live.

---

## Stage 5b: Cloud Tasks Setup (Required for Background Processing)

The backend uses **Google Cloud Tasks** for reliable background processing of webhook events and admin sync triggers. This prevents HTTP timeouts during large data syncs.

### Option A: Automated Setup (Recommended)

Run the provided setup script:

```bash
./scripts/deployment/setup-cloud-tasks.sh
```

This script will:
1. Create the Cloud Tasks queue (`tech-market-sync-queue`)
2. Create the Dead Letter Queue (`tech-market-sync-dlq`)
3. Grant Cloud Tasks permission to invoke your Cloud Run service
4. Configure environment variables on Cloud Run

### Option B: Manual Setup

If you prefer to set up Cloud Tasks manually:

**1. Create the Dead Letter Queue:**
```bash
gcloud tasks queues create tech-market-sync-dlq \
  --location=australia-southeast1 \
  --max-dispatches-per-second=1
```

**2. Create the primary sync queue:**
```bash
gcloud tasks queues create tech-market-sync-queue \
  --location=australia-southeast1 \
  --max-dispatches-per-second=10 \
  --max-concurrent-dispatches=5 \
  --max-attempts=5 \
  --min-backoff=60s \
  --max-backoff=3600s \
  --dead-letter-queue=tech-market-sync-dlq
```

**3. Get the Cloud Tasks service account:**
```bash
CLOUD_TASKS_SA=$(gcloud tasks queues describe tech-market-sync-queue \
  --location=australia-southeast1 \
  --format="value(status.cloudTasksServiceAccount)")
```

**4. Grant Cloud Tasks permission to invoke Cloud Run:**
```bash
gcloud run services add-iam-policy-binding tech-market-backend \
  --region australia-southeast1 \
  --member="serviceAccount:$CLOUD_TASKS_SA" \
  --role="roles/run.invoker"
```

**5. Update Cloud Run environment variables:**
```bash
gcloud run services update tech-market-backend \
  --region australia-southeast1 \
  --set-env-vars="\
GCP_PROJECT_ID=$PROJECT_ID,\
CLOUD_TASKS_QUEUE_NAME=tech-market-sync-queue,\
CLOUD_TASKS_LOCATION=australia-southeast1,\
APP_BASE_URL=https://tech-market-backend-xxx.a.run.app"
```

### Verify Cloud Tasks is Working

**Test the webhook:**
```bash
curl -X POST https://your-backend-url.a.run.app/api/webhook/apify/data-changed \
  -H "Content-Type: application/json" \
  -H "X-Apify-Webhook-Secret: your-secret" \
  -d '{"eventType": "ACTIVITY", "resource": {"defaultDatasetId": "test123"}}'
```

You should receive a `202 Accepted` response immediately with a correlation ID.

**Monitor Cloud Tasks:**
- Queue Dashboard: https://console.cloud.google.com/tasks/queues/australia-southeast1/tech-market-sync-queue
- Cloud Run Logs: https://console.cloud.google.com/run/detail/australia-southeast1/tech-market-backend/logs

---

## Stage 6: Schema Migrations & Data Reprocessing

When backend schema changes affect the **Silver layer** (i.e. the `raw_jobs` or `raw_companies` BigQuery tables), those tables must be dropped and rebuilt from the lossless **Bronze layer** (`raw_ingestions`).

> **Why?** BigQuery does not support `ALTER TABLE DROP COLUMN`. The only way to remove a column from an existing table is to recreate it. The application automatically recreates the table with the correct schema on startup if it doesn't exist.
>
> The **Bronze layer** (`raw_ingestions`) is never touched — it's the source of truth and stores the full original JSON payload from Apify.

### When to do this

Run this workflow any time you:
- Remove or rename a column from `JobRecord` or `CompanyRecord`
- Add a non-nullable column that the old data didn't populate

### Steps

**1. Drop the Silver layer tables via the BigQuery CLI:**
```bash
bq rm -f -t techmarket.raw_jobs
bq rm -f -t techmarket.raw_companies
```

**2. Trigger a reprocess from the raw ingestion records:**

The backend exposes a protected admin endpoint that reads all records from `raw_ingestions`, re-parses them through the full mapper/parser pipeline, and re-inserts them into the newly created `raw_jobs` and `raw_companies` tables (with the correct schema).

```bash
# Replace YOUR_WEBHOOK_SECRET with the value of APIFY_WEBHOOK_SECRET in your Cloud Run config
curl -X POST https://tech-market-backend-181692518949.australia-southeast1.run.app/api/admin/reprocess-jobs \
  -H "x-apify-signature: YOUR_WEBHOOK_SECRET"
```

The tables will be auto-created with the correct schema by the application the first time it runs a query after the drop. The reprocess typically completes in a few seconds for current data volumes.

---

## Stage 7: Production Hardening

For production environments, the following configurations are mandatory to ensure security, reliability, and performance.

### 1. Secure Secrets with Google Secret Manager
Never use `--set-env-vars` for sensitive data in production. Instead, reference secrets stored in Google Secret Manager.

```bash
gcloud run services update tech-market-backend \
  --region australia-southeast1 \
  --set-secrets "\
    APIFY_TOKEN=APIFY_TOKEN_SECRET_NAME:latest,\
    APIFY_WEBHOOK_SECRET=APIFY_WEBHOOK_SECRET_NAME:latest"
```

### 2. Explicit BigQuery Configuration
To prevent the application from attempting to resolve datasets in the wrong region or project (which causes `NOT_FOUND` errors), explicitly set the project and dataset:

```bash
gcloud run services update tech-market-backend \
  --region australia-southeast1 \
  --update-env-vars "\
    SPRING_CLOUD_GCP_BIGQUERY_PROJECT_ID=your-project-id,\
    SPRING_CLOUD_GCP_BIGQUERY_DATASET_NAME=techmarket"
```

### 3. Resource Allocation for JVM
Java/Kotlin applications typically require more than the default 512Mi memory for peak processing (like data re-mapping).

- **Recommended Memory**: 2Gi
- **Why**: Prevents `OutOfMemory` errors during large-scale historical data migrations or complex mapping operations.

```bash
gcloud run services update tech-market-backend \
  --region australia-southeast1 \
  --memory 2Gi
```

