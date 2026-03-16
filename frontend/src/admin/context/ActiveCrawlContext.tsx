import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { triggerCrawl } from '../lib/adminApi';

export interface ActiveCrawl {
  companyId: string;
  companyName: string;
  url: string;
  status: 'running' | 'done' | 'error';
  result?: string;
}

interface ActiveCrawlContextType {
  activeCrawl: ActiveCrawl | null;
  startCrawl: (companyId: string, companyName: string, url: string) => void;
  clearCrawl: () => void;
}

const ActiveCrawlContext = createContext<ActiveCrawlContextType | undefined>(undefined);

export const ActiveCrawlProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [activeCrawl, setActiveCrawl] = useState<ActiveCrawl | null>(null);
  const queryClient = useQueryClient();
  // Prevent double-firing if startCrawl is called while one is already running
  const inFlight = useRef(false);

  const startCrawl = useCallback((companyId: string, companyName: string, url: string) => {
    if (inFlight.current) return;
    inFlight.current = true;

    setActiveCrawl({ companyId, companyName, url, status: 'running' });

    triggerCrawl(companyId, { url })
      .then((result: any) => {
        const stats = result?.crawlMeta?.extractionStats;
        const meta = result?.crawlMeta;
        const summary = stats
          ? `${stats.jobsRaw} raw → ${stats.jobsValid} valid → ${stats.jobsTech} tech, ${meta?.pagesVisited ?? 0} pages`
          : `${meta?.totalJobsFound ?? 0} jobs, ${meta?.pagesVisited ?? 0} pages`;
        setActiveCrawl(prev => prev ? { ...prev, status: 'done', result: summary } : null);
        queryClient.invalidateQueries({ queryKey: ['admin-company', companyId] });
        queryClient.invalidateQueries({ queryKey: ['admin-companies'] });
        queryClient.invalidateQueries({ queryKey: ['admin-runs'] });
      })
      .catch((e: Error) => {
        setActiveCrawl(prev => prev ? { ...prev, status: 'error', result: e.message } : null);
      })
      .finally(() => {
        inFlight.current = false;
      });
  }, [queryClient]);

  const clearCrawl = useCallback(() => {
    if (activeCrawl?.status === 'running') return; // don't clear while in flight
    setActiveCrawl(null);
  }, [activeCrawl]);

  return (
    <ActiveCrawlContext.Provider value={{ activeCrawl, startCrawl, clearCrawl }}>
      {children}
    </ActiveCrawlContext.Provider>
  );
};

export const useActiveCrawl = () => {
  const ctx = useContext(ActiveCrawlContext);
  if (!ctx) throw new Error('useActiveCrawl must be used within ActiveCrawlProvider');
  return ctx;
};
