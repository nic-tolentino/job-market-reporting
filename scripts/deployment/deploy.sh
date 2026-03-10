#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
REQUIRED_VARS=("GCP_PROJECT_ID" "GCP_REGION" "GCP_BACKEND_SERVICE_NAME" "APIFY_TOKEN" "APIFY_DATASET_ID" "APIFY_WEBHOOK_SECRET" "GCS_BRONZE_BUCKET")
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo "Error: $var is not set in .env"
        exit 1
    fi
done

# Check for --cloud flag
USE_CLOUD_BUILD=false
if [[ "$1" == "--cloud" ]]; then
    USE_CLOUD_BUILD=true
fi

# Check if Docker is running
if [ "$USE_CLOUD_BUILD" = false ]; then
    if ! docker info >/dev/null 2>&1; then
        echo "⚠️  Docker is not running locally."
        echo "Falling back to Google Cloud Build (this might take a few minutes as it builds in the cloud)..."
        USE_CLOUD_BUILD=true
    fi
fi

cd "$PROJECT_ROOT/backend" || exit

if [ "$USE_CLOUD_BUILD" = true ]; then
    echo "☁️  Starting Cloud Build & Deployment (No local Docker needed)..."
    
    gcloud run deploy "$GCP_BACKEND_SERVICE_NAME" \
        --source . \
        --project "$GCP_PROJECT_ID" \
        --region "$GCP_REGION" \
        --allow-unauthenticated \
        --min-instances 0 \
        --max-instances 2 \
        --cpu 1 \
        --memory 2Gi \
        --port 8080 \
        --update-env-vars="\
APIFY_DATASET_ID=$APIFY_DATASET_ID,\
SPRING_CLOUD_GCP_PROJECT_ID=$GCP_PROJECT_ID,\
SPRING_CLOUD_GCP_BIGQUERY_PROJECT_ID=$GCP_PROJECT_ID,\
SPRING_CLOUD_GCP_BIGQUERY_DATASET_NAME=techmarket,\
GCS_BRONZE_BUCKET=$GCS_BRONZE_BUCKET"
else
    echo "🚀 Starting Fast Local Build & Deployment (Requires Docker)..."

    # Construct the image tag
    IMAGE_TAG="${GCP_REGION}-docker.pkg.dev/${GCP_PROJECT_ID}/tech-market-repo/${GCP_BACKEND_SERVICE_NAME}:latest"

    echo "📦 Building Docker image locally..."
    # --platform linux/amd64 is crucial for Mac M1/M2/M3 compatibility with Cloud Run
    docker build --platform linux/amd64 -t "$IMAGE_TAG" .

    if [ $? -ne 0 ]; then
        echo "❌ Docker build failed."
        echo "Try running with --cloud to build in the cloud instead: ./scripts/deployment/deploy.sh --cloud"
        exit 1
    fi

    echo "📤 Pushing image to Google Artifact Registry..."
    docker push "$IMAGE_TAG"

    if [ $? -ne 0 ]; then
        echo "❌ Docker push failed."
        exit 1
    fi

    echo "🚀 Deploying to Cloud Run from image..."
    gcloud run deploy "$GCP_BACKEND_SERVICE_NAME" \
        --image "$IMAGE_TAG" \
        --project "$GCP_PROJECT_ID" \
        --region "$GCP_REGION" \
        --allow-unauthenticated \
        --min-instances 0 \
        --max-instances 2 \
        --cpu 1 \
        --memory 2Gi \
        --port 8080 \
        --update-env-vars="\
APIFY_DATASET_ID=$APIFY_DATASET_ID,\
SPRING_CLOUD_GCP_PROJECT_ID=$GCP_PROJECT_ID,\
SPRING_CLOUD_GCP_BIGQUERY_PROJECT_ID=$GCP_PROJECT_ID,\
SPRING_CLOUD_GCP_BIGQUERY_DATASET_NAME=techmarket,\
GCS_BRONZE_BUCKET=$GCS_BRONZE_BUCKET"
fi

echo ""
echo "✅ Deployment complete!"
echo "💡 Note: APIFY_TOKEN and APIFY_WEBHOOK_SECRET are managed via Secret Manager."
