import React, { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react';
import { getLogStreamUrl } from '../lib/adminApi';
import { getToken } from '../lib/auth';

export interface CrawlLog {
  timestamp: number;
  level: string;
  companyId?: string;
  message: string;
}

interface CrawlLogContextType {
  logs: CrawlLog[];
  isConnected: boolean;
  clearLogs: () => void;
  maxLogs: number;
  setMaxLogs: (n: number) => void;
}

const CrawlLogContext = createContext<CrawlLogContextType | undefined>(undefined);

export const CrawlLogProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [logs, setLogs] = useState<CrawlLog[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [maxLogs, setMaxLogsState] = useState(1000);
  
  // Use a ref for maxLogs to avoid restarting the SSE connection when maxLogs changes
  const maxLogsRef = useRef(maxLogs);
  
  const setMaxLogs = useCallback((n: number) => {
    maxLogsRef.current = n;
    setMaxLogsState(n);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  useEffect(() => {
    let eventSource: EventSource | null = null;
    let retryTimeout: any;

    const connect = () => {
      const token = getToken();
      if (!token) return;

      // EventSource doesn't support custom headers natively for the connection,
      // but our backend expects "Authorization: Bearer <token>".
      // Usually, we use a URL param for EventSource or a polyfill.
      // However, the existing implementation uses adminApi.getLogStreamUrl() 
      // which handles the token (likely via a temporary cookie or URL param).
      
      const url = getLogStreamUrl(token);
      eventSource = new EventSource(url);

      eventSource.onopen = () => {
        setIsConnected(true);
        console.log('SSE: Connected to crawl logs');
      };

      eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          setLogs(prev => {
            const next = [...prev, data];
            if (next.length > maxLogsRef.current) {
              return next.slice(next.length - maxLogsRef.current);
            }
            return next;
          });
        } catch (e) {
          console.error('SSE: Failed to parse log message', e);
        }
      };

      eventSource.onerror = (e) => {
        setIsConnected(false);
        console.error('SSE: Error in crawl logs connection', e);
        eventSource?.close();
        // Retry logic
        retryTimeout = setTimeout(connect, 5000);
      };
    };

    connect();

    return () => {
      eventSource?.close();
      clearTimeout(retryTimeout);
    };
  }, []);

  return (
    <CrawlLogContext.Provider value={{ logs, isConnected, clearLogs, maxLogs, setMaxLogs }}>
      {children}
    </CrawlLogContext.Provider>
  );
};

export const useCrawlLogContext = () => {
  const context = useContext(CrawlLogContext);
  if (context === undefined) {
    throw new Error('useCrawlLogContext must be used within a CrawlLogProvider');
  }
  return context;
};
