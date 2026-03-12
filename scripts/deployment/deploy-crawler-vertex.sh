#!/bin/bash

# Crawler Service Deployment Script (Gemini API)
# Deploys the self-hosted AI crawler to Google Cloud Run

set -e

# Get the directory of the script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
if [ -z "$GCP_PROJECT_ID" ]; then
    echo "Error: GCP_PROJECT_ID is not set in .env"
    exit 1
fi

CRAWLER_SERVICE_NAME="crawler-service"
GCP_REGION="${GCP_REGION:-australia-southeast1}"

echo "🚀 Deploying Crawler Service to Cloud Run"
echo "=========================================="
echo "Project: $GCP_PROJECT_ID"
echo "Region: $GCP_REGION"
echo "Service: $CRAWLER_SERVICE_NAME"
echo ""

cd "$PROJECT_ROOT/crawler-service" || exit

echo "☁️  Starting Cloud Build & Deployment (with layer caching)..."
echo ""

gcloud builds submit \
    --config cloudbuild.yaml \
    --project "$GCP_PROJECT_ID" \
    --substitutions="_REGION=$GCP_REGION" \
    .

echo ""
echo "✅ Deployment complete!"
echo ""
SERVICE_URL=$(gcloud run services describe "$CRAWLER_SERVICE_NAME" \
    --project "$GCP_PROJECT_ID" \
    --region "$GCP_REGION" \
    --format='value(status.url)')
echo "📡 Service URL: $SERVICE_URL"
echo ""
echo "🧪 Test the service:"
echo "   curl $SERVICE_URL/health"
echo ""
echo "📊 View logs:"
echo "   gcloud run services logs read $CRAWLER_SERVICE_NAME --region $GCP_REGION --project $GCP_PROJECT_ID"
