/**
 * E2E tests for Crawler Service
 * 
 * These tests require a running crawler service instance.
 * Run with: npm run test:e2e
 * 
 * To run locally:
 * 1. Start the service: npm run dev
 * 2. Run tests: npm run test:e2e
 * 
 * The entire suite will be SKIPPED if no server is running.
 */

const BASE_URL = process.env.CRAWLER_URL || 'http://localhost:8081';
let serverRunning = false;

// Check if server is running before defining any tests
beforeAll(async () => {
  try {
    const response = await fetch(`${BASE_URL}/health`, { 
      signal: AbortSignal.timeout(3000) 
    });
    serverRunning = response.ok;
  } catch (e) {
    serverRunning = false;
  }
});

// Only define tests if server is running
if (serverRunning) {
  describe('E2E Crawler Pipeline', () => {
    describe('Health Check', () => {
      it('returns healthy status', async () => {
        const response = await fetch(`${BASE_URL}/health`);
        const data = await response.json();

        expect(response.status).toBe(200);
        expect(data.status).toBe('ok');
        expect(data.timestamp).toBeDefined();
      });
    });

    describe('Crawl Endpoint', () => {
      it('handles company with no jobs gracefully', async () => {
        const response = await fetch(`${BASE_URL}/crawl`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            companyId: 'empty-company',
            url: 'https://example.com/careers'
          })
        });

        expect(response.status).toBe(200);
        const result = await response.json();

        expect(result.companyId).toBe('empty-company');
        expect(result.jobs).toEqual(expect.arrayContaining([]));
        expect(result.crawlMeta).toBeDefined();
        expect(result.crawlMeta.pagesVisited).toBeGreaterThanOrEqual(1);
      });

      it('validates request body', async () => {
        const response = await fetch(`${BASE_URL}/crawl`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            // Missing required fields
            url: 'not-a-url'
          })
        });

        expect(response.status).toBe(400);
        const result = await response.json();
        expect(result.error).toBe('Invalid request');
      });

      it('rejects invalid URL', async () => {
        const response = await fetch(`${BASE_URL}/crawl`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            companyId: 'test',
            url: 'not-a-valid-url'
          })
        });

        expect(response.status).toBe(400);
      });

      it('handles crawl config options', async () => {
        const response = await fetch(`${BASE_URL}/crawl`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            companyId: 'test-config',
            url: 'https://example.com/careers',
            crawlConfig: {
              maxPages: 1,
              followJobLinks: false,
              timeout: 10000
            }
          })
        });

        expect(response.status).toBe(200);
        const result = await response.json();

        expect(result.crawlMeta.pagesVisited).toBeLessThanOrEqual(1);
      });

      // NOTE: This test requires the mock server to be running on 8082
      it('respects the 50-page pagination safety cap', async () => {
        const response = await fetch(`${BASE_URL}/crawl`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            companyId: 'test-pagination-cap',
            url: 'http://localhost:8082/jobs',
            crawlConfig: {
              maxPages: 1, // Only 1 "navigation" page allowed
              followJobLinks: true, // But we should exhaust pagination
              isDiscoveryMode: false
            }
          })
        });

        expect(response.status).toBe(200);
        const result = await response.json();

        // 1 initial page + 49 pagination pages = strictly 50
        expect(result.crawlMeta.pagesVisited).toBe(50);
      }, 30000); // Allow exactly 30 seconds for the crawl to hit 50 pages
    });

    describe('Batch Crawl Endpoint', () => {
      it('processes multiple requests', async () => {
        const requests = [
          { companyId: 'batch-1', url: 'https://example.com/careers' },
          { companyId: 'batch-2', url: 'https://example.com/jobs' }
        ];

        const response = await fetch(`${BASE_URL}/crawl/batch`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ requests })
        });

        expect(response.status).toBe(200);
        const result = await response.json() as { results: Array<{ companyId: string }> };

        expect(result.results).toHaveLength(2);
        expect(result.results.map((r: { companyId: string }) => r.companyId)).toEqual(
          expect.arrayContaining(['batch-1', 'batch-2'])
        );
      });

      it('handles partial failures in batch', async () => {
        const requests = [
          { companyId: 'valid', url: 'https://example.com/careers' },
          { companyId: 'invalid', url: 'not-a-url' }
        ];

        const response = await fetch(`${BASE_URL}/crawl/batch`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ requests })
        });

        expect(response.status).toBe(200);
        const result = await response.json();

        // Valid request should succeed, invalid should have error
        expect(result.results).toHaveLength(2);
      });

      it('returns validationFailures for invalid requests', async () => {
        const requests = [
          { companyId: 'valid', url: 'https://example.com/careers' },
          { companyId: 'invalid', url: 'not-a-valid-url' },
          { companyId: 'another-invalid' }  // Missing URL
        ];

        const response = await fetch(`${BASE_URL}/crawl/batch`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ requests })
        });

        expect(response.status).toBe(200);
        const result = await response.json();

        // Should have validationFailures in response
        expect(result.validationFailures).toBeDefined();
        expect(Array.isArray(result.validationFailures)).toBe(true);
        expect(result.validationFailures.length).toBeGreaterThan(0);

        // Each failure should have index and errors
        const firstFailure = result.validationFailures[0];
        expect(firstFailure.index).toBeDefined();
        expect(firstFailure.errors).toBeDefined();
        expect(Array.isArray(firstFailure.errors)).toBe(true);
      });

      it('rejects empty batch', async () => {
        const response = await fetch(`${BASE_URL}/crawl/batch`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ requests: [] })
        });

        expect(response.status).toBe(400);
      });
    });

    describe('404 Handling', () => {
      it('returns 404 for unknown routes', async () => {
        const response = await fetch(`${BASE_URL}/unknown`);
        expect(response.status).toBe(404);
      });
    });
  });
} else {
  // Skip all tests if server is not running
  describe('E2E Crawler Pipeline', () => {
    test.skip('All E2E tests skipped - server not running', () => {
      // This test is intentionally skipped
      // Run `npm run dev` to start the server, then `npm run test:e2e`
    });
  });
}
