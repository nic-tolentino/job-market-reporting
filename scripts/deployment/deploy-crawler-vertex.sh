#!/bin/bash

# Crawler Service Deployment Script (Vertex AI)
# Deploys the self-hosted AI crawler to Google Cloud Run with Vertex AI

set -e

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")"

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
REQUIRED_VARS=("GCP_PROJECT_ID" "GCP_REGION")
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo "Error: $var is not set in .env"
        exit 1
    fi
done

CRAWLER_SERVICE_NAME="crawler-service"

echo "🚀 Deploying Crawler Service to Cloud Run (Vertex AI)"
echo "======================================================"
echo "Project: $GCP_PROJECT_ID"
echo "Region: $GCP_REGION"
echo "Service: $CRAWLER_SERVICE_NAME"
echo ""

cd "$PROJECT_ROOT/crawler-service" || exit

# Build and deploy
echo "📦 Building and deploying with Cloud Build..."
echo ""

gcloud run deploy "$CRAWLER_SERVICE_NAME" \
    --source . \
    --project "$GCP_PROJECT_ID" \
    --region "$GCP_REGION" \
    --allow-unauthenticated \
    --min-instances 0 \
    --max-instances 3 \
    --cpu 1 \
    --memory 2Gi \
    --timeout 300 \
    --port 8080 \
    --set-env-vars="\
NODE_ENV=production,\
GCP_PROJECT_ID=$GCP_PROJECT_ID,\
GCP_REGION=$GCP_REGION,\
GEMINI_MODEL=gemini-2.0-flash" \
    --service-account="$GCP_SERVICE_ACCOUNT"

# Get the service URL
SERVICE_URL=$(gcloud run services describe "$CRAWLER_SERVICE_NAME" \
    --project "$GCP_PROJECT_ID" \
    --region "$GCP_REGION" \
    --format='value(status.url)')

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📡 Service URL: $SERVICE_URL"
echo ""
echo "🧪 Test the service:"
echo "   curl $SERVICE_URL/health"
echo ""
echo "📊 View logs:"
echo "   gcloud run services logs read $CRAWLER_SERVICE_NAME --region $GCP_REGION --project $GCP_PROJECT_ID"
echo ""
echo "🔧 Update backend to use crawler:"
echo "   Set CRAWLER_SERVICE_URL=$SERVICE_URL in backend .env"
echo ""
echo "⚠️  Prerequisites:"
echo "   1. Service account must have 'Vertex AI User' role"
echo "   2. Vertex AI API must be enabled"
echo "   3. Check quota at: https://console.cloud.google.com/vertex-ai/quotas"
