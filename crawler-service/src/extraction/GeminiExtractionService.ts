import { GoogleGenerativeAI, GenerativeModel } from '@google/generative-ai';
import { NormalizedJob } from '../api/types';

/**
 * Default extraction prompt for job listings
 */
const DEFAULT_EXTRACTION_PROMPT = `You are a job listing extraction assistant. Extract all job postings from the following career page content.

Company: {{companyName}}
Industry: {{industries}}
Known locations: {{officeLocations}}

For each job, return a JSON object with these fields:
- title (string, required)
- location (string — city, state/region, country)
- department (string or null)
- employmentType ("Full-time" | "Part-time" | "Contract" | "Internship" | null)
- seniorityLevel ("Junior" | "Mid" | "Senior" | "Lead" | "Principal" | "Director" | "VP" | null)
- workModel ("Remote" | "Hybrid" | "On-site" | null)
- salaryMin (integer, annual, or null)
- salaryMax (integer, annual, or null)
- salaryCurrency (ISO 4217 code, or null)
- description (first 500 characters of the job description)
- postedAt (ISO date string, or null)
- applyUrl (direct application URL, or null)

Rules:
- Only include CURRENT, OPEN job postings. Ignore expired or example listings.
- If salary is given as hourly/daily, convert to annual estimate.
- If location is ambiguous, use the company's known locations as context.
- Return a JSON array. If no jobs found, return [].

Career page content:
{{pageContent}}`;

export interface ExtractionConfig {
  prompt?: string;
  companyName?: string;
  industries?: string[];
  officeLocations?: string[];
  extractionHints?: Record<string, string>;
}

export interface ExtractionResult {
  jobs: NormalizedJob[];
  model: string;
  tokenUsage: {
    input: number;
    output: number;
  };
}

/**
 * Service for extracting job data using Gemini Flash
 * 
 * Creates Gemini client once in constructor for efficiency.
 */
export class GeminiExtractionService {
  private apiKey: string;
  private model: string;
  private geminiModel: GenerativeModel | null = null;

  constructor(apiKey?: string, model: string = 'gemini-2.0-flash') {
    this.apiKey = apiKey || process.env.GEMINI_API_KEY || '';
    this.model = model;
    
    if (!this.apiKey) {
      console.warn('GEMINI_API_KEY not set. Extraction will fail.');
    } else {
      // Initialize Gemini client once
      const genAI = new GoogleGenerativeAI(this.apiKey);
      this.geminiModel = genAI.getGenerativeModel({ model: this.model });
    }
  }

  /**
   * Extracts jobs from page content using Gemini Flash
   */
  async extractJobs(content: string, config: ExtractionConfig = {}): Promise<ExtractionResult> {
    if (!this.geminiModel) {
      throw new Error('Gemini client not initialized. Check GEMINI_API_KEY.');
    }

    const prompt = this.buildPrompt(content, config);

    try {
      const result = await this.geminiModel.generateContent(prompt);
      const response = await result.response;

      // Check for API errors (quota, billing, invalid key)
      if (response.promptFeedback?.blockReason) {
        throw new Error(
          `Gemini API blocked request: ${response.promptFeedback.blockReason}. ` +
          `This usually means the API key is invalid, billing is not enabled, or quota is exceeded. ` +
          `Check: https://console.cloud.google.com/billing`
        );
      }

      const text = response.text();
      
      // Check if response is empty (possible API error)
      if (!text || text.trim().length === 0) {
        throw new Error(
          'Gemini API returned empty response. ' +
          'This may indicate API key issues, quota exceeded, or billing not enabled. ' +
          'Check: https://console.cloud.google.com/billing'
        );
      }

      const llmJobs = this.parseJobsFromResponse(text);

      // Convert LLM jobs to NormalizedJob with service-set fields
      const jobs = llmJobs.map(job => this.toNormalizedJob(job, config.companyName));

      return {
        jobs,
        model: this.model,
        tokenUsage: {
          input: response.usageMetadata?.promptTokenCount || 0,
          output: response.usageMetadata?.candidatesTokenCount || 0
        }
      };
    } catch (error) {
      // Enhance Gemini API error messages
      if (error.message?.includes('API key not valid')) {
        throw new Error(
          `Gemini API Error: Invalid or expired API key. ` +
          `Details: ${error.message}. ` +
          `Solutions: 1) Check API key is correct, 2) Ensure billing is enabled at https://console.cloud.google.com/billing, ` +
          `3) Verify API key has Gemini API permissions`
        );
      }
      if (error.message?.includes('quota') || error.message?.includes('Quota exceeded')) {
        throw new Error(
          `Gemini API Error: Quota exceeded. ` +
          `Details: ${error.message}. ` +
          `Solutions: 1) Wait for quota to reset (usually 1 minute), 2) Enable billing at https://console.cloud.google.com/billing, ` +
          `3) Request quota increase at https://cloud.google.com/vertex-ai/docs/quotas`
        );
      }
      if (error.message?.includes('billing')) {
        throw new Error(
          `Gemini API Error: Billing not enabled. ` +
          `Details: ${error.message}. ` +
          `Solution: Enable billing at https://console.cloud.google.com/billing to continue using Gemini API`
        );
      }
      // Re-throw other errors
      throw error;
    }
  }

