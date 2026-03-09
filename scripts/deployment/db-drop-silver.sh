#!/bin/bash

# Get the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$DIR")/.."

# Load .env
if [ -f "$PROJECT_ROOT/.env" ]; then
    export $(grep -v '^#' "$PROJECT_ROOT/.env" | xargs)
fi

# Check required variables
if [ -z "$GCP_PROJECT_ID" ]; then
    echo "Error: GCP_PROJECT_ID is not set in .env"
    exit 1
fi

DATASET="techmarket"

echo "⚠️  WARNING: This will drop the silver layer tables: $DATASET.raw_jobs and $DATASET.raw_companies"
echo "The bronze layer (raw_ingestions) will be preserved."
read -p "Are you sure? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Operation cancelled."
    exit 1
fi

echo "🗑 Dropping tables in $GCP_PROJECT_ID..."

bq rm -f -t "$GCP_PROJECT_ID:$DATASET.raw_jobs"
bq rm -f -t "$GCP_PROJECT_ID:$DATASET.raw_companies"

echo "✅ Tables dropped. They will be recreated on next application startup or reprocess."
