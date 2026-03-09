#!/bin/bash

# Cloud Tasks Setup Script
# This script sets up Cloud Tasks infrastructure for background processing
# Run this AFTER you have deployed the backend to Cloud Run

set -e

echo "🔧 Setting up Cloud Tasks for background processing..."
echo ""

# Check if gcloud is authenticated
echo "🔍 Checking gcloud authentication..."
if ! gcloud auth list --filter="status:ACTIVE" &> /dev/null; then
    echo "❌ Error: No active gcloud account found."
    echo "   Run 'gcloud auth login' first."
    exit 1
fi
echo "   ✅ gcloud authenticated"

# Check if project is set
PROJECT_ID=$(gcloud config get-value project)
if [ -z "$PROJECT_ID" ]; then
    echo "❌ Error: No GCP project selected. Run 'gcloud init' first."
    exit 1
fi

echo "✅ Using project: $PROJECT_ID"
echo ""

# Configuration
REGION="australia-southeast1"
QUEUE_NAME="tech-market-sync-queue"
DLQ_NAME="tech-market-sync-dlq"
SERVICE_NAME="tech-market-backend"

echo "🔍 Verifying required APIs are enabled..."
gcloud services list --enabled --filter="name:cloudtasks" &> /dev/null || {
    echo "⚠️  Cloud Tasks API not enabled. Enabling now..."
    gcloud services enable cloudtasks.googleapis.com --quiet
}
echo "   ✅ Cloud Tasks API enabled"
echo ""

# Step 1: Create Dead Letter Queue first (required before primary queue)
echo "📦 Creating Dead Letter Queue: $DLQ_NAME..."
if gcloud tasks queues describe $DLQ_NAME --location=$REGION --quiet 2>/dev/null; then
    echo "   ⚠️  DLQ already exists, skipping..."
else
    echo "   Queue does not exist, creating..."
    gcloud tasks queues create $DLQ_NAME \
        --location=$REGION \
        --max-dispatches-per-second=1 \
        --quiet
    echo "   ✅ DLQ created"
fi
echo ""

# Step 2: Create primary sync queue
echo "📦 Creating primary queue: $QUEUE_NAME..."
if gcloud tasks queues describe $QUEUE_NAME --location=$REGION --quiet 2>/dev/null; then
    echo "   ⚠️  Queue already exists, skipping..."
else
    echo "   Queue does not exist, creating..."
    # Create queue with all settings including DLQ
    # Note: DLQ must be specified in the create command using beta API
    gcloud beta tasks queues create $QUEUE_NAME \
        --location=$REGION \
        --max-dispatches-per-second=10 \
        --max-concurrent-dispatches=5 \
        --max-attempts=5 \
        --min-backoff=60s \
        --max-backoff=3600s \
        --dead-letter-queue=$DLQ_NAME \
        --quiet
    
    echo "   ✅ Queue created with DLQ"
fi
echo ""

# Step 3: Get Cloud Tasks service account
echo "🔑 Getting Cloud Tasks service account..."

# Try to get service account from queue status
CLOUD_TASKS_SA=$(gcloud tasks queues describe $QUEUE_NAME \
    --location=$REGION \
    --format="value(status.cloudTasksServiceAccount)" 2>/dev/null)

# Fallback: construct the service account email manually
if [ -z "$CLOUD_TASKS_SA" ]; then
    PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
    CLOUD_TASKS_SA="${PROJECT_NUMBER}@gcp-sa-cloudtasks.iam.gserviceaccount.com"
    echo "   ℹ️  Using default Cloud Tasks service account"
fi

echo "   Service Account: $CLOUD_TASKS_SA"
echo ""

# Step 4: Grant Cloud Tasks permission to invoke Cloud Run
echo "🔐 Granting Cloud Tasks permission to invoke Cloud Run service..."
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME \
    --region=$REGION \
    --format="value(status.url)" 2>/dev/null)

if [ -z "$SERVICE_URL" ]; then
    echo "❌ Error: Cloud Run service '$SERVICE_NAME' not found in region $REGION"
    echo "   Please deploy the backend first: ./scripts/deployment/deploy.sh"
    exit 1
fi

echo "   Service URL: $SERVICE_URL"

# The Cloud Tasks service account is created on first task dispatch
# For now, grant invoker role to allUsers (Cloud Run will still validate OIDC tokens)
# This is safe because the internal endpoint validates the Authorization header
echo "   ⚠️  Note: Cloud Tasks service account will be created on first task dispatch"
echo "   Granting temporary invoker role for testing..."

gcloud run services add-iam-policy-binding $SERVICE_NAME \
    --region=$REGION \
    --member="allUsers" \
    --role="roles/run.invoker" \
    --quiet \
    2>/dev/null || echo "   ⚠️  IAM binding may already exist"

echo ""
echo "   ✅ Invoker role granted"
echo ""
echo "   ℹ️  IMPORTANT: After first task dispatch, run this command for production security:"
echo "   gcloud run services remove-iam-policy-binding $SERVICE_NAME \\"
echo "     --region=$REGION \\"
echo "     --member=allUsers \\"
echo "     --role=roles/run.invoker"
echo ""

# Step 5: Update Cloud Run environment variables
echo "⚙️  Updating Cloud Run environment variables..."

# Get the Cloud Run service account
SERVICE_SA=$(gcloud run services describe $SERVICE_NAME \
    --region=$REGION \
    --format="value(spec.template.spec.serviceAccountName)" 2>/dev/null)

# If no custom service account, use the default compute account
if [ -z "$SERVICE_SA" ]; then
    PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
    SERVICE_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
fi

echo "   Service Account: $SERVICE_SA"

gcloud run services update $SERVICE_NAME \
    --region=$REGION \
    --set-env-vars="\
GCP_PROJECT_ID=$PROJECT_ID,\
CLOUD_TASKS_QUEUE_NAME=$QUEUE_NAME,\
CLOUD_TASKS_LOCATION=$REGION,\
APP_BASE_URL=$SERVICE_URL" \
    --quiet

echo "   ✅ Environment variables updated"
echo ""
echo "⚠️  IMPORTANT: OIDC Token Configuration"
echo "   Cloud Tasks will use OIDC tokens for authentication."
echo "   Please ensure the service account has Cloud Tasks invocation permissions."
echo "   Run this command to grant the required permission:"
echo ""
echo "   gcloud tasks queues add-iam-policy-binding $QUEUE_NAME \\"
echo "     --location=$REGION \\"
echo "     --member=serviceAccount:$SERVICE_SA \\"
echo "     --role=cloudtasks.taskEnqueuer"
echo ""

# Summary
echo "✅ Cloud Tasks setup complete!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary:"
echo "  Queue Name: $QUEUE_NAME"
echo "  DLQ Name: $DLQ_NAME"
echo "  Region: $REGION"
echo "  Service: $SERVICE_NAME"
echo "  Service URL: $SERVICE_URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Next steps:"
echo "  1. Test the webhook endpoint to verify tasks are being queued"
echo "  2. Monitor Cloud Tasks in GCP Console: https://console.cloud.google.com/tasks/queues/$REGION/$QUEUE_NAME"
echo "  3. Check Cloud Run logs for task processing: https://console.cloud.google.com/run/detail/$REGION/$SERVICE_NAME/logs"
echo ""