  /**
   * Builds the extraction prompt with company-specific context
   */
  private buildPrompt(content: string, config: ExtractionConfig): string {
    let prompt = config.prompt || DEFAULT_EXTRACTION_PROMPT;
    
    // Replace template variables
    prompt = prompt
      .replace('{{companyName}}', config.companyName || 'Unknown')
      .replace('{{industries}}', config.industries?.join(', ') || 'Unknown')
      .replace('{{officeLocations}}', config.officeLocations?.join(', ') || 'Unknown')
      .replace('{{pageContent}}', content);
    
    // Append extraction hints if provided
    if (config.extractionHints && Object.keys(config.extractionHints).length > 0) {
      const hintsText = Object.entries(config.extractionHints)
        .map(([key, value]) => `- ${key}: ${value}`)
        .join('\n');
      
      prompt += `\n\nAdditional context for this company:\n${hintsText}`;
    }
    
    return prompt;
  }

  /**
   * Parses job array from LLM response
   * Handles various JSON formatting issues
   */
  private parseJobsFromResponse(text: string): LlmJobData[] {
    try {
      // Try to parse as-is first
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed;
      }
      // Check if jobs are nested
      if (parsed.jobs && Array.isArray(parsed.jobs)) {
        return parsed.jobs;
      }
      return [];
    } catch {
      // Try to extract JSON from markdown code blocks
      const jsonMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/);
      if (jsonMatch) {
        try {
          const parsed = JSON.parse(jsonMatch[1]);
          if (Array.isArray(parsed)) {
            return parsed;
          }
          if (parsed.jobs && Array.isArray(parsed.jobs)) {
            return parsed.jobs;
          }
        } catch {
          // Fall through to empty
        }
      }
      
      // Try to find array in text
      const arrayMatch = text.match(/\[[\s\S]*\]/);
      if (arrayMatch) {
        try {
          const parsed = JSON.parse(arrayMatch[0]);
          if (Array.isArray(parsed)) {
            return parsed;
          }
          if (parsed.jobs && Array.isArray(parsed.jobs)) {
            return parsed.jobs;
          }
        } catch {
          // Ignore parse errors
        }
      }
      
      console.warn('Failed to parse LLM response as JSON');
      return [];
    }
  }

  /**
   * Converts LLM-extracted job data to NormalizedJob
   * Service sets fields that LLM cannot infer (platformId, source, companyName)
   */
  private toNormalizedJob(llmJob: LlmJobData, companyName?: string): NormalizedJob {
    // Generate platformId from job title and date
    const dateStr = llmJob.postedAt 
      ? llmJob.postedAt.replace(/-/g, '').slice(0, 8)  // "2024-03-10" → "20240310"
      : new Date().toISOString().split('T')[0].replace(/-/g, '');
    
    const slug = llmJob.title.toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
      .slice(0, 50);
    
    return {
      platformId: llmJob.platformId || `crawl-${slug}-${dateStr}`,
      source: 'Crawler',
      title: llmJob.title || '',
      companyName: llmJob.companyName || companyName || '',
      location: llmJob.location || null,
      descriptionHtml: null,
      descriptionText: llmJob.description || null,
      salaryMin: llmJob.salaryMin || null,
      salaryMax: llmJob.salaryMax || null,
      salaryCurrency: llmJob.salaryCurrency || null,
      employmentType: llmJob.employmentType || null,
      seniorityLevel: llmJob.seniorityLevel || null,
      workModel: llmJob.workModel || null,
      department: llmJob.department || null,
      postedAt: llmJob.postedAt || null,
      applyUrl: llmJob.applyUrl || null,
      platformUrl: llmJob.platformUrl || null
    };
  }
}

/**
 * Intermediate type for LLM-extracted job data
 * More flexible than NormalizedJob to accommodate LLM output variations
 */
export interface LlmJobData {
  platformId?: string;
  title: string;
  location?: string | null;
  department?: string | null;
  employmentType?: string | null;
  seniorityLevel?: string | null;
  workModel?: string | null;
  salaryMin?: number | null;
  salaryMax?: number | null;
  salaryCurrency?: string | null;
  description?: string | null;
  postedAt?: string | null;
  applyUrl?: string | null;
  platformUrl?: string | null;
  companyName?: string;  // Optional - service will fill if not provided
}
