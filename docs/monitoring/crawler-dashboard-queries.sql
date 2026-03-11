# Crawler Monitoring Dashboard - BigQuery Queries

**Dataset**: `crawler_analytics`  
**Table**: `crawl_results`

---

## 1. Daily Crawl Overview

```sql
-- Daily crawl summary for the last 30 days
SELECT
    DATE(crawl_date) as crawl_day,
    COUNT(*) as total_crawls,
    COUNTIF(success) as successful_crawls,
    ROUND(COUNTIF(success) * 100.0 / COUNT(*), 2) as success_rate_pct,
    ROUND(AVG(extraction_confidence), 3) as avg_confidence,
    ROUND(AVG(quality_score), 3) as avg_quality,
    ROUND(AVG(jobs_extracted), 1) as avg_jobs_per_company,
    COUNTIF(anomaly_detected) as anomalies
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
GROUP BY crawl_day
ORDER BY crawl_day DESC;
```

---

## 2. ATS Provider Distribution

```sql
-- Which ATS providers are most common among our companies?
SELECT
    COALESCE(detected_ats_provider, 'UNKNOWN') as ats_provider,
    COUNT(DISTINCT company_id) as company_count,
    ROUND(AVG(extraction_confidence), 3) as avg_confidence,
    ROUND(AVG(quality_score), 3) as avg_quality,
    ROUND(AVG(jobs_extracted), 1) as avg_jobs_per_company
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
GROUP BY ats_provider
ORDER BY company_count DESC;
```

---

## 3. Companies Needing Attention (Low Quality)

```sql
-- Companies with poor extraction quality needing prompt optimization
SELECT
    company_id,
    COUNT(*) as crawl_count,
    ROUND(AVG(quality_score), 3) as avg_quality,
    ROUND(AVG(extraction_confidence), 3) as avg_confidence,
    ROUND(AVG(jobs_extracted), 1) as avg_jobs,
    STRING_AGG(DISTINCT quality_tier, ', ' ORDER BY quality_tier) as quality_tiers,
    COUNTIF(anomaly_detected) as anomaly_count
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
GROUP BY company_id
HAVING AVG(quality_score) < 0.5
ORDER BY avg_quality ASC
LIMIT 50;
```

---

## 4. Extraction Quality Trends

```sql
-- Quality score trends over time (7-day rolling average)
SELECT
    DATE(crawl_date) as crawl_day,
    ROUND(AVG(quality_score), 3) as daily_quality,
    ROUND(AVG(AVG(quality_score)) OVER (
        ORDER BY DATE(crawl_date)
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ), 3) as rolling_7day_quality,
    ROUND(AVG(extraction_confidence), 3) as daily_confidence,
    COUNT(*) as crawls_that_day
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 90 DAY)
GROUP BY crawl_day
ORDER BY crawl_day DESC;
```

---

## 5. Crawl Performance Metrics

```sql
-- Crawl duration and performance
SELECT
    DATE(crawl_date) as crawl_day,
    ROUND(AVG(duration_ms) / 1000, 2) as avg_duration_sec,
    APPROX_QUANTILES(duration_ms, 4)[OFFSET(3)] as p75_duration_ms,
    APPROX_QUANTILES(duration_ms, 4)[OFFSET(4)] as p95_duration_ms,
    ROUND(AVG(pages_visited), 1) as avg_pages_per_crawl,
    COUNT(*) as total_crawls
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
  AND success = TRUE
GROUP BY crawl_day
ORDER BY crawl_day DESC;
```

---

## 6. Error Analysis

```sql
-- Most common crawl errors
SELECT
    error_message,
    COUNT(*) as error_count,
    COUNT(DISTINCT company_id) as affected_companies,
    DATE(MIN(crawl_date)) as first_occurrence,
    DATE(MAX(crawl_date)) as last_occurrence
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
  AND success = FALSE
  AND error_message IS NOT NULL
GROUP BY error_message
ORDER BY error_count DESC
LIMIT 20;
```

---

## 7. Job Extraction Volume

```sql
-- Total jobs extracted per day
SELECT
    DATE(crawl_date) as crawl_day,
    SUM(jobs_extracted) as total_jobs,
    COUNT(DISTINCT company_id) as companies_crawled,
    ROUND(SUM(jobs_extracted) * 100.0 / SUM(SUM(jobs_extracted)) OVER (), 2) as pct_of_total
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
  AND success = TRUE
GROUP BY crawl_day
ORDER BY crawl_day DESC;
```

---

## 8. ATS Detection Confidence

```sql
-- Confidence breakdown by ATS provider
SELECT
    detected_ats_provider,
    COUNT(*) as detections,
    ROUND(AVG(confidence) * 100, 1) as avg_confidence_pct,
    COUNTIF(confidence >= 0.9) as high_confidence,
    COUNTIF(confidence BETWEEN 0.7 AND 0.9) as medium_confidence,
    COUNTIF(confidence < 0.7) as low_confidence
FROM (
    SELECT
        detected_ats_provider,
        CASE
            WHEN detected_ats_provider IS NULL THEN 0
            WHEN detected_ats_identifier IS NOT NULL THEN 0.95
            ELSE 0.7
        END as confidence
    FROM `PROJECT_ID.crawler_analytics.crawl_results`
    WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
)
GROUP BY detected_ats_provider
ORDER BY detections DESC;
```

---

## 9. Company Crawl History

```sql
-- Full crawl history for a specific company
SELECT
    crawl_date,
    pages_visited,
    jobs_extracted,
    detected_ats_provider,
    detected_ats_identifier,
    extraction_confidence,
    quality_score,
    quality_tier,
    anomaly_detected,
    duration_ms,
    success,
    error_message
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE company_id = 'COMPANY_ID'
ORDER BY crawl_date DESC
LIMIT 100;
```

---

## 10. Anomaly Detection Summary

```sql
-- Summary of detected anomalies
SELECT
    DATE(crawl_date) as crawl_day,
    COUNTIF(quality_score < 0.4) as poor_quality_count,
    COUNTIF(anomaly_detected AND REGEXP_CONTAINS(error_message, 'dropped')) as job_count_drops,
    COUNTIF(anomaly_detected AND REGEXP_CONTAINS(error_message, 'increased')) as job_count_spikes,
    COUNTIF(field_completion_rate < 0.5) as low_field_completion,
    COUNTIF(consistency_score < 0.7) as low_consistency
FROM `PROJECT_ID.crawler_analytics.crawl_results`
WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
GROUP BY crawl_day
ORDER BY crawl_day DESC;
```

---

## Dashboard Configuration (Looker Studio / Data Studio)

### Recommended Visualizations

1. **Time Series Chart**: Daily success rate + quality score (7-day rolling)
2. **Bar Chart**: ATS provider distribution
3. **Table**: Companies needing attention (quality < 0.5)
4. **Scorecard**: Today's stats (crawls, success rate, avg quality)
5. **Heat Map**: Quality score by company over time
6. **Pie Chart**: Anomaly types breakdown

### Alert Thresholds

| Metric | Warning | Critical |
|:---|:---|:---|
| Success Rate | < 90% | < 80% |
| Avg Quality Score | < 0.6 | < 0.4 |
| Avg Confidence | < 0.7 | < 0.5 |
| Anomaly Rate | > 10% | > 20% |
| Avg Duration | > 60s | > 120s |
