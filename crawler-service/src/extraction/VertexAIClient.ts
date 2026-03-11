import { GoogleAuth } from 'google-auth-library';

/**
 * Vertex AI client for Gemini API
 * Uses service account authentication instead of API keys
 */
export class VertexAIClient {
  private auth: GoogleAuth;
  private project: string;
  private location: string;
  private model: string;

  constructor(project: string, location: string = 'us-central1', model: string = 'gemini-2.0-flash') {
    this.project = project;
    this.location = location;
    this.model = model;
    
    // Initialize auth with service account from environment
    this.auth = new GoogleAuth({
      scopes: ['https://www.googleapis.com/auth/cloud-platform']
    });
  }

  /**
   * Generate content using Vertex AI
   */
  async generateContent(prompt: string): Promise<VertexAIResponse> {
    const client = await this.auth.getClient();
    const accessToken = await client.getAccessToken();
    
    const url = `https://${this.location}-aiplatform.googleapis.com/v1/projects/${this.project}/locations/${this.location}/publishers/google/models/${this.model}:generateContent`;
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        contents: [{
          parts: [{
            text: prompt
          }]
        }],
        generationConfig: {
          temperature: 0.1,
          topP: 0.8,
          topK: 40,
          maxOutputTokens: 8192
        }
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw this.parseVertexAIError(error);
    }

    const data = await response.json();
    return this.parseVertexAIResponse(data);
  }

  /**
   * Parse Vertex AI response to match Gemini API format
   */
  private parseVertexAIResponse(data: any): VertexAIResponse {
    const candidate = data.candidates?.[0];
    if (!candidate) {
      throw new Error('No candidates in Vertex AI response');
    }

    return {
      text: candidate.content?.parts?.[0]?.text || '',
      usageMetadata: {
        promptTokenCount: data.usageMetadata?.promptTokenCount || 0,
        candidatesTokenCount: data.usageMetadata?.candidatesTokenCount || 0,
        totalTokenCount: data.usageMetadata?.totalTokenCount || 0
      }
    };
  }

  /**
   * Parse Vertex AI errors into user-friendly messages
   */
  private parseVertexAIError(error: any): Error {
    const status = error.error?.status || error.status || 'UNKNOWN';
    const message = error.error?.message || error.message || 'Unknown Vertex AI error';
    
    let userMessage = `Vertex AI Error: ${message}`;
    
    if (status === 'PERMISSION_DENIED') {
      userMessage = `Vertex AI Permission Denied. Solutions: 1) Ensure service account has "Vertex AI User" role, 2) Check service account has access to project ${this.project}, 3) Verify Vertex AI API is enabled`;
    } else if (status === 'RESOURCE_EXHAUSTED') {
      userMessage = `Vertex AI Quota Exceeded. Solutions: 1) Wait for quota reset, 2) Request quota increase at https://console.cloud.google.com/vertex-ai/quotas, 3) Reduce request frequency`;
    } else if (status === 'INVALID_ARGUMENT') {
      userMessage = `Vertex AI Invalid Request. Details: ${message}. Check prompt format and model parameters`;
    } else if (status === 'UNAVAILABLE') {
      userMessage = `Vertex AI Service Unavailable. This is usually temporary. Retry in a few seconds`;
    }
    
    return new Error(userMessage);
  }
}

export interface VertexAIResponse {
  text: string;
  usageMetadata: {
    promptTokenCount: number;
    candidatesTokenCount: number;
    totalTokenCount: number;
  };
}
