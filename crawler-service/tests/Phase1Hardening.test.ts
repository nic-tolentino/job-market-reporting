import http from 'http';

// 1. Hoisted Mocks
jest.mock('../src/extraction/VertexAIClient', () => {
    return {
        VertexAIClient: class {
            generateContent = jest.fn().mockImplementation(async (prompt: string) => {
                const text = prompt.toLowerCase().includes('role mixer') ? JSON.stringify([
                    { title: 'Senior Software Engineer', location: 'Remote', applyUrl: 'https://ex.com/1' },
                    { title: 'Customer Success Manager', location: 'Remote', applyUrl: 'https://ex.com/2' }, 
                    { title: 'Data Scientist', location: 'Remote', applyUrl: 'https://ex.com/3' },
                    { title: 'Receptionist', location: 'Remote', applyUrl: 'https://ex.com/4' }
                ]) : '[]';
                
                return { 
                    text, 
                    usageMetadata: { promptTokenCount: 1, candidatesTokenCount: 1, totalTokenCount: 2 } 
                };
            });
        }
    };
});

jest.mock('../src/utils/RobotsChecker', () => {
    return {
        RobotsChecker: class {
            canFetch = jest.fn().mockResolvedValue({ allowed: true });
        }
    };
});

import { CrawlerService } from '../src/api/CrawlerService';

describe('Phase 1 Hardening: Integration Tests', () => {
    let server: http.Server;
    const PORT = 8092; // Use a high, unique port
    const BASE_URL = `http://localhost:${PORT}`;

    beforeAll((done) => {
        server = http.createServer((req, res) => {
            res.writeHead(200, { 'Content-Type': 'text/html' });
            
            if (req.url === '/jobs/infinite') {
                res.end('<html><body><a href="/jobs/infinite?p=2" rel="next">Next</a></body></html>');
            } else if (req.url?.startsWith('/jobs/infinite?p=')) {
                const page = parseInt(req.url.split('=')[1] || '1');
                res.end(`<html><body><a href="/jobs/infinite?p=${page + 1}" rel="next">Next</a></body></html>`);
            } else if (req.url === '/jobs/role-mix' || req.url?.startsWith('/jobs/role-mix?')) {
                res.end('<html><body><h1>Role Mixer</h1><p>Check out our roles.</p></body></html>');
            } else {
                res.end('<html><body>Test Server</body></html>');
            }
        });
        server.listen(PORT, () => done());
    });

    afterAll((done) => {
        server.close(() => done());
    });

    it('Scenario 1: respects the pagination safety cap', async () => {
        const service = new CrawlerService('long-dummy-key-to-avoid-validation-errors');
        const result = await service.crawl({
            companyId: 'pagination-test',
            url: `${BASE_URL}/jobs/infinite`,
            crawlConfig: {
                maxPages: 10, 
                followJobLinks: true,
                paginationLimit: 2 
            },
            seedData: {
                url: `${BASE_URL}/jobs/infinite`,
                category: 'general'
            }
        });

        // 1 NAV + 2 PAG = 3 pages visited. The fourth page is hit but truncated.
        expect(result.crawlMeta.pagesVisited).toBe(3);
        expect(result.crawlMeta.pagination_pattern).toBe('query:p');
    }, 60000); // 60s for pagination test

    it('Scenario 2: filters out non-tech roles for general seeds', async () => {
        const service = new CrawlerService('long-dummy-key-to-avoid-validation-errors');
        const result = await service.crawl({
            companyId: 'role-mixer',
            url: `${BASE_URL}/jobs/role-mix`,
            seedData: {
                url: `${BASE_URL}/jobs/role-mix`,
                category: 'general'
            }
        });

        const titles = result.jobs.map(j => j.title);
        expect(titles).toContain('Senior Software Engineer');
        expect(titles).toContain('Data Scientist');
        expect(titles).not.toContain('Customer Success Manager');
        expect(titles).not.toContain('Receptionist');
    }, 30000);

    it('Scenario 3: detects CONTRACTION signals when job counts decrease', async () => {
        const service = new CrawlerService('long-dummy-key-to-avoid-validation-errors');
        const result = await service.crawl({
            companyId: 'contraction-test',
            url: `${BASE_URL}/jobs/role-mix?test=contraction`,
            seedData: {
                url: `${BASE_URL}/jobs/role-mix?test=contraction`,
                category: 'general',
                lastKnownJobCount: 50 
            }
        });

        expect(result.crawlMeta.jobYieldSignal?.type).toBe('CONTRACTION');
        expect(result.crawlMeta.jobYieldSignal?.previousJobs).toBe(50);
        expect(result.crawlMeta.jobYieldSignal?.newJobs).toBe(2);
    }, 30000);

    it('Scenario 4: tech-filtered seeds still apply negative filtering as insurance', async () => {
        const service = new CrawlerService('long-dummy-key-to-avoid-validation-errors');
        const result = await service.crawl({
            companyId: 'tech-insurance',
            url: `${BASE_URL}/jobs/role-mix?test=tech-filtered`,
            seedData: {
                url: `${BASE_URL}/jobs/role-mix?test=tech-filtered`,
                category: 'tech-filtered'
            }
        });

        const titles = result.jobs.map(j => j.title);
        // Should keep tech roles
        expect(titles).toContain('Senior Software Engineer');
        expect(titles).toContain('Data Scientist');
        // Should STILL drop non-tech roles even if it's a tech board (insurance)
        expect(titles).not.toContain('Customer Success Manager');
        // Receptionist has no tech keyword, so it should be dropped by the tech-only insurance filter
        expect(titles).not.toContain('Receptionist');
    }, 30000);

    it('Scenario 5: errorMessage surfaces correctly on crawl failure', async () => {
        const service = new CrawlerService('long-dummy-key-to-avoid-validation-errors');
        const result = await service.crawl({
            companyId: 'fail-test',
            url: 'http://localhost:1', 
        });

        expect(result).toBeDefined();
        expect(result.crawlMeta).toBeDefined();
        expect((result.crawlMeta as any).status).toBe('FAILED');
        expect(result.crawlMeta.errorMessage).toBeDefined();
    }, 30000);
});
