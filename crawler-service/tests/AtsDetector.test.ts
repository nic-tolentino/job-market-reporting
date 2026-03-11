import { detectAts } from '../src/detector/AtsDetector';

describe('AtsDetector', () => {
  describe('Greenhouse detection', () => {
    it('detects Greenhouse from iframe src', () => {
      const html = '<iframe src="https://boards.greenhouse.io/airwallex">';
      const result = detectAts(html);
      
      expect(result).toEqual({
        provider: 'GREENHOUSE',
        identifier: 'airwallex',
        confidence: 0.95,
        evidence: expect.stringContaining('iframe')
      });
    });
    
    it('detects Greenhouse from URL pattern', () => {
      const html = '<a href="https://boards.greenhouse.io/acme-corp">';
      const result = detectAts(html);
      
      expect(result?.provider).toBe('GREENHOUSE');
      expect(result?.confidence).toBeGreaterThanOrEqual(0.85);
    });
  });
  
  describe('Lever detection', () => {
    it('detects Lever from script tag', () => {
      const html = '<script src="https://assets.lever.co/widget.js">';
      const result = detectAts(html);
      
      expect(result).toEqual({
        provider: 'LEVER',
        identifier: null,
        confidence: 0.90,
        evidence: expect.stringContaining('script')
      });
    });
    
    it('detects Lever from URL pattern', () => {
      const html = '<a href="https://web.lever.co/envato-2/apply">';
      const result = detectAts(html);
      
      expect(result?.provider).toBe('LEVER');
    });
  });
  
  describe('Ashby detection', () => {
    it('detects Ashby from script tag', () => {
      const html = '<script src="https://jobs.ashbyhq.com/widget.js">';
      const result = detectAts(html);
      
      expect(result?.provider).toBe('ASHBY');
      expect(result?.confidence).toBeGreaterThanOrEqual(0.90);
    });
    
    it('detects Ashby from URL pattern', () => {
      const html = '<a href="https://jobs.ashbyhq.com/airwallex">';
      const result = detectAts(html);
      
      expect(result?.provider).toBe('ASHBY');
    });
  });
  
  describe('Workday detection', () => {
    it('detects Workday from URL pattern', () => {
      const html = '<a href="https://wd3.myworkdayjobs.com/Company/job/123">';
      const result = detectAts(html);
      
      expect(result?.provider).toBe('WORKDAY');
      expect(result?.confidence).toBeGreaterThanOrEqual(0.85);
    });
    
    it('extracts Workday identifier from subdomain', () => {
      const html = '<a href="https://acme.wd3.myworkdayjobs.com/jobs">';
      const result = detectAts(html);
      
      expect(result?.identifier).toBe('acme');
    });
  });
  
  describe('Workable detection', () => {
    it('detects Workable from meta tag', () => {
      const html = '<meta name="generator" content="Workable">';
      const result = detectAts(html);
      
      expect(result?.provider).toBe('WORKABLE');
      expect(result?.confidence).toBeGreaterThanOrEqual(0.70);
    });
  });
  
  describe('Unknown ATS', () => {
    it('returns null for unknown ATS', () => {
      const html = '<div class="custom-jobs"><h1>Careers</h1></div>';
      const result = detectAts(html);
      
      expect(result).toBeNull();
    });
    
    it('returns null for empty HTML', () => {
      const result = detectAts('');
      expect(result).toBeNull();
    });
  });
});
