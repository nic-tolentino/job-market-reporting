# BigQuery tables for crawler operational state
# See docs/data/crawler-persistence-plan.md for full schema and design rationale

# crawler_seeds — upserted after every crawl; one row per (company_id, url)
# Answers: "what is this seed's health right now?"
resource "google_bigquery_table" "crawler_seeds" {
  dataset_id          = google_bigquery_dataset.techmarket.dataset_id
  table_id            = "crawler_seeds"
  project             = var.gcp_project_id
  description         = "Current seed health state. One row per (company_id, url) pair, upserted after each crawl."
  deletion_protection = false

  schema = jsonencode([
    { name = "company_id",                   type = "STRING",    mode = "REQUIRED"  },
    { name = "url",                          type = "STRING",    mode = "REQUIRED"  },
    { name = "category",                     type = "STRING",    mode = "NULLABLE"  },
    { name = "status",                       type = "STRING",    mode = "NULLABLE"  },
    { name = "pagination_pattern",           type = "STRING",    mode = "NULLABLE"  },
    { name = "last_known_job_count",         type = "INTEGER",   mode = "NULLABLE"  },
    { name = "last_known_page_count",        type = "INTEGER",   mode = "NULLABLE"  },
    { name = "last_crawled_at",              type = "TIMESTAMP", mode = "NULLABLE"  },
    { name = "last_duration_ms",             type = "INTEGER",   mode = "NULLABLE"  },
    { name = "error_message",               type = "STRING",    mode = "NULLABLE"  },
    { name = "consecutive_zero_yield_count", type = "INTEGER",   mode = "NULLABLE"  },
    { name = "ats_provider",                 type = "STRING",    mode = "NULLABLE"  },
    { name = "ats_identifier",               type = "STRING",    mode = "NULLABLE"  },
    { name = "ats_direct_url",               type = "STRING",    mode = "NULLABLE"  },
  ])
}

# crawl_runs — append-only; one row per crawl execution, never updated
# Answers: "how has yield trended over time?"
resource "google_bigquery_table" "crawl_runs" {
  dataset_id          = google_bigquery_dataset.techmarket.dataset_id
  table_id            = "crawl_runs"
  project             = var.gcp_project_id
  description         = "Append-only crawl execution history. One row per crawl, never updated."
  deletion_protection = false

  time_partitioning {
    type  = "DAY"
    field = "started_at"
  }

  schema = jsonencode([
    { name = "run_id",             type = "STRING",    mode = "REQUIRED" },
    { name = "batch_id",           type = "STRING",    mode = "NULLABLE" },
    { name = "company_id",         type = "STRING",    mode = "REQUIRED" },
    { name = "seed_url",           type = "STRING",    mode = "REQUIRED" },
    { name = "is_targeted",        type = "BOOL",      mode = "REQUIRED" },
    { name = "started_at",         type = "TIMESTAMP", mode = "REQUIRED" },
    { name = "duration_ms",        type = "INTEGER",   mode = "NULLABLE" },
    { name = "pages_visited",      type = "INTEGER",   mode = "NULLABLE" },
    { name = "jobs_raw",           type = "INTEGER",   mode = "NULLABLE" },
    { name = "jobs_valid",         type = "INTEGER",   mode = "NULLABLE" },
    { name = "jobs_tech",          type = "INTEGER",   mode = "NULLABLE" },
    { name = "jobs_final",         type = "INTEGER",   mode = "NULLABLE" },
    { name = "confidence_avg",     type = "FLOAT",     mode = "NULLABLE" },
    { name = "ats_provider",       type = "STRING",    mode = "NULLABLE" },
    { name = "ats_identifier",     type = "STRING",    mode = "NULLABLE" },
    { name = "ats_direct_url",     type = "STRING",    mode = "NULLABLE" },
    { name = "pagination_pattern", type = "STRING",    mode = "NULLABLE" },
    { name = "status",             type = "STRING",    mode = "REQUIRED" },
    { name = "error_message",      type = "STRING",    mode = "NULLABLE" },
    { name = "model_used",         type = "STRING",    mode = "NULLABLE" },
  ])
}
