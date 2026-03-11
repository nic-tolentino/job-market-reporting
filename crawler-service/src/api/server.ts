import express, { Request, Response } from 'express';
import cors from 'cors';
import { z } from 'zod';
import { CrawlerService } from './CrawlerService';
import { CrawlRequest, CrawlResponse } from './types';

/**
 * Zod schema for validating crawl requests
 */
const CrawlRequestSchema = z.object({
  companyId: z.string().min(1),
  url: z.string().url(),
  crawlConfig: z.object({
    maxPages: z.number().int().positive().optional(),
    followJobLinks: z.boolean().optional(),
    extractionPrompt: z.string().nullable().optional(),
    cssSelectors: z.object({
      jobListSelector: z.string().optional(),
      removeSelectors: z.array(z.string()).optional()
    }).nullable().optional(),
    knownAtsProvider: z.string().nullable().optional(),
    timeout: z.number().int().positive().optional()
  }).optional()
});

/**
 * Creates and configures the Express API server
 */
export function createApp(crawlerService: CrawlerService): express.Application {
  const app = express();
  
  // Middleware
  app.use(cors());
  app.use(express.json({ limit: '10mb' }));
  
  // Health check endpoint with API key validation
  app.get('/health', async (req: Request, res: Response) => {
    // Validate Gemini API key
    const apiKeyValidation = await crawlerService.validateGeminiApiKey();
    
    res.json({ 
      status: apiKeyValidation.valid ? 'ok' : 'degraded',
      timestamp: new Date().toISOString(),
      geminiApiKey: apiKeyValidation.valid ? 'valid' : 'invalid',
      geminiApiError: apiKeyValidation.error || undefined
    });
  });
  
  // Main crawl endpoint
  app.post('/crawl', async (req: Request, res: Response) => {
    try {
      // Validate request
      const validationResult = CrawlRequestSchema.safeParse(req.body);
      if (!validationResult.success) {
        return res.status(400).json({
          error: 'Invalid request',
          details: validationResult.error.errors
        });
      }
      
      const crawlRequest: CrawlRequest = validationResult.data;
      
      // Execute crawl
      const result = await crawlerService.crawl(crawlRequest);
      
      res.json(result);
    } catch (error) {
      console.error('Crawl error:', error);
      
      if (error instanceof Error) {
        res.status(500).json({
          error: 'Crawl failed',
          message: error.message
        });
      } else {
        res.status(500).json({
          error: 'Crawl failed',
          message: 'Unknown error occurred'
        });
      }
    }
  });
  
  // Batch crawl endpoint (for multiple companies)
  app.post('/crawl/batch', async (req: Request, res: Response) => {
    try {
      const { requests } = req.body as { requests: CrawlRequest[] };

      if (!Array.isArray(requests)) {
        return res.status(400).json({
          error: 'Invalid request',
          message: 'requests must be an array'
        });
      }

      // Validate all requests and track failures
      const validatedRequests: CrawlRequest[] = [];
      const validationFailures: Array<{ index: number; companyId?: string; errors: any[] }> = [];
      
      requests.forEach((req, index) => {
        const result = CrawlRequestSchema.safeParse(req);
        if (result.success) {
          validatedRequests.push(result.data);
        } else {
          validationFailures.push({
            index,
            companyId: (req as any)?.companyId,
            errors: result.error.errors
          });
        }
      });

      if (validatedRequests.length === 0) {
        return res.status(400).json({
          error: 'No valid requests',
          validationFailures
        });
      }

      // Process requests in parallel with concurrency limit
      const concurrencyLimit = 5;
      const results: Array<CrawlResponse & { index: number; error?: string }> = [];

      for (let i = 0; i < validatedRequests.length; i += concurrencyLimit) {
        const batch = validatedRequests.slice(i, i + concurrencyLimit);
        const batchResults = await Promise.all(
          batch.map((req, batchIndex) => 
            crawlerService.crawl(req)
              .then(result => ({ ...result, index: requests.findIndex(r => r.companyId === req.companyId) }))
              .catch(err => ({
                companyId: req.companyId,
                index: requests.findIndex(r => r.companyId === req.companyId),
                crawlMeta: {
                  pagesVisited: 0,
                  totalJobsFound: 0,
                  detectedAtsProvider: null,
                  detectedAtsIdentifier: null,
                  crawlDurationMs: 0,
                  extractionModel: 'none',
                  extractionConfidence: 0
                },
                jobs: [],
                error: err.message
              }))
          )
        );
        results.push(...batchResults);
      }

      // Sort results by original request order
      results.sort((a, b) => a.index - b.index);

      res.json({ 
        results: results.map(({ index, ...result }) => result),
        validationFailures: validationFailures.length > 0 ? validationFailures : undefined
      });
    } catch (error) {
      console.error('Batch crawl error:', error);
      res.status(500).json({
        error: 'Batch crawl failed',
        message: error instanceof Error ? error.message : 'Unknown error'
      });
    }
  });
  
  // 404 handler
  app.use((req: Request, res: Response) => {
    res.status(404).json({ error: 'Not found' });
  });
  
  return app;
}
