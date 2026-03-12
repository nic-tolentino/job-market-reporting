import { PlaywrightCrawler } from 'crawlee';
import { detectAts } from '../detector/AtsDetector';
import { extractContent, wrapContentForLlm } from '../extractor/ContentExtractor';
import { GeminiExtractionService, ExtractionConfig } from '../extraction/GeminiExtractionService';
import { validateJobs } from '../validator/JobValidator';
import { RobotsChecker } from '../utils/RobotsChecker';
import { CrawlRequest, CrawlResponse, CrawlMeta, NormalizedJob } from '../api/types';
import { DEFAULT_MODEL } from '../config/model-config';

/**
 * Main crawler service that orchestrates the crawl pipeline
 */
export class CrawlerService {
  private extractionService: GeminiExtractionService;
  private robotsChecker: RobotsChecker;

  constructor(geminiApiKey: string, geminiModel: string = DEFAULT_MODEL) {
    this.extractionService = new GeminiExtractionService(geminiApiKey, geminiModel);
    this.robotsChecker = new RobotsChecker('DevAssemblyBot', 60, 500);
  }

  /**
   * Validates Gemini configuration
   */
  async validateGeminiConfig(): Promise<{ valid: boolean; error?: string }> {
    try {
      await this.extractionService.extractJobs('Test', { companyName: 'Test' });
      return { valid: true };
    } catch (error) {
      const errorMessage = (error as Error).message;
      return { valid: false, error: errorMessage };
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

    const { html, pagesVisited } = await this.fetchPageContent(url, config, robotsCheck.crawlDelay);
    const atsDetection = detectAts(html);
    const extracted = extractContent(html);
    const wrappedContent = wrapContentForLlm(extracted.mainContent);

    const extractionConfig: ExtractionConfig = {
      prompt: crawlConfig.extractionPrompt || undefined,
      companyName: companyId,
      extractionHints: (crawlConfig as any).extraction_hints
    };

    const extractionResult = await this.extractionService.extractJobs(wrappedContent, extractionConfig);
    const { validJobs } = validateJobs(extractionResult.jobs);

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

    return {
      companyId,
      crawlMeta: {
        pagesVisited,
        totalJobsFound: validJobs.length,
        detectedAtsProvider: atsDetection?.provider || null,
        detectedAtsIdentifier: atsDetection?.identifier || null,
        crawlDurationMs: Date.now() - startTime,
        extractionModel: extractionResult.model,
        extractionConfidence: Math.round(avgConfidence * 100) / 100
      },
      jobs: validJobs
    };
  }

  private async fetchPageContent(
    url: string,
    config: { maxPages: number; followJobLinks: boolean; timeout: number },
    crawlDelay?: number
  ): Promise<{ html: string; pagesVisited: number }> {
    let pagesVisited = 0;
    const results: string[] = [];

    if (crawlDelay && crawlDelay > 0) {
      await new Promise(resolve => setTimeout(resolve, crawlDelay));
    }

    const crawler = new PlaywrightCrawler({
      maxRequestsPerCrawl: config.maxPages,
      maxConcurrency: 1,
      requestHandler: async ({ page, request, enqueueLinks, log }) => {
        pagesVisited++;
        log.info(`Crawled page ${pagesVisited}: ${request.url}`);
        try {
          await page.waitForLoadState('networkidle', { timeout: config.timeout });
        } catch (e: unknown) {
          log.warning(`Timeout: ${(e as Error).message}`);
        }
        const html = await page.content();
        results.push(html);
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
      await crawler.teardown();
    }

    return { html: results.join('\n---PAGE_BREAK---\n'), pagesVisited };
  }
}
