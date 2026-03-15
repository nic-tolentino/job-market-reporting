import { authHeader } from './auth';
import type {
  AdminCompany,
  AdminCompanyListResponse,
  AdminCompanyDetail,
  CrawlRun,
  UpsertSeedRequest,
  TriggerCrawlRequest,
} from '../types/admin';

const BASE = '/api/admin';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const fullPath = `${BASE}${path}`;

  const res = await fetch(fullPath, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
      ...(options?.headers as Record<string, string> | undefined),
    },
  });

  if (res.status === 401) {
    throw new Error('UNAUTHORIZED');
  }

  if (!res.ok) {
    const body = await res.text().catch(() => 'Unknown error');
    throw new Error(`API error ${res.status}: ${body}`);
  }

  return res.json() as Promise<T>;
}

// ---------------------------------------------------------------------------
// Companies
// ---------------------------------------------------------------------------

export interface ListCompaniesParams {
  page?: number;
  limit?: number;
  search?: string;
  seedStatus?: string;
  hqCountry?: string;
}

export async function listCompanies(params: ListCompaniesParams = {}): Promise<AdminCompanyListResponse> {
  const qs = new URLSearchParams();
  if (params.page !== undefined) qs.set('page', String(params.page));
  if (params.limit !== undefined) qs.set('limit', String(params.limit));
  if (params.search) qs.set('search', params.search);
  if (params.seedStatus) qs.set('seedStatus', params.seedStatus);
  if (params.hqCountry) qs.set('hqCountry', params.hqCountry);
  const query = qs.toString() ? `?${qs.toString()}` : '';
  return request<AdminCompanyListResponse>(`/crawler/companies${query}`);
}

export async function getCompany(companyId: string): Promise<AdminCompanyDetail> {
  return request<AdminCompanyDetail>(`/crawler/companies/${companyId}`);
}

// ---------------------------------------------------------------------------
// Seeds
// ---------------------------------------------------------------------------

export async function upsertSeed(body: UpsertSeedRequest): Promise<{ status: string }> {
  return request('/crawler/seeds', {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

// ---------------------------------------------------------------------------
// Crawl trigger
// ---------------------------------------------------------------------------

export async function triggerCrawl(
  companyId: string,
  body: TriggerCrawlRequest,
): Promise<unknown> {
  return request(`/crawler/companies/${companyId}/crawl`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

// ---------------------------------------------------------------------------
// Runs
// ---------------------------------------------------------------------------

export interface ListRunsParams {
  page?: number;
  limit?: number;
  companyId?: string;
}

export async function listRuns(
  params: ListRunsParams = {},
): Promise<{ data: CrawlRun[]; page: number; limit: number }> {
  const qs = new URLSearchParams();
  if (params.page !== undefined) qs.set('page', String(params.page));
  if (params.limit !== undefined) qs.set('limit', String(params.limit));
  if (params.companyId) qs.set('companyId', params.companyId);
  const query = qs.toString() ? `?${qs.toString()}` : '';
  return request(`/crawler/runs${query}`);
}

// ---------------------------------------------------------------------------
// Health
// ---------------------------------------------------------------------------

export async function getAdminHealth(): Promise<{ backend: string; crawlerService: string }> {
  return request('/crawler/health');
}

// ---------------------------------------------------------------------------
// Pipeline
// ---------------------------------------------------------------------------

export async function getQueueStats(): Promise<any> {
  return request('/pipeline/queue');
}

export async function getIngestionHistory(): Promise<any> {
  return request('/pipeline/history');
}

// ---------------------------------------------------------------------------
// Analytics
// ---------------------------------------------------------------------------

export async function getAnalyticsSummary(): Promise<any> {
  return request('/analytics/summary');
}

export async function getFeedback(): Promise<any[]> {
  return request('/analytics/feedback');
}

// ---------------------------------------------------------------------------
// Logs (SSE)
// ---------------------------------------------------------------------------

export function getLogStreamUrl(token: string): string {
  return `${BASE}/crawler/logs?token=${token}`;
}
