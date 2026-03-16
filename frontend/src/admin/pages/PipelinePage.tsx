import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { 
  Activity, 
  Clock, 
  AlertCircle, 
  CheckCircle2, 
  RefreshCw, 
  Database, 
  Trash2, 
  Play,
  Upload,
  Heart,
  Building2,
  ChevronRight
} from 'lucide-react';
import {
  getQueueStats,
  getIngestionHistory,
  reprocessAll,
  wipeSilver,
  ingestDataset,
  deleteDataset,
  syncCompanies,
  runHealthCheck,
  createCrawlerDailyBatch,
  processCrawlerDataset,
} from '../lib/adminApi';
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
  const [ingestId, setIngestId] = useState('');
  const [isWiping, setIsWiping] = useState(false);
  const [isReprocessing, setIsReprocessing] = useState(false);
  const [isIngesting, setIsIngesting] = useState(false);
  const [isSyncingCompanies, setIsSyncingCompanies] = useState(false);
  const [isCheckingHealth, setIsCheckingHealth] = useState(false);
  const [isCreatingBatch, setIsCreatingBatch] = useState(false);
  const [reprocessingId, setReprocessingId] = useState<string | null>(null);

  const { data: queue, isLoading: queueLoading } = useQuery<QueueStats>({
    queryKey: ['admin-pipeline-queue'],
    queryFn: getQueueStats,
    refetchInterval: 15000,
  });

  const { data: historyData, refetch: refetchHistory } = useQuery({
    queryKey: ['admin-pipeline-history'],
    queryFn: getIngestionHistory,
  });

  const events = historyData?.data || [];

  const handleReprocess = async () => {
    if (!confirm('This will wipe the Silver layer (Jobs & Companies) and reprocess everything from Bronze. Continue?')) return;
    setIsReprocessing(true);
    try {
      await reprocessAll();
      alert('Reprocessing complete');
      refetchHistory();
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setIsReprocessing(false);
    }
  };

  const handleWipe = async () => {
    if (!confirm('CRITICAL: This will DROP and recreate the Jobs and Companies tables. Data will be lost until the next reprocess. Continue?')) return;
    setIsWiping(true);
    try {
      await wipeSilver();
      alert('Silver tables wiped successfully');
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setIsWiping(false);
    }
  };

  const handleIngest = async () => {
    if (!ingestId) return;
    setIsIngesting(true);
    try {
      await ingestDataset(ingestId);
      alert(`Dataset ${ingestId} queued for ingestion`);
      setIngestId('');
      refetchHistory();
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setIsIngesting(false);
    }
  };

  const handleDeleteDataset = async (datasetId: string) => {
    if (!confirm(`This will permanently delete Bronze files and metadata for dataset ${datasetId}. Continue?`)) return;
    try {
      await deleteDataset(datasetId);
      alert('Dataset deleted successfully');
      refetchHistory();
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    }
  };

  const handleSyncCompanies = async () => {
    setIsSyncingCompanies(true);
    try {
      await syncCompanies();
      alert('Company manifest sync complete');
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setIsSyncingCompanies(false);
    }
  };

  const handleCreateDailyBatch = async () => {
    setIsCreatingBatch(true);
    try {
      const res = await createCrawlerDailyBatch();
      if (res.status === 'no_files') {
        alert(`No crawler GCS files found for today. Run some crawls first.`);
      } else {
        alert(`Daily batch created: ${res.datasetId} (${res.fileCount} files)`);
        refetchHistory();
      }
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setIsCreatingBatch(false);
    }
  };

  const handleReprocessCrawlerDataset = async (datasetId: string) => {
    if (!confirm(`Re-process crawler dataset ${datasetId} into raw_jobs?`)) return;
    setReprocessingId(datasetId);
    try {
      await processCrawlerDataset(datasetId);
      alert(`Dataset ${datasetId} re-processed successfully`);
      refetchHistory();
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setReprocessingId(null);
    }
  };

  const handleHealthCheck = async () => {
    setIsCheckingHealth(true);
    try {
      const res = await runHealthCheck();
      alert(`Health check complete: ${JSON.stringify(res.summary)}`);
    } catch (e: any) {
      alert(`Error: ${e.message}`);
    } finally {
      setIsCheckingHealth(false);
    }
  };

  return (
    <div className="p-6 space-y-6 max-w-5xl overflow-auto pb-20">
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-lg font-semibold text-primary">Pipeline</h1>
          <p className="text-sm text-muted mt-0.5">Cloud Tasks sync queue and ingestion history</p>
        </div>
        <div className="flex gap-2">
          <button 
            onClick={handleWipe}
            disabled={isWiping}
            className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-red-600 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg hover:bg-red-100 transition-colors disabled:opacity-50"
          >
            <Database size={14} />
            {isWiping ? 'Wiping...' : 'Wipe Silver Layer'}
          </button>
          <button 
            onClick={handleReprocess}
            disabled={isReprocessing}
            className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-primary bg-surface border border-border rounded-lg hover:bg-surface-hover transition-colors shadow-theme-sm disabled:opacity-50"
          >
            <RefreshCw size={14} className={isReprocessing ? 'animate-spin' : ''} />
            {isReprocessing ? 'Reprocessing...' : 'Reprocess All Data'}
          </button>
        </div>
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

      <div className="grid md:grid-cols-2 gap-6">
        {/* Pipeline Actions */}
        <div className="bg-card border border-border rounded-xl shadow-theme-sm overflow-hidden p-4 space-y-4">
          <div>
            <h2 className="text-sm font-medium text-secondary mb-4 flex items-center gap-2">
              <Play size={14} />
              Manual Ingestion
            </h2>
            <div className="flex gap-3">
              <div className="flex-1">
                <input 
                  type="text" 
                  placeholder="Apify Dataset ID"
                  value={ingestId}
                  onChange={(e) => setIngestId(e.target.value)}
                  className="w-full px-3 py-2 text-sm bg-surface border border-border rounded-lg focus:ring-2 focus:ring-primary/20 outline-none transition-all font-mono"
                />
              </div>
              <button 
                onClick={handleIngest}
                disabled={!ingestId || isIngesting}
                className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary rounded-lg hover:opacity-90 transition-opacity disabled:opacity-50 transition-all shadow-lg shadow-primary/20 whitespace-nowrap"
              >
                <Upload size={14} />
                {isIngesting ? 'Queuing...' : 'Trigger Ingest'}
              </button>
            </div>
            <p className="text-[10px] text-muted mt-2">
              Queue an Apify dataset for end-to-end ingestion (Bronze + Silver).
            </p>
          </div>

          <div className="pt-4 border-t border-border space-y-3">
            <h2 className="text-sm font-medium text-secondary flex items-center gap-2">
              <RefreshCw size={14} />
              Maintenance Tasks
            </h2>
            <div className="grid grid-cols-1 gap-2">
              <button 
                onClick={handleSyncCompanies}
                disabled={isSyncingCompanies}
                className="flex items-center justify-between px-3 py-2 text-sm text-secondary bg-surface border border-border rounded-lg hover:border-primary/30 transition-all group"
              >
                <div className="flex items-center gap-2">
                  <Building2 size={16} className="text-muted group-hover:text-primary transition-colors" />
                  <span>Sync Company Manifest</span>
                </div>
                {isSyncingCompanies ? <RefreshCw size={14} className="animate-spin text-muted" /> : <ChevronRight size={14} className="text-muted" />}
              </button>
              
              <button
                onClick={handleHealthCheck}
                disabled={isCheckingHealth}
                className="flex items-center justify-between px-3 py-2 text-sm text-secondary bg-surface border border-border rounded-lg hover:border-primary/30 transition-all group"
              >
                <div className="flex items-center gap-2">
                  <Heart size={16} className="text-muted group-hover:text-red-500 transition-colors" />
                  <span>Run Job Health Checks</span>
                </div>
                {isCheckingHealth ? <RefreshCw size={14} className="animate-spin text-muted" /> : <ChevronRight size={14} className="text-muted" />}
              </button>

              <button
                onClick={handleCreateDailyBatch}
                disabled={isCreatingBatch}
                className="flex items-center justify-between px-3 py-2 text-sm text-secondary bg-surface border border-border rounded-lg hover:border-primary/30 transition-all group"
              >
                <div className="flex items-center gap-2">
                  <Database size={16} className="text-muted group-hover:text-primary transition-colors" />
                  <span>Archive Today's Crawler Jobs</span>
                </div>
                {isCreatingBatch ? <RefreshCw size={14} className="animate-spin text-muted" /> : <ChevronRight size={14} className="text-muted" />}
              </button>
            </div>
          </div>
        </div>

        {/* Info Box */}
        <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800 rounded-xl p-5 flex flex-col justify-center">
          <h3 className="text-sm font-medium text-blue-800 dark:text-blue-300 mb-2 flex items-center gap-2">
            <Activity size={16} />
            Pipeline Architecture
          </h3>
          <p className="text-xs text-blue-700 dark:text-blue-400 leading-relaxed space-y-2">
            <span>
              The pipeline implements a **Medallion Architecture**:
            </span>
            <ul className="list-disc ml-4 mt-2 space-y-1">
              <li><strong>Bronze:</strong> Raw JSON is stored as immutable gzip files in GCS.</li>
              <li><strong>Silver:</strong> Cleaned, mapped, and deduplicated records in BigQuery.</li>
              <li><strong>Cloud Tasks:</strong> Used for reliability and handling long-running sync operations.</li>
            </ul>
          </p>
        </div>
      </div>

      {/* Recent Events & Logs */}
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="bg-card border border-border rounded-xl shadow-theme-sm overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-border bg-elevated flex justify-between items-center">
            <h2 className="text-sm font-medium text-secondary">Recent Ingestion Events</h2>
            <button 
              onClick={() => refetchHistory()}
              className="text-xs text-muted hover:text-primary transition-colors"
            >
              Refresh
            </button>
          </div>
          <div className="divide-y divide-border-subtle overflow-y-auto max-h-[500px]">
            {events.map((event: any) => (
              <div key={event.eventId} className="px-4 py-3 flex items-center justify-between hover:bg-surface-hover transition-colors group">
                <div className="flex items-center gap-3">
                  <div className={event.status === 'COMPLETED' || event.status === 'SUCCESS' ? 'text-green-500' : 'text-red-500'}>
                    <CheckCircle2 size={16} />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-primary">{event.type}</p>
                    <p className="text-xs text-muted">Source: {event.source} · {new Date(event.startedAt).toLocaleString()}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <div className="text-right mr-2">
                    <p className="text-sm font-semibold text-primary">{event.count ?? 0} jobs</p>
                    <p className="text-[10px] text-muted font-mono max-w-[120px] truncate" title={event.datasetId}>ID: {event.datasetId}</p>
                  </div>
                  {event.source === 'crawler' && (
                    <button
                      onClick={() => handleReprocessCrawlerDataset(event.datasetId)}
                      disabled={reprocessingId === event.datasetId}
                      className="p-1.5 text-muted hover:text-primary hover:bg-surface rounded-md transition-all opacity-0 group-hover:opacity-100"
                      title="Re-process crawler dataset into raw_jobs"
                    >
                      {reprocessingId === event.datasetId
                        ? <RefreshCw size={14} className="animate-spin" />
                        : <Play size={14} />}
                    </button>
                  )}
                  <button
                    onClick={() => handleDeleteDataset(event.datasetId)}
                    className="p-1.5 text-muted hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md transition-all opacity-0 group-hover:opacity-100"
                    title="Delete dataset"
                  >
                    <Trash2 size={14} />
                  </button>
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
    </div>
  );
}
