# Phase 3: Cloud Tasks Integration

**Goal**: Implement distributed crawling with proper rate limiting using Cloud Tasks

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Cloud Scheduler (nightly 2am NZST)                         │
│      │                                                       │
│      ▼                                                       │
│  Cloud Tasks Queue (crawler-queue)                          │
│      │  Rate limit: 2 req/sec per domain                    │
│      │  Max concurrent: 100                                 │
│      ▼                                                       │
│  Cloud Run (Crawler Service)                                │
│      │  POST /crawl/task                                    │
│      │  { companyId, url, crawlConfig, attempt }            │
│      ▼                                                       │
│  Crawl Company → Log to BQ → Update Manifest                │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Steps

### Step 1: Create Cloud Tasks Queue

```bash
# Create queue with rate limiting
gcloud tasks queues create crawler-queue \
  --location=us-central1 \
  --max-concurrent-dispatches=100 \
  --max-dispatches-per-second=10 \
  --max-burst-size=20

# Configure retry policy
gcloud tasks queues update crawler-queue \
  --location=us-central1 \
  --max-attempts=3 \
  --min-backoff=5s \
  --max-backoff=1m \
  --max-doublings=5
```

### Step 2: Update CrawlerBatchSyncService

```kotlin
@Service
class CrawlerBatchSyncService(
    private val cloudTasksClient: CloudTasksClient,
    @Value("\${gcp.project-id}") private val projectId: String,
    @Value("\${gcp.location-id}") private val locationId: String,
    // ... other dependencies
) {
    
    fun syncAllCompanies() {
        val companies = getCompaniesToCrawl()
        val queuePath = QueueName.of(projectId, locationId, "crawler-queue").toString()
        
        log.info("Enqueueing ${companies.size} crawl tasks")
        
        companies.forEach { companyId ->
            val config = crawlConfigService.loadConfig(companyId)
            
            val task = Task.newBuilder()
                .setHttpRequest(
                    HttpRequest.newBuilder()
                        .setHttpMethod(HttpMethod.POST)
                        .setUrl("$crawlerServiceUrl/crawl/task")
                        .setHeaders(
                            mutableMapOf(
                                "Content-Type" to "application/json"
                            ).toMapHeaders()
                        )
                        .setBody(
                            ByteString.copyFromUtf8(
                                objectMapper.writeValueAsString(
                                    CrawlTaskRequest(
                                        companyId = companyId,
                                        url = config.careersUrl,
                                        crawlConfig = CrawlConfig(
                                            maxPages = config.maxPages,
                                            extractionHints = config.extractionHints
                                        ),
                                        attempt = 1
                                    )
                                )
                            )
                        )
                        .build()
                )
                .build()
            
            cloudTasksClient.createTask(CreateTaskRequest.newBuilder()
                .setParent(queuePath)
                .setTask(task)
                .build())
        }
        
        log.info("All crawl tasks enqueued successfully")
    }
}
```

### Step 3: Add Task Handler Endpoint

```kotlin
@RestController
@RequestMapping("/crawl")
class CrawlerTaskController(
    private val crawlerService: CrawlerService,
    private val metadataLogger: CrawlMetadataLogger,
    private val crawlConfigService: CrawlConfigService,
    private val qualityScorer: ExtractionQualityScorer
) {
    
    @PostMapping("/task")
    fun handleCrawlTask(@RequestBody request: CrawlTaskRequest): CrawlTaskResponse {
        val startTime = System.currentTimeMillis()
        
        try {
            log.info("Processing crawl task for ${request.companyId} (attempt ${request.attempt})")
            
            // Execute crawl
            val result = crawlerService.crawl(
                CrawlRequest(
                    companyId = request.companyId,
                    url = request.url,
                    crawlConfig = request.crawlConfig
                )
            )
            
            // Calculate quality score
            val qualityScore = qualityScorer.calculateQualityScore(result.jobs)
            
            // Log to BigQuery
            metadataLogger.logCrawlResult(
                companyId = request.companyId,
                crawlDate = Instant.now(),
                pagesVisited = result.crawlMeta.pagesVisited,
                jobsExtracted = result.jobs.size,
                detectedAtsProvider = result.crawlMeta.detectedAtsProvider,
                detectedAtsIdentifier = result.crawlMeta.detectedAtsIdentifier,
                extractionConfidence = result.crawlMeta.extractionConfidence,
                qualityScore = qualityScore,
                durationMs = System.currentTimeMillis() - startTime,
                success = true
            )
            
            // Update manifest
            crawlConfigService.updateAfterCrawl(
                companyId = request.companyId,
                jobsCount = result.jobs.size,
                quality = qualityScore.overall,
                detectedAtsProvider = result.crawlMeta.detectedAtsProvider,
                detectedAtsIdentifier = result.crawlMeta.detectedAtsIdentifier
            )
            
            return CrawlTaskResponse(
                success = true,
                jobsExtracted = result.jobs.size,
                qualityScore = qualityScore.overall
            )
            
        } catch (e: Exception) {
            log.error("Crawl task failed for ${request.companyId}: ${e.message}")
            
            // Log failure to BigQuery
            metadataLogger.logCrawlResult(
                companyId = request.companyId,
                crawlDate = Instant.now(),
                pagesVisited = 0,
                jobsExtracted = 0,
                detectedAtsProvider = null,
                detectedAtsIdentifier = null,
                extractionConfidence = 0.0,
                qualityScore = QualityScore(0.0, 0.0, 0.0, 0.0, true, listOf(e.message)),
                durationMs = System.currentTimeMillis() - startTime,
                success = false,
                errorMessage = e.message
            )
            
            throw e  // Cloud Tasks will retry
        }
    }
}

data class CrawlTaskRequest(
    val companyId: String,
    val url: String,
    val crawlConfig: CrawlConfig,
    val attempt: Int = 1
)

data class CrawlTaskResponse(
    val success: Boolean,
    val jobsExtracted: Int,
    val qualityScore: Double
)
```

