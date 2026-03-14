# Crawler-Backend Contract & Test Coverage

**March 14, 2026** | **Status: Pending Implementation**

---

## Problem

The crawler service (Node.js/TypeScript) and Kotlin backend share a JSON contract (`CrawlMeta`) with no enforced type safety across the boundary. The Kotlin `CrawlMetaDto` is hand-maintained. A renamed field, changed enum value, or nullability assumption in either service fails silently at runtime — not at compile time.

This is the highest-risk seam in the current architecture.

---

## Recommended Architecture Changes

### 1. JSON Schema Snapshot + Cross-Service Fixture Test (Do First)
Publish a `crawl-meta-schema.json` alongside the crawler service. Both the crawler's test suite and the Kotlin backend validate incoming/outgoing payloads against this file.

Commit a `fixtures/sample-crawl-response.json` to the crawler repo. The Kotlin backend's test suite deserializes this fixture using `CrawlMetaDto` and asserts required fields are non-null. When `CrawlMeta` changes, the fixture must be updated — making drift visible as a git diff.

```kotlin
@Test
fun `crawler response can be deserialized into CrawlMetaDto`() {
    val rawJson = File("fixtures/sample-crawl-response.json").readText()
    val dto = objectMapper.readValue(rawJson, CrawlMetaDto::class.java)
    assertNotNull(dto.companyId)
    assertNotNull(dto.exitState)
    assertNotNull(dto.pagesVisited)
}
```

### 2. `schemaVersion` Field on `CrawlMeta`
Add a `schemaVersion` field to the crawler response and Kotlin DTO. The backend logs a warning (or rejects) responses where the version doesn't match what it expects.

```typescript
// crawler types.ts
schemaVersion: "1.0"
```
```kotlin
// CrawlMetaDto.kt
val schemaVersion: String
```

Trivial to add now, valuable when the schema evolves.

### 3. Backend Deserialization Guard
Wrap `CrawlMetaDto` deserialization in explicit validation rather than trusting Jackson's silent null-defaulting behaviour.

```kotlin
fun handleCrawlResult(raw: String): CrawlMetaDto {
    val dto = try {
        objectMapper.readValue(raw, CrawlMetaDto::class.java)
    } catch (e: JsonMappingException) {
        logger.error("CRAWL_META_PARSE_FAILURE raw={} error={}", raw, e.message)
        throw CrawlContractViolationException(e)
    }

    requireNotNull(dto.companyId) { "companyId missing from CrawlMeta" }
    requireNotNull(dto.exitState) { "exitState missing from CrawlMeta" }
    return dto
}
```

### 4. OpenAPI Spec for Crawler Service
Define a formal OpenAPI spec for the crawler HTTP API, including the full `CrawlMeta` response schema. Use `openapi-generator` to auto-generate the Kotlin client/DTOs.

This eliminates hand-maintained `CrawlMetaDto` entirely — when the crawler changes its response shape, the spec is updated, the Kotlin client is regenerated, and the mismatch becomes a compile error.

### 5. Pact Consumer-Driven Contract Tests
The Kotlin backend (consumer) publishes its expectations as a Pact contract. The crawler (provider) verifies against it in CI.

If the crawler renames a field or adds an enum value, Pact verification fails in the crawler's CI pipeline before deployment — not after the backend silently misreads responses.

Recommended when the system scales to multiple consumers of the crawler API.

---

## Priority Order

| Priority | Change | Effort | Value |
|---|---|---|---|
| 1 | JSON Schema snapshot + fixture test | Low | High — immediately closes drift risk |
| 2 | `schemaVersion` field | Trivial | Medium — runtime safety net |
| 3 | Backend deserialization guard | Low | Medium — explicit failure surface |
| 4 | OpenAPI spec + generated Kotlin client | Medium | High — durable long-term solution |
| 5 | Pact contract tests | High | High — right answer at scale |

---

## Missing Crawler-Side Test Coverage

These scenarios are implemented in code but only covered by mocked unit tests, not integration tests against real crawl behaviour.

| Test | Current State | Required |
|---|---|---|
| Pagination cap (50 pages) | Logic exists, untested | Integration test verifying `PAGINATION_TRUNCATED` fires and crawl stops |
| Contraction signal | Mock only | Test with `seedData.lastKnownPageCount` > observed pages |
| `tech-filtered` secondary validation | No test | Verify a mislabelled general seed still passes keyword gate |
| Growth signal (`JOB_COUNT_CHANGE`) | Mock only | Integration test against fixture with known job count |

---

## When to Action

Raise and implement before the Kotlin backend Phase 3 integration is built out and before the `CrawlMeta` contract is considered stable. The cross-service fixture test (#1) should be implemented as part of Phase 3 backend work.
