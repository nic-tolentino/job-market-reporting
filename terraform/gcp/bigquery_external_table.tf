# BigQuery External Table for querying GCS files directly
resource "google_bigquery_table" "raw_ingestions_external" {
  dataset_id = google_bigquery_dataset.techmarket.dataset_id
  table_id   = "raw_ingestions_external"
  project    = var.gcp_project_id

  external_data_configuration {
    autodetect            = true
    source_format         = "NEWLINE_DELIMITED_JSON"
    compression           = "GZIP"
    ignore_unknown_values = true

    source_uris = [
      "gs://techmarket-bronze-ingestions/apify/*",
      "gs://techmarket-bronze-ingestions/ats/*",
    ]

    json_options {
      encoding = "UTF-8"
    }
  }

  deletion_protection = false
}

# Metadata table with clustering for query optimization
resource "google_bigquery_table" "ingestion_metadata" {
  dataset_id = google_bigquery_dataset.techmarket.dataset_id
  table_id   = "ingestion_metadata"
  project    = var.gcp_project_id

  schema = <<EOF
[
  {"name": "dataset_id", "type": "STRING", "mode": "REQUIRED"},
  {"name": "source", "type": "STRING", "mode": "REQUIRED"},
  {"name": "ingested_at", "type": "TIMESTAMP", "mode": "REQUIRED"},
  {"name": "target_country", "type": "STRING"},
  {"name": "schema_version", "type": "STRING"},
  {"name": "record_count", "type": "INTEGER"},
  {"name": "file_count", "type": "INTEGER"},
  {"name": "uncompressed_size_bytes", "type": "INTEGER"},
  {"name": "compressed_size_bytes", "type": "INTEGER"},
  {"name": "compression_ratio", "type": "FLOAT"},
  {"name": "processing_status", "type": "STRING"},
  {"name": "files", "type": "STRING", "mode": "REPEATED"},
  {"name": "metadata_id", "type": "STRING"}
]
EOF

  # Clustering for query performance and cost reduction
  # Queries filtering by source + date range will scan less data
  clustering = ["source", "ingested_at"]

  deletion_protection = false
}
