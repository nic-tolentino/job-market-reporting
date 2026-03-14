import { useQuery } from '@tanstack/react-query';
import { CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import { getAdminHealth } from '../lib/adminApi';
import { MetricCard } from '../components/MetricCard';

function HealthChip({ label, status }: { label: string; status: string }) {
  const ok = status === 'ok';
  return (
    <div className={`flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full ${
      ok ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
    }`}>
      {ok ? <CheckCircle size={12} /> : <XCircle size={12} />}
      <span>{label}: {status}</span>
    </div>
  );
}

export function DashboardPage() {
  const { data: health } = useQuery({
    queryKey: ['admin-health'],
    queryFn: getAdminHealth,
    refetchInterval: 30000,
  });

  return (
    <div className="p-6 space-y-6 max-w-5xl">
      <div>
        <h1 className="text-lg font-semibold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-0.5">Pipeline overview and system health</p>
      </div>

      {/* Health strip */}
      <div className="flex flex-wrap gap-2">
        {health ? (
          <>
            <HealthChip label="Backend" status={health.backend} />
            <HealthChip label="Crawler" status={health.crawlerService} />
          </>
        ) : (
          <div className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full bg-gray-100 text-gray-500">
            <AlertTriangle size={12} /> Checking health…
          </div>
        )}
      </div>

      {/* Metric cards — placeholders until BigQuery analytics endpoints are built */}
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        <MetricCard label="Total companies" value="1,262" sub="in manifest" />
        <MetricCard
          label="Companies with active seed"
          value={null}
          sub="Requires crawler_seeds data"
        />
        <MetricCard label="Total jobs (Silver)" value={null} sub="Query raw_jobs" />
        <MetricCard label="Jobs added (7d)" value={null} sub="Query raw_jobs" />
        <MetricCard label="Gemini cost (24h)" value={null} sub="Requires crawl_runs" />
        <MetricCard label="Cost per job (24h)" value={null} sub="Requires crawl_runs" />
      </div>

      {/* Placeholder panels */}
      <div className="grid md:grid-cols-2 gap-4">
        <div className="border border-gray-200 rounded-lg p-4 bg-white">
          <h2 className="text-sm font-medium text-gray-700 mb-3">Recent activity</h2>
          <p className="text-xs text-gray-400">
            Activity feed will populate once crawl runs and ingestions are recorded in BigQuery.
          </p>
        </div>
        <div className="border border-gray-200 rounded-lg p-4 bg-white">
          <h2 className="text-sm font-medium text-gray-700 mb-3">Alerts</h2>
          <p className="text-xs text-gray-400">
            Alerts will appear as seed health data accumulates in crawler_seeds.
          </p>
        </div>
      </div>
    </div>
  );
}
