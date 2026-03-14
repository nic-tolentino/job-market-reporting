-- BigQuery DDL for Crawler Operational State
-- Dataset: techmarket (as defined in Persistence Plan)

CREATE TABLE IF NOT EXISTS `tech-market-insights.techmarket.crawler_seeds` (
  company_id STRING NOT NULL OPTIONS(description="Unique ID of the company from the manifest"),
  url STRING NOT NULL OPTIONS(description="The seed URL provided to the crawler"),
  category STRING NOT NULL OPTIONS(description="The source-of-truth category from Git (general, tech-filtered, unknown)"),
  status STRING OPTIONS(description="ACTIVE | BLOCKED | TIMEOUT | STALE"),
  pagination_pattern STRING OPTIONS(description="The detected pagination regex or query param key"),
  last_known_job_count INT64 OPTIONS(description="Number of valid jobs found in the last successful run"),
  last_known_page_count INT64 OPTIONS(description="Number of pages visited in the last successful run"),
  last_crawled_at TIMESTAMP OPTIONS(description="Time of completion of the last crawl attempt"),
  last_duration_ms INT64 OPTIONS(description="Duration of the last crawl in milliseconds"),
  error_message STRING OPTIONS(description="Truncated error message from the crawler service"),
  consecutive_zero_yield_count INT64 DEFAULT 0 OPTIONS(description="Counter for yield-based staleness logic")
)
PRIMARY TAG (company_id, url) -- Note: BigQuery doesn't enforce PKs, but metadata is helpful
CLUSTER BY company_id, status;

-- Indexing/Optimization
-- Clustered by company_id for efficient per-company lookups and status for maintenance job filtering.
