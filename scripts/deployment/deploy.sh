#!/bin/bash

# Full deployment script — deploys both the backend and crawler service.
# Run this to push a complete release to production.
#
# Usage:
#   ./scripts/deployment/deploy.sh           # local Docker (fast)
#   ./scripts/deployment/deploy.sh --cloud   # Cloud Build (no local Docker required)
#
# To deploy only one service:
#   ./scripts/deployment/deploy-backend.sh [--cloud]
#   ./scripts/deployment/deploy-crawler.sh [--cloud]

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

CLOUD_FLAG=""
if [[ "$1" == "--cloud" ]]; then
    CLOUD_FLAG="--cloud"
fi

echo "========================================"
echo "  Full Deployment: Backend + Crawler"
echo "========================================"
echo ""

echo "--- Starting Parallel Deployment: Backend & Crawler ---"
echo "(Outputs will be interleaved, but this is 2x faster)"
echo ""

# Start backend deployment in background
"$DIR/deploy-backend.sh" $CLOUD_FLAG &
BACKEND_PID=$!

# Start crawler deployment in background
"$DIR/deploy-crawler.sh" $CLOUD_FLAG &
CRAWLER_PID=$!

# Wait for both to finish
wait $BACKEND_PID
wait $CRAWLER_PID

echo ""
echo "========================================"
echo "  ✅ Full deployment complete!"
echo "========================================"
