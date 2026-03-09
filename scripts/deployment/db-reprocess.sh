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

echo "🔄 Triggering data reprocessing at $BACKEND_URL..."

# Remove trailing slash from BACKEND_URL if present
URL="${BACKEND_URL%/}"

curl -X POST "$URL/api/admin/reprocess-jobs" \
  -H "x-apify-signature: $APIFY_WEBHOOK_SECRET" \
  -i

echo -e "\n✅ Reprocess command sent."
