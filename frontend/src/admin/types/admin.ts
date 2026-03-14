// Admin panel TypeScript types

export interface AdminCompany {
  companyId: string;
  name: string;
  logoUrl: string;
  hqCountry: string | null;
  verificationLevel: string | null;
  employeesCount: number | null;
  // Aggregated from crawler_seeds
  seedStatus: 'ACTIVE' | 'STALE' | 'BLOCKED' | null;
  seedCount: number;
  lastCrawledAt: string | null;
  totalJobsLastRun: number;
  atsProvider: string | null;
  maxZeroYieldCount: number;
}

export interface AdminCompanyListResponse {
  data: AdminCompany[];
  page: number;
  limit: number;
  total: number;
}

export interface CrawlerSeed {
  companyId: string;
  url: string;
  category: 'tech-filtered' | 'general' | 'careers' | 'homepage' | 'unknown' | null;
  status: 'ACTIVE' | 'STALE' | 'BLOCKED' | 'TIMEOUT' | null;
  paginationPattern: string | null;
  lastKnownJobCount: number | null;
  lastKnownPageCount: number | null;
  lastCrawledAt: string | null;
  lastDurationMs: number | null;
  errorMessage: string | null;
  consecutiveZeroYieldCount: number;
  atsProvider: string | null;
  atsIdentifier: string | null;
  atsDirectUrl: string | null;
}

export interface CrawlRun {
  runId: string;
  batchId: string | null;
  companyId: string;
  seedUrl: string;
  isTargeted: boolean;
  startedAt: string;
  durationMs: number | null;
  pagesVisited: number | null;
  jobsRaw: number | null;
  jobsValid: number | null;
  jobsTech: number | null;
  jobsFinal: number | null;
  confidenceAvg: number | null;
  atsProvider: string | null;
  atsDirectUrl: string | null;
  paginationPattern: string | null;
  status: 'ACTIVE' | 'FAILED' | 'BLOCKED' | 'TIMEOUT';
  errorMessage: string | null;
  modelUsed: string | null;
}

export interface AdminCompanyDetail {
  companyId: string;
  seeds: CrawlerSeed[];
  recentRuns: CrawlRun[];
}

export interface UpsertSeedRequest {
  companyId: string;
  url: string;
  category: string | null;
  status: string | null;
}

export interface TriggerCrawlRequest {
  url: string;
  maxPages?: number;
  isDiscovery?: boolean;
  seedData?: Record<string, unknown>;
}

export type SeedCategory = 'tech-filtered' | 'general' | 'careers' | 'homepage' | 'unknown';
export type SeedStatus = 'ACTIVE' | 'STALE' | 'BLOCKED' | 'TIMEOUT';
export type RunStatus = 'ACTIVE' | 'FAILED' | 'BLOCKED' | 'TIMEOUT';
