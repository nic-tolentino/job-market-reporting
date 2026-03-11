/**
 * API Types for Crawler Service
 */

export interface CrawlRequest {
  companyId: string;
  url: string;
  crawlConfig?: CrawlConfig;
}

export interface CrawlConfig {
  maxPages?: number;
  followJobLinks?: boolean;
  extractionPrompt?: string | null;
  cssSelectors?: CssSelectors | null;
  knownAtsProvider?: string | null;
  timeout?: number;
}

export interface CssSelectors {
  jobListSelector?: string;
  removeSelectors?: string[];
}

export interface CrawlResponse {
  companyId: string;
  crawlMeta: CrawlMeta;
  jobs: NormalizedJob[];
}

export interface CrawlMeta {
  pagesVisited: number;
  totalJobsFound: number;
  detectedAtsProvider: string | null;
  detectedAtsIdentifier: string | null;
  crawlDurationMs: number;
  extractionModel: string;
  extractionConfidence: number;
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
