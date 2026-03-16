import { CrawlerService } from '../src/api/CrawlerService';

describe('CrawlerService.normalizeListingUrl', () => {
    // -------------------------------------------------------------------------
    // Core pagination param stripping
    // -------------------------------------------------------------------------

    it('strips "page" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?page=3'))
            .toBe('https://example.com/jobs');
    });

    it('strips "p" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?p=2'))
            .toBe('https://example.com/jobs');
    });

    it('strips "offset" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?offset=50'))
            .toBe('https://example.com/jobs');
    });

    it('strips "start" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?start=10'))
            .toBe('https://example.com/jobs');
    });

    it('strips "cursor" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?cursor=abc123'))
            .toBe('https://example.com/jobs');
    });

    it('strips "skip" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?skip=20'))
            .toBe('https://example.com/jobs');
    });

    it('strips "index" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?index=1'))
            .toBe('https://example.com/jobs');
    });

    it('strips "batch" query param', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?batch=2'))
            .toBe('https://example.com/jobs');
    });

    // -------------------------------------------------------------------------
    // Preserves non-pagination params
    // -------------------------------------------------------------------------

    it('preserves non-pagination query params', () => {
        const result = CrawlerService.normalizeListingUrl('https://example.com/jobs?category=engineering&page=2');
        expect(result).toBe('https://example.com/jobs?category=engineering');
    });

    it('strips only pagination params when multiple params present', () => {
        const result = CrawlerService.normalizeListingUrl(
            'https://example.com/jobs?q=developer&offset=50&location=auckland'
        );
        expect(result).toBe('https://example.com/jobs?q=developer&location=auckland');
    });

    // -------------------------------------------------------------------------
    // query: paginationPattern
    // -------------------------------------------------------------------------

    it('strips the named param from paginationPattern query:<param>', () => {
        const result = CrawlerService.normalizeListingUrl(
            'https://example.com/jobs?pg=3&category=tech',
            'query:pg'
        );
        expect(result).toBe('https://example.com/jobs?category=tech');
    });

    it('ignores paginationPattern when it does not start with query:', () => {
        // path-based pattern — no extra stripping beyond CORE_PAGINATION_PARAMS
        const result = CrawlerService.normalizeListingUrl(
            'https://example.com/jobs/page/3?q=dev',
            'path:/jobs/page/{n}'
        );
        expect(result).toBe('https://example.com/jobs/page/3?q=dev');
    });

    // -------------------------------------------------------------------------
    // URL with no query string
    // -------------------------------------------------------------------------

    it('returns URL unchanged when there are no query params', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/careers'))
            .toBe('https://example.com/careers');
    });

    // -------------------------------------------------------------------------
    // Malformed URL
    // -------------------------------------------------------------------------

    it('returns the input unchanged when URL is malformed', () => {
        const badUrl = 'not-a-valid-url';
        expect(CrawlerService.normalizeListingUrl(badUrl)).toBe(badUrl);
    });

    it('handles empty string gracefully', () => {
        expect(CrawlerService.normalizeListingUrl('')).toBe('');
    });

    // -------------------------------------------------------------------------
    // Trailing slash / path preservation
    // -------------------------------------------------------------------------

    it('preserves trailing slash', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs/?page=1'))
            .toBe('https://example.com/jobs/');
    });

    it('preserves URL path and hash', () => {
        expect(CrawlerService.normalizeListingUrl('https://example.com/jobs?page=2#top'))
            .toBe('https://example.com/jobs#top');
    });
});
