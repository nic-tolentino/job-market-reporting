# Comprehensive GCP Deployment Guide (From Scratch)

This guide takes you step-by-step from having **no Google Cloud account** to having your `Tech Market Backend` completely hosted on Google Cloud Run with a serverless BigQuery database.

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
