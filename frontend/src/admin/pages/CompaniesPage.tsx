import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight, ArrowUp, ArrowDown, RefreshCw } from 'lucide-react';
import { listCompanies, syncManifest } from '../lib/adminApi';
import { StatusBadge } from '../components/StatusBadge';
import { CompanyDetailPanel } from '../components/CompanyDetailPanel';
import type { AdminCompany } from '../types/admin';

const SEED_STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'STALE', label: 'Stale' },
  { value: 'BLOCKED', label: 'Blocked' },
  { value: 'NONE', label: 'No seeds (Discovery)' },
];

const PRESET_FILTERS: { label: string; seedStatus?: string; description: string }[] = [
  { label: 'Discovery queue', seedStatus: 'NONE', description: 'No seeds yet' },
  { label: 'Needs attention', seedStatus: 'STALE', description: 'STALE seeds' },
  { label: 'Working well', seedStatus: 'ACTIVE', description: 'Active seeds' },
];


function formatDate(iso: string | null): string {
  if (!iso) return 'Never';
  const d = new Date(iso);
  const now = new Date();
  const diffDays = Math.floor((now.getTime() - d.getTime()) / 86400000);
  if (diffDays === 0) return 'Today';
  if (diffDays === 1) return '1d ago';
  if (diffDays < 30) return `${diffDays}d ago`;
  return d.toLocaleDateString();
}

function SortableHeader({
  label,
  field,
  currentSort,
  order,
  onSort,
}: {
  label: string;
  field: string;
  currentSort: string;
  order: 'ASC' | 'DESC';
  onSort: (field: string) => void;
}) {
  const active = currentSort === field;
  return (
    <th
      className="px-4 py-2 text-xs font-medium text-gray-500 uppercase tracking-wide cursor-pointer hover:bg-gray-100 transition-colors group"
      onClick={() => onSort(field)}
    >
      <div className="flex items-center gap-1">
        {label}
        <div className={`transition-opacity ${active ? 'opacity-100' : 'opacity-0 group-hover:opacity-40'}`}>
          {active && order === 'DESC' ? <ArrowDown size={12} /> : <ArrowUp size={12} />}
        </div>
      </div>
    </th>
  );
}

function CompanyRow({
  company,
  onClick,
}: {
  company: AdminCompany;
  onClick: () => void;
}) {
  return (
    <tr
      className="hover:bg-gray-50 cursor-pointer border-b border-gray-100"
      onClick={onClick}
    >
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          {company.logoUrl ? (
            <img
              src={company.logoUrl}
              alt=""
              className="w-6 h-6 rounded object-contain"
              onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
            />
          ) : (
            <div className="w-6 h-6 rounded bg-gray-200 flex items-center justify-center text-xs text-gray-500">
              {company.name[0]}
            </div>
          )}
          <div>
            <p className="text-sm font-medium text-gray-900">{company.name}</p>
            <p className="text-xs text-gray-400 font-mono">{company.companyId}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-3">
        <StatusBadge status={company.seedStatus} />
      </td>
      <td className="px-4 py-3 text-sm text-gray-600">
        {company.atsProvider ?? <span className="text-gray-300">—</span>}
      </td>
      <td className="px-4 py-3 text-sm text-gray-600">
        {formatDate(company.lastCrawledAt)}
      </td>
      <td className="px-4 py-3 text-sm">
        {company.seedCount > 0 ? (
          <span className={company.totalJobsLastRun === 0 ? 'text-orange-500' : 'text-gray-900'}>
            {company.totalJobsLastRun}
          </span>
        ) : (
          <span className="text-gray-300">—</span>
        )}
      </td>
      <td className="px-4 py-3 text-sm text-gray-600">
        {company.hqCountry ?? <span className="text-gray-300">—</span>}
      </td>
      <td className="px-4 py-3 text-sm text-gray-500">
        {company.seedCount}
      </td>
    </tr>
  );
}

