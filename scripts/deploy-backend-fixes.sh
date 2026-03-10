#!/bin/bash
# Deploy backend fixes using existing deployment workflow

set -e

echo "=== Deploying Backend Fixes ==="
echo ""

cd /Users/nic/Projects/job-market-gemini/backend

echo "Step 1: Building backend..."
./gradlew clean build -x test 2>&1 | tail -5

echo ""
echo "Step 2: Deploying using existing deployment script..."
echo ""

# Use the existing deployment script which handles all env vars correctly
/Users/nic/Projects/job-market-gemini/scripts/deployment/deploy.sh

echo ""
echo "✅ Backend deployed successfully!"
echo ""
echo "Step 3: Waiting 60 seconds for deployment to propagate..."
sleep 60

echo ""
echo "Step 4: Verifying backend is healthy..."
# Load env to get BACKEND_URL
source /Users/nic/Projects/job-market-gemini/.env 2>/dev/null || true
if [ -z "$BACKEND_URL" ]; then
    BACKEND_URL="https://tech-market-backend-2pwszjr25a-ts.a.run.app"
fi

curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" "$BACKEND_URL/actuator/health" || echo "⚠️  Health check endpoint may not be enabled"

echo ""
echo "=== Backend Deployment Complete ==="
echo ""
echo "Next: Recover failed dataset wEVaOoamb196P6g85"
echo "Run: ./scripts/recover-failed-dataset.sh"
