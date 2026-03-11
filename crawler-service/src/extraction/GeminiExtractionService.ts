import { VertexAIClient } from './VertexAIClient';
import { NormalizedJob } from '../api/types';

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
 * Service for extracting job data using Vertex AI (Gemini)
 */
export class GeminiExtractionService {
  private vertexClient: VertexAIClient;
  private model: string;

  constructor(project: string, location: string = 'us-central1', model: string = 'gemini-2.0-flash') {
    this.model = model;
    this.vertexClient = new VertexAIClient(project, location, model);
  }

  /**
   * Extracts jobs from page content using Vertex AI
   */
  async extractJobs(content: string, config: ExtractionConfig = {}): Promise<ExtractionResult> {
    const prompt = this.buildPrompt(content, config);

    try {
      const response = await this.vertexClient.generateContent(prompt);

      // Check if response is empty
      if (!response.text || response.text.trim().length === 0) {
        throw new Error(
          'Vertex AI returned empty response. ' +
          'This may indicate API issues or quota exceeded. ' +
          'Check: https://console.cloud.google.com/vertex-ai'
        );
      }

      const llmJobs = this.parseJobsFromResponse(response.text);

      // Convert LLM jobs to NormalizedJob with service-set fields
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
      // Enhance Vertex AI error messages
      const errorMessage = (error as Error).message || 'Unknown error';
      
      if (errorMessage.includes('Permission Denied')) {
        throw new Error(
          `Vertex AI Error: Service account lacks permissions. ` +
          `Solutions: 1) Grant "Vertex AI User" role to service account, 2) Verify service account has access to project, ` +
          `3) Enable Vertex AI API at https://console.cloud.google.com/vertex-ai`
        );
      }
      if (errorMessage.includes('Quota Exceeded')) {
        throw new Error(
          `Vertex AI Error: Quota exceeded. ` +
          `Solutions: 1) Wait for quota reset, 2) Request quota increase at https://console.cloud.google.com/vertex-ai/quotas`
        );
      }
      if (errorMessage.includes('API not enabled')) {
        throw new Error(
          `Vertex AI Error: API not enabled. ` +
          `Solution: Enable Vertex AI API at https://console.cloud.google.com/vertex-ai`
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
   * Parses job data from LLM response
   */
  private parseJobsFromResponse(text: string): LlmJobData[] {
    try {
      // Remove markdown code blocks if present
      const cleanText = text.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
      
      // Try parsing as array
      let jobs: LlmJobData[];
      try {
        jobs = JSON.parse(cleanText) as LlmJobData[];
      } catch {
        // Try parsing as object with jobs array
        const obj = JSON.parse(cleanText) as { jobs?: LlmJobData[] };
        jobs = obj.jobs || [];
      }

      // Ensure array
      if (!Array.isArray(jobs)) {
        jobs = [jobs];
      }

      return jobs;
    } catch (error) {
      console.error('Failed to parse LLM response:', error);
      console.error('Response text:', text);
      return [];
    }
  }

  /**
   * Converts LLM job data to NormalizedJob format
   */
  private toNormalizedJob(llmJob: LlmJobData, companyName?: string): NormalizedJob {
    // Generate platformId from title and date
    const slug = llmJob.title.toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
      .slice(0, 50);
    
    const dateStr = (llmJob.postedAt as string || new Date().toISOString()).replace(/-/g, '').slice(0, 8);
    const platformId = llmJob.platformId as string || `${slug}-${dateStr}`;

    return {
      platformId,
      source: 'Crawler',
      title: llmJob.title,
      companyName: llmJob.companyName || companyName || 'Unknown',
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
      postedAt: llmJob.postedAt ? (llmJob.postedAt as string).replace(/-/g, '').slice(0, 8) : null,
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
- description
- salaryMin, salaryMax, salaryCurrency (if mentioned)
- employmentType (Full-time, Part-time, Contract, Internship)
- seniorityLevel (Junior, Mid, Senior, Lead, Principal, Director, VP)
- workModel (Remote, Hybrid, On-site)
- department
- postedAt (ISO date if mentioned)
- applyUrl

Return as JSON array. If no jobs found, return empty array [].`;
