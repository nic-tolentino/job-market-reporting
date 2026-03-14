import * as fs from 'fs';
import * as path from 'path';
import { extractContent } from '../src/extractor/ContentExtractor';
import { GeminiExtractionService } from '../src/extraction/GeminiExtractionService';

// Mock VertexAIClient to avoid real API calls during regression tests
// Unless a real key is provided in the environment
jest.mock('../src/extraction/VertexAIClient', () => {
  return {
    VertexAIClient: class {
      generateContent = jest.fn().mockImplementation(async (prompt: string) => {
        if (prompt.includes('Acme Corp')) {
          return {
            text: JSON.stringify([
              {
                title: 'Senior Software Engineer',
                location: 'Sydney, Australia',
                employmentType: 'Full-time',
                description: 'We are looking for a Senior Software Engineer...',
                applyUrl: 'https://example.com/jobs/123'
              },
              {
                title: 'Product Manager',
                location: 'Remote',
                employmentType: 'Full-time',
                description: 'Acme Corp is seeking a Product Manager...',
                applyUrl: 'https://example.com/jobs/456'
              }
            ]),
            usageMetadata: { promptTokenCount: 100, candidatesTokenCount: 50, totalTokenCount: 150 }
          };
        }
        return { text: '[]', usageMetadata: { promptTokenCount: 0, candidatesTokenCount: 0, totalTokenCount: 0 } };
      });
    }
  };
});

describe('Extraction Regression Tests', () => {
  const fixturesDir = path.join(__dirname, 'fixtures');
  
  it('correctly extracts jobs from acme-careers.html fixture', async () => {
    const html = fs.readFileSync(path.join(fixturesDir, 'acme-careers.html'), 'utf-8');
    
    // 1. Verify ContentExtractor
    const { textContent } = extractContent(html);
    expect(textContent).toContain('Senior Software Engineer');
    expect(textContent).toContain('Product Manager');
    
    // 2. Verify GeminiExtractionService (Mocked)
    const service = new GeminiExtractionService('dummy-key-of-sufficient-length');
    const result = await service.extractJobs(textContent, { companyName: 'Acme Corp' });
    
    expect(result.jobs).toHaveLength(2);
    expect(result.jobs[0].title).toBe('Senior Software Engineer');
    expect(result.jobs[1].title).toBe('Product Manager');
    expect(result.jobs[0].location).toBe('Sydney, Australia');
  });

  it('handles empty or malformed HTML gracefully', async () => {
    const html = '<html><body><h1>No Jobs Here</h1></body></html>';
    const { textContent } = extractContent(html);
    
    const service = new GeminiExtractionService('dummy-key-of-sufficient-length');
    const result = await service.extractJobs(textContent, { companyName: 'Empty Corp' });
    
    expect(result.jobs).toEqual([]);
  });
});
