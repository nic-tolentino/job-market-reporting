import { useQuery } from '@tanstack/react-query';
import { Activity, Clock, AlertCircle, CheckCircle2 } from 'lucide-react';
import { getQueueStats, getIngestionHistory } from '../lib/adminApi';
import { AdminLogPanel } from '../components/AdminLogPanel';

// Types for the pipeline page
interface QueueStats {
  status: string;
  name?: string;
  rateLimits?: {
    maxDispatchesPerSecond: number;
    maxConcurrentDispatches: number;
  };
  error?: string;
}

export function PipelinePage() {
  const { data: queue, isLoading: queueLoading } = useQuery<QueueStats>({
    queryKey: ['admin-pipeline-queue'],
    queryFn: getQueueStats,
    refetchInterval: 15000,
  });

  const { data: historyData } = useQuery({
    queryKey: ['admin-pipeline-history'],
    queryFn: getIngestionHistory,
  });

  const events = historyData?.data || [];

  return (
    <div className="p-6 space-y-6 max-w-5xl overflow-auto">
      <div>
        <h1 className="text-lg font-semibold text-primary">Pipeline</h1>
        <p className="text-sm text-muted mt-0.5">Cloud Tasks sync queue and ingestion history</p>
      </div>

      {/* Queue Status */}
      <div className="grid md:grid-cols-3 gap-4">
        <div className="bg-card border border-border rounded-xl p-4 shadow-theme-sm">
          <div className="flex items-center gap-2 text-muted mb-2">
            <Activity size={14} />
            <span className="text-xs font-medium uppercase tracking-wider">Queue status</span>
          </div>
          <div className="flex items-end gap-2">
            <span className={`text-2xl font-bold ${
              queue?.status === 'RUNNING' ? 'text-primary' : 'text-red-600 dark:text-red-400'
            }`}>
              {queueLoading ? '…' : (queue?.status ?? 'UNKNOWN')}
            </span>
            {queue?.status === 'RUNNING' && <span className="text-xs text-green-600 dark:text-green-400 mb-1">HEALTHY</span>}
          </div>
        </div>
        <div className="bg-card border border-border rounded-xl p-4 shadow-theme-sm">
          <div className="flex items-center gap-2 text-muted mb-2">
            <Clock size={14} />
            <span className="text-xs font-medium uppercase tracking-wider">Concurrency</span>
          </div>
          <div className="flex items-end gap-2">
            <span className="text-2xl font-bold text-primary">
              {queue?.rateLimits?.maxConcurrentDispatches ?? 0}
            </span>
            <span className="text-xs text-muted mb-1">max workers</span>
          </div>
        </div>
        <div className="bg-card border border-border rounded-xl p-4 shadow-theme-sm">
          <div className="flex items-center gap-2 text-muted mb-2">
            <AlertCircle size={14} />
            <span className="text-xs font-medium uppercase tracking-wider">Rate Limit</span>
          </div>
          <div className="flex items-end gap-2">
            <span className="text-2xl font-bold text-primary">
              {queue?.rateLimits?.maxDispatchesPerSecond ?? 0}
            </span>
            <span className="text-xs text-muted mb-1">tasks/sec</span>
          </div>
        </div>
      </div>

      {/* Recent Events */}
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="bg-card border border-border rounded-xl shadow-theme-sm overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-border bg-elevated">
            <h2 className="text-sm font-medium text-secondary">Recent Ingestion Events</h2>
          </div>
          <div className="divide-y divide-border-subtle overflow-y-auto max-h-[400px]">
            {events.map((event: any) => (
              <div key={event.eventId} className="px-4 py-3 flex items-center justify-between hover:bg-surface-hover transition-colors">
                <div className="flex items-center gap-3">
                  <div className={event.status === 'SUCCESS' ? 'text-green-500' : 'text-red-500'}>
                    <CheckCircle2 size={16} />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-primary">{event.type}</p>
                    <p className="text-xs text-muted">Source: {event.source} · {new Date(event.startedAt).toLocaleString()}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm font-semibold text-primary">{event.count ?? 0} jobs</p>
                  <p className="text-[10px] text-muted font-mono">ID: {event.eventId}</p>
                </div>
              </div>
            ))}
            {events.length === 0 && (
              <div className="px-4 py-8 text-center text-sm text-muted">
                No recent events logged
              </div>
            )}
          </div>
        </div>

        <AdminLogPanel />
      </div>

      {/* Cloud Tasks Info */}
      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800 rounded-lg p-4">
        <h3 className="text-sm font-medium text-blue-800 dark:text-blue-300 mb-1">Background Processing</h3>
        <p className="text-xs text-blue-700 dark:text-blue-400 leading-relaxed">
          The sync pipeline uses Google Cloud Tasks to distribute company crawls and record ingestions.
          Manual triggers from this panel are queued as tasks to ensure reliability and avoid
          Cloud Run timeout limits.
        </p>
      </div>
    </div>
  );
}
