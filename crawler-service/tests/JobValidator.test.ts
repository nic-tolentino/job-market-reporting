import { validateJob, validateJobs } from '../src/validator/JobValidator';
import { NormalizedJob } from '../src/api/types';

describe('JobValidator', () => {
  const createValidJob = (overrides: Partial<NormalizedJob> = {}): NormalizedJob => ({
    platformId: 'test-123',
    source: 'Crawler',
    title: 'Senior Software Engineer',
    companyName: 'Test Corp',
    location: 'Sydney, AU',
    descriptionHtml: null,
    descriptionText: 'We are hiring...',
    salaryMin: 120000,
    salaryMax: 150000,
    salaryCurrency: 'AUD',
    employmentType: 'Full-time',
    seniorityLevel: 'Senior',
    workModel: 'Hybrid',
    department: 'Engineering',
    postedAt: '2024-03-10',
    applyUrl: 'https://testcorp.com/apply/123',
    platformUrl: 'https://testcorp.com/careers/123',
    ...overrides
  });
  
  describe('validateJob', () => {
    it('accepts valid job with all fields', () => {
      const job = createValidJob();
      const result = validateJob(job);
      
      expect(result.valid).toBe(true);
      expect(result.confidence).toBeGreaterThan(0.8);
      expect(result.errors).toHaveLength(0);
    });
    
    it('rejects job without title', () => {
      const job = createValidJob({ title: '' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('title'));
    });
    
    it('rejects job with title too short', () => {
      const job = createValidJob({ title: 'AB' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('short'));
    });
    
    it('rejects job with title too long', () => {
      const job = createValidJob({ title: 'A'.repeat(201) });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('long'));
    });
    
    it('accepts job without location (optional field)', () => {
      const job = createValidJob({ location: null });
      const result = validateJob(job);
      
      expect(result.valid).toBe(true);
    });
    
    it('rejects invalid employment type', () => {
      const job = createValidJob({ employmentType: 'Invalid' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('employment type'));
    });
    
    it('accepts valid employment types', () => {
      const validTypes = ['Full-time', 'Part-time', 'Contract', 'Internship', 'Temporary', 'Permanent'];
      
      for (const type of validTypes) {
        const job = createValidJob({ employmentType: type });
        const result = validateJob(job);
        
        expect(result.valid).toBe(true);
      }
    });
    
    it('rejects invalid work model', () => {
      const job = createValidJob({ workModel: 'Invalid' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('work model'));
    });
    
    it('accepts valid work models', () => {
      const validModels = ['Remote', 'Hybrid', 'On-site'];
      
      for (const model of validModels) {
        const job = createValidJob({ workModel: model });
        const result = validateJob(job);
        
        expect(result.valid).toBe(true);
      }
    });
    
    it('rejects invalid apply URL', () => {
      const job = createValidJob({ applyUrl: 'not-a-url' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('URL'));
    });
    
    it('rejects invalid posted date', () => {
      const job = createValidJob({ postedAt: 'not-a-date' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('date'));
    });
    
    it('accepts valid ISO date', () => {
      const job = createValidJob({ postedAt: '2024-03-10' });
      const result = validateJob(job);
      
      expect(result.valid).toBe(true);
    });
    
    it('rejects salary min greater than max', () => {
      const job = createValidJob({ salaryMin: 150000, salaryMax: 120000 });
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('min is greater than max'));
    });
  });
  
  describe('validateJobs', () => {
    it('filters out invalid jobs', () => {
      const jobs = [
        createValidJob(),
        createValidJob({ title: '' }),
        createValidJob()
      ];
      
      const { validJobs, rejectedJobs } = validateJobs(jobs);
      
      expect(validJobs).toHaveLength(2);
      expect(rejectedJobs).toHaveLength(1);
      expect(rejectedJobs[0].reason).toContain('title');
    });
    
    it('returns empty arrays for empty input', () => {
      const { validJobs, rejectedJobs } = validateJobs([]);
      
      expect(validJobs).toHaveLength(0);
      expect(rejectedJobs).toHaveLength(0);
    });
    
    it('respects minConfidence threshold', () => {
      const jobs = [
        createValidJob({ location: null, employmentType: null, workModel: null })
      ];
      
      // With default threshold (0.5), should pass
      let result = validateJobs(jobs);
      expect(result.validJobs).toHaveLength(1);
      
      // With higher threshold (0.9), should fail
      result = validateJobs(jobs, 0.9);
      expect(result.validJobs).toHaveLength(0);
      expect(result.rejectedJobs).toHaveLength(1);
    });
  });
});
