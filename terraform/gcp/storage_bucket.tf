# GCS Bucket for Bronze Layer Cold Storage
resource "google_storage_bucket" "bronze_ingestions" {
  name          = "techmarket-bronze-ingestions"
  location      = var.gcp_region
  project       = var.gcp_project_id
  storage_class = "STANDARD"  # Transition to COLDLINE via lifecycle

  uniform_bucket_level_access = true

  # Lifecycle rules for cost optimization
  lifecycle_rule {
    condition {
      age = 90  # Days
    }
    action {
      type          = "SetStorageClass"
      storage_class = "COLDLINE"
    }
  }

  lifecycle_rule {
    condition {
      age = 365  # 1 year
    }
    action {
      type          = "SetStorageClass"
      storage_class = "ARCHIVE"
    }
  }
}

# IAM: Allow backend service account full object management (create, read, delete)
# roles/storage.objectAdmin includes all permissions needed for Bronze layer operations
resource "google_storage_bucket_iam_member" "backend_admin" {
  bucket = google_storage_bucket.bronze_ingestions.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${var.gcp_service_account}"
}
