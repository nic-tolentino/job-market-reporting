import { VertexAIClient } from './VertexAIClient';
import { NormalizedJob } from '../api/types';
import { DEFAULT_MODEL } from '../config/model-config';

export interface LlmJobData {
  title: string;
  location?: string;
  description?: string;
  salaryMin?: number;
  salaryMax?: number;
  salaryCurrency?: string;
  employmentType?: string;
  seniorityLevel?: string;
  workModel?: string;
  department?: string;
  postedAt?: string;
  applyUrl?: string;
  [key: string]: unknown;
}

export interface ExtractionResult {
  jobs: NormalizedJob[];
  model: string;
  tokenUsage: {
    input: number;
    output: number;
  };
}

export interface ExtractionConfig {
  companyName?: string;
  industries?: string[];
  officeLocations?: string[];
  prompt?: string;
  extractionHints?: Record<string, string>;
  /** The URL of the page being extracted, used to resolve relative job links. */
  listingUrl?: string;
}

/**
 * Service for extracting job data using Gemini API
 */
export class GeminiExtractionService {
  private vertexClient: VertexAIClient;
  private model: string;

  constructor(apiKey: string, model: string = DEFAULT_MODEL) {
    this.model = model;
    this.vertexClient = new VertexAIClient(apiKey, model);
  }

  /**
   * Extracts jobs from page content using Gemini
   */
  async extractJobs(content: string, config: ExtractionConfig = {}): Promise<ExtractionResult> {
    const prompt = this.buildPrompt(content, config);

    try {
      const response = await this.vertexClient.generateContent(prompt);

      if (!response.text || response.text.trim().length === 0) {
        throw new Error('Gemini API returned empty response.');
      }

      // Clean the text from markdown code blocks before validation/parsing
      const cleanText = response.text.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();

      // Log if response doesn't start with JSON after cleaning
      if (!cleanText.startsWith('[') && !cleanText.startsWith('{')) {
        console.warn('Gemini returned non-JSON response:', cleanText.substring(0, 200));
      }

      const llmJobs = this.parseJobsFromResponse(cleanText);
      const jobs = llmJobs.map(job => this.toNormalizedJob(job, config.companyName, config.listingUrl));

      return {
        jobs,
        model: this.model,
        tokenUsage: {
          input: response.usageMetadata.promptTokenCount || 0,
          output: response.usageMetadata.candidatesTokenCount || 0
        }
      };
    } catch (error) {
      console.error('Extraction error:', (error as Error).message);
      throw error;
    }
  }

  private buildPrompt(content: string, config: ExtractionConfig): string {
    let prompt = config.prompt || DEFAULT_EXTRACTION_PROMPT;

    prompt = prompt
      .replace('{{companyName}}', config.companyName || 'Unknown')
      .replace('{{industries}}', config.industries?.join(', ') || 'Unknown')
      .replace('{{officeLocations}}', config.officeLocations?.join(', ') || 'Unknown')
      .replace('{{pageContent}}', content);

    if (config.extractionHints && Object.keys(config.extractionHints).length > 0) {
      const hintsText = Object.entries(config.extractionHints)
        .map(([key, value]) => `- ${key}: ${value}`)
        .join('\n');
      prompt += `\n\nAdditional context for this company:\n${hintsText}`;
    }

    return prompt;
  }

  private parseJobsFromResponse(text: string): LlmJobData[] {
    try {
      const cleanText = text.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
      let jobs: LlmJobData[];
      try {
        jobs = JSON.parse(cleanText) as LlmJobData[];
      } catch {
        const obj = JSON.parse(cleanText) as { jobs?: LlmJobData[] };
        jobs = obj.jobs || [];
      }
      if (!Array.isArray(jobs)) {
        jobs = [jobs];
      }
      return jobs;
    } catch (error) {
      console.error('Failed to parse LLM response:', error);
      return [];
    }
  }

  /**
   * Normalizes a raw LLM enum value to the closest allowed value, or null.
   * Handles common variants: "Full Time" → "Full-time", "on_site" → "On-site", etc.
   */
  private normalizeEnum<T extends string>(value: unknown, allowed: readonly T[]): T | null {
    if (!value || typeof value !== 'string') return null;
    const raw = value.trim();
    // Exact match first
    if (allowed.includes(raw as T)) return raw as T;
    // Normalize separators then case-insensitive match
    const normalized = raw.toLowerCase().replace(/[-_\s]+/g, '-');
    return allowed.find(v => v.toLowerCase().replace(/[-_\s]+/g, '-') === normalized) ?? null;
  }

