#!/bin/bash

# Crawler Service Deployment Script
# Deploys the self-hosted AI crawler to Google Cloud Run

set -e

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

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
CRAWLER_IMAGE="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/tech-market-repo/${CRAWLER_SERVICE_NAME}:latest"

echo "🚀 Deploying Crawler Service to Cloud Run"
echo "=========================================="
echo "Project: $GCP_PROJECT_ID"
echo "Region: $GCP_REGION"
echo "Service: $CRAWLER_SERVICE_NAME"
echo ""

# Check for --cloud flag
USE_CLOUD_BUILD=false
if [[ "$1" == "--cloud" ]]; then
    USE_CLOUD_BUILD=true
fi

# Check if Docker is running
if [ "$USE_CLOUD_BUILD" = false ]; then
    if ! docker info >/dev/null 2>&1; then
        echo "⚠️  Docker is not running locally."
        echo "Falling back to Google Cloud Build..."
        USE_CLOUD_BUILD=true
    fi
fi

cd "$PROJECT_ROOT/crawler-service" || exit

# Check if GEMINI_API_KEY is set (only if not using Vertex AI)
# Note: Vertex AI version uses service account permissions instead
if [ -z "$GEMINI_API_KEY" ] && [[ "$0" != *"vertex"* ]]; then
    echo "⚠️  GEMINI_API_KEY not found in environment"
    echo "Please set it in .env or export it before deploying"
    echo ""
    read -p "Do you want to continue without GEMINI_API_KEY? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

if [ "$USE_CLOUD_BUILD" = true ]; then
    echo "☁️  Starting Cloud Build & Deployment (No local Docker needed)..."
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
PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1" \
        --set-secrets="\
GEMINI_API_KEY=GEMINI_API_KEY:latest"
else
    echo "🚀 Starting Fast Local Build & Deployment (Requires Docker)..."

    echo "📦 Building Docker image locally..."
    # --platform linux/amd64 is crucial for Mac M1/M2/M3 compatibility with Cloud Run
    docker build --platform linux/amd64 -t "$CRAWLER_IMAGE" .

    if [ $? -ne 0 ]; then
        echo "❌ Docker build failed."
        echo "Try running with --cloud to build in the cloud instead: ./scripts/deployment/deploy-crawler.sh --cloud"
        exit 1
    fi

    echo "📤 Pushing image to Google Artifact Registry..."
    docker push "$CRAWLER_IMAGE"

    if [ $? -ne 0 ]; then
        echo "❌ Docker push failed."
        exit 1
    fi

    echo "🚀 Deploying to Cloud Run from image..."
    gcloud run deploy "$CRAWLER_SERVICE_NAME" \
        --image "$CRAWLER_IMAGE" \
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
PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1" \
        --set-secrets="\
GEMINI_API_KEY=GEMINI_API_KEY:latest"
fi

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
