/**
 * API Types for Crawler Service
 */

export interface CrawlRequest {
  companyId: string;
  url: string;
  crawlConfig?: CrawlConfig;
  seedData?: SeedMetadata; // Optional: metadata from a previous crawl of this seed
}

export interface CrawlConfig {
  maxPages?: number;
  followJobLinks?: boolean;
  extractionPrompt?: string | null;
  cssSelectors?: CssSelectors | null;
  knownAtsProvider?: string | null;
  timeout?: number;
  isDiscoveryMode?: boolean; // If true, crawler ignores same-path restriction to scout for boards
  paginationLimit?: number;
  extractionHints?: Array<{ key: string; value: string }>;
}

export interface SeedMetadata {
  url: string;
  category: 'tech-filtered' | 'general' | 'careers' | 'homepage' | 'unknown';
  lastKnownPageCount?: number;
  lastKnownJobCount?: number;
  lastVerified?: string;
  pagination_pattern?: string;
  status?: 'ACTIVE' | 'BLOCKED' | 'TIMEOUT' | 'STALE';
}

export interface CssSelectors {
  jobListSelector?: string;
  removeSelectors?: string[];
}

export interface CrawlResponse {
  companyId: string;
  crawlMeta: CrawlMeta;
  jobs: NormalizedJob[];
  extractionStats?: ExtractionStats;
}

export interface CrawlMeta {
  pagesVisited: number;
  totalJobsFound: number;
  detectedAtsProvider: string | null;
  detectedAtsIdentifier: string | null;
  crawlDurationMs: number;
  extractionModel: string;
  extractionConfidence: number;
  lastCrawledAt: string;
  pagination_pattern?: string;
  errorMessage?: string;
  paginationSignal?: {
    type: 'GROWTH' | 'CONTRACTION';
    previousPages: number;
    newPages: number;
  };
  jobYieldSignal?: {
    type: 'GROWTH' | 'CONTRACTION';
    previousJobs: number;
    newJobs: number;
    delta: number;
  };
  status?: 'ACTIVE' | 'BLOCKED' | 'TIMEOUT' | 'STALE' | 'FAILED';
  /**
   * Direct URL to the ATS job board when a provider was detected but no jobs
   * could be extracted from the crawled page (e.g. Greenhouse embed, iFrame).
   * Use this as the seed URL for a follow-up targeted crawl.
   */
  atsDirectUrl?: string;
}

export interface ExtractionStats {
  jobsRaw: number;    // total from Gemini before validation
  jobsValid: number;  // after validateJobs()
  jobsTech: number;   // after tech/negative keyword filter
}

export interface NormalizedJob {
  platformId: string;
  source: string;
  title: string;
  companyName: string;
  location: string | null;
  descriptionHtml: string | null;
  descriptionText: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  employmentType: string | null;
  seniorityLevel: string | null;
  workModel: string | null;
  department: string | null;
  postedAt: string | null;
  applyUrl: string | null;
  platformUrl: string | null;
}

export interface AtsDetectionResult {
  provider: string;
  identifier: string | null;
  confidence: number;
  evidence: string;
}

export interface ValidationResult {
  valid: boolean;
  confidence: number;
  errors: string[];
}
