import { useState, type FormEvent } from 'react';
import { setToken } from '../lib/auth';

interface LoginPageProps {
  onLogin: () => void;
}

export function LoginPage({ onLogin }: LoginPageProps) {
  const [token, setTokenValue] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!token.trim()) return;

    setLoading(true);
    setError('');

    try {
      const apiBase = import.meta.env.VITE_API_URL || '/api';
      const res = await fetch(`${apiBase}/admin/crawler/health`, {
        headers: { Authorization: `Bearer ${token.trim()}` },
      });

      if (res.status === 401) {
        setError('Invalid token');
        return;
      }

      if (!res.ok) {
        setError(`Server error: ${res.status}`);
        return;
      }

      setToken(token.trim());
      onLogin();
    } catch (err) {
      setError('Could not reach the backend. Is it running?');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-page flex items-center justify-center p-4">
      <div className="bg-card rounded-xl shadow-lg w-full max-w-sm p-8 border border-border">
        <div className="mb-6">
          <p className="text-xs font-semibold text-muted uppercase tracking-widest mb-1">Admin</p>
          <h1 className="text-xl font-bold text-primary">DevAssembly</h1>
          <p className="text-sm text-muted mt-1">Enter your admin token to continue</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-secondary mb-1.5">
              Admin token
            </label>
            <input
              type="password"
              autoFocus
              autoComplete="current-password"
              className="w-full border border-border rounded-lg px-3 py-2 text-sm bg-card text-primary focus:outline-none focus:ring-2 focus:ring-accent"
              placeholder="••••••••••••"
              value={token}
              onChange={(e) => setTokenValue(e.target.value)}
            />
          </div>

          {error && (
            <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded px-3 py-2">{error}</p>
          )}

          <button
            type="submit"
            disabled={!token.trim() || loading}
            className="w-full bg-accent text-white rounded-lg py-2 text-sm font-medium hover:bg-accent-hover disabled:opacity-50 transition-colors"
          >
            {loading ? 'Checking…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}
