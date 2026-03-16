import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { X, Plus, RefreshCw, Trash2, ExternalLink, FileText } from 'lucide-react';
import { getCompany, upsertSeed, deleteSeed, listJobsForCompany, deleteJobsForCompany } from '../lib/adminApi';
import { useActiveCrawl } from '../context/ActiveCrawlContext';
import { StatusBadge } from './StatusBadge';
import type { CrawlerSeed } from '../types/admin';

interface CompanyDetailPanelProps {
  companyId: string;
  companyName: string;
  onClose: () => void;
}

const CATEGORY_OPTIONS = ['tech-filtered', 'general', 'careers', 'homepage', 'unknown'];
const STATUS_OPTIONS = ['ACTIVE', 'STALE', 'BLOCKED', 'TIMEOUT'];

function SeedRow({
  seed,
  companyId,
  onTriggerCrawl,
}: {
  seed: CrawlerSeed;
  companyId: string;
  onTriggerCrawl: (url: string) => void;
}) {
  const qc = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [editUrl, setEditUrl] = useState(seed.url);
  const [editCategory, setEditCategory] = useState(seed.category ?? 'general');
  const [editStatus, setEditStatus] = useState(seed.status ?? 'ACTIVE');

  const mutation = useMutation({
    mutationFn: () =>
      upsertSeed({ companyId, url: editUrl, category: editCategory, status: editStatus }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-company', companyId] });
      setEditing(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteSeed(companyId, seed.url),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-company', companyId] }),
  });

  if (editing) {
    return (
      <div className="border border-blue-200 dark:border-blue-800 rounded-lg p-3 bg-blue-50 dark:bg-blue-900/20 space-y-2">
        <div>
          <label className="text-xs text-secondary block mb-0.5">URL</label>
          <input
            className="w-full text-sm border border-border bg-card text-primary rounded px-2 py-1 font-mono focus:outline-none focus:ring-2 focus:ring-accent"
            value={editUrl}
            onChange={(e) => setEditUrl(e.target.value)}
          />
        </div>
        <div className="flex gap-2">
          <div className="flex-1">
            <label className="text-xs text-secondary block mb-0.5">Category</label>
            <select
              className="w-full text-sm border border-border bg-card text-primary rounded px-2 py-1"
              value={editCategory}
              onChange={(e) => setEditCategory(e.target.value as any)}
            >
              {CATEGORY_OPTIONS.map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>
          <div className="flex-1">
            <label className="text-xs text-secondary block mb-0.5">Status</label>
            <select
              className="w-full text-sm border border-border bg-card text-primary rounded px-2 py-1"
              value={editStatus}
              onChange={(e) => setEditStatus(e.target.value as any)}
            >
              {STATUS_OPTIONS.map((s) => <option key={s}>{s}</option>)}
            </select>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending}
            className="text-sm bg-accent text-inverted px-3 py-1 rounded hover:bg-accent-hover disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving…' : 'Save'}
          </button>
          <button
            onClick={() => setEditing(false)}
            className="text-sm text-secondary px-3 py-1 rounded border border-border hover:bg-surface-hover"
          >
            Cancel
          </button>
        </div>
        {mutation.isError && (
          <p className="text-xs text-red-600 dark:text-red-400">{(mutation.error as Error).message}</p>
        )}
      </div>
    );
  }

  return (
    <div className="border border-border rounded-lg p-3 hover:border-border transition-colors">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-mono text-secondary truncate">{seed.url}</p>
          <div className="flex items-center gap-2 mt-1">
            <StatusBadge status={seed.status} />
            {seed.category && (
              <span className="text-xs text-muted bg-elevated rounded px-1.5 py-0.5">
                {seed.category}
              </span>
            )}
            {seed.atsProvider && (
              <span className="text-xs text-purple-700 dark:text-purple-300 bg-purple-50 dark:bg-purple-900/30 rounded px-1.5 py-0.5">
                {seed.atsProvider}
              </span>
            )}
          </div>
          <div className="flex gap-3 mt-1.5 text-xs text-muted">
            {seed.lastKnownJobCount !== null && (
              <span>{seed.lastKnownJobCount} jobs</span>
            )}
            {seed.lastCrawledAt && (
              <span>crawled {new Date(seed.lastCrawledAt).toLocaleDateString()}</span>
            )}
            {seed.consecutiveZeroYieldCount > 0 && (
              <span className="text-orange-600 dark:text-orange-400">{seed.consecutiveZeroYieldCount}× zero yield</span>
            )}
          </div>
          {seed.atsDirectUrl && (
            <div className="mt-1.5 flex items-center gap-1 text-xs">
              <span className="text-muted">ATS hint:</span>
              <a
                href={seed.atsDirectUrl}
                target="_blank"
                rel="noreferrer"
                className="text-blue-600 dark:text-blue-400 underline truncate max-w-[200px]"
              >
                {seed.atsDirectUrl}
              </a>
              <button
                onClick={() => onTriggerCrawl(seed.atsDirectUrl!)}
                className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 font-medium"
              >
                Use as seed
              </button>
            </div>
          )}
          {seed.errorMessage && (
            <p className="mt-1 text-xs text-red-500 dark:text-red-400 truncate">{seed.errorMessage}</p>
          )}
        </div>
        <div className="flex gap-1 shrink-0">
          <button
            onClick={() => onTriggerCrawl(seed.url)}
            title="Crawl now"
            className="p-1 text-muted hover:text-accent rounded"
          >
            <RefreshCw size={14} />
          </button>
          <button
            onClick={() => setEditing(true)}
            className="text-xs text-muted hover:text-primary px-2 py-1 rounded border border-border hover:border-primary"
          >
            Edit
          </button>
          <button
            onClick={() => {
              if (confirm(`Delete seed?\n${seed.url}`)) deleteMutation.mutate();
            }}
            disabled={deleteMutation.isPending}
            title="Delete seed"
            className="p-1 text-muted hover:text-red-500 dark:hover:text-red-400 rounded disabled:opacity-50"
          >
            <Trash2 size={14} />
          </button>
        </div>
      </div>
    </div>
  );
}

export function CompanyDetailPanel({
  companyId,
  companyName,
  onClose,
}: CompanyDetailPanelProps) {
  const [activeTab, setActiveTab] = useState<'seeds' | 'history' | 'jobs'>('seeds');
  const [addingNew, setAddingNew] = useState(false);
  const [newUrl, setNewUrl] = useState('');
  const [newCategory, setNewCategory] = useState('tech-filtered');
  const [selectedJobIds, setSelectedJobIds] = useState<Set<string>>(new Set());
  const [crawlFormOpen, setCrawlFormOpen] = useState(false);
  const [crawlUrl, setCrawlUrl] = useState('');
  const [crawlDiscovery, setCrawlDiscovery] = useState(false);

  const { activeCrawl, startCrawl, clearCrawl } = useActiveCrawl();
  const isThisCrawlRunning = activeCrawl?.companyId === companyId && activeCrawl.status === 'running';
  const thisCrawlResult = activeCrawl?.companyId === companyId ? activeCrawl : null;

  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-company', companyId],
    queryFn: () => getCompany(companyId),
  });

  const { data: jobsData, isLoading: jobsLoading, refetch: refetchJobs } = useQuery({
    queryKey: ['admin-company-jobs', companyId],
    queryFn: () => listJobsForCompany(companyId),
    enabled: activeTab === 'jobs',
  });

  const deleteJobsMutation = useMutation({
    mutationFn: (jobIds?: string[]) => deleteJobsForCompany(companyId, jobIds),
    onSuccess: () => {
      setSelectedJobIds(new Set());
      refetchJobs();
    },
  });

  const addSeedMutation = useMutation({
    mutationFn: () =>
      upsertSeed({ companyId, url: newUrl, category: newCategory, status: 'ACTIVE' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-company', companyId] });
      setAddingNew(false);
      setNewUrl('');
    },
  });

  const handleOpenCrawlForm = () => {
    // Pre-fill with first active seed URL, or website, or empty
    const firstActiveSeed = data?.seeds.find(s => s.status === 'ACTIVE')?.url
      ?? data?.seeds[0]?.url
      ?? data?.website
      ?? '';
    setCrawlUrl(firstActiveSeed);
    setCrawlFormOpen(true);
  };

  const handleStartCrawl = (url: string, discovery?: boolean) => {
    setCrawlFormOpen(false);
    startCrawl(companyId, data?.name ?? companyName, url, { isDiscovery: discovery ?? crawlDiscovery });
  };

  const jobs = jobsData?.data ?? [];
  const allJobIds = jobs.map(j => j.jobId);
  const allSelected = allJobIds.length > 0 && allJobIds.every(id => selectedJobIds.has(id));

  const toggleJob = (jobId: string) => {
    setSelectedJobIds(prev => {
      const next = new Set(prev);
      next.has(jobId) ? next.delete(jobId) : next.add(jobId);
      return next;
    });
  };

  const toggleAll = () => {
    setSelectedJobIds(allSelected ? new Set() : new Set(allJobIds));
  };

  const handleDeleteSelected = () => {
    const ids = Array.from(selectedJobIds);
    if (!confirm(`Delete ${ids.length} job${ids.length !== 1 ? 's' : ''} from the Silver table?`)) return;
    deleteJobsMutation.mutate(ids);
  };

  const handleDeleteAll = () => {
    if (!confirm(`Delete ALL ${jobs.length} jobs for this company from the Silver table? This cannot be undone.`)) return;
    deleteJobsMutation.mutate(undefined);
  };

  const tabs = [
    { id: 'seeds' as const, label: `Seeds (${data?.seeds.length ?? '…'})` },
    { id: 'history' as const, label: `History (${data?.recentRuns.length ?? '…'})` },
    { id: 'jobs' as const, label: `Jobs (${jobsData ? jobs.length : '…'})` },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b border-border bg-card">
        <div className="flex items-start justify-between">
          <div>
            <h2 className="font-semibold text-primary text-sm">{data?.name || companyName}</h2>
            <div className="flex items-center gap-2 mt-0.5">
              <p className="text-xs text-muted font-mono">{companyId}</p>
              {data?.website && (
                <>
                  <span className="text-muted">·</span>
                  <a
                    href={data.website}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-blue-600 dark:text-blue-400 hover:underline"
                  >
                    Website
                  </a>
                </>
              )}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handleOpenCrawlForm}
              disabled={isThisCrawlRunning}
              className="flex items-center gap-1.5 text-xs px-3 py-1.5 bg-accent text-inverted rounded-lg hover:bg-accent-hover disabled:opacity-50 transition-colors"
            >
              <RefreshCw size={12} className={isThisCrawlRunning ? 'animate-spin' : ''} />
              {isThisCrawlRunning ? 'Crawling…' : 'Crawl'}
            </button>
            <button onClick={onClose} className="text-muted hover:text-secondary">
              <X size={18} />
            </button>
          </div>
        </div>

        {/* Crawl form */}
        {crawlFormOpen && (
          <div className="mt-3 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg space-y-2">
            {/* Seed quick-pick */}
            {data?.seeds && data.seeds.length > 0 && (
              <div className="space-y-1">
                <p className="text-xs font-medium text-secondary">Crawl a seed</p>
                {data.seeds.map((seed) => (
                  <button
                    key={seed.url}
                    onClick={() => handleStartCrawl(seed.url)}
                    className="w-full text-left text-xs px-2 py-1.5 rounded border border-blue-200 dark:border-blue-700 bg-card hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors flex items-center gap-2"
                  >
                    <StatusBadge status={seed.status} />
                    <span className="font-mono truncate text-secondary">{seed.url}</span>
                  </button>
                ))}
                <p className="text-xs text-muted pt-1">— or enter a custom URL —</p>
              </div>
            )}
            {/* Custom URL input */}
            <input
              className="w-full text-sm border border-border bg-card text-primary rounded px-2 py-1.5 font-mono focus:outline-none focus:ring-2 focus:ring-accent"
              placeholder="https://careers.example.com/jobs"
              value={crawlUrl}
              onChange={(e) => setCrawlUrl(e.target.value)}
              autoFocus={!data?.seeds?.length}
            />
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={crawlDiscovery}
                onChange={(e) => setCrawlDiscovery(e.target.checked)}
                className="rounded border-border accent-accent"
              />
              <span className="text-xs text-secondary">Discovery mode</span>
              <span className="text-xs text-muted">(follow links to find job pages)</span>
            </label>
            <div className="flex gap-2">
              <button
                onClick={() => handleStartCrawl(crawlUrl)}
                disabled={!crawlUrl.trim()}
                className="text-sm bg-accent text-inverted px-3 py-1 rounded hover:bg-accent-hover disabled:opacity-50"
              >
                Start crawl
              </button>
              <button
                onClick={() => setCrawlFormOpen(false)}
                className="text-sm text-secondary px-3 py-1 rounded border border-border hover:bg-surface-hover"
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-border bg-card">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === tab.id
                ? 'border-accent text-accent'
                : 'border-transparent text-muted hover:text-secondary'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {isLoading && <p className="text-sm text-muted text-center py-8">Loading…</p>}
        {error && (
          <p className="text-sm text-red-500 dark:text-red-400 text-center py-8">
            {(error as Error).message}
          </p>
        )}

        {/* Crawl status banner */}
        {thisCrawlResult && (
          <div className={`text-xs px-3 py-2 rounded flex items-center justify-between gap-2 ${
            thisCrawlResult.status === 'error'
              ? 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300'
              : thisCrawlResult.status === 'running'
              ? 'bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300'
              : 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300'
          }`}>
            <span>
              {thisCrawlResult.status === 'running' && `Crawling ${thisCrawlResult.url}…`}
              {thisCrawlResult.status === 'done' && `Done — ${thisCrawlResult.result}`}
              {thisCrawlResult.status === 'error' && `Error: ${thisCrawlResult.result}`}
            </span>
            {thisCrawlResult.status !== 'running' && (
              <button onClick={clearCrawl} className="shrink-0 opacity-60 hover:opacity-100">✕</button>
            )}
          </div>
        )}

        {/* Seeds tab */}
        {activeTab === 'seeds' && data && (
          <>
            {data.seeds.length === 0 && !addingNew && (
              <p className="text-sm text-muted text-center py-4">No seeds yet</p>
            )}
            {data.seeds.map((seed) => (
              <SeedRow
                key={seed.url}
                seed={seed}
                companyId={companyId}
                onTriggerCrawl={(url) => startCrawl(companyId, data.name ?? companyName, url)}
              />
            ))}

            {/* Add seed form */}
            {addingNew ? (
              <div className="border border-blue-200 dark:border-blue-800 rounded-lg p-3 bg-blue-50 dark:bg-blue-900/20 space-y-2">
                <p className="text-xs font-medium text-secondary">New seed</p>
                <input
                  className="w-full text-sm border border-border bg-card text-primary rounded px-2 py-1 font-mono focus:outline-none focus:ring-2 focus:ring-accent"
                  placeholder="https://careers.example.com/jobs"
                  value={newUrl}
                  onChange={(e) => setNewUrl(e.target.value)}
                />
                <select
                  className="w-full text-sm border border-border bg-card text-primary rounded px-2 py-1"
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value)}
                >
                  {CATEGORY_OPTIONS.map((c) => <option key={c}>{c}</option>)}
                </select>
                <div className="flex gap-2">
                  <button
                    onClick={() => addSeedMutation.mutate()}
                    disabled={!newUrl || addSeedMutation.isPending}
                    className="text-sm bg-accent text-inverted px-3 py-1 rounded hover:bg-accent-hover disabled:opacity-50"
                  >
                    {addSeedMutation.isPending ? 'Saving…' : 'Add'}
                  </button>
                  <button
                    onClick={() => setAddingNew(false)}
                    className="text-sm text-secondary px-3 py-1 rounded border border-border hover:bg-surface-hover"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => setAddingNew(true)}
                className="w-full flex items-center justify-center gap-1 text-sm text-accent border border-dashed border-accent/40 rounded-lg py-2 hover:bg-accent-subtle transition-colors"
              >
                <Plus size={14} /> Add seed
              </button>
            )}
          </>
        )}

        {/* Jobs tab */}
        {activeTab === 'jobs' && (
          <>
            {/* Toolbar */}
            <div className="flex items-center justify-between gap-2 pb-2 border-b border-border">
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={toggleAll}
                  disabled={jobs.length === 0}
                  className="rounded border-border accent-accent"
                />
                <span className="text-xs text-muted">
                  {selectedJobIds.size > 0 ? `${selectedJobIds.size} selected` : `${jobs.length} jobs`}
                </span>
              </div>
              <div className="flex gap-2">
                {selectedJobIds.size > 0 && (
                  <button
                    onClick={handleDeleteSelected}
                    disabled={deleteJobsMutation.isPending}
                    className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-red-600 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg hover:bg-red-100 disabled:opacity-50 transition-colors"
                  >
                    <Trash2 size={12} />
                    Delete {selectedJobIds.size}
                  </button>
                )}
                {jobs.length > 0 && (
                  <button
                    onClick={handleDeleteAll}
                    disabled={deleteJobsMutation.isPending}
                    className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium text-muted border border-border rounded-lg hover:text-red-600 hover:border-red-300 disabled:opacity-50 transition-colors"
                  >
                    <Trash2 size={12} />
                    Delete all
                  </button>
                )}
              </div>
            </div>

            {jobsLoading && <p className="text-sm text-muted text-center py-8">Loading jobs…</p>}
            {deleteJobsMutation.isError && (
              <p className="text-xs text-red-500 py-1">{(deleteJobsMutation.error as Error).message}</p>
            )}

            {!jobsLoading && jobs.length === 0 && (
              <p className="text-sm text-muted text-center py-8">No jobs in Silver table</p>
            )}

            <div className="divide-y divide-border-subtle">
              {jobs.map((job) => (
                <div
                  key={job.jobId}
                  className={`flex items-start gap-2.5 py-2.5 group ${
                    selectedJobIds.has(job.jobId) ? 'bg-accent-subtle' : 'hover:bg-surface-hover'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={selectedJobIds.has(job.jobId)}
                    onChange={() => toggleJob(job.jobId)}
                    className="mt-0.5 rounded border-border accent-accent shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs font-medium text-primary truncate">{job.title}</span>
                      {job.hasDescription && (
                        <FileText size={10} className="text-green-500 shrink-0" title="Has description" />
                      )}
                      {job.applyUrl && (
                        <a
                          href={job.applyUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="text-muted hover:text-accent shrink-0"
                          title="Open apply URL"
                        >
                          <ExternalLink size={10} />
                        </a>
                      )}
                    </div>
                    <div className="flex gap-2 mt-0.5 text-[10px] text-muted flex-wrap">
                      {job.location && <span>{job.location}</span>}
                      {job.workModel && <span>{job.workModel}</span>}
                      {job.seniorityLevel && <span>{job.seniorityLevel}</span>}
                      <span className="text-muted/60">{job.source}</span>
                      {job.postedDate && <span>{job.postedDate}</span>}
                      {job.urlStatus && job.urlStatus !== 'UNKNOWN' && (
                        <span className={
                          job.urlStatus === 'ACTIVE' ? 'text-green-600 dark:text-green-400'
                          : job.urlStatus?.startsWith('CLOSED') ? 'text-red-500 dark:text-red-400'
                          : 'text-yellow-600 dark:text-yellow-400'
                        }>
                          {job.urlStatus}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}

        {/* History tab */}
        {activeTab === 'history' && data && (
          <>
            {data.recentRuns.length === 0 && (
              <p className="text-sm text-muted text-center py-4">No crawl history yet</p>
            )}
            {data.recentRuns.map((run) => (
              <div
                key={run.runId}
                className="border border-border rounded-lg p-3 text-xs space-y-1"
              >
                <div className="flex items-center justify-between">
                  <span className="text-muted">
                    {new Date(run.startedAt).toLocaleString()}
                  </span>
                  <StatusBadge status={run.status} />
                </div>
                <p className="font-mono text-secondary truncate">{run.seedUrl}</p>
                <div className="flex gap-3 text-muted flex-wrap">
                  <span>{run.jobsFinal ?? 0} jobs</span>
                  {(run.jobsRaw !== null || run.jobsValid !== null || run.jobsTech !== null) && (
                    <span className="text-[10px] bg-elevated px-1 rounded" title="raw / valid / tech">
                      {run.jobsRaw ?? '?'}/{run.jobsValid ?? '?'}/{run.jobsTech ?? '?'}
                    </span>
                  )}
                  {run.detailPagesAttempted !== null && run.detailPagesAttempted !== undefined && (
                    <span
                      className={`text-[10px] px-1 rounded ${
                        run.descriptionCoverage !== null && run.descriptionCoverage !== undefined
                          ? run.descriptionCoverage >= 0.8
                            ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                            : run.descriptionCoverage >= 0.4
                            ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300'
                            : 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300'
                          : 'bg-elevated'
                      }`}
                      title={`Detail pages: ${run.detailPagesEnriched ?? 0}/${run.detailPagesAttempted} enriched`}
                    >
                      desc {run.descriptionCoverage !== null && run.descriptionCoverage !== undefined
                        ? `${Math.round(run.descriptionCoverage * 100)}%`
                        : '–'}
                    </span>
                  )}
                  <span>{run.pagesVisited ?? 0} pages</span>
                  {run.durationMs && <span>{(run.durationMs / 1000).toFixed(1)}s</span>}
                  {run.confidenceAvg !== null && run.confidenceAvg !== undefined && (
                    <span>conf {run.confidenceAvg.toFixed(2)}</span>
                  )}
                  {run.atsProvider && <span className="text-purple-600 dark:text-purple-400">{run.atsProvider}</span>}
                </div>
                {run.errorMessage && (
                  <p className="text-red-500 dark:text-red-400 truncate">{run.errorMessage}</p>
                )}
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  );
}
