#!/bin/bash
# Recover failed dataset wEVaOoamb196P6g85

set -e

DATASET_ID="wEVaOoamb196P6g85"
TIMESTAMP="2026-02-26T12:52:00Z"

# Load environment variables
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
if [ -z "$APIFY_WEBHOOK_SECRET" ]; then
    echo "❌ Error: APIFY_WEBHOOK_SECRET is not set in .env"
    exit 1
fi

if [ -z "$BACKEND_URL" ]; then
    echo "⚠️  BACKEND_URL not set, using default..."
    BACKEND_URL="https://tech-market-backend-2pwszjr25a-ts.a.run.app"
fi

echo "=== Recovering Failed Dataset: $DATASET_ID ==="
echo "Backend URL: $BACKEND_URL"
echo ""

# Step 1: Delete FAILED metadata row
echo "Step 1: Deleting FAILED metadata row..."
bq query --use_legacy_sql=false \
  "DELETE FROM techmarket.ingestion_metadata WHERE dataset_id = '$DATASET_ID'" 2>&1 | grep -E "affected rows|Error"

echo ""
echo "Step 2: Verifying deletion..."
count=$(bq query --use_legacy_sql=false --format=csv \
  "SELECT COUNT(*) FROM techmarket.ingestion_metadata WHERE dataset_id = '$DATASET_ID'" 2>&1 | tail -1)

if [ "$count" = "0" ]; then
  echo "✅ Metadata row deleted successfully"
else
  echo "❌ Failed to delete metadata row"
  exit 1
fi

echo ""
echo "Step 3: Triggering re-ingestion..."
response=$(curl -s -w "\n%{http_code}" -X POST \
  "$BACKEND_URL/api/admin/trigger-sync?datasetId=$DATASET_ID&ingestedAt=$TIMESTAMP" \
  -H "x-apify-signature: $APIFY_WEBHOOK_SECRET")

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
  echo "✅ Triggered successfully"
  echo "Response: $body"
else
  echo "❌ Failed to trigger (HTTP $http_code)"
  echo "Response: $body"
  exit 1
fi

echo ""
echo "Step 4: Waiting 90 seconds for Cloud Tasks to process..."
sleep 90

echo ""
echo "Step 5: Checking ingestion status..."
status=$(bq query --use_legacy_sql=false --format=csv \
  "SELECT processing_status, record_count FROM techmarket.ingestion_metadata WHERE dataset_id = '$DATASET_ID'" 2>&1 | tail -1)

if echo "$status" | grep -q "COMPLETED"; then
  echo "✅ Dataset $DATASET_ID is COMPLETED"
  echo "   $status"
  
  # Check companies count
  companies=$(bq query --use_legacy_sql=false --format=csv \
    "SELECT COUNT(*) FROM techmarket.raw_companies" 2>&1 | tail -1)
  echo "   Companies in raw_companies: $companies"
  
  # Check total jobs
  jobs=$(bq query --use_legacy_sql=false --format=csv \
    "SELECT COUNT(*) FROM techmarket.raw_jobs" 2>&1 | tail -1)
  echo "   Total jobs in raw_jobs: $jobs"
  
  echo ""
  echo "✅ Recovery successful!"
  exit 0
elif echo "$status" | grep -q "PENDING"; then
  echo "⚠️ Dataset $DATASET_ID is still PENDING - may need more time"
  echo "   Check again in a few minutes"
  exit 1
else
  echo "❌ Dataset $DATASET_ID status: $status"
  exit 1
fi
