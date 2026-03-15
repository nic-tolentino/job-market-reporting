import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { X, Plus, RefreshCw } from 'lucide-react';
import { getCompany, upsertSeed, triggerCrawl } from '../lib/adminApi';
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

  if (editing) {
    return (
      <div className="border border-blue-200 rounded-lg p-3 bg-blue-50 space-y-2">
        <div>
          <label className="text-xs text-gray-600 block mb-0.5">URL</label>
          <input
            className="w-full text-sm border border-gray-300 rounded px-2 py-1 font-mono"
            value={editUrl}
            onChange={(e) => setEditUrl(e.target.value)}
          />
        </div>
        <div className="flex gap-2">
          <div className="flex-1">
            <label className="text-xs text-gray-600 block mb-0.5">Category</label>
            <select
              className="w-full text-sm border border-gray-300 rounded px-2 py-1"
              value={editCategory}
              onChange={(e) => setEditCategory(e.target.value as any)}
            >
              {CATEGORY_OPTIONS.map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>
          <div className="flex-1">
            <label className="text-xs text-gray-600 block mb-0.5">Status</label>
            <select
              className="w-full text-sm border border-gray-300 rounded px-2 py-1"
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
            className="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving…' : 'Save'}
          </button>
          <button
            onClick={() => setEditing(false)}
            className="text-sm text-gray-600 px-3 py-1 rounded border border-gray-300 hover:bg-gray-100"
          >
            Cancel
          </button>
        </div>
        {mutation.isError && (
          <p className="text-xs text-red-600">{(mutation.error as Error).message}</p>
        )}
      </div>
    );
  }

  return (
    <div className="border border-gray-200 rounded-lg p-3 hover:border-gray-300 transition-colors">
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <p className="text-xs font-mono text-gray-700 truncate">{seed.url}</p>
          <div className="flex items-center gap-2 mt-1">
            <StatusBadge status={seed.status} />
            {seed.category && (
              <span className="text-xs text-gray-500 bg-gray-100 rounded px-1.5 py-0.5">
                {seed.category}
              </span>
            )}
            {seed.atsProvider && (
              <span className="text-xs text-purple-700 bg-purple-50 rounded px-1.5 py-0.5">
                {seed.atsProvider}
              </span>
            )}
          </div>
          <div className="flex gap-3 mt-1.5 text-xs text-gray-500">
            {seed.lastKnownJobCount !== null && (
              <span>{seed.lastKnownJobCount} jobs</span>
            )}
            {seed.lastCrawledAt && (
              <span>crawled {new Date(seed.lastCrawledAt).toLocaleDateString()}</span>
            )}
            {seed.consecutiveZeroYieldCount > 0 && (
              <span className="text-orange-600">{seed.consecutiveZeroYieldCount}× zero yield</span>
            )}
          </div>
          {seed.atsDirectUrl && (
            <div className="mt-1.5 flex items-center gap-1 text-xs">
              <span className="text-gray-500">ATS hint:</span>
              <a
                href={seed.atsDirectUrl}
                target="_blank"
                rel="noreferrer"
                className="text-blue-600 underline truncate max-w-[200px]"
              >
                {seed.atsDirectUrl}
              </a>
              <button
                onClick={() => onTriggerCrawl(seed.atsDirectUrl!)}
                className="text-blue-600 hover:text-blue-800 font-medium"
              >
                Use as seed
              </button>
            </div>
          )}
          {seed.errorMessage && (
            <p className="mt-1 text-xs text-red-500 truncate">{seed.errorMessage}</p>
          )}
        </div>
        <div className="flex gap-1 shrink-0">
          <button
            onClick={() => onTriggerCrawl(seed.url)}
            title="Crawl now"
            className="p-1 text-gray-400 hover:text-blue-600 rounded"
          >
            <RefreshCw size={14} />
          </button>
          <button
            onClick={() => setEditing(true)}
            className="text-xs text-gray-500 hover:text-gray-800 px-2 py-1 rounded border border-gray-200 hover:border-gray-400"
          >
            Edit
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
  const [activeTab, setActiveTab] = useState<'seeds' | 'history'>('seeds');
  const [addingNew, setAddingNew] = useState(false);
  const [newUrl, setNewUrl] = useState('');
  const [newCategory, setNewCategory] = useState('tech-filtered');
  const [crawlStatus, setCrawlStatus] = useState<string | null>(null);

  const qc = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-company', companyId],
    queryFn: () => getCompany(companyId),
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

  const crawlMutation = useMutation({
    mutationFn: (url: string) => triggerCrawl(companyId, { url }),
    onMutate: () => setCrawlStatus('Running…'),
    onSuccess: (result: any) => {
      const stats = result?.crawlMeta?.extractionStats;
      const meta = result?.crawlMeta;
      const statsText = stats 
        ? `(${stats.jobsRaw} raw -> ${stats.jobsValid} valid -> ${stats.jobsTech} tech)`
        : `${meta?.totalJobsFound ?? 0} jobs`;
      
      setCrawlStatus(
        `Done — ${statsText}, ${meta?.pagesVisited ?? 0} pages`
      );
      qc.invalidateQueries({ queryKey: ['admin-company', companyId] });
    },
    onError: (e: Error) => setCrawlStatus(`Error: ${e.message}`),
  });

  const tabs = [
    { id: 'seeds' as const, label: `Seeds (${data?.seeds.length ?? '…'})` },
    { id: 'history' as const, label: `History (${data?.recentRuns.length ?? '…'})` },
  ];

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-gray-200 bg-white">
        <div>
          <h2 className="font-semibold text-gray-900 text-sm">{companyName}</h2>
          <p className="text-xs text-gray-500 font-mono">{companyId}</p>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X size={18} />
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 bg-white">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === tab.id
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {isLoading && <p className="text-sm text-gray-500 text-center py-8">Loading…</p>}
        {error && (
          <p className="text-sm text-red-500 text-center py-8">
            {(error as Error).message}
          </p>
        )}

        {/* Crawl status banner */}
        {crawlStatus && (
          <div className={`text-xs px-3 py-2 rounded ${
            crawlStatus.startsWith('Error') ? 'bg-red-50 text-red-700' : 'bg-blue-50 text-blue-700'
          }`}>
            {crawlStatus}
          </div>
        )}

        {/* Seeds tab */}
        {activeTab === 'seeds' && data && (
          <>
            {data.seeds.length === 0 && !addingNew && (
              <p className="text-sm text-gray-500 text-center py-4">No seeds yet</p>
            )}
            {data.seeds.map((seed) => (
              <SeedRow
                key={seed.url}
                seed={seed}
                companyId={companyId}
                onTriggerCrawl={(url) => crawlMutation.mutate(url)}
              />
            ))}

            {/* Add seed form */}
            {addingNew ? (
              <div className="border border-blue-200 rounded-lg p-3 bg-blue-50 space-y-2">
                <p className="text-xs font-medium text-gray-700">New seed</p>
                <input
                  className="w-full text-sm border border-gray-300 rounded px-2 py-1 font-mono"
                  placeholder="https://careers.example.com/jobs"
                  value={newUrl}
                  onChange={(e) => setNewUrl(e.target.value)}
                />
                <select
                  className="w-full text-sm border border-gray-300 rounded px-2 py-1"
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value)}
                >
                  {CATEGORY_OPTIONS.map((c) => <option key={c}>{c}</option>)}
                </select>
                <div className="flex gap-2">
                  <button
                    onClick={() => addSeedMutation.mutate()}
                    disabled={!newUrl || addSeedMutation.isPending}
                    className="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 disabled:opacity-50"
                  >
                    {addSeedMutation.isPending ? 'Saving…' : 'Add'}
                  </button>
                  <button
                    onClick={() => setAddingNew(false)}
                    className="text-sm text-gray-600 px-3 py-1 rounded border border-gray-300 hover:bg-gray-100"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => setAddingNew(true)}
                className="w-full flex items-center justify-center gap-1 text-sm text-blue-600 border border-dashed border-blue-300 rounded-lg py-2 hover:bg-blue-50 transition-colors"
              >
                <Plus size={14} /> Add seed
              </button>
            )}
          </>
        )}

        {/* History tab */}
        {activeTab === 'history' && data && (
          <>
            {data.recentRuns.length === 0 && (
              <p className="text-sm text-gray-500 text-center py-4">No crawl history yet</p>
            )}
            {data.recentRuns.map((run) => (
              <div
                key={run.runId}
                className="border border-gray-200 rounded-lg p-3 text-xs space-y-1"
              >
                <div className="flex items-center justify-between">
                  <span className="text-gray-500">
                    {new Date(run.startedAt).toLocaleString()}
                  </span>
                  <StatusBadge status={run.status} />
                </div>
                <p className="font-mono text-gray-600 truncate">{run.seedUrl}</p>
                <div className="flex gap-3 text-gray-500">
                  <span>{run.jobsFinal ?? 0} jobs</span>
                  {(run.jobsRaw !== null || run.jobsValid !== null || run.jobsTech !== null) && (
                    <span className="text-[10px] bg-gray-100 px-1 rounded">
                      {run.jobsRaw ?? '?'}/{run.jobsValid ?? '?'}/{run.jobsTech ?? '?'}
                    </span>
                  )}
                  <span>{run.pagesVisited ?? 0} pages</span>
                  {run.durationMs && <span>{(run.durationMs / 1000).toFixed(1)}s</span>}
                  {run.confidenceAvg !== null && run.confidenceAvg !== undefined && (
                    <span>conf {run.confidenceAvg.toFixed(2)}</span>
                  )}
                  {run.atsProvider && <span className="text-purple-600">{run.atsProvider}</span>}
                </div>
                {run.errorMessage && (
                  <p className="text-red-500 truncate">{run.errorMessage}</p>
                )}
              </div>
            ))}
          </>
        )}
      </div>
    </div>
  );
}