  private toNormalizedJob(llmJob: LlmJobData, companyName?: string, listingUrl?: string): NormalizedJob {
    const slug = llmJob.title.toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
      .slice(0, 50);

    const dateStr = llmJob.postedAt
      ? (llmJob.postedAt as string).slice(0, 10)
      : new Date().toISOString().split('T')[0];
    const platformId = llmJob.platformId as string || `${slug}-${dateStr.replace(/-/g, '')}`;

    const EMPLOYMENT_TYPES = ['Full-time', 'Part-time', 'Contract', 'Internship', 'Temporary', 'Permanent'] as const;
    const WORK_MODELS = ['Remote', 'Hybrid', 'On-site'] as const;
    const SENIORITY_LEVELS = ['Junior', 'Mid', 'Senior', 'Lead', 'Principal', 'Director', 'VP', 'Lead/Principal'] as const;

    // Resolve relative URLs against the listing page origin so detail-page
    // enrichment can follow them, and interpret relative date expressions
    // ("Today", "2 Days Ago") into ISO dates rather than discarding them.
    const rawApplyUrl = this.resolveUrl(llmJob.applyUrl, listingUrl) || null;
    const rawPostedAt = this.resolveDate(llmJob.postedAt as string | undefined) || null;

    return {
      platformId,
      source: 'Crawler',
      title: llmJob.title,
      companyName: (llmJob.companyName as string) || companyName || 'Unknown',
      location: llmJob.location || null,
      descriptionHtml: (llmJob.description as string) || null,
      descriptionText: (llmJob.description as string)?.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim() || null,
      salaryMin: llmJob.salaryMin || null,
      salaryMax: llmJob.salaryMax || null,
      salaryCurrency: llmJob.salaryCurrency || null,
      employmentType: this.normalizeEnum(llmJob.employmentType, EMPLOYMENT_TYPES),
      seniorityLevel: this.normalizeEnum(llmJob.seniorityLevel, SENIORITY_LEVELS),
      workModel: this.normalizeEnum(llmJob.workModel, WORK_MODELS),
      department: llmJob.department || null,
      postedAt: rawPostedAt,
      applyUrl: rawApplyUrl,
      platformUrl: rawApplyUrl,
    };
  }

  /**
   * Resolves a URL to an absolute https URL.
   * - Already-absolute URLs are returned as-is (after validation).
   * - Relative paths (e.g. "/careers/123") are resolved against the listing page
   *   origin so detail-page enrichment can follow them.
   * - Unparseable strings return null.
   */
  private resolveUrl(url: unknown, listingUrl?: string): string | null {
    if (!url || typeof url !== 'string') return null;
    const raw = url.trim();
    try {
      const u = new URL(raw);
      return u.protocol === 'http:' || u.protocol === 'https:' ? u.toString() : null;
    } catch {
      // Not an absolute URL — try resolving relative to the listing page
      if (listingUrl && raw.startsWith('/')) {
        try {
          return new URL(raw, listingUrl).toString();
        } catch {
          return null;
        }
      }
      return null;
    }
  }

  /**
   * Converts a date string to YYYY-MM-DD format.
   * - Already-ISO dates are returned as-is.
   * - Relative expressions ("Today", "Yesterday", "2 Days Ago", "3 Weeks Ago")
   *   are resolved against the current date so freshness data is preserved.
   * - Unrecognised strings return null.
   */
  private resolveDate(date: string | undefined): string | null {
    if (!date) return null;
    const raw = date.trim();

    // Already ISO YYYY-MM-DD
    if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) return raw;

    // ISO with time component — strip to date
    if (/^\d{4}-\d{2}-\d{2}T/.test(raw)) return raw.slice(0, 10);

    const lower = raw.toLowerCase();
    const today = new Date();
    today.setUTCHours(0, 0, 0, 0);

    if (lower === 'today' || lower === 'just posted' || lower === 'posted today') {
      return today.toISOString().slice(0, 10);
    }
    if (lower === 'yesterday') {
      today.setUTCDate(today.getUTCDate() - 1);
      return today.toISOString().slice(0, 10);
    }

    // "X days ago" / "posted X days ago"
    const daysMatch = lower.match(/(\d+)\s+days?\s+ago/);
    if (daysMatch) {
      today.setUTCDate(today.getUTCDate() - parseInt(daysMatch[1], 10));
      return today.toISOString().slice(0, 10);
    }

    // "X weeks ago"
    const weeksMatch = lower.match(/(\d+)\s+weeks?\s+ago/);
    if (weeksMatch) {
      today.setUTCDate(today.getUTCDate() - parseInt(weeksMatch[1], 10) * 7);
      return today.toISOString().slice(0, 10);
    }

    // "X months ago" — approximate
    const monthsMatch = lower.match(/(\d+)\s+months?\s+ago/);
    if (monthsMatch) {
      today.setUTCMonth(today.getUTCMonth() - parseInt(monthsMatch[1], 10));
      return today.toISOString().slice(0, 10);
    }

    return null;
  }
}

const DEFAULT_EXTRACTION_PROMPT = `You are a job data extraction expert. Extract all job listings from the following career page content.

Company: {{companyName}}
Industries: {{industries}}
Office Locations: {{officeLocations}}

Page Content:
{{pageContent}}

Extract these fields for each job:
- title (required)
- location (city, country)
- description (Provide foundational HTML formatting using tags like <p>, <ul>, <li>, <strong> to match the original structure)
- salaryMin, salaryMax, salaryCurrency (if mentioned)
- employmentType (Full-time, Part-time, Contract, Internship)
- seniorityLevel (Junior, Mid, Senior, Lead, Principal, Director, VP)
- workModel (Remote, Hybrid, On-site)
- department
- postedAt (ISO date if mentioned)
- applyUrl

IMPORTANT: Return ONLY a valid JSON array. Do not include any explanatory text.
If no jobs are found, return an empty array: []

Example format:
[{"title": "Software Engineer", "location": "Sydney, Australia", "employmentType": "Full-time", "description": "<p>We are looking for...</p>"}]`;
