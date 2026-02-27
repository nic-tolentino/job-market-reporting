# Reliable Background Processing in Cloud Run

Currently, our webhook calls and admin sync triggers run synchronously. This works while our data volume is low (~100 jobs), but as we scale or add LLM extraction, it will lead to HTTP timeouts and potential CPU throttling in Cloud Run.

## The Challenge
Google Cloud Run throttles CPU to nearly zero as soon as an HTTP response is sent. This means standard Java/Kotlin `@Async` tasks or background threads often "freeze" and fail to complete once the user/webhook receives their "200 OK".

## Proposed Solutions

### Option 1: Cloud Tasks (Highly Recommended)
Instead of processing the data immediately, the webhook handler creates a task in a Google Cloud Tasks queue.
- **Workflow:** Webhook receives request -> Pushes task to Queue -> Responds with 202 Accepted -> Cloud Tasks calls a *separate* endpoint on our backend to do the actual work.
- **Pros:** Extremely reliable, automatic retries, decoupling of ingestion and processing.
- **Cons:** Requires setting up a Cloud Tasks queue and another endpoint.

### Option 2: Always-on CPU
Change the Cloud Run configuration to "CPU is always allocated".
- **Workflow:** We can use standard `@Async` methods or Kotlin Coroutines.
- **Pros:** Easiest code change.
- **Cons:** Significantly more expensive (you pay for the instance even when it's idle). Not recommended for our "$0/month" goal.

### Option 3: Integration with Pub/Sub
- **Workflow:** Webhook publishes a message to a Pub/Sub topic. Cloud Run (or a Cloud Function) is triggered by that topic.
- **Pros:** Very standard for large-scale GCP event-driven architectures.
- **Cons:** Slightly more complexity than Cloud Tasks for a simple single-service app.

## Future Implementation Plan
1. Create a `CloudTasksService` in the backend.
2. Define a secure internal endpoint (e.g., `/api/internal/sync-task`).
3. Update `ApifyWebhookController` and `AdminController` to simply "schedule" the work instead of doing it.
