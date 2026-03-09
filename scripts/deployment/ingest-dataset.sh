#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
if [ -z "$BACKEND_URL" ]; then
    echo "Error: BACKEND_URL is not set in .env"
    exit 1
fi
if [ -z "$APIFY_WEBHOOK_SECRET" ]; then
    echo "Error: APIFY_WEBHOOK_SECRET is not set in .env"
    exit 1
fi

DATASET_ID=${1:-$APIFY_DATASET_ID}

if [ -z "$DATASET_ID" ]; then
    echo "Error: No dataset ID provided and APIFY_DATASET_ID is not set in .env"
    echo "Usage: $0 [dataset_id]"
    exit 1
fi

echo "📥 Triggering ingestion for dataset: $DATASET_ID at $BACKEND_URL..."

# Remove trailing slash from BACKEND_URL if present
URL="${BACKEND_URL%/}"

curl -X POST "$URL/api/admin/trigger-sync?datasetId=$DATASET_ID" \
  -H "x-apify-signature: $APIFY_WEBHOOK_SECRET" \
  -i

echo -e "\n✅ Ingestion command sent."
