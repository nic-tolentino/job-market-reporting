import { PlaywrightCrawler, PlaywrightCrawlerOptions } from 'crawlee';
import { detectAts } from '../detector/AtsDetector';
import { extractContent, wrapContentForLlm } from '../extractor/ContentExtractor';
import { GeminiExtractionService, ExtractionConfig } from '../extraction/GeminiExtractionService';
import { validateJobs } from '../validator/JobValidator';
import { RobotsChecker } from '../utils/RobotsChecker';
import { CrawlRequest, CrawlResponse, CrawlMeta, NormalizedJob } from '../api/types';

/**
 * Main crawler service that orchestrates the crawl pipeline
 *
 * Creates a new PlaywrightCrawler instance per crawl request to avoid
 * concurrency issues and allow proper request isolation.
 */
export class CrawlerService {
  private extractionService: GeminiExtractionService;
  private robotsChecker: RobotsChecker;

  constructor(gcpProjectId?: string, gcpRegion: string = 'us-central1', geminiModel: string = 'gemini-2.0-flash') {
    this.extractionService = new GeminiExtractionService(gcpProjectId, gcpRegion, geminiModel);
    this.robotsChecker = new RobotsChecker('DevAssemblyBot', 60, 500);
  }

  /**
   * Validates Vertex AI configuration and returns detailed error if invalid
   */
  async validateVertexAIConfig(): Promise<{ valid: boolean; error?: string }> {
    if (!process.env.GCP_PROJECT_ID) {
      return {
        valid: false,
        error: 'GCP_PROJECT_ID not set. Please set the environment variable to enable Vertex AI.'
      };
    }

    // Quick validation by making a test request
    try {
      await this.extractionService.extractJobs('Test', { companyName: 'Test' });
      return { valid: true };
    } catch (error) {
      const errorMessage = (error as Error).message || 'Unknown error';
      
      if (errorMessage.includes('Permission Denied')) {
        return {
          valid: false,
          error: 'Service account lacks permissions. Grant "Vertex AI User" role.'
        };
      }
      if (errorMessage.includes('Quota')) {
        return {
          valid: false,
          error: 'Vertex AI quota exceeded. Wait or request increase.'
        };
      }
      
      return { valid: false, error: `Vertex AI error: ${errorMessage}` };
    }
  }

  /**
   * Executes a crawl for a single company
   */
  async crawl(request: CrawlRequest): Promise<CrawlResponse> {
    const startTime = Date.now();
    const { companyId, url, crawlConfig = {} } = request;
    
    const config = {
      maxPages: crawlConfig.maxPages || 5,
      followJobLinks: crawlConfig.followJobLinks ?? true,
      timeout: crawlConfig.timeout || 30000
    };
    
    // Check robots.txt before crawling
    const robotsCheck = await this.robotsChecker.canFetch(url);
    if (!robotsCheck.allowed) {
      console.warn(`Crawl blocked by robots.txt: ${url} - ${robotsCheck.reason}`);
      return {
        companyId,
        crawlMeta: {
          pagesVisited: 0,
          totalJobsFound: 0,
          detectedAtsProvider: null,
          detectedAtsIdentifier: null,
          crawlDurationMs: Date.now() - startTime,
          extractionModel: 'none',
          extractionConfidence: 0
        },
        jobs: []
      };
    }
    
    // Fetch the page content
    const { html, pagesVisited } = await this.fetchPageContent(url, config, robotsCheck.crawlDelay);
    
    // Detect ATS provider BEFORE sanitization (needs iframe/script tags)
    const atsDetection = detectAts(html);
    
    // Extract and clean content (sanitizes for LLM)
    const extracted = extractContent(html);
    
    // Wrap content for safe LLM processing
    const wrappedContent = wrapContentForLlm(extracted.mainContent);
    
    // Extract jobs using Gemini
    const extractionConfig: ExtractionConfig = {
      prompt: crawlConfig.extractionPrompt || undefined,
      companyName: companyId,
      extractionHints: (crawlConfig as any).extraction_hints
    };
    
    const extractionResult = await this.extractionService.extractJobs(
      wrappedContent,
      extractionConfig
    );
    
    // Validate extracted jobs
    const { validJobs, rejectedJobs } = validateJobs(extractionResult.jobs);
    
    // Calculate confidence score based on field completeness
    // Note: This is different from JobValidator's confidence calculation
    // - Validator confidence: starts at 1.0, subtracts for errors (used for accept/reject)
    // - This confidence: weighted sum of field completeness (used for crawlMeta reporting)
    const avgConfidence = validJobs.length > 0
      ? validJobs.reduce((sum, job) => {
          let score = 0;
          if (job.title) score += 0.3;
          if (job.location) score += 0.2;
          if (job.applyUrl) score += 0.2;
          if (job.postedAt) score += 0.15;
          if (job.employmentType) score += 0.15;
          return sum + score;
        }, 0) / validJobs.length
      : 0;
    
    const crawlDuration = Date.now() - startTime;
    
    return {
      companyId,
      crawlMeta: {
        pagesVisited,
        totalJobsFound: validJobs.length,
        detectedAtsProvider: atsDetection?.provider || null,
        detectedAtsIdentifier: atsDetection?.identifier || null,
        crawlDurationMs: crawlDuration,
        extractionModel: extractionResult.model,
        extractionConfidence: Math.round(avgConfidence * 100) / 100
      },
      jobs: validJobs
    };
  }

  /**
   * Fetches page content using Playwright
   * Creates a new crawler instance per request for isolation
   */
  private async fetchPageContent(
    url: string,
    config: { maxPages: number; followJobLinks: boolean; timeout: number },
    crawlDelay?: number
  ): Promise<{ html: string; pagesVisited: number }> {
    let pagesVisited = 0;
    const results: string[] = [];
    
    // Apply crawl delay before starting (from robots.txt)
    // Note: This is a one-time delay before crawling starts.
    // For Phase 2, inter-request delays should be implemented via
    // PlaywrightCrawler's requestHandler or Cloud Tasks rate limiting.
    if (crawlDelay && crawlDelay > 0) {
      await new Promise(resolve => setTimeout(resolve, crawlDelay));
    }
    
    // Create crawler instance for this request only
    const crawler = new PlaywrightCrawler({
      maxRequestsPerCrawl: config.maxPages,
      maxConcurrency: 1,  // Single page at a time per request
      requestHandler: async ({ page, request, enqueueLinks, log }) => {
        pagesVisited++;
        log.info(`Crawled page ${pagesVisited}: ${request.url}`);
        
        // Wait for dynamic content
        try {
          await page.waitForLoadState('networkidle', { timeout: config.timeout });
        } catch (e: unknown) {
          log.warning(`Timeout waiting for networkidle: ${(e as Error).message}`);
        }
        
        // Get page HTML
        const html = await page.content();
        results.push(html);
        
        // Follow job links if configured
        if (config.followJobLinks && pagesVisited < config.maxPages) {
          await enqueueLinks({
            strategy: 'same-hostname',
            globs: ['**/jobs/**', '**/career/**', '**/position/**']
          });
        }
      },
      failedRequestHandler: async ({ request, log }, error) => {
        log.error(`Request ${request.url} failed: ${(error as Error).message}`);
      }
    });
    
    try {
      await crawler.run([url]);
    } finally {
      // Clean up crawler resources
      await crawler.teardown();
    }
    
    // Combine HTML from all pages
    const mainHtml = results.join('\n---PAGE_BREAK---\n');
    
    return { html: mainHtml, pagesVisited };
  }
}
