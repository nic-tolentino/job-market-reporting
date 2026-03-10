#!/bin/bash
# Trigger a single dataset and wait for completion

DATASET_ID=$1
INGESTED_AT=$2
WEBHOOK_SECRET=$3
BACKEND_URL="https://tech-market-backend.a.run.app"

if [ -z "$DATASET_ID" ] || [ -z "$INGESTED_AT" ]; then
  echo "Usage: $0 <dataset_id> <ingested_at_timestamp> <webhook_secret>"
  echo "Example: $0 q1lYhL1JW43Z3eaTO 2026-03-06T04:25:18Z your-secret"
  exit 1
fi

echo "=== Triggering Dataset: $DATASET_ID ==="
echo "Timestamp: $INGESTED_AT"
echo ""

# Trigger the sync
echo "Triggering sync..."
response=$(curl -s -w "\n%{http_code}" -X POST \
  "$BACKEND_URL/api/admin/trigger-sync?datasetId=$DATASET_ID&ingestedAt=$INGESTED_AT" \
  -H "x-apify-signature: $WEBHOOK_SECRET")

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
echo "Waiting 90 seconds for Cloud Tasks to process..."
sleep 90

# Check status
echo ""
echo "Checking ingestion status..."
status=$(bq query --use_legacy_sql=false --format=csv \
  "SELECT processing_status FROM techmarket.ingestion_metadata WHERE dataset_id='$DATASET_ID'" 2>&1 | tail -1)

if [ "$status" = "COMPLETED" ]; then
  echo "✅ Dataset $DATASET_ID is COMPLETED"
  
  # Get record count
  record_count=$(bq query --use_legacy_sql=false --format=csv \
    "SELECT record_count FROM techmarket.ingestion_metadata WHERE dataset_id='$DATASET_ID'" 2>&1 | tail -1)
  echo "   Records: $record_count"
  
  exit 0
elif [ "$status" = "PENDING" ]; then
  echo "⚠️ Dataset $DATASET_ID is still PENDING - may need more time"
  exit 1
elif [ -z "$status" ]; then
  echo "❌ Dataset $DATASET_ID not found in ingestion_metadata"
  exit 1
else
  echo "❌ Dataset $DATASET_ID status: $status"
  exit 1
fi
