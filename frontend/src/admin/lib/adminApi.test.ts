import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as adminApi from './adminApi';
import * as auth from './auth';

vi.mock('./auth', () => ({
  authHeader: vi.fn(() => ({ Authorization: 'Bearer test-token' })),
  getToken: vi.fn(() => 'test-token'),
}));

describe('adminApi', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  it('should construct correct URLs and headers', async () => {
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ data: [] }),
    } as Response);

    await adminApi.listCompanies({ page: 1, search: 'test' });

    expect(mockFetch).toHaveBeenCalledWith(
      '/api/admin/crawler/companies?page=1&search=test',
      expect.objectContaining({
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        }),
      })
    );
  });

  it('should throw UNAUTHORIZED on 401', async () => {
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValueOnce({
      status: 401,
      ok: false,
    } as Response);

    await expect(adminApi.listCompanies()).rejects.toThrow('UNAUTHORIZED');
  });

  it('should throw detailed error on non-ok response', async () => {
    const mockFetch = vi.mocked(fetch);
    mockFetch.mockResolvedValueOnce({
      status: 500,
      ok: false,
      text: async () => 'Internal Error',
    } as Response);

    await expect(adminApi.getAdminHealth()).rejects.toThrow('API error 500: Internal Error');
  });

  it('should construct log stream URL correctly', () => {
    const url = adminApi.getLogStreamUrl('my-token');
    expect(url).toBe('/api/admin/crawler/logs?token=my-token');
  });
});
