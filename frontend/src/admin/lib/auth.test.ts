import { describe, it, expect, beforeEach } from 'vitest';
import { getToken, setToken, clearToken, isAuthenticated, authHeader } from './auth';

describe('auth utilities', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('should handle token round-trips', () => {
    expect(getToken()).toBeNull();
    setToken('test-token');
    expect(getToken()).toBe('test-token');
    clearToken();
    expect(getToken()).toBeNull();
  });

  it('should check authentication status correctly', () => {
    expect(isAuthenticated()).toBe(false);
    setToken('test-token');
    expect(isAuthenticated()).toBe(true);
  });

  it('should generate correct auth header', () => {
    expect(authHeader()).toEqual({});
    setToken('test-token');
    expect(authHeader()).toEqual({ Authorization: 'Bearer test-token' });
  });
});
