import { RobotsChecker } from '../src/utils/RobotsChecker';

// Mock fetch globally for all tests
const mockFetch = jest.fn();
global.fetch = mockFetch as any;

describe('RobotsChecker', () => {
  let checker: RobotsChecker;

  beforeEach(() => {
    checker = new RobotsChecker('TestBot', 60, 500);
    mockFetch.mockClear();
  });

  afterEach(() => {
    checker.clearCache();
  });

  describe('canFetch', () => {
    it('allows crawling when no robots.txt exists', async () => {
      mockFetch.mockRejectedValue(new Error('Not found'));

      const result = await checker.canFetch('https://example.com/careers');

      expect(result.allowed).toBe(true);
      expect(result.crawlDelay).toBe(500);
    });

    it('allows crawling when robots.txt returns 404', async () => {
      mockFetch.mockResolvedValue({
        ok: false,
        status: 404
      });

      const result = await checker.canFetch('https://example.com/careers');

      expect(result.allowed).toBe(true);
    });

    it('blocks crawling when URL is disallowed', async () => {
      const robotsTxt = `
User-agent: *
Disallow: /careers
`;
      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      const result = await checker.canFetch('https://example.com/careers');

      expect(result.allowed).toBe(false);
      expect(result.reason).toContain('Blocked');
    });

    it('allows crawling when URL is not disallowed', async () => {
      const robotsTxt = `
User-agent: TestBot
Disallow: /private/
Allow: /careers
`;
      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      const result = await checker.canFetch('https://example.com/careers');

      expect(result.allowed).toBe(true);
    });

    it('respects wildcard user-agent', async () => {
      const robotsTxt = `
User-agent: *
Disallow: /admin/
`;
      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      const result = await checker.canFetch('https://example.com/careers');

      expect(result.allowed).toBe(true);
    });

    it('caches robots.txt results', async () => {
      const robotsTxt = `User-agent: *
Disallow: /blocked/`;

      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      await checker.canFetch('https://example.com/page');
      await checker.canFetch('https://example.com/page2');

      expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('respects cache TTL', async () => {
      const shortTtlChecker = new RobotsChecker('TestBot', 0.001, 500);

      const robotsTxt = 'User-agent: *\nDisallow:';
      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      await shortTtlChecker.canFetch('https://example.com/page');
      await new Promise(resolve => setTimeout(resolve, 100));
      await shortTtlChecker.canFetch('https://example.com/page2');

      expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('handles network timeouts gracefully', async () => {
      mockFetch.mockRejectedValue(new Error('Timeout'));

      const result = await checker.canFetch('https://example.com/careers');

      expect(result.allowed).toBe(true);
      expect(result.crawlDelay).toBe(500);
    });
  });

  describe('getStats', () => {
    it('returns cache statistics', async () => {
      const robotsTxt = 'User-agent: *\nDisallow:';
      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      await checker.canFetch('https://example1.com/careers');
      await checker.canFetch('https://example2.com/careers');

      const stats = checker.getStats();

      expect(stats.size).toBe(2);
      expect(stats.entries).toHaveLength(2);
      expect(stats.entries.map(e => e.domain)).toEqual(
        expect.arrayContaining(['example1.com', 'example2.com'])
      );
    });
  });

  describe('clearCache', () => {
    it('clears all cached entries', async () => {
      const robotsTxt = 'User-agent: *\nDisallow:';
      mockFetch.mockResolvedValue({
        ok: true,
        text: () => Promise.resolve(robotsTxt)
      });

      await checker.canFetch('https://example.com/careers');
      
      expect(checker.getStats().size).toBe(1);

      checker.clearCache();
      
      expect(checker.getStats().size).toBe(0);
    });
  });
});
