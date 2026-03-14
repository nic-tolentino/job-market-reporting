import { CrawlerService } from '../src/api/CrawlerService';

describe('Pagination Detection', () => {
    const startUrl = 'https://example.com/jobs/search';

    it('identifies relative page param as pagination', () => {
        expect(CrawlerService.isPaginationLink('https://example.com/jobs/search?page=2', startUrl)).toBe(true);
    });

    it('identifies "p" param as pagination', () => {
        expect(CrawlerService.isPaginationLink('https://example.com/jobs/search?p=3', startUrl)).toBe(true);
    });

    it('identifies offset/start as pagination', () => {
        expect(CrawlerService.isPaginationLink('https://example.com/jobs/search?offset=50', startUrl)).toBe(true);
        expect(CrawlerService.isPaginationLink('https://example.com/jobs/search?start=10', startUrl)).toBe(true);
    });

    it('rejects different path on same hostname', () => {
        expect(CrawlerService.isPaginationLink('https://example.com/about', startUrl)).toBe(false);
    });

    it('rejects different hostname', () => {
        expect(CrawlerService.isPaginationLink('https://another-site.com/jobs/search?page=2', startUrl)).toBe(false);
    });

    it('identifies pagination even with unrelated query params', () => {
        expect(CrawlerService.isPaginationLink('https://example.com/jobs/search?q=engineer&page=2', startUrl)).toBe(true);
    });

    it('handles malformed start URL gracefully', () => {
        expect(CrawlerService.isPaginationLink('https://example.com/jobs/search?page=2', 'not-a-url')).toBe(false);
    });

    it('handles malformed link URL gracefully', () => {
        expect(CrawlerService.isPaginationLink('not-a-url', startUrl)).toBe(false);
    });

    it('handles cursor/index/batch/skip tokens', () => {
        expect(CrawlerService.isPaginationLink(startUrl + '?cursor=xyz123', startUrl)).toBe(true);
        expect(CrawlerService.isPaginationLink(startUrl + '?index=1', startUrl)).toBe(true);
        expect(CrawlerService.isPaginationLink(startUrl + '?batch=2', startUrl)).toBe(true);
        expect(CrawlerService.isPaginationLink(startUrl + '?skip=20', startUrl)).toBe(true);
    });
});
