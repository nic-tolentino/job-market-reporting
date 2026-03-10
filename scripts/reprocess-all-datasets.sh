#!/bin/bash
# Reprocess all datasets to restore full company data

set -e

# Load environment
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Get backend URL from .env (loaded above)
if [ -z "$BACKEND_URL" ]; then
    echo "⚠️  BACKEND_URL not set in .env, attempting to get from Cloud Run..."
    BACKEND_URL=$(gcloud run services describe tech-market-backend --region=australia-southeast1 --format="value(status.url)" 2>/dev/null)
    if [ -z "$BACKEND_URL" ]; then
        echo "❌ Failed to get backend URL. Please set BACKEND_URL in .env"
        exit 1
    fi
fi
echo "Backend URL: $BACKEND_URL"
echo ""

echo "Step 1: Triggering reprocessing..."
echo "This will:"
echo "  - Wipe raw_jobs and raw_companies tables"
echo "  - Re-extract all data from Bronze layer (GCS files)"
echo "  - Re-populate with all 8 datasets"
echo ""

# Check for webhook secret
if [ -z "$APIFY_WEBHOOK_SECRET" ]; then
    echo "❌ Error: APIFY_WEBHOOK_SECRET is not set in .env"
    echo "   Please add it to .env and try again"
    exit 1
fi

# Trigger reprocess using webhook secret authentication
echo "Triggering reprocess..."
response=$(curl -s -w "\n%{http_code}" -X POST \
  "$BACKEND_URL/api/admin/reprocess-jobs" \
  -H "x-apify-signature: $APIFY_WEBHOOK_SECRET")

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo "✅ Reprocessing triggered successfully"
    echo "Response: $body"
else
    echo "❌ Failed to trigger reprocessing (HTTP $http_code)"
    echo "Response: $body"
    exit 1
fi

echo ""
echo "Step 2: Waiting for reprocessing to complete..."
echo "This may take 3-5 minutes for all 8 datasets..."
echo ""

# Poll every 30 seconds until jobs count stabilizes or timeout after 5 minutes
MAX_ATTEMPTS=10
ATTEMPT=0
PREV_JOBS=0
STABLE_COUNT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    echo "  Checking progress (attempt $ATTEMPT/$MAX_ATTEMPTS)..."
    
    # Get current jobs count
    CURRENT_JOBS=$(bq query --use_legacy_sql=false --format=csv \
        "SELECT COUNT(*) FROM techmarket.raw_jobs" 2>&1 | tail -1)
    
    # Get current companies count
    CURRENT_COMPANIES=$(bq query --use_legacy_sql=false --format=csv \
        "SELECT COUNT(*) FROM techmarket.raw_companies" 2>&1 | tail -1)
    
    echo "    Jobs: $CURRENT_JOBS, Companies: $CURRENT_COMPANIES"
    
    # Check if count is stable (same as previous check)
    if [ "$CURRENT_JOBS" = "$PREV_JOBS" ] && [ "$CURRENT_JOBS" -gt "0" ]; then
        STABLE_COUNT=$((STABLE_COUNT + 1))
        echo "    Count stable ($STABLE_COUNT/2 checks)"
        
        # If stable for 2 consecutive checks, we're done
        if [ $STABLE_COUNT -ge 2 ]; then
            echo ""
            echo "  ✅ Reprocessing complete!"
            break
        fi
    else
        STABLE_COUNT=0
    fi
    
    PREV_JOBS=$CURRENT_JOBS
    
    # Wait 30 seconds before next check (unless this is the last attempt)
    if [ $ATTEMPT -lt $MAX_ATTEMPTS ]; then
        sleep 30
    fi
done

echo ""
echo "Step 3: Final verification..."
echo ""

# Check jobs count
jobs=$(bq query --use_legacy_sql=false --format=csv \
  "SELECT COUNT(*) FROM techmarket.raw_jobs" 2>&1 | tail -1)
echo "Jobs in raw_jobs: $jobs"

# Check companies count
companies=$(bq query --use_legacy_sql=false --format=csv \
  "SELECT COUNT(*) FROM techmarket.raw_companies" 2>&1 | tail -1)
echo "Companies in raw_companies: $companies"

# Check datasets
datasets=$(bq query --use_legacy_sql=false --format=csv \
  "SELECT COUNT(*) FROM techmarket.ingestion_metadata WHERE processing_status = 'COMPLETED'" 2>&1 | tail -1)
echo "Datasets COMPLETED: $datasets"

echo ""
if [ "$datasets" = "8" ] && [ "$jobs" -gt "2000" ] && [ "$companies" -gt "0" ]; then
    echo "✅ Reprocessing successful!"
    echo ""
    echo "Final state:"
    echo "  - $datasets datasets COMPLETED"
    echo "  - ~$jobs total records ingested"
    echo "  - ~$jobs unique jobs (after deduplication)"
    echo "  - ~$companies unique companies (from all datasets)"
else
    echo "⚠️  Reprocessing may still be in progress"
    echo "   Current state: $datasets datasets, $jobs jobs, $companies companies"
    echo ""
    echo "   Wait a few more minutes and check manually:"
    echo "   bq query \"SELECT COUNT(*) FROM techmarket.raw_jobs\""
    echo "   bq query \"SELECT COUNT(*) FROM techmarket.raw_companies\""
fi
