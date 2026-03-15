import { useCrawlLogContext } from '../context/CrawlLogContext';

export interface LogMessage {
  companyId?: string;
  level: string;
  message: string;
  timestamp: number;
}

export function useCrawlLogs() {
  const { logs, isConnected, clearLogs } = useCrawlLogContext();
  
  return { 
    logs, 
    status: isConnected ? 'connected' : 'connecting',
    clearLogs
  };
}
