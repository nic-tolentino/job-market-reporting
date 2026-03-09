# GCP Configuration
variable "gcp_project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "gcp_region" {
  description = "The GCP region for resources"
  type        = string
  default     = "australia-southeast1"
}

variable "gcp_service_account" {
  description = "The service account email for Cloud Run and other services"
  type        = string
}
