import { useQuery } from '@tanstack/react-query';
import { listRuns } from '../lib/adminApi';
import { StatusBadge } from '../components/StatusBadge';
import { ActiveCrawlMonitor } from '../components/ActiveCrawlMonitor';

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

export function CrawlsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-runs'],
    queryFn: () => listRuns({ limit: 100 }),
  });

  return (
    <div className="flex flex-col h-full">
      <div className="px-6 py-4 border-b border-gray-200 bg-white">
        <h1 className="text-lg font-semibold text-gray-900">Crawl Runs</h1>
        <p className="text-sm text-gray-500 mt-0.5">Execution history from crawl_runs</p>
      </div>

      <div className="p-6 pb-0">
        <ActiveCrawlMonitor />
      </div>

      <div className="flex-1 overflow-auto">
        {error && (
          <div className="p-6 text-sm text-red-500">
            {(error as Error).message}
          </div>
        )}
        {isLoading && (
          <div className="p-6 text-sm text-gray-400 text-center">Loading…</div>
        )}
        {data?.data.length === 0 && !isLoading && (
          <div className="p-6 text-sm text-gray-400 text-center">
            No crawl runs yet. Runs are recorded in BigQuery after each crawl completes.
          </div>
        )}
        {data && data.data.length > 0 && (
          <table className="w-full text-left">
            <thead className="sticky top-0 bg-gray-50 border-b border-gray-200 z-10">
              <tr>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Company</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Started</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Duration</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Pages</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Jobs</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Conf</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">ATS</th>
                <th className="px-4 py-2 text-xs font-medium text-gray-500 uppercase">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.data.map((run) => (
                <tr key={run.runId} className="border-b border-gray-100 hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{run.companyId}</td>
                  <td className="px-4 py-3 text-xs text-gray-500">{formatDate(run.startedAt)}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {run.durationMs ? `${(run.durationMs / 1000).toFixed(1)}s` : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">{run.pagesVisited ?? '—'}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={run.jobsFinal === 0 ? 'text-orange-500' : 'text-gray-900'}>
                      {run.jobsFinal ?? '—'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600">
                    {run.confidenceAvg !== null && run.confidenceAvg !== undefined
                      ? run.confidenceAvg.toFixed(2)
                      : '—'}
                  </td>
                  <td className="px-4 py-3 text-sm text-purple-600">{run.atsProvider ?? '—'}</td>
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
