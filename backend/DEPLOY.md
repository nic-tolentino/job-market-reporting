# GCP Deployment Guide for Job Market Backend

This guide outlines the steps to deploy the Spring Boot backend to Google Cloud Run.

## Prerequisites
- Google Cloud CLI (`gcloud`) installed and authenticated.
- A GCP Project with Billing enabled.
- The BigQuery API and Cloud Run API enabled in your GCP project.

## 1. Create an Artifact Registry Repository
First, create a Docker repository in Google Cloud Artifact Registry to hold your container images.

```bash
gcloud artifacts repositories create tech-market-repo \
    --repository-format=docker \
    --location=us-central1 \
    --description="Docker repository for Backend"
```

## 2. Build & Deploy to Cloud Run
Run the following command from the `backend/` directory. Google Cloud will read the `Dockerfile`, build the image remotely using Cloud Build, push it to your registry, and deploy it to Cloud Run.

```bash
gcloud run deploy tech-market-backend \
    --source . \
    --region us-central1 \
    --allow-unauthenticated \
    --min-instances 0 \
    --max-instances 2 \
    --port 8080 \
    --set-env-vars="\
APIFY_TOKEN=your_real_apify_token,\
APIFY_DATASET_ID=your_real_apify_dataset_id,\
APIFY_WEBHOOK_SECRET=your_made_up_secure_password,\
SPRING_CLOUD_GCP_PROJECT_ID=your_gcp_project_id"
```
*(Make sure to replace the environment variable placeholders with your actual production values!).*

## 3. Grant BigQuery Access
Cloud Run automatically creates a default compute service account. You must ensure that this service account (e.g., `123456789-compute@developer.gserviceaccount.com`) has the following IAM roles in your GCP project so it can read and write to your datasets:
- **BigQuery Data Editor**
- **BigQuery Job User**

## 4. Post-Deployment Updates
After the deployment completes, Cloud Run will provide you with a live URL (e.g., `https://tech-market-backend-abc.a.run.app`).

1. **Update Vercel Frontend**: Update your Vercel project's environment variables (e.g., `VITE_API_URL` or equivalent) to point to the new backend URL.
2. **Update Apify Webhook**: 
   - Go to your Apify Console.
   - Update the webhook URL to `https://tech-market-backend-abc.a.run.app/api/webhook/apify/data-changed`.
   - Add an HTTP Header: Key: `X-Apify-Webhook-Secret`, Value: `<the password you set in step 2>`.
