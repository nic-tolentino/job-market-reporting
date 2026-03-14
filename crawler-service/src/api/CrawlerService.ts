import { PlaywrightCrawler, Configuration } from 'crawlee';
import * as path from 'path';
import * as fs from 'fs';
import * as os from 'os';
import { detectAts } from '../detector/AtsDetector';
import { extractContent, wrapContentForLlm } from '../extractor/ContentExtractor';
import { GeminiExtractionService, ExtractionConfig } from '../extraction/GeminiExtractionService';
import { validateJobs } from '../validator/JobValidator';
import { RobotsChecker } from '../utils/RobotsChecker';
import { CrawlRequest, CrawlResponse, CrawlMeta, NormalizedJob } from '../api/types';
import { DEFAULT_MODEL } from '../config/model-config';
import {
  CRAWLER_USER_AGENT,
  PAGE_BUDGET_TARGETED,
  PAGE_BUDGET_DISCOVERY,
  DEFAULT_PAGINATION_LIMIT,
  INTER_EXTRACTION_DELAY_MS,
  NETWORK_IDLE_TIMEOUT_MS,
  PAGINATION_PARAMS,
  CORE_PAGINATION_PARAMS,
  DISCOVERY_REGEXPS,
  TECH_KEYWORDS,
  NEGATIVE_KEYWORDS,
  ERROR_MESSAGE_MAX_LENGTH,
} from '../config/crawler-constants';

/**
 * Constructs the direct ATS job-board URL from a detected provider and identifier.
 * Returns null for providers whose URLs cannot be derived from the identifier alone.
 * Use the result as a targeted-mode seed URL for a follow-up crawl.
 */
function buildAtsDirectUrl(provider: string | null, identifier: string | null): string | undefined {
  if (!provider || !identifier) return undefined;
  switch (provider) {
    case 'GREENHOUSE':      return `https://boards.greenhouse.io/${identifier}`;
    case 'LEVER':           return `https://jobs.lever.co/${identifier}`;
    case 'ASHBY':           return `https://jobs.ashbyhq.com/${identifier}`;
    case 'SMARTRECRUITERS': return `https://jobs.smartrecruiters.com/${identifier}`;
    case 'WORKABLE':        return `https://apply.workable.com/${identifier}`;
    default:                return undefined;
  }
}

/**
 * Main crawler service that orchestrates the crawl pipeline
 */
export class CrawlerService {
  private extractionService: GeminiExtractionService;
  private robotsChecker: RobotsChecker;

