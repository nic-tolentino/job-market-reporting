#!/bin/bash
# Trigger missing datasets with custom ingestion times

# Load environment variables if .env exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Use environment variables or defaults
WEBHOOK_SECRET=${APIFY_WEBHOOK_SECRET}
BACKEND_URL=${BACKEND_URL:-"https://tech-market-backend.a.run.app"}

# Missing datasets (dataset_id|timestamp)
DATASETS=(
  "buP6fe11modyaeIdZ|2026-03-06T03:16:34Z"
  "2AXVyR5WlEhNdJAJI|2026-02-27T08:41:30Z"
  "wEVaOoamb196P6g85|2026-02-26T12:52:00Z"
)

echo "=== Triggering Remaining 3 Datasets (30s delay) ==="
echo "Backend URL: $BACKEND_URL"
echo ""

for i in "${!DATASETS[@]}"; do
  IFS='|' read -r dataset_id timestamp <<< "${DATASETS[$i]}"
  
  # URL encode the timestamp (replace : with %3A)
  encoded_timestamp="${timestamp//:/%3A}"
  
  echo "[$((i+1))/${#DATASETS[@]}] Triggering: $dataset_id"
  echo "      Timestamp: $timestamp"
  
  response=$(curl -s -w "\n%{http_code}" -X POST \
    "$BACKEND_URL/api/admin/trigger-sync?datasetId=$dataset_id&ingestedAt=$encoded_timestamp" \
    -H "x-apify-signature: $WEBHOOK_SECRET")
  
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')
  
  if [ "$http_code" = "200" ]; then
    echo "      ✅ Status: QUEUED"
  else
    echo "      ❌ Status: FAILED ($http_code)"
    echo "      Response: $body"
  fi
  echo ""
  
  # Wait 30 seconds between triggers to avoid race conditions
  if [ $i -lt $((${#DATASETS[@]}-1)) ]; then
    echo "      Waiting 30 seconds before next trigger..."
    sleep 30
  fi
done

echo "=== All Triggers Complete ==="
echo ""
echo "Next steps:"
echo "1. Wait 2-3 minutes for Cloud Tasks to process"
echo "2. Check BigQuery: SELECT dataset_id, processing_status FROM techmarket.ingestion_metadata ORDER BY ingested_at DESC"
