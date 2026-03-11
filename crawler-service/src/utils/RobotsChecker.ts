import robotsParser from 'robots-parser';

type RobotsParser = ReturnType<typeof robotsParser>;

/**
 * Cache entry for robots.txt parsers
 */
interface RobotsCacheEntry {
  parser: RobotsParser;
  fetchedAt: Date;
  allowed: boolean;
}

/**
 * Robots.txt compliance checker
 * 
 * Caches robots.txt results to avoid repeated fetches for the same domain.
 * Respects crawl-delay directives and disallow rules.
 */
export class RobotsChecker {
  private cache = new Map<string, RobotsCacheEntry>();
  private cacheTtlMs: number;
  private userAgent: string;
  private defaultCrawlDelay: number;

  constructor(
    userAgent: string = 'DevAssemblyBot',
    cacheTtlMinutes: number = 60,
    defaultCrawlDelay: number = 500 // ms
  ) {
    this.userAgent = userAgent;
    this.cacheTtlMs = cacheTtlMinutes * 60 * 1000;
    this.defaultCrawlDelay = defaultCrawlDelay;
  }

  /**
   * Checks if a URL can be fetched according to robots.txt
   */
  async canFetch(url: string): Promise<{ allowed: boolean; crawlDelay?: number; reason?: string }> {
    const urlObj = new URL(url);
    const domain = urlObj.hostname;
    
    // Check cache first
    const cached = this.getCached(domain);
    if (cached) {
      if (!cached.allowed) {
        return { allowed: false, reason: 'Blocked by robots.txt (cached)' };
      }
      
      const crawlDelay = this.extractCrawlDelay(cached.parser, domain);
      return { allowed: true, crawlDelay };
    }

    // Fetch robots.txt
    try {
      const robotsUrl = `${urlObj.protocol}//${domain}/robots.txt`;
      const response = await fetch(robotsUrl, {
        signal: AbortSignal.timeout(5000)
      });

      if (!response.ok) {
        // No robots.txt found - allow crawling
        return this.cacheResult(domain, true, undefined);
      }

      const robotsTxt = await response.text();
      const baseUrl = `${urlObj.protocol}//${domain}`;
      const parser = robotsParser(baseUrl, robotsTxt);

      // Check if URL is allowed
      const allowed = parser.isAllowed(url);
      
      if (!allowed) {
        return this.cacheResult(domain, false, parser, 'Blocked by robots.txt');
      }

      const crawlDelay = this.extractCrawlDelay(parser, domain);
      return this.cacheResult(domain, true, parser, undefined, crawlDelay);
    } catch (error) {
      // Network error or timeout - allow with default delay
      console.warn(`Failed to fetch robots.txt for ${domain}:`, error);
      return { allowed: true, crawlDelay: this.defaultCrawlDelay };
    }
  }

  /**
   * Extracts crawl-delay from robots.txt
   */
  private extractCrawlDelay(parser: RobotsParser, domain: string): number {
    // robots-parser doesn't expose crawl-delay directly, so we use default
    // In production, you'd parse this from the robots.txt content
    const match = parser.toString().match(/Crawl-delay:\s*(\d+)/i);
    if (match) {
      return parseInt(match[1], 10) * 1000; // Convert to ms
    }
    return this.defaultCrawlDelay;
  }

  /**
   * Gets cached entry if valid
   */
  private getCached(domain: string): RobotsCacheEntry | undefined {
    const entry = this.cache.get(domain);
    if (!entry) return undefined;
    
    const age = Date.now() - entry.fetchedAt.getTime();
    if (age > this.cacheTtlMs) {
      this.cache.delete(domain);
      return undefined;
    }
    
    return entry;
  }

  /**
   * Caches and returns result
   */
  private cacheResult(
    domain: string,
    allowed: boolean,
    parser?: RobotsParser,
    reason?: string,
    crawlDelay?: number
  ): { allowed: boolean; crawlDelay?: number; reason?: string } {
    if (parser) {
      this.cache.set(domain, {
        parser,
        fetchedAt: new Date(),
        allowed
      });
    }
    
    return { allowed, crawlDelay, reason };
  }

  /**
   * Clears the cache (useful for testing)
   */
  clearCache(): void {
    this.cache.clear();
  }

  /**
   * Gets cache statistics
   */
  getStats(): { size: number; entries: Array<{ domain: string; allowed: boolean; age: number }> } {
    const now = Date.now();
    return {
      size: this.cache.size,
      entries: Array.from(this.cache.entries()).map(([domain, entry]) => ({
        domain,
        allowed: entry.allowed,
        age: now - entry.fetchedAt.getTime()
      }))
    };
  }
}