  constructor(geminiApiKey: string, geminiModel: string = DEFAULT_MODEL) {
    this.extractionService = new GeminiExtractionService(geminiApiKey, geminiModel);
    this.robotsChecker = new RobotsChecker(CRAWLER_USER_AGENT, 60, 500);
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
   * De-duplicate jobs to prevent false inflations.
   */
  public static deduplicateJobs(jobs: NormalizedJob[]): NormalizedJob[] {
    return jobs.filter((job, index, self) =>
      index === self.findIndex((j) => (
        j.title === job.title && j.location === job.location
      ))
    );
  }

  /**
   * Helper to identify if a link is a logical extension of the current board (pagination) vs discovery.
   */
  public static isPaginationLink(linkUrl: string, startUrl: string): boolean {
    try {
      const link = new URL(linkUrl);
      const start = new URL(startUrl);
      
      // Must be same hostname and path
      if (link.hostname !== start.hostname || link.pathname !== start.pathname) {
        return false;
      }
      
      // Check for common pagination parameters
      for (const p of PAGINATION_PARAMS) {
        if (link.searchParams.has(p)) {
          return true;
        }
      }
      
      return false;
    } catch (e) {
      return false;
    }
  }

  /**
   * Executes a crawl for a single company
   */
  async crawl(request: CrawlRequest): Promise<CrawlResponse> {
    const startTime = Date.now();
    const { companyId, url, crawlConfig = {}, seedData } = request;

    try {
      // TARGETED MODE: If we have a verified seed URL, jump directly to it
      const effectiveUrl = seedData?.url || url;
      const isTargetedMode = !!seedData?.url;

      if (isTargetedMode) {
        console.log(`[TARGETED_MODE] Skipping homepage discovery. Jumping to: ${effectiveUrl}`);
      }

      const config = {
        // Budget: 10 navigation pages + 50 pagination pages = 60
        maxPages: crawlConfig.maxPages || (isTargetedMode ? PAGE_BUDGET_TARGETED : PAGE_BUDGET_DISCOVERY),
        followJobLinks: crawlConfig.followJobLinks ?? true,
        timeout: crawlConfig.timeout || 30000,
        isDiscoveryMode: crawlConfig.isDiscoveryMode ?? !isTargetedMode,
        paginationLimit: crawlConfig.paginationLimit
      };

      console.log(`Checking robots.txt for: ${effectiveUrl}`);
      const robotsCheck = await this.robotsChecker.canFetch(effectiveUrl);
      
      if (!robotsCheck.allowed) {
        console.warn(`[ROBOTS_BLOCKED] Crawl denied by robots.txt for: ${effectiveUrl}`);
        return {
          companyId,
          crawlMeta: {
            pagesVisited: 0,
            totalJobsFound: 0,
            detectedAtsProvider: null,
            detectedAtsIdentifier: null,
            crawlDurationMs: Date.now() - startTime,
            extractionModel: DEFAULT_MODEL,
            extractionConfidence: 0,
            lastCrawledAt: new Date().toISOString(),
            errorMessage: 'robots.txt blocked',
            status: 'BLOCKED'
          },
          jobs: []
        };
      }
      
      let crawlError: string | undefined = undefined;
      const { results, pagesVisited, detectedPaginationPattern } = await this.fetchPageContent(effectiveUrl, config, companyId, robotsCheck.crawlDelay);
      
      if (pagesVisited === 0) {
        crawlError = 'Failed to reach any pages';
      }
      
      let allValidJobs: NormalizedJob[] = [];
      let detectedAtsProvider: string | null = null;
      let detectedAtsIdentifier: string | null = null;

      let paginationSignal: CrawlMeta['paginationSignal'] = undefined;
      let jobYieldSignal: CrawlMeta['jobYieldSignal'] = undefined;

      let jobsRaw = 0;
      let jobsValid = 0;
      let jobsTech = 0;

      // Growth/Contraction Signal Logging
      // Only emit when we have a prior baseline to compare against.
      if (seedData && seedData.lastKnownPageCount !== undefined) {
        if (pagesVisited > seedData.lastKnownPageCount) {
          paginationSignal = { type: 'GROWTH', previousPages: seedData.lastKnownPageCount, newPages: pagesVisited };
          console.log(`[SIGNAL] PAGINATION_GROWTH: ${companyId} increased from ${seedData.lastKnownPageCount} to ${pagesVisited} pages.`);
        } else if (pagesVisited < seedData.lastKnownPageCount) {
          paginationSignal = { type: 'CONTRACTION', previousPages: seedData.lastKnownPageCount, newPages: pagesVisited };
          console.log(`[SIGNAL] PAGINATION_CONTRACTION: ${companyId} decreased from ${seedData.lastKnownPageCount} to ${pagesVisited} pages.`);
        }
      }

      if (results.length > 0) {
        const extractionConfig: ExtractionConfig = {
          prompt: crawlConfig.extractionPrompt || undefined,
          companyName: companyId,
          extractionHints: crawlConfig.extractionHints?.reduce((acc, hint) => {
            acc[hint.key] = hint.value;
            return acc;
          }, {} as Record<string, string>)
        };

        // Process each page individually to avoid TPM limits
        for (let i = 0; i < results.length; i++) {
          const html = results[i];
          console.log(`Processing page ${i + 1}/${results.length}...`);
          
          const atsDetection = detectAts(html);
          if (atsDetection) {
            detectedAtsProvider = atsDetection.provider;
            detectedAtsIdentifier = atsDetection.identifier;
          }

          const extracted = extractContent(html);
          const wrappedContent = wrapContentForLlm(extracted.textContent);
          
          try {
            if (i > 0) await new Promise(resolve => setTimeout(resolve, INTER_EXTRACTION_DELAY_MS));
            const extractionResult = await this.extractionService.extractJobs(wrappedContent, extractionConfig);
            
            jobsRaw += extractionResult.jobs.length;

            // Post-Extraction Filtering for non-tech seeds
            let { validJobs } = validateJobs(extractionResult.jobs);
            jobsValid += validJobs.length;

            if (seedData?.category === 'general' || seedData?.category === 'careers' || seedData?.category === 'homepage' || seedData?.category === 'unknown') {
              const originalCount = validJobs.length;
              validJobs = validJobs.filter(job => {
                const lowerTitle = job.title?.toLowerCase() || '';
                const hasTechKeyword = TECH_KEYWORDS.some(kw => lowerTitle.includes(kw));
                const hasNegativeKeyword = NEGATIVE_KEYWORDS.some(kw => lowerTitle.includes(kw));
                return hasTechKeyword && !hasNegativeKeyword;
              });
              if (validJobs.length < originalCount) {
                console.log(`[FILTER] Dropped ${originalCount - validJobs.length} non-tech roles from general seed.`);
              }
            } else if (seedData?.category === 'tech-filtered') {
              // Insurance: must have at least one tech keyword even if board is filtered
              const originalCount = validJobs.length;
              validJobs = validJobs.filter(job => {
                const lowerTitle = job.title?.toLowerCase() || '';
                return TECH_KEYWORDS.some(kw => lowerTitle.includes(kw));
              });
              if (validJobs.length < originalCount) {
                console.log(`[FILTER] Dropped ${originalCount - validJobs.length} unrelated roles from tech-filtered seed.`);
              }
            }
            
            jobsTech += validJobs.length;
            allValidJobs = [...allValidJobs, ...validJobs];
          } catch (error) {
            console.error(`Error extracting from page ${i + 1}:`, (error as Error).message);
          }
        }
      }

      // De-duplicate jobs before applying yield signal to prevent false inflations
      const uniqueJobs = CrawlerService.deduplicateJobs(allValidJobs);

      // Growth/Contraction Signal (Jobs)
      // Only emit when we have a prior baseline and the count actually changed.
      if (seedData && seedData.lastKnownJobCount !== undefined) {
        const diff = uniqueJobs.length - seedData.lastKnownJobCount;
        if (diff !== 0) {
          jobYieldSignal = {
            type: diff > 0 ? 'GROWTH' : 'CONTRACTION',
            previousJobs: seedData.lastKnownJobCount,
            newJobs: uniqueJobs.length,
            delta: diff
          };
          console.log(`[SIGNAL] JOB_COUNT_CHANGE: ${companyId} yield changed by ${diff > 0 ? '+' : ''}${diff} jobs.`);
        }
      }

      const { averageConfidence } = validateJobs(uniqueJobs);

      // Surface the ATS direct URL when we detected a provider but extracted no
      // jobs — this gives the caller a ready-made seed URL for a targeted crawl.
      const atsDirectUrl = uniqueJobs.length === 0
        ? buildAtsDirectUrl(detectedAtsProvider, detectedAtsIdentifier)
        : undefined;

      const crawlDurationMs = Date.now() - startTime;
      const response: CrawlResponse = {
        companyId,
        crawlMeta: {
          pagesVisited,
          totalJobsFound: uniqueJobs.length,
          detectedAtsProvider,
          detectedAtsIdentifier,
          crawlDurationMs,
          extractionModel: DEFAULT_MODEL,
          extractionConfidence: averageConfidence,
          lastCrawledAt: new Date().toISOString(),
          pagination_pattern: detectedPaginationPattern,
          jobYieldSignal: jobYieldSignal || undefined,
          paginationSignal: paginationSignal || undefined,
          status: (crawlError !== undefined && crawlError !== null && crawlError !== '') ? 'FAILED' : 'ACTIVE',
          errorMessage: crawlError,
          atsDirectUrl,
        },
        jobs: uniqueJobs,
        extractionStats: {
          jobsRaw,
          jobsValid,
          jobsTech
        }
      };
      
      return response;
    } catch (error: any) {
      console.error(`[CRAWL_CRITICAL_FAILURE] ${companyId}:`, error);
      return {
        companyId,
        crawlMeta: {
          pagesVisited: 0,
          totalJobsFound: 0,
          detectedAtsProvider: null,
          detectedAtsIdentifier: null,
          crawlDurationMs: Date.now() - startTime,
          extractionModel: DEFAULT_MODEL,
          extractionConfidence: 0,
          lastCrawledAt: new Date().toISOString(),
          errorMessage: error.message?.split('\n')[0].substring(0, ERROR_MESSAGE_MAX_LENGTH) || 'Unknown error',
          status: 'FAILED'
        },
        jobs: []
      };
    }
  }

  private async fetchPageContent(
    url: string,
    config: { maxPages: number; followJobLinks: boolean; timeout: number; isDiscoveryMode?: boolean; paginationLimit?: number },
    companyId: string,
    crawlDelay?: number
  ): Promise<{ results: string[]; pagesVisited: number; detectedPaginationPattern?: string }> {
    const storageId = `${companyId}-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
    const storagePath = path.join(os.tmpdir(), 'crawler-storage', storageId);
    
    // Ensure storage path exists
    if (!fs.existsSync(path.dirname(storagePath))) {
      fs.mkdirSync(path.dirname(storagePath), { recursive: true });
    }

    const configOverride = new Configuration({
      storageClientOptions: {
        localDataDirectory: storagePath
      },
      purgeOnStart: true
    });

    let pagesVisited = 0;
    let paginationPagesVisited = 0;
    const PAGINATION_LIMIT = config.paginationLimit || DEFAULT_PAGINATION_LIMIT;
    const results: string[] = [];
    let detectedPaginationPattern: string | undefined = undefined;

    if (crawlDelay && crawlDelay > 0) {
      await new Promise(resolve => setTimeout(resolve, crawlDelay));
    }

    const crawler = new PlaywrightCrawler({
      maxRequestsPerCrawl: config.maxPages,
      requestHandlerTimeoutSecs: Math.floor(config.timeout / 1000),
      
      async requestHandler({ request, page, enqueueLinks, log }) {
        const isRequestDiscoveryMode = request.userData.isDiscoveryMode;
        
        pagesVisited++;
        log.info(`Processing ${request.url} (Discovery: ${isRequestDiscoveryMode})`);

        // Wait for JS-rendered content (ATS widgets, SPAs) to finish loading.
        // networkidle resolves once all network requests have settled for 500ms.
        // We time-out gracefully so pages with persistent beacons don't stall.
        await page.waitForLoadState('networkidle', { timeout: NETWORK_IDLE_TIMEOUT_MS })
          .catch(() => { /* non-fatal — proceed with whatever has rendered */ });
        
        const html = await page.content();
        results.push(html);

        if (isRequestDiscoveryMode) {
          await enqueueLinks({
            strategy: 'same-domain',
            label: 'DISCOVERY',
            userData: { isDiscoveryMode: true },
            regexps: DISCOVERY_REGEXPS,
          });
        } else {
          paginationPagesVisited++;
          
          if (paginationPagesVisited < PAGINATION_LIMIT) {
            const currentUrl = page.url();
            const links = await page.$$eval('a[href]', (els) => els.map(el => (el as HTMLAnchorElement).href));
            
            for (const link of links) {
              if (CrawlerService.isPaginationLink(link, currentUrl)) {
                await enqueueLinks({
                  urls: [link],
                  label: 'PAGINATION',
                  userData: { isDiscoveryMode: false }
                });
                
                if (!detectedPaginationPattern) {
                  try {
                    const lUrl = new URL(link);
                    const sUrl = new URL(currentUrl);
                    for (const p of CORE_PAGINATION_PARAMS) {
                      if (lUrl.searchParams.has(p) && lUrl.searchParams.get(p) !== sUrl.searchParams.get(p)) {
                        detectedPaginationPattern = `query:${p}`;
                        break;
                      }
                    }
                  } catch (e) {}
                }
              }
            }
          }
        }
      },
      
      errorHandler({ request, error, log }) {
        log.error(`Request ${request.url} failed: ${(error as any).message}`);
      },

      failedRequestHandler({ request, error, log }) {
        log.error(`Request ${request.url} failed finally: ${(error as any).message}`);
        throw error; // Throw to trigger the crawl method's catch block
      }
    }, configOverride);

    await crawler.run([{ url, userData: { isDiscoveryMode: config.isDiscoveryMode } }]);

    // Cleanup storage after crawl
    try {
      if (fs.existsSync(storagePath)) {
        fs.rmSync(storagePath, { recursive: true, force: true });
      }
    } catch (e) {
      console.warn(`Failed to cleanup storage at ${storagePath}:`, e);
    }

    return { results, pagesVisited, detectedPaginationPattern };
  }
}
