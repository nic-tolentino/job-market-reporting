import { useEffect, useState, useRef } from 'react';
import { Terminal, Activity, XCircle } from 'lucide-react';
import { useCrawlLogs } from '../hooks/useCrawlLogs';

export function ActiveCrawlMonitor() {
  const { logs, status, clearLogs } = useCrawlLogs();
  const [activeCrawls, setActiveCrawls] = useState<Set<string>>(new Set());
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Recompute active crawls based on current log buffer
    const active = new Set<string>();
    
    // Process logs from oldest to newest to track status accurately
    [...logs].reverse().forEach(log => {
      if (log.message.includes('Starting')) {
        if (log.companyId) active.add(log.companyId);
      } else if (log.message.includes('finished') || log.message.includes('failed')) {
        if (log.companyId) active.delete(log.companyId);
      }
    });
    
    setActiveCrawls(active);
  }, [logs]);

  return (
    <div className="bg-gray-900 rounded-lg border border-gray-800 flex flex-col h-[400px]">
      <div className="px-4 py-2.5 border-b border-gray-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Terminal size={14} className="text-blue-400" />
          <span className="text-xs font-semibold text-gray-300 uppercase tracking-wider font-mono">
            Live Crawl Monitor
          </span>
          {status === 'connected' && (
            <span className="flex h-2 w-2 rounded-full bg-green-500 animate-pulse" />
          )}
        </div>
        <div className="flex items-center gap-3">
          <button 
            onClick={clearLogs}
            className="text-[10px] text-gray-500 hover:text-gray-300 transition-colors uppercase font-bold"
          >
            Clear
          </button>
          <div className="flex items-center gap-1.5">
            <Activity size={12} className="text-green-400" />
            <span className="text-[10px] text-gray-400">{activeCrawls.size} active</span>
          </div>
          {status === 'error' && (
            <div className="flex items-center gap-1 text-[10px] text-red-400">
              <XCircle size={12} />
              <span>Connection failed</span>
            </div>
          )}
        </div>
      </div>

      <div 
        ref={scrollRef}
        className="flex-1 overflow-auto p-4 font-mono text-[11px] space-y-1.5 selection:bg-blue-500/30"
      >
        {logs.length === 0 && status === 'connected' && (
          <div className="text-gray-600 italic">Waiting for crawl activity...</div>
        )}
        {logs.map((log, i) => (
          <div key={i} className="flex gap-2">
            <span className="text-gray-500 shrink-0">
              [{new Date(log.timestamp).toLocaleTimeString([], { hour12: false })}]
            </span>
            <span className={`shrink-0 font-bold ${
              log.level === 'ERROR' ? 'text-red-400' : 
              log.level === 'SUCCESS' ? 'text-green-400' : 'text-blue-400'
            }`}>
              {(log.companyId || 'SYSTEM').padStart(12)}
            </span>
            <span className="text-gray-300 break-all">{log.message}</span>
          </div>
        ))}
      </div>

      {activeCrawls.size > 0 && (
        <div className="px-4 py-2 border-t border-gray-800 bg-gray-900/50 flex gap-2 overflow-hidden items-center">
          <span className="text-[10px] text-gray-500 uppercase font-bold shrink-0">Running:</span>
          {Array.from(activeCrawls).map(id => (
            <span key={id} className="text-[10px] bg-blue-900/30 text-blue-400 px-2 py-0.5 rounded border border-blue-800/50">
              {id}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
