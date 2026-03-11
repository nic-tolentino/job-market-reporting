# Crawler Service Alerting Configuration

**Service**: Crawler Service (Cloud Run)  
**Monitoring**: Cloud Monitoring + BigQuery  
**Notification**: Slack + Email

---

## Alert Policies

### 1. High Crawl Failure Rate

**Condition**: Crawl success rate drops below threshold

```yaml
alertPolicy:
  displayName: "Crawler - High Failure Rate"
  conditions:
    - displayName: "Success rate < 80% (1hr)"
      conditionThreshold:
        filter: 'resource.type="cloud_run_revision" AND metric.type="custom.googleapis.com/crawler/success_rate"'
        comparison: COMPARISON_LT
        thresholdValue: 0.8
        duration: 300s  # 5 minutes
        aggregations:
          - alignmentPeriod: 3600s  # 1 hour
            perSeriesAligner: ALIGN_MEAN
  notificationChannels:
    - "projects/PROJECT_ID/notificationChannels/SLACK_CHANNEL"
  documentation:
    content: |
      Crawler failure rate exceeded 20% in the last hour.
      
      Check:
      - Cloud Run logs for error patterns
      - Gemini API status
      - Target site availability
      
      Dashboard: [LINK_TO_DASHBOARD]
```

---

### 2. Low Extraction Quality

**Condition**: Average quality score drops below threshold

```yaml
alertPolicy:
  displayName: "Crawler - Low Extraction Quality"
  conditions:
    - displayName: "Avg quality score < 0.5 (6hr)"
      conditionThreshold:
        filter: 'metric.type="custom.googleapis.com/crawler/quality_score"'
        comparison: COMPARISON_LT
        thresholdValue: 0.5
        duration: 21600s  # 6 hours
        aggregations:
          - alignmentPeriod: 21600s
            perSeriesAligner: ALIGN_MEAN
  notificationChannels:
    - "projects/PROJECT_ID/notificationChannels/EMAIL_TEAM"
  documentation:
    content: |
      Extraction quality score dropped below 0.5.
      
      Actions:
      - Review companies with quality < 0.4 in BigQuery
      - Check for prompt injection patterns
      - Consider prompt optimization for affected companies
      
      Query: See docs/monitoring/crawler-dashboard-queries.sql (Query #3)
```

---

### 3. Gemini API Errors

**Condition**: Gemini API error rate exceeds threshold

```yaml
alertPolicy:
  displayName: "Crawler - Gemini API Errors"
  conditions:
    - displayName: "Gemini error rate > 5% (15min)"
      conditionThreshold:
        filter: 'resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/request/count" AND metric.labels.response_code=~"5.."'
        comparison: COMPARISON_GT
        thresholdValue: 0.05
        duration: 900s
        aggregations:
          - alignmentPeriod: 900s
            perSeriesAligner: ALIGN_PERCENTAGE
  notificationChannels:
    - "projects/PROJECT_ID/notificationChannels/SLACK_CHANNEL"
  documentation:
    content: |
      Gemini API returning errors for > 5% of requests.
      
      Check:
      - Google Cloud Vertex AI status
      - API quota limits
      - Circuit breaker status
      
      Fallback: Crawler will skip extraction and retry on next run
```

---

### 4. High Latency

**Condition**: Crawl duration exceeds acceptable limits

```yaml
alertPolicy:
  displayName: "Crawler - High Latency"
  conditions:
    - displayName: "P95 duration > 60s (30min)"
      conditionPercentile:
        filter: 'resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/request_latencies"'
        percentileValue: 95
        thresholdValue: 60000  # milliseconds
        duration: 1800s
        aggregations:
          - alignmentPeriod: 1800s
            perSeriesAligner: ALIGN_PERCENTILE_95
  notificationChannels:
    - "projects/PROJECT_ID/notificationChannels/SLACK_CHANNEL"
  documentation:
    content: |
      Crawler P95 latency exceeded 60 seconds.
      
      Check:
      - Cloud Run instance count
      - Target site response times
      - Network connectivity
      
      Consider: Increase concurrency or add rate limiting
```

---

### 5. Anomaly Spike

**Condition**: Unusual number of extraction anomalies detected

```yaml
alertPolicy:
  displayName: "Crawler - Anomaly Spike"
  conditions:
    - displayName: "Anomaly rate > 15% (1hr)"
      conditionThreshold:
        filter: 'metric.type="custom.googleapis.com/crawler/anomaly_rate"'
        comparison: COMPARISON_GT
        thresholdValue: 0.15
        duration: 3600s
        aggregations:
          - alignmentPeriod: 3600s
            perSeriesAligner: ALIGN_MEAN
  notificationChannels:
    - "projects/PROJECT_ID/notificationChannels/EMAIL_TEAM"
  documentation:
    content: |
      Unusual spike in extraction anomalies detected.
      
      Review:
      - Anomaly types in BigQuery (Query #10)
      - Recent prompt changes
      - Target site structure changes
      
      Action: May need prompt adjustments for affected companies
```

---

### 6. Cloud Run Resource Exhaustion

**Condition**: Cloud Run approaching resource limits

```yaml
alertPolicy:
  displayName: "Crawler - Resource Exhaustion"
  conditions:
    - displayName: "Container memory > 90% (5min)"
      conditionThreshold:
        filter: 'resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/container/memory/utilization"'
        comparison: COMPARISON_GT
        thresholdValue: 0.9
        duration: 300s
        aggregations:
          - alignmentPeriod: 300s
            perSeriesAligner: ALIGN_MAX
  notificationChannels:
    - "projects/PROJECT_ID/notificationChannels/SLACK_CHANNEL"
  documentation:
    content: |
      Cloud Run container memory utilization exceeded 90%.
      
      Actions:
      - Check for memory leaks
      - Reduce concurrency setting
      - Increase memory allocation
      
      Current limit: 2GB per instance
```

---

## Notification Channels

### Slack Integration

```yaml
notificationChannel:
  displayName: "Crawler Alerts - Engineering"
  type: "slack"
  labels:
    channel_name: "#crawler-alerts"
  description: "Critical crawler service alerts"
```

### Email Integration

```yaml
notificationChannel:
  displayName: "Crawler Team - Email"
  type: "email"
  labels:
    email_address: "engineering@company.com"
  description: "Non-critical alerts and daily summaries"
```

---

## Runbook: Responding to Alerts

### High Failure Rate

1. Check Cloud Run logs for error patterns
2. Verify Gemini API status at status.cloud.google.com
3. Check if specific companies or all are failing
4. If Gemini API issue: Wait for recovery, retries will handle it
5. If specific sites: Check robots.txt, rate limiting

### Low Quality Score

1. Run BigQuery Query #3 to identify affected companies
2. Review extraction hints for those companies
3. Check for prompt injection patterns in raw HTML
4. Update company-specific prompts if needed
5. Re-crawl affected companies

### High Latency

1. Check Cloud Run concurrent request count
2. Review target site response times
3. Consider reducing maxConcurrency setting
4. Check for network issues

---

## Dashboard Links

- **Main Dashboard**: [Looker Studio Link]
- **BigQuery Console**: [BQ Link]
- **Cloud Run Metrics**: [Cloud Run Link]
- **Error Reporting**: [Error Reporting Link]
