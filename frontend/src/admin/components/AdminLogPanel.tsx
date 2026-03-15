import { useState, useEffect, useRef } from 'react';
import { Terminal, Search, Trash2 } from 'lucide-react';
import { useCrawlLogs } from '../hooks/useCrawlLogs';
import type { LogMessage } from '../hooks/useCrawlLogs';

export function AdminLogPanel() {
  const { logs, setLogs } = useCrawlLogs(100);
  const [filter, setFilter] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);

  // We want newest at bottom for the log panel
  const bottomLogs = [...logs].reverse();

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs]);

  const filteredLogs = bottomLogs.filter((l: LogMessage) => 
    l.message.toLowerCase().includes(filter.toLowerCase()) || 
    l.companyId.toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <div className="bg-gray-900 rounded-xl overflow-hidden flex flex-col h-[400px] border border-gray-800 shadow-xl">
      <div className="px-4 py-2 border-b border-gray-800 bg-gray-900/50 flex items-center justify-between">
        <div className="flex items-center gap-2 text-gray-400">
          <Terminal size={14} />
          <span className="text-xs font-mono font-medium uppercase tracking-wider">Live Log Stream</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search className="absolute left-2 top-1/2 -translate-y-1/2 text-gray-500" size={12} />
            <input 
              type="text" 
              placeholder="Filter logs..." 
              value={filter}
              onChange={e => setFilter(e.target.value)}
              className="bg-gray-800 text-xs text-gray-300 pl-7 pr-2 py-1 rounded border border-gray-700 focus:outline-none focus:border-blue-500 w-40"
            />
          </div>
          <button 
            onClick={() => setLogs([])}
            className="text-gray-500 hover:text-white transition-colors"
            title="Clear logs"
          >
            <Trash2 size={14} />
          </button>
        </div>
      </div>
      
      <div 
        ref={scrollRef}
        className="flex-1 overflow-y-auto p-4 font-mono text-[11px] leading-relaxed space-y-1"
      >
        {filteredLogs.length === 0 && (
          <div className="h-full flex items-center justify-center text-gray-600 italic">
            Waiting for logs...
          </div>
        )}
        {filteredLogs.map((log: LogMessage, i: number) => (
          <div key={i} className="flex gap-3 animate-in fade-in duration-300">
            <span className="text-gray-600 shrink-0 select-none">
              {new Date(log.timestamp).toLocaleTimeString([], { hour12: false })}
            </span>
            <span className={`shrink-0 font-bold select-none w-12 ${
              log.level === 'ERROR' ? 'text-red-500' : 
              log.level === 'WARNING' ? 'text-yellow-500' : 
              log.level === 'SUCCESS' ? 'text-green-500' : 
              'text-blue-400'
            }`}>
              [{log.level}]
            </span>
            <span className="text-purple-400 shrink-0 select-none w-20 truncate">
              {log.companyId}:
            </span>
            <span className="text-gray-300 break-all">
              {log.message}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
