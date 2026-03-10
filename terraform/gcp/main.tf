terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.22.0"
    }
  }
}

provider "google" {
  project = var.gcp_project_id
  region  = var.gcp_region
}

# Principal BigQuery dataset
resource "google_bigquery_dataset" "techmarket" {
  dataset_id                  = "techmarket"
  friendly_name               = "techmarket"
  description                 = "Main dataset for Tech Market insights"
  location                    = var.gcp_region
  project                     = var.gcp_project_id
  delete_contents_on_destroy = false
}