export function CompaniesPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [seedStatus, setSeedStatus] = useState('');
  const [selected, setSelected] = useState<AdminCompany | null>(null);
  const [sortBy, setSortBy] = useState('name');
  const [sortOrder, setSortOrder] = useState<'ASC' | 'DESC'>('ASC');
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const handleSyncManifest = async () => {
    setSyncing(true);
    setSyncResult(null);
    try {
      const res = await syncManifest();
      setSyncResult(res.message ?? 'Sync complete');
      queryClient.invalidateQueries({ queryKey: ['admin-companies'] });
    } catch (e) {
      setSyncResult(`Error: ${(e as Error).message}`);
    } finally {
      setSyncing(false);
    }
  };

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortBy(field);
      setSortOrder('ASC');
    }
    setPage(0);
  };

  // Simple debounce on search
  const handleSearchChange = (val: string) => {
    setSearch(val);
    clearTimeout((window as any).__adminSearchTimer);
    (window as any).__adminSearchTimer = setTimeout(() => {
      setDebouncedSearch(val);
      setPage(0);
    }, 300);
  };

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-companies', page, debouncedSearch, seedStatus, sortBy, sortOrder],
    queryFn: () =>
      listCompanies({
        page,
        limit: 50,
        search: debouncedSearch || undefined,
        seedStatus: seedStatus || undefined,
        sortBy,
        sortOrder,
      }),
    placeholderData: (prev) => prev,
  });

  const totalPages = data ? Math.ceil(data.total / 50) : 0;

  return (
    <div className="flex h-full">
      {/* Main content */}
      <div className={`flex-1 flex flex-col min-w-0 ${selected ? 'hidden md:flex' : 'flex'}`}>
        {/* Toolbar */}
        <div className="px-6 py-4 border-b border-gray-200 bg-white space-y-3">
          <div className="flex items-center gap-3">
            <div className="relative flex-1 max-w-xs">
              <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                className="w-full pl-8 pr-3 py-1.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Search companies…"
                value={search}
                onChange={(e) => handleSearchChange(e.target.value)}
              />
            </div>
            <select
              className="text-sm border border-gray-300 rounded-lg px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={seedStatus}
              onChange={(e) => { setSeedStatus(e.target.value); setPage(0); }}
            >
              {SEED_STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
            {(debouncedSearch || seedStatus) && (
              <button
                onClick={() => { setSearch(''); setDebouncedSearch(''); setSeedStatus(''); setPage(0); }}
                className="text-xs text-gray-500 hover:text-gray-800"
              >
                Clear
              </button>
            )}
            <div className="ml-auto flex items-center gap-2">
              {syncResult && (
                <span className={`text-xs ${syncResult.startsWith('Error') ? 'text-red-500' : 'text-green-600'}`}>
                  {syncResult}
                </span>
              )}
              <button
                onClick={handleSyncManifest}
                disabled={syncing}
                title="Sync company manifest (reads data/companies/**/*.json → BigQuery)"
                className="flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border border-gray-300 text-gray-600 hover:border-gray-500 hover:text-gray-900 disabled:opacity-50 transition-colors"
              >
                <RefreshCw size={12} className={syncing ? 'animate-spin' : ''} />
                Sync manifest
              </button>
            </div>
          </div>

          {/* Preset filters */}
          <div className="flex gap-2 flex-wrap">
            {PRESET_FILTERS.map((preset) => (
              <button
                key={preset.label}
                onClick={() => {
                  setSeedStatus(preset.seedStatus ?? '');
                  setPage(0);
                }}
                className={`text-xs px-3 py-1 rounded-full border transition-colors ${
                  seedStatus === (preset.seedStatus ?? '')
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'border-gray-300 text-gray-600 hover:border-gray-500'
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto">
          {error && (
            <div className="p-6 text-red-500 text-sm">
              Error: {(error as Error).message}
            </div>
          )}
          <table className="w-full text-left">
            <thead className="sticky top-0 bg-gray-50 border-b border-gray-200 z-10">
              <tr>
                <SortableHeader label="Company" field="name" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
                <SortableHeader label="Seed" field="seedStatus" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
                <SortableHeader label="ATS" field="atsProvider" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
                <SortableHeader label="Last crawl" field="lastCrawledAt" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
                <SortableHeader label="Jobs" field="totalJobsLastRun" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
                <SortableHeader label="Country" field="hqCountry" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
                <SortableHeader label="Seeds" field="seedCount" currentSort={sortBy} order={sortOrder} onSort={handleSort} />
              </tr>
            </thead>
            <tbody>
              {isLoading && !data && (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-sm text-gray-400">
                    Loading…
                  </td>
                </tr>
              )}
              {data?.data.map((company) => (
                <CompanyRow
                  key={company.companyId}
                  company={company}
                  onClick={() => setSelected(company)}
                />
              ))}
              {data?.data.length === 0 && !isLoading && (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-sm text-gray-400">
                    No companies found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && totalPages > 1 && (
          <div className="px-6 py-3 border-t border-gray-200 bg-white flex items-center justify-between">
            <p className="text-xs text-gray-500">
              {data.total.toLocaleString()} companies · page {page + 1} of {totalPages}
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-1 rounded text-gray-500 hover:text-gray-800 disabled:opacity-30"
              >
                <ChevronLeft size={16} />
              </button>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="p-1 rounded text-gray-500 hover:text-gray-800 disabled:opacity-30"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Detail panel (slide-in) */}
      {selected && (
        <div className="w-full md:w-[480px] border-l border-gray-200 bg-white flex flex-col h-full overflow-hidden">
          <CompanyDetailPanel
            companyId={selected.companyId}
            companyName={selected.name}
            onClose={() => setSelected(null)}
          />
        </div>
      )}
    </div>
  );
}
