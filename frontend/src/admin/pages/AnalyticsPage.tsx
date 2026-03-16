import { useQuery } from '@tanstack/react-query';
import { BarChart3, Users, Briefcase, History, MessageSquare, XCircle } from 'lucide-react';
import { getAnalyticsSummary, getFeedback } from '../lib/adminApi';
import { MetricCard } from '../components/MetricCard';

export function AnalyticsPage() {
  const { data: summary, isLoading: loadingSummary, error: errorSummary } = useQuery({
    queryKey: ['analytics-summary'],
    queryFn: getAnalyticsSummary,
    refetchInterval: 60000,
  });

  const { data: feedback, isLoading: loadingFeedback, error: errorFeedback } = useQuery({
    queryKey: ['admin-feedback'],
    queryFn: getFeedback,
  });

  if (loadingSummary || loadingFeedback) {
    return <div className="p-6 text-muted text-sm animate-pulse">Loading analytics...</div>;
  }

  if (errorSummary || errorFeedback) {
    return (
      <div className="p-6">
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg text-sm flex items-center gap-2">
          <XCircle size={16} />
          Failed to load analytics data. Please check your connection or permissions.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 max-w-6xl overflow-auto h-full">
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-lg font-semibold text-primary flex items-center gap-2">
            <BarChart3 size={20} className="text-accent" />
            Analytics & Insights
          </h1>
          <p className="text-sm text-muted mt-0.5">Platform-wide metrics and user feedback</p>
        </div>
      </div>

      {/* Hero Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <MetricCard
          label="Total Job Records"
          value={summary?.totalJobsInPersistence?.toLocaleString()}
          icon={<Briefcase size={16} />}
          sub="Stored in BigQuery"
        />
        <MetricCard
          label="Active Vacancies"
          value={summary?.activeJobs?.toLocaleString()}
          icon={<Briefcase size={16} />}
          sub="Currently live"
        />
        <MetricCard
          label="Total Vacancies"
          value={summary?.globalStats?.totalVacancies?.toLocaleString()}
          icon={<Briefcase size={16} />}
          sub="Aggregate count"
        />
        <MetricCard
          label="Remote Ratio"
          value={`${summary?.globalStats?.remotePercentage}%`}
          icon={<Users size={16} />}
          sub="Global average"
        />
      </div>

      <div className="grid md:grid-cols-3 gap-6">
        {/* Recent Ingestions */}
        <div className="md:col-span-2 space-y-4">
          <div className="bg-card border border-border rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-border-subtle bg-elevated flex items-center justify-between">
              <h2 className="text-sm font-semibold text-secondary flex items-center gap-2">
                <History size={16} />
                Recent Ingestions
              </h2>
            </div>
            <div className="divide-y divide-border-subtle">
              {summary?.recentIngestions?.map((ing: any) => (
                <div key={ing.datasetId} className="px-4 py-3 hover:bg-surface-hover transition-colors flex items-center justify-between">
                  <div>
                    <p className="text-xs font-mono text-primary">{ing.datasetId}</p>
                    <p className="text-[10px] text-muted mt-0.5">
                      {new Date(ing.ingestedAt).toLocaleString()}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-semibold text-primary">{ing.recordCount} rows</p>
                    <span className={`text-[10px] px-1.5 py-0.5 rounded-full inline-block mt-1 ${
                      ing.status === 'COMPLETED'
                        ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                        : 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                    }`}>
                      {ing.status}
                    </span>
                  </div>
                </div>
              ))}
              {(!summary?.recentIngestions || summary.recentIngestions.length === 0) && (
                <div className="px-4 py-8 text-center text-xs text-muted italic">
                  No recent ingestion data available
                </div>
              )}
            </div>
          </div>

          {/* User Feedback */}
          <div className="bg-card border border-border rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-border-subtle bg-elevated flex items-center justify-between">
              <h2 className="text-sm font-semibold text-secondary flex items-center gap-2">
                <MessageSquare size={16} />
                Recent Feedback
              </h2>
            </div>
            <div className="divide-y divide-border-subtle">
              {feedback?.slice(0, 5).map((f: any, i: number) => (
                <div key={i} className="px-4 py-3">
                  <div className="flex justify-between items-start">
                    <p className="text-xs font-medium text-primary truncate max-w-[200px]">
                      {f.context || 'General Feedback'}
                    </p>
                    <p className="text-[10px] text-muted">
                      {new Date(f.timestamp).toLocaleDateString()}
                    </p>
                  </div>
                  <p className="text-xs text-secondary mt-1 line-clamp-2">{f.message}</p>
                </div>
              ))}
              {(!feedback || feedback.length === 0) && (
                <div className="px-4 py-8 text-center text-xs text-muted italic">
                  No feedback received yet
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Top Lists */}
        <div className="space-y-6">
          <div className="bg-card border border-border rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-border-subtle bg-elevated">
              <h2 className="text-sm font-semibold text-secondary">Top Technologies</h2>
            </div>
            <div className="p-3 space-y-2">
              {summary?.topTech?.slice(0, 10).map((tech: any) => (
                <div key={tech.name} className="flex items-center justify-between">
                  <span className="text-xs text-secondary">{tech.name}</span>
                  <span className="text-xs font-medium text-primary">{tech.count}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-card border border-border rounded-lg overflow-hidden">
            <div className="px-4 py-3 border-b border-border-subtle bg-elevated">
              <h2 className="text-sm font-semibold text-secondary">Market Leaders</h2>
            </div>
            <div className="p-3 space-y-2">
              {summary?.topCompanies?.slice(0, 10).map((comp: any) => (
                <div key={comp.name} className="flex items-center justify-between">
                  <span className="text-xs text-secondary truncate mr-2">{comp.name}</span>
                  <span className="text-xs font-medium text-primary">{comp.activeJobs}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
