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
      const jobs = llmJobs.map(job => this.toNormalizedJob(job, config.companyName));

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

  private toNormalizedJob(llmJob: LlmJobData, companyName?: string): NormalizedJob {
    const slug = llmJob.title.toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
      .slice(0, 50);
    
    const dateStr = llmJob.postedAt 
      ? (llmJob.postedAt as string).slice(0, 10) 
      : new Date().toISOString().split('T')[0];
    const platformId = llmJob.platformId as string || `${slug}-${dateStr.replace(/-/g, '')}`;

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
      employmentType: llmJob.employmentType || null,
      seniorityLevel: llmJob.seniorityLevel || null,
      workModel: llmJob.workModel || null,
      department: llmJob.department || null,
      postedAt: llmJob.postedAt ? (llmJob.postedAt as string).slice(0, 10) : null,
      applyUrl: llmJob.applyUrl || null,
      platformUrl: null
    };
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
