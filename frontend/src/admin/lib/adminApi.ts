import { authHeader } from './auth';
import type {
  AdminCompany,
  AdminCompanyListResponse,
  AdminCompanyDetail,
  CrawlRun,
  UpsertSeedRequest,
  TriggerCrawlRequest,
} from '../types/admin';

const BASE = '/api/admin/crawler';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
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
  return request<AdminCompanyListResponse>(`/companies${query}`);
}

export async function getCompany(companyId: string): Promise<AdminCompanyDetail> {
  return request<AdminCompanyDetail>(`/companies/${companyId}`);
}

// ---------------------------------------------------------------------------
// Seeds
// ---------------------------------------------------------------------------

export async function upsertSeed(body: UpsertSeedRequest): Promise<{ status: string }> {
  return request('/seeds', {
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
  return request(`/companies/${companyId}/crawl`, {
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
  return request(`/runs${query}`);
}

// ---------------------------------------------------------------------------
// Health
// ---------------------------------------------------------------------------

export async function getAdminHealth(): Promise<{ backend: string; crawlerService: string }> {
  return request('/health');
}
