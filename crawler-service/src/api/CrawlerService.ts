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
  private gcpProjectId: string | undefined;
  private gcpRegion: string;
  private geminiModel: string;

  constructor(
    gcpProjectId?: string,
    gcpRegion: string = 'us-central1',
    geminiModel: string = 'gemini-2.0-flash'
  ) {
    this.gcpProjectId = gcpProjectId;
    this.gcpRegion = gcpRegion;
    this.geminiModel = geminiModel;
    this.extractionService = new GeminiExtractionService(gcpProjectId, gcpRegion, geminiModel);
    this.robotsChecker = new RobotsChecker('DevAssemblyBot', 60, 500);
  }

  /**
   * Validates Vertex AI configuration and returns detailed error if invalid
   */
  async validateVertexAIConfig(): Promise<{ valid: boolean; error?: string }> {
    if (!this.gcpProjectId) {
      return {
        valid: false,
        error: 'GCP_PROJECT_ID not set. Please set the environment variable to enable Vertex AI.'
      };
    }

    // Quick validation by making a test request
    try {
      const testResult = await this.extractionService.extractJobs('Test', { companyName: 'Test' });
      return { valid: true };
    } catch (error) {
      const errorMessage = (error as Error).message || 'Unknown error';
      
      // Provide actionable error messages
      if (errorMessage.includes('Permission Denied') || errorMessage.includes('PERMISSION_DENIED')) {
        return {
          valid: false,
          error: `Service account lacks permissions. Solutions: 1) Grant "Vertex AI User" role, 2) Verify service account access, 3) Check at https://console.cloud.google.com/vertex-ai`
        };
      }
      if (errorMessage.includes('Quota') || errorMessage.includes('RESOURCE_EXHAUSTED')) {
        return {
          valid: false,
          error: `Vertex AI quota exceeded. Solutions: 1) Wait for quota reset, 2) Request increase at https://console.cloud.google.com/vertex-ai/quotas`
        };
      }
      if (errorMessage.includes('API not enabled') || errorMessage.includes('NOT_FOUND')) {
        return {
          valid: false,
          error: `Vertex AI API not enabled. Solution: Enable at https://console.cloud.google.com/vertex-ai`
        };
      }
      
      return {
        valid: false,
        error: `Vertex AI error: ${errorMessage}`
      };
    }
  }