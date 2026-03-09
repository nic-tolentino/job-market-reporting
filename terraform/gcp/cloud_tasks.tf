# Cloud Tasks Infrastructure for Tech Market Backend
# This file defines the Cloud Tasks queues for background processing

# Primary sync queue for processing job data syncs
resource "google_cloud_tasks_queue" "sync_queue" {
  name     = "tech-market-sync-queue"
  location = var.gcp_region

  rate_limits {
    max_dispatches_per_second = 10
    max_concurrent_dispatches = 5
  }

  retry_config {
    max_attempts      = 5
    min_backoff       = "60s"
    max_backoff       = "3600s"
    max_doublings     = 5
  }

  dead_letter_queue {
    dead_letter_queue = google_cloud_tasks_queue.dlq.id
    max_attempts      = 5
  }

  stackdriver_logging_config {
    sampling_ratio = 1.0
  }
}

# Dead letter queue for failed tasks
resource "google_cloud_tasks_queue" "dlq" {
  name     = "tech-market-sync-dlq"
  location = var.gcp_region

  rate_limits {
    max_dispatches_per_second = 1
  }

  stackdriver_logging_config {
    sampling_ratio = 1.0
  }
}
