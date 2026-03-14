import { CrawlerService } from '../src/api/CrawlerService';
import { NormalizedJob as Job } from '../src/api/types';

describe('Job Deduplication', () => {
    it('deduplicates identical jobs (same title and location)', () => {
        const jobs: Partial<Job>[] = [
            { title: 'Engineer', location: 'London' },
            { title: 'Engineer', location: 'London' },
            { title: 'Manager', location: 'Remote' }
        ];

        const result = CrawlerService.deduplicateJobs(jobs as Job[]);
        expect(result).toHaveLength(2);
        expect(result[0].title).toBe('Engineer');
        expect(result[1].title).toBe('Manager');
    });

    it('keeps jobs with same title but different location', () => {
        const jobs: Partial<Job>[] = [
            { title: 'Engineer', location: 'London' },
            { title: 'Engineer', location: 'New York' }
        ];

        const result = CrawlerService.deduplicateJobs(jobs as Job[]);
        expect(result).toHaveLength(2);
    });

    it('handles null/undefined title or location as distinct if they differ', () => {
        const jobs: Partial<Job>[] = [
            { title: 'Engineer', location: undefined },
            { title: 'Engineer', location: 'London' }
        ];

        const result = CrawlerService.deduplicateJobs(jobs as Job[]);
        expect(result).toHaveLength(2);
    });

    it('deduplicates when both title and location are missing (if identical)', () => {
        const jobs: Partial<Job>[] = [
            { title: undefined, location: undefined },
            { title: undefined, location: undefined }
        ];

        const result = CrawlerService.deduplicateJobs(jobs as Job[]);
        expect(result).toHaveLength(1);
    });
});
