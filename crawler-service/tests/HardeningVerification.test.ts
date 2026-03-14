import { CrawlerService } from '../src/api/CrawlerService';
import { validateJobs } from '../src/validator/JobValidator';
import { GeminiExtractionService } from '../src/extraction/GeminiExtractionService';
import { NormalizedJob } from '../src/api/types';

describe('Hardening Verification Tests', () => {

    describe('Bug 1: postedAt Normalization & Validation', () => {
        it('should preserve dashes in postedAt and pass validation', () => {
            const gemini = new GeminiExtractionService('dummy-key');
            // Mock LLM response job
            const llmJob = {
                title: 'Software Engineer',
                postedAt: '2024-03-14'
            };
            
            const normalized = (gemini as any).toNormalizedJob(llmJob, 'TestCompany');
            expect(normalized.postedAt).toBe('2024-03-14');
            
            const validation = validateJobs([normalized]);
            expect(validation.validJobs.length).toBe(1);
            expect(validation.rejectedJobs.length).toBe(0);
        });

        it('should handle ISO strings with time correctly', () => {
            const gemini = new GeminiExtractionService('dummy-key');
            const llmJob = {
                title: 'Software Engineer',
                postedAt: '2024-03-14T12:00:00Z'
            };
            
            const normalized = (gemini as any).toNormalizedJob(llmJob, 'TestCompany');
            expect(normalized.postedAt).toBe('2024-03-14');
            
            const validation = validateJobs([normalized]);
            expect(validation.validJobs.length).toBe(1);
        });
    });

    describe('Arch 7: Confidence Score Alignment', () => {
        it('should return averageConfidence from validateJobs', () => {
            const jobs: NormalizedJob[] = [
                {
                    title: 'Software Engineer',
                    companyName: 'Test',
                    location: 'London',
                    postedAt: '2024-03-14',
                    applyUrl: 'https://example.com/apply',
                    source: 'Crawler',
                    platformId: 'test-1',
                    descriptionHtml: null,
                    descriptionText: null,
                    salaryMin: null,
                    salaryMax: null,
                    salaryCurrency: null,
                    employmentType: 'Full-time',
                    seniorityLevel: 'Senior',
                    workModel: 'Remote',
                    department: 'Engineering',
                    platformUrl: null
                }
            ];

            const result = validateJobs(jobs);
            expect(result.averageConfidence).toBeGreaterThan(0.8);
            expect(result.averageConfidence).toBeLessThanOrEqual(1.0);
        });
    });

    describe('Bug 4: Batch Indexing', () => {
        // The fix (i + batchIndex positional indexing) lives in server.ts and requires
        // a running Express app to exercise meaningfully. Covered by crawler.e2e.test.ts
        // when the server is available. A pure unit test here would only restate the
        // Array.prototype.map contract, not the server behaviour.
        test.todo('verify duplicate-companyId batch ordering via E2E (crawler.e2e.test.ts)');
    });
});