### Step 4: Add Dead Letter Queue

```yaml
# cloud-tasks-dlq.yaml
queue: crawler-dlq
location: us-central1
rateLimits:
  maxConcurrentDispatches: 10
  maxDispatchesPerSecond: 1
retryConfig:
  maxAttempts: 1  # DLQ doesn't retry
```

```kotlin
// In CrawlerTaskController
@ExceptionHandler(TaskRetryExhaustedException::class)
fun handleRetryExhausted(@RequestBody request: CrawlTaskRequest) {
    log.error("Crawl task permanently failed for ${request.companyId} after ${request.attempt} attempts")
    
    // Send to dead letter queue for manual review
    dlqService.sendToDeadLetter(
        companyId = request.companyId,
        url = request.url,
        error = "Max retries exhausted",
        attempts = request.attempt
    )
}
```

### Step 5: Update Cloud Scheduler

```bash
# Update scheduler to trigger batch enqueue instead of direct crawl
gcloud scheduler jobs update http crawler-nightly \
  --schedule="0 2 * * *" \
  --uri="https://backend-service-xxx.a.run.app/api/internal/crawler/sync/all" \
  --http-method=POST \
  --oidc-service-account-email="scheduler@PROJECT_ID.iam.gserviceaccount.com" \
  --oidc-audience="backend-service-xxx.a.run.app"
```

---

## Configuration

### Queue Configuration

```yaml
# crawler-queue.yaml
name: crawler-queue
location: us-central1
rateLimits:
  maxConcurrentDispatches: 100  # Max parallel crawls
  maxDispatchesPerSecond: 10    # Global rate limit
retryConfig:
  maxAttempts: 3
  minBackoff: 5s
  maxBackoff: 1m
  maxDoublings: 5
  # Retry on: 5xx errors, timeouts, connection errors
```

### Per-Domain Rate Limiting

```kotlin
// In CrawlerService
class RateLimiter {
    private val domainLastRequest = ConcurrentHashMap<String, Long>()
    private val minDelayMs = 500L  // 2 req/sec per domain
    
    suspend fun waitForSlot(domain: String) {
        val lastRequest = domainLastRequest[domain] ?: 0
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequest
        
        if (elapsed < minDelayMs) {
            delay(minDelayMs - elapsed)
        }
        
        domainLastRequest[domain] = System.currentTimeMillis()
    }
}

// Usage in fetchPageContent
rateLimiter.waitForSlot(URI(url).host)
```

---

## Monitoring

### Task Metrics to Track

```sql
-- Task success rate
SELECT
    DATE(timestamp) as day,
    COUNT(*) as total_tasks,
    COUNTIF(response_code = 200) as successful,
    ROUND(COUNTIF(response_code = 200) * 100.0 / COUNT(*), 2) as success_rate
FROM `PROJECT_ID.cloud_tasks.task_attempts`
WHERE queue_name = 'crawler-queue'
  AND timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
GROUP BY day
ORDER BY day DESC;

-- Retry analysis
SELECT
    attempt_number,
    COUNT(*) as retries_at_this_level,
    AVG(duration_ms) as avg_duration
FROM `PROJECT_ID.cloud_tasks.task_attempts`
WHERE queue_name = 'crawler-queue'
  AND timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
GROUP BY attempt_number
ORDER BY attempt_number;
```

---

## Cost Analysis

### Cloud Tasks Pricing (as of 2026)

- First 1,000 tasks/day: Free
- Additional tasks: $0.0001 per task
- 1,257 companies × 30 days = 37,710 tasks/month
- Cost: ~$2.50/month

### Benefits vs Current Approach

| Metric | Current (Coroutines) | With Cloud Tasks |
|:---|:---|:---|
| Max concurrency | Limited by Cloud Run instances | 100 concurrent |
| Rate limiting | Manual implementation | Built-in per queue |
| Retry handling | Custom code | Built-in with backoff |
| Dead letter | Not implemented | Built-in DLQ |
| Visibility | Limited | Full task tracking |
| Cost | $0 | ~$2.50/month |

---

## Rollout Plan

### Phase 3.1: Parallel Testing (Week 1)
- [ ] Deploy Cloud Tasks integration alongside existing coroutine approach
- [ ] Route 10% of companies through Cloud Tasks
- [ ] Compare success rates and quality scores
- [ ] Monitor retry behavior

### Phase 3.2: Gradual Migration (Week 2)
- [ ] Increase Cloud Tasks traffic to 50%
- [ ] Fine-tune rate limits based on observed behavior
- [ ] Update alerting thresholds
- [ ] Document operational procedures

### Phase 3.3: Full Cutover (Week 3)
- [ ] Migrate 100% to Cloud Tasks
- [ ] Remove coroutine-based crawling code
- [ ] Update runbooks
- [ ] Conduct post-mortem review

---

## Success Criteria

- [ ] 99%+ task success rate (after retries)
- [ ] < 5% tasks sent to DLQ
- [ ] Avg crawl duration < 30s
- [ ] No rate limit violations
- [ ] All companies crawled within 3-hour window

---

**Estimated Implementation Time**: 2-3 days  
**Priority**: High (enables reliable scaling to 1,257 companies)
