/**
 * Mock Gemini service for testing
 */
export class MockGeminiService {
  private responseQueue: any[] = [];
  private shouldFail = false;
  private failWith?: Error;

  /**
   * Queues a response to be returned by the next generateContent call
   */
  mockResponse(response: { jobs: any[]; tokenUsage?: { input: number; output: number } }): void {
    this.responseQueue.push({
      success: true,
      data: response
    });
  }

  /**
   * Configures the mock to fail on next call
   */
  mockFailure(error?: Error): void {
    this.shouldFail = true;
    this.failWith = error || new Error('Gemini API error');
  }

  /**
   * Resets mock state
   */
  reset(): void {
    this.responseQueue = [];
    this.shouldFail = false;
    this.failWith = undefined;
  }

  /**
   * Simulates the Gemini API generateContent method
   */
  async generateContent(prompt: string): Promise<{
    response: {
      text: () => string;
      usageMetadata?: {
        promptTokenCount?: number;
        candidatesTokenCount?: number;
      };
    };
  }> {
    if (this.shouldFail && this.failWith) {
      throw this.failWith;
    }

    const queued = this.responseQueue.shift();
    if (!queued) {
      // Return default response if nothing queued
      return {
        response: {
          text: () => JSON.stringify([]),
          usageMetadata: {
            promptTokenCount: 1000,
            candidatesTokenCount: 500
          }
        }
      };
    }

    if (queued.success) {
      return {
        response: {
          text: () => JSON.stringify(queued.data.jobs),
          usageMetadata: queued.data.tokenUsage || {
            promptTokenCount: 1000,
            candidatesTokenCount: 500
          }
        }
      };
    }

    throw queued.error;
  }
}

/**
 * Creates a mock job for testing
 */
export function createMockJob(overrides: any = {}): any {
  return {
    platformId: `mock-job-${Date.now()}`,
    source: 'Crawler',
    title: 'Software Engineer',
    companyName: 'Test Corp',
    location: 'Sydney, AU',
    descriptionHtml: null,
    descriptionText: 'We are hiring a software engineer...',
    salaryMin: 120000,
    salaryMax: 150000,
    salaryCurrency: 'AUD',
    employmentType: 'Full-time',
    seniorityLevel: 'Mid',
    workModel: 'Hybrid',
    department: 'Engineering',
    postedAt: new Date().toISOString().split('T')[0],
    applyUrl: 'https://testcorp.com/apply/123',
    platformUrl: 'https://testcorp.com/careers/123',
    ...overrides
  };
}

/**
 * Loads a fixture file for testing
 */
export function loadFixture(name: string): string {
  // In real tests, this would load from a fixtures directory
  const fixtures: Record<string, string> = {
    'airwallex-careers.html': `
      <!DOCTYPE html>
      <html>
        <head><title>Careers - Airwallex</title></head>
        <body>
          <nav>Navigation</nav>
          <main>
            <h1>Careers at Airwallex</h1>
            <div class="job-listing">
              <h2>Senior Software Engineer</h2>
              <p class="location">Melbourne, AU</p>
              <p class="description">We are looking for...</p>
            </div>
            <div class="job-listing">
              <h2>Product Manager</h2>
              <p class="location">Sydney, AU</p>
              <p class="description">Lead our product team...</p>
            </div>
          </main>
          <footer>Footer</footer>
          <script>alert('should be removed')</script>
        </body>
      </html>
    `,
    'greenhouse-embedded.html': `
      <!DOCTYPE html>
      <html>
        <body>
          <iframe src="https://boards.greenhouse.io/airwallex"></iframe>
        </body>
      </html>
    `,
    'lever-embedded.html': `
      <!DOCTYPE html>
      <html>
        <body>
          <script src="https://assets.lever.co/widget.js"></script>
        </body>
      </html>
    `,
    'empty-careers.html': `
      <!DOCTYPE html>
      <html>
        <head><title>Careers</title></head>
        <body>
          <h1>No open positions</h1>
          <p>Check back later!</p>
        </body>
      </html>
    `
  };

  return fixtures[name] || '';
}
