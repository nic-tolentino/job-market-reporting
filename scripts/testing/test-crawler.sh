#!/bin/bash

# Crawler Service Test Script
# Tests the deployed crawler service

set -e

# Configuration
CRAWLER_URL="${CRAWLER_SERVICE_URL:-http://localhost:8080}"
TEST_COMPANY="test-$(date +%s)"

echo "🧪 Crawler Service Test Suite"
echo "=============================="
echo "Service URL: $CRAWLER_URL"
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
echo "--------------------"
HEALTH_RESPONSE=$(curl -s "$CRAWLER_URL/health")
echo "$HEALTH_RESPONSE" | jq .

if echo "$HEALTH_RESPONSE" | jq -e '.status == "ok"' > /dev/null; then
    echo "✅ Health check passed"
else
    echo "❌ Health check failed"
    exit 1
fi
echo ""

# Test 2: Empty Career Page
echo "Test 2: Empty Career Page"
echo "-------------------------"
EMPTY_RESPONSE=$(curl -s -X POST "$CRAWLER_URL/crawl" \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "'$TEST_COMPANY'-empty",
    "url": "https://example.com/careers"
  }')
echo "$EMPTY_RESPONSE" | jq '.companyId, .crawlMeta.pagesVisited, .jobs | length'

if echo "$EMPTY_RESPONSE" | jq -e '.companyId != null' > /dev/null; then
    echo "✅ Empty page crawl completed"
else
    echo "❌ Empty page crawl failed"
    echo "$EMPTY_RESPONSE"
fi
echo ""

# Test 3: Batch Crawl
echo "Test 3: Batch Crawl (2 companies)"
echo "----------------------------------"
BATCH_RESPONSE=$(curl -s -X POST "$CRAWLER_URL/crawl/batch" \
  -H "Content-Type: application/json" \
  -d '{
    "requests": [
      {"companyId": "'$TEST_COMPANY'-1", "url": "https://example.com/careers"},
      {"companyId": "'$TEST_COMPANY'-2", "url": "https://example.com/jobs"}
    ]
  }')
echo "$BATCH_RESPONSE" | jq '.results | length'

if echo "$BATCH_RESPONSE" | jq -e '.results | length == 2' > /dev/null; then
    echo "✅ Batch crawl completed"
else
    echo "❌ Batch crawl failed"
    echo "$BATCH_RESPONSE"
fi
echo ""

# Test 4: Validation
echo "Test 4: Request Validation"
echo "--------------------------"
INVALID_RESPONSE=$(curl -s -X POST "$CRAWLER_URL/crawl" \
  -H "Content-Type: application/json" \
  -d '{
    "companyId": "test",
    "url": "not-a-valid-url"
  }')
echo "$INVALID_RESPONSE" | jq .

if echo "$INVALID_RESPONSE" | jq -e '.error != null' > /dev/null; then
    echo "✅ Validation working"
else
    echo "⚠️  Validation may not be working correctly"
fi
echo ""

# Test 5: 404 Handling
echo "Test 5: 404 Handling"
echo "--------------------"
NOT_FOUND_RESPONSE=$(curl -s -w "\n%{http_code}" "$CRAWLER_URL/unknown")
HTTP_CODE=$(echo "$NOT_FOUND_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "404" ]; then
    echo "✅ 404 handling working"
else
    echo "❌ 404 handling failed (got HTTP $HTTP_CODE)"
fi
echo ""

# Summary
echo "=============================="
echo "📊 Test Summary"
echo "=============================="
echo "All basic tests completed!"
echo ""
echo "Next steps:"
echo "1. Test with real company career pages"
echo "2. Check BigQuery for logged results"
echo "3. Monitor quality scores"
echo ""
echo "BigQuery query to check results:"
echo "SELECT * FROM \`tech-market-insights.crawler_analytics.crawl_results\`"
echo "WHERE company_id LIKE '%$TEST_COMPANY%'"
echo "ORDER BY crawl_date DESC"
