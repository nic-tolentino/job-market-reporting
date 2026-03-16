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

  describe('listing-page jobs (no applyUrl)', () => {
    // Listing pages don't have direct apply URLs — only platformUrl (job detail link).
    // Removing the applyUrl confidence penalty means these jobs should pass validation
    // even with sparse metadata, as long as they have a title and location.
    it('accepts a listing-page job with title + location but no applyUrl or optional fields', () => {
      const result = validateJob({
        platformId: 'test',
        source: 'Crawler',
        title: 'Mobile Software Engineer',
        companyName: 'Datacom',
        location: 'Auckland, New Zealand',
        descriptionHtml: null,
        descriptionText: null,
        employmentType: null,
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: null,
        applyUrl: null,
        platformUrl: 'https://datacom.com/nz/en/careers/search/job/12345',
      });
      expect(result.valid).toBe(true);
      expect(result.confidence).toBeGreaterThanOrEqual(0.5);
    });

    it('accepts a listing-page job with title only (no location, no applyUrl)', () => {
      // Worst-case listing page: only a title was extracted
      const result = validateJob({
        platformId: 'test',
        source: 'Crawler',
        title: 'Senior Software Engineer',
        companyName: 'Datacom',
        location: null,
        descriptionHtml: null,
        descriptionText: null,
        employmentType: null,
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: null,
        applyUrl: null,
        platformUrl: null,
      });
      // title only: 1.0 − 0.15 (no location) − 0.10 − 0.10 − 0.10 = 0.55
      expect(result.valid).toBe(true);
      expect(result.confidence).toBeCloseTo(0.55);
    });
  });

  describe('location validation', () => {
    it('accepts single all-caps city names (e.g. AUCKLAND)', () => {
      const result = validateJob({
        platformId: 'test',
        source: 'Crawler',
        title: 'Software Engineer',
        companyName: 'Datacom',
        location: 'AUCKLAND',
        descriptionHtml: null,
        descriptionText: null,
        employmentType: null,
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: null,
        applyUrl: null,
        platformUrl: null,
      });
      expect(result.valid).toBe(true);
    });

    it('rejects multi-word all-caps strings that look like instructions', () => {
      const result = validateJob({
        platformId: 'test',
        source: 'Crawler',
        title: 'Software Engineer',
        companyName: 'Test',
        location: 'APPLY NOW',
        descriptionHtml: null,
        descriptionText: null,
        employmentType: null,
        seniorityLevel: null,
        workModel: null,
        department: null,
        postedAt: null,
        applyUrl: null,
        platformUrl: null,
      });
      expect(result.errors).toContain('Location format appears invalid');
    });

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
