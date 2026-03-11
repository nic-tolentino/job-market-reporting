import { validateJob } from '../src/validator/JobValidator';

describe('JobValidator - Edge Cases', () => {
  describe('salary validation', () => {
    const baseJob = {
      platformId: 'test',
      source: 'Crawler',
      title: 'Engineer',
      companyName: 'Test',
      location: 'Sydney',
      descriptionHtml: null,
      descriptionText: 'Description',
      employmentType: 'Full-time',
      seniorityLevel: 'Mid',
      workModel: 'Hybrid',
      department: 'Engineering',
      postedAt: '2024-03-10',
      applyUrl: 'https://apply.url',
      platformUrl: null
    };

    it('accepts job with salaryMin = 0', () => {
      const job = { ...baseJob, salaryMin: 0, salaryMax: 100000 };
      const result = validateJob(job);
      
      expect(result.valid).toBe(true);
    });

    it('rejects job where salaryMin > salaryMax', () => {
      const job = { ...baseJob, salaryMin: 150000, salaryMax: 100000 };
      const result = validateJob(job);
      
      expect(result.valid).toBe(false);
      expect(result.errors).toContainEqual(expect.stringContaining('min is greater than max'));
    });

    it('accepts job with null salaries', () => {
      const job = { ...baseJob, salaryMin: null, salaryMax: null };
      const result = validateJob(job);
      
      expect(result.valid).toBe(true);
    });

    it('accepts job with only salaryMin', () => {
      const job = { ...baseJob, salaryMin: 100000, salaryMax: null };
      const result = validateJob(job);
      
      expect(result.valid).toBe(true);
    });
  });

  describe('location validation', () => {
    it('accepts 2-letter ISO country codes', () => {
      const job = {
        platformId: 'test',
        source: 'Crawler',
        title: 'Engineer',
        companyName: 'Test',
        location: 'NZ',
        descriptionHtml: null,
        descriptionText: 'Description',
        employmentType: 'Full-time',
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: null,
        applyUrl: null,
        platformUrl: null
      };
      
      const result = validateJob(job);
      expect(result.valid).toBe(true);
    });

    it('accepts Remote locations', () => {
      const job = {
        ...{
          platformId: 'test',
          source: 'Crawler',
          title: 'Engineer',
          companyName: 'Test',
          descriptionHtml: null,
          descriptionText: 'Description',
          employmentType: 'Full-time',
          seniorityLevel: null,
          workModel: null,
          department: null,
          postedAt: null,
          applyUrl: null,
          platformUrl: null
        },
        location: 'Remote'
      };
      
      const result = validateJob(job);
      expect(result.valid).toBe(true);
    });
  });

  describe('date validation', () => {
    it('accepts ISO date with Z timezone', () => {
      const job = {
        platformId: 'test',
        source: 'Crawler',
        title: 'Engineer',
        companyName: 'Test',
        location: 'Sydney',
        descriptionHtml: null,
        descriptionText: 'Description',
        employmentType: 'Full-time',
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: '2024-03-10T02:00:00Z',
        applyUrl: null,
        platformUrl: null
      };
      
      const result = validateJob(job);
      expect(result.valid).toBe(true);
    });

    it('accepts ISO date with +/- offset', () => {
      const job = {
        platformId: 'test',
        source: 'Crawler',
        title: 'Engineer',
        companyName: 'Test',
        location: 'Sydney',
        descriptionHtml: null,
        descriptionText: 'Description',
        employmentType: 'Full-time',
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: '2024-03-10T13:00:00+13:00',
        applyUrl: null,
        platformUrl: null
      };
      
      const result = validateJob(job);
      expect(result.valid).toBe(true);
    });
  });
});
