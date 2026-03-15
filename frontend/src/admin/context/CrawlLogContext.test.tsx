import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import React from 'react';
import { CrawlLogProvider, useCrawlLogContext } from './CrawlLogContext';
import * as auth from '../lib/auth';

// Help testing by exposing context values in a test component
const TestComponent = () => {
  const { logs, isConnected } = useCrawlLogContext();
  return (
    <div>
      <div data-testid="status">{isConnected ? 'connected' : 'disconnected'}</div>
      <div data-testid="log-count">{logs.length}</div>
      <div data-testid="last-log">{logs[logs.length - 1]?.message}</div>
    </div>
  );
};

describe('CrawlLogContext', () => {
  let mockEventSource: any;

  beforeEach(() => {
    vi.spyOn(auth, 'getToken').mockReturnValue('test-token');
    
    mockEventSource = {
      close: vi.fn(),
      onopen: null,
      onmessage: null,
      onerror: null,
    };
    
    const MockEventSource = vi.fn().mockImplementation(function(this: any) {
      return mockEventSource;
    });

    vi.stubGlobal('EventSource', MockEventSource);
  });

  it('should connect on mount and handle messages', async () => {
    render(
      <CrawlLogProvider>
        <TestComponent />
      </CrawlLogProvider>
    );

    expect(EventSource).toHaveBeenCalled();
    
    // Simulate open
    act(() => {
      mockEventSource.onopen();
    });
    expect(screen.getByTestId('status')).toHaveTextContent('connected');

    // Simulate message
    act(() => {
      mockEventSource.onmessage({
        data: JSON.stringify({
          timestamp: Date.now(),
          level: 'INFO',
          message: 'Test message'
        })
      });
    });

    expect(screen.getByTestId('log-count')).toHaveTextContent('1');
    expect(screen.getByTestId('last-log')).toHaveTextContent('Test message');
  });

  it('should clean up EventSource on unmount', () => {
    const { unmount } = render(
      <CrawlLogProvider>
        <TestComponent />
      </CrawlLogProvider>
    );

    unmount();
    expect(mockEventSource.close).toHaveBeenCalled();
  });

  it('should respect maxLogs', () => {
    const TestMaxLogs = () => {
      const { logs, setMaxLogs } = useCrawlLogContext();
      return (
        <div>
          <button onClick={() => setMaxLogs(2)}>Set Max 2</button>
          <div data-testid="count">{logs.length}</div>
        </div>
      );
    };

    render(
      <CrawlLogProvider>
        <TestMaxLogs />
      </CrawlLogProvider>
    );

    act(() => {
      screen.getByText('Set Max 2').click();
    });

    // Send 3 messages
    act(() => {
      mockEventSource.onmessage({ data: JSON.stringify({ message: '1' }) });
      mockEventSource.onmessage({ data: JSON.stringify({ message: '2' }) });
      mockEventSource.onmessage({ data: JSON.stringify({ message: '3' }) });
    });

    expect(screen.getByTestId('count')).toHaveTextContent('2');
  });
});
