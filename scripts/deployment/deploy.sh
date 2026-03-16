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

echo "--- Step 1/2: Backend ---"
"$DIR/deploy-backend.sh" $CLOUD_FLAG

echo ""
echo "--- Step 2/2: Crawler Service ---"
"$DIR/deploy-crawler.sh" $CLOUD_FLAG

echo ""
echo "========================================"
echo "  ✅ Full deployment complete!"
echo "========================================"
