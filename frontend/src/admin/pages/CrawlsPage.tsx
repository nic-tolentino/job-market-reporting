import { useQuery } from '@tanstack/react-query';
import { listRuns } from '../lib/adminApi';
import { StatusBadge } from '../components/StatusBadge';
import { ActiveCrawlMonitor } from '../components/ActiveCrawlMonitor';
import { useActiveCrawl } from '../context/ActiveCrawlContext';
import { RefreshCw } from 'lucide-react';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

export function CrawlsPage() {
  const { activeCrawl } = useActiveCrawl();
  const isCrawlRunning = activeCrawl?.status === 'running';

  const { data, isLoading, error, refetch, isFetching } = useQuery({
    queryKey: ['admin-runs'],
    queryFn: () => listRuns({ limit: 100 }),
    // Poll every 5s while a crawl is running so new runs appear automatically
    refetchInterval: isCrawlRunning ? 5_000 : false,
    staleTime: 0,
  });

  return (
    <div className="flex flex-col h-full">
      <div className="px-6 py-4 border-b border-border bg-card">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-lg font-semibold text-primary">Crawl Runs</h1>
            <p className="text-sm text-muted mt-0.5">Execution history from crawl_runs</p>
          </div>
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border border-border text-secondary hover:border-primary disabled:opacity-50"
          >
            <RefreshCw size={12} className={isFetching ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
        {activeCrawl && (
          <div className={`mt-2 text-xs px-3 py-2 rounded flex items-center gap-2 ${
            activeCrawl.status === 'running'
              ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300'
              : activeCrawl.status === 'error'
              ? 'bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400'
              : 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300'
          }`}>
            {activeCrawl.status === 'running' && <RefreshCw size={11} className="animate-spin shrink-0" />}
            <span>
              <strong>{activeCrawl.companyName}</strong>
              {activeCrawl.status === 'running' && ` — crawling ${activeCrawl.url}…`}
              {activeCrawl.status === 'done' && ` — done: ${activeCrawl.result}`}
              {activeCrawl.status === 'error' && ` — error: ${activeCrawl.result}`}
            </span>
          </div>
        )}
      </div>

      <div className="p-6 pb-0">
        <ActiveCrawlMonitor />
      </div>

      <div className="flex-1 overflow-auto">
        {error && (
          <div className="p-6 text-sm text-red-500 dark:text-red-400">
            {(error as Error).message}
          </div>
        )}
        {isLoading && (
          <div className="p-6 text-sm text-muted text-center">Loading…</div>
        )}
        {data?.data.length === 0 && !isLoading && (
          <div className="p-6 text-sm text-muted text-center">
            No crawl runs yet. Runs are recorded in BigQuery after each crawl completes.
          </div>
        )}
        {data && data.data.length > 0 && (
          <table className="w-full text-left">
            <thead className="sticky top-0 bg-elevated border-b border-border z-10">
              <tr>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Company</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Started</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Duration</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Pages</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Jobs</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Conf</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">ATS</th>
                <th className="px-4 py-2 text-xs font-medium text-muted uppercase">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.data.map((run) => (
                <tr key={run.runId} className="border-b border-border-subtle hover:bg-surface-hover">
                  <td className="px-4 py-3 text-sm font-medium text-primary">{run.companyId}</td>
                  <td className="px-4 py-3 text-xs text-muted">{formatDate(run.startedAt)}</td>
                  <td className="px-4 py-3 text-sm text-secondary">
                    {run.durationMs ? `${(run.durationMs / 1000).toFixed(1)}s` : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm text-secondary">{run.pagesVisited ?? '—'}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={run.jobsFinal === 0 ? 'text-orange-500 dark:text-orange-400' : 'text-primary'}>
                      {run.jobsFinal ?? '—'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-secondary">
                    {run.confidenceAvg !== null && run.confidenceAvg !== undefined
                      ? run.confidenceAvg.toFixed(2)
                      : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm text-purple-600 dark:text-purple-400">{run.atsProvider ?? '—'}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={run.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
