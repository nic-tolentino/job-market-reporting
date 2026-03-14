import { NormalizedJob, ValidationResult } from '../api/types';

/** Minimum confidence score a job must achieve to be considered valid. */
export const MIN_CONFIDENCE_THRESHOLD = 0.5;

/**
 * Validation rules for job fields
 */
const VALIDATION_RULES = {
  title: {
    required: true,
    minLength: 3,
    maxLength: 200
  },
  location: {
    required: false,
    maxLength: 200
  },
  employmentType: {
    allowedValues: ['Full-time', 'Part-time', 'Contract', 'Internship', 'Temporary', 'Permanent']
  },
  workModel: {
    allowedValues: ['Remote', 'Hybrid', 'On-site']
  },
  seniorityLevel: {
    allowedValues: ['Junior', 'Mid', 'Senior', 'Lead', 'Principal', 'Director', 'VP', 'Lead/Principal']
  },
  applyUrl: {
    required: false,
    isUrl: true
  },
  postedAt: {
    required: false,
    isDate: true
  }
};

/**
 * Validates a job object against quality rules
 * Returns validation result with confidence score
 */
export function validateJob(job: Partial<NormalizedJob>): ValidationResult {
  const errors: string[] = [];
  let confidence = 1.0;
  
  // Validate title (required)
  if (!job.title || job.title.trim().length === 0) {
    errors.push('Missing required field: title');
    confidence -= 0.5;
  } else if (job.title.length < VALIDATION_RULES.title.minLength) {
    errors.push(`Title too short (min ${VALIDATION_RULES.title.minLength} chars)`);
    confidence -= 0.2;
  } else if (job.title.length > VALIDATION_RULES.title.maxLength) {
    errors.push(`Title too long (max ${VALIDATION_RULES.title.maxLength} chars)`);
    confidence -= 0.2;
  }
  
  // Validate location (optional but boosts confidence)
  if (job.location) {
    if (job.location.length > VALIDATION_RULES.location.maxLength) {
      errors.push(`Location too long (max ${VALIDATION_RULES.location.maxLength} chars)`);
      confidence -= 0.1;
    }
    // Check for reasonable location format
    if (!isValidLocation(job.location)) {
      errors.push('Location format appears invalid');
      confidence -= 0.1;
    }
  }
  
  // Validate employment type
  if (job.employmentType && !VALIDATION_RULES.employmentType.allowedValues.includes(job.employmentType)) {
    errors.push(`Invalid employment type: ${job.employmentType}`);
    confidence -= 0.1;
  }
  
  // Validate work model
  if (job.workModel && !VALIDATION_RULES.workModel.allowedValues.includes(job.workModel)) {
    errors.push(`Invalid work model: ${job.workModel}`);
    confidence -= 0.1;
  }
  
  // Validate seniority level
  if (job.seniorityLevel && !VALIDATION_RULES.seniorityLevel.allowedValues.includes(job.seniorityLevel)) {
    errors.push(`Invalid seniority level: ${job.seniorityLevel}`);
    confidence -= 0.1;
  }
  
  // Validate apply URL
  if (job.applyUrl && !isValidUrl(job.applyUrl)) {
    errors.push('Invalid apply URL format');
    confidence -= 0.1;
  }
  
  // Validate posted date
  if (job.postedAt && !isValidDate(job.postedAt)) {
    errors.push('Invalid posted date format');
    confidence -= 0.1;
  }
  
  // Validate salary range
  if (job.salaryMin != null && job.salaryMax != null && job.salaryMin > job.salaryMax) {
    errors.push('Salary min is greater than max');
    confidence -= 0.1;
  }

  // Penalize missing optional fields (for confidence scoring)
  if (!job.location) confidence -= 0.15;
  if (!job.employmentType) confidence -= 0.1;
  if (!job.workModel) confidence -= 0.1;
  if (!job.postedAt) confidence -= 0.1;
  if (!job.applyUrl) confidence -= 0.15;

  // Ensure confidence doesn't go below 0
  confidence = Math.max(0, confidence);

  // Jobs are invalid if:
  // 1. Confidence < 0.5, OR
  // 2. Missing required field (title), OR  
  // 3. Title validation failed (too short/long), OR
  // 4. Data integrity errors (invalid enum values, malformed URLs/dates, salary range error)
  const hasRequiredError = errors.some(e => e.includes('Missing required'));
  const hasTitleError = errors.some(e => e.includes('Title too'));
  const hasDataIntegrityError = errors.some(e => 
    e.includes('Invalid employment') || 
    e.includes('Invalid work') ||
    e.includes('Invalid apply URL') ||
    e.includes('Invalid posted date') ||
    e.includes('Salary min is greater')
  );
  const valid = confidence >= MIN_CONFIDENCE_THRESHOLD && !hasRequiredError && !hasTitleError && !hasDataIntegrityError;

  return {
    valid,
    confidence,
    errors
  };
}

/**
 * Validates multiple jobs and filters out invalid ones
 */
export function validateJobs(jobs: NormalizedJob[], minConfidence: number = MIN_CONFIDENCE_THRESHOLD): {
  validJobs: NormalizedJob[];
  rejectedJobs: Array<{ job: NormalizedJob; reason: string }>;
  averageConfidence: number;
} {
  const validJobs: NormalizedJob[] = [];
  const rejectedJobs: Array<{ job: NormalizedJob; reason: string }> = [];
  
  for (const job of jobs) {
    const result = validateJob(job);
    if (result.valid && result.confidence >= minConfidence) {
      validJobs.push(job);
    } else {
      rejectedJobs.push({
        job,
        reason: result.errors.join('; ') || `Confidence ${result.confidence} below threshold ${minConfidence}`
      });
    }
  }
  
  const avgConfidence = validJobs.length > 0
    ? validJobs.reduce((sum, job) => sum + validateJob(job).confidence, 0) / validJobs.length
    : 0;
  
  return { validJobs, rejectedJobs, averageConfidence: avgConfidence };
}

/**
 * Checks if a string is a valid URL
 */
function isValidUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Checks if a string is a valid ISO date
 */
function isValidDate(dateStr: string): boolean {
  // Check ISO 8601 format (with optional time and timezone)
  const isoRegex = /^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2}(\.\d{3})?(Z|[+-]\d{2}:?\d{2})?)?$/;
  if (!isoRegex.test(dateStr)) {
    return false;
  }
  
  const date = new Date(dateStr);
  return !isNaN(date.getTime());
}

/**
 * Checks if a location string appears valid
 */
function isValidLocation(location: string): boolean {
  // Basic checks for reasonable location format
  const trimmed = location.trim();
  
  // Too short (but allow 2-letter country codes)
  if (trimmed.length < 2) return false;
  
  // Allow 2-letter ISO country codes
  if (/^[A-Z]{2}$/.test(trimmed)) return true;
  
  // All caps (likely invalid unless it's a country code)
  if (/^[A-Z]{3,}$/.test(trimmed)) return false;
  
  // All numbers (likely invalid)
  if (/^\d+$/.test(trimmed)) return false;
  
  // Common remote indicators are valid
  if (trimmed.toLowerCase().includes('remote')) return true;
  if (trimmed.toLowerCase().includes('hybrid')) return true;
  
  // Should contain at least one letter
  if (!/[a-zA-Z]/.test(trimmed)) return false;
  
  return true;
}
