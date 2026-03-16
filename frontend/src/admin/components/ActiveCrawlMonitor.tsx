import { useEffect, useRef } from 'react';
import { Terminal, Activity, XCircle } from 'lucide-react';
import { useCrawlLogs } from '../hooks/useCrawlLogs';

const URL_RE = /https?:\/\/[^\s]+/g;

/** Renders a log message, highlighting any embedded URLs in cyan. */
function LogMessage({ message }: { message: string }) {
  const parts: React.ReactNode[] = [];
  let last = 0;
  let match: RegExpExecArray | null;
  URL_RE.lastIndex = 0;
  while ((match = URL_RE.exec(message)) !== null) {
    if (match.index > last) parts.push(message.slice(last, match.index));
    parts.push(
      <span key={match.index} className="text-cyan-400 break-all">{match[0]}</span>
    );
    last = match.index + match[0].length;
  }
  if (last < message.length) parts.push(message.slice(last));
  return <span className="text-gray-300 break-all">{parts}</span>;
}

export function ActiveCrawlMonitor() {
  const { logs, status, clearLogs } = useCrawlLogs();
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new logs arrive
  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [logs]);

  // Derive active crawls from log stream
  const activeCrawls = new Set<string>();
  [...logs].reverse().forEach(log => {
    if (log.message.startsWith('Starting crawl')) {
      if (log.companyId) activeCrawls.add(log.companyId);
    } else if (log.message.startsWith('Crawl complete') || log.message.startsWith('Crawl failed')) {
      if (log.companyId) activeCrawls.delete(log.companyId);
    }
  });

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
          <div key={i} className={`flex gap-2 rounded px-1 -mx-1 ${
            log.level === 'ERROR'   ? 'bg-red-950/30' :
            log.level === 'WARNING' ? 'bg-yellow-950/30' :
            log.level === 'SUCCESS' ? 'bg-green-950/20' : ''
          }`}>
            <span className="text-gray-500 shrink-0">
              [{new Date(log.timestamp).toLocaleTimeString([], { hour12: false })}]
            </span>
            <span className={`shrink-0 font-bold ${
              log.level === 'ERROR'   ? 'text-red-400' :
              log.level === 'SUCCESS' ? 'text-green-400' :
              log.level === 'WARNING' ? 'text-yellow-400' : 'text-blue-400'
            }`}>
              {(log.companyId || 'SYSTEM').padStart(12)}
            </span>
            <LogMessage message={log.message} />
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
