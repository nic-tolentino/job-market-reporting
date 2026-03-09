#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
REQUIRED_VARS=("GCP_PROJECT_ID" "GCP_REGION" "GCP_BACKEND_SERVICE_NAME" "APIFY_TOKEN" "APIFY_DATASET_ID" "APIFY_WEBHOOK_SECRET")
for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        echo "Error: $var is not set in .env"
        exit 1
    fi
done

echo "🚀 Deploying $GCP_BACKEND_SERVICE_NAME to $GCP_REGION in project $GCP_PROJECT_ID..."

cd "$PROJECT_ROOT/backend" || exit

# Use --update-env-vars instead of --set-env-vars to avoid type conflicts with existing secrets
# and to prevent wiping out other environment variables not managed by this script.
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
SPRING_CLOUD_GCP_BIGQUERY_DATASET_NAME=techmarket"

echo ""
echo "💡 Note: APIFY_TOKEN and APIFY_WEBHOOK_SECRET were not updated Literals."
echo "If you need to update them, use Secret Manager or run the specific secret update command in DEPLOY.md."
echo "✅ Deployment complete!"
