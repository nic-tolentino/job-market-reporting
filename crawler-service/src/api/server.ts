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

  // Health check endpoint with Vertex AI validation
  app.get('/health', async (req: Request, res: Response) => {
    // Validate Vertex AI configuration
    const configValidation = await crawlerService.validateVertexAIConfig();
    
    res.json({ 
      status: configValidation.valid ? 'ok' : 'degraded',
      timestamp: new Date().toISOString(),
      vertexAI: configValidation.valid ? 'configured' : 'not-configured',
      vertexAIError: configValidation.error || undefined
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
