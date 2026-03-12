import { DEFAULT_MODEL } from '../config/model-config';

/**
 * Vertex AI Client for Gemini API
 * Uses API key authentication with aiplatform endpoint
 */
export interface VertexAIResponse {
  text: string;
  usageMetadata: {
    promptTokenCount: number;
    candidatesTokenCount: number;
    totalTokenCount: number;
  };
}

export class VertexAIClient {
  private apiKey: string;
  private model: string;

  constructor(apiKey: string, model: string = DEFAULT_MODEL) {
    if (!apiKey || apiKey.length < 20) {
      throw new Error(
        'Invalid Vertex AI API key. Get one from Google Cloud Console.'
      );
    }
    this.apiKey = apiKey;
    this.model = model;
  }

  /**
   * Generate content using Gemini API
   * Uses streaming endpoint with API key auth
   */
  async generateContent(prompt: string): Promise<VertexAIResponse> {
    const url = `https://aiplatform.googleapis.com/v1/publishers/google/models/${this.model}:streamGenerateContent?key=${this.apiKey}&alt=sse`;

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        contents: [{
          role: 'user',
          parts: [{
            text: prompt
          }]
        }]
      })
    });

    if (!response.ok) {
      const error = await response.json();
      throw this.parseError(error, response.status);
    }

    const text = await response.text();
    return this.parseSSEText(text);
  }

  /**
   * Parse SSE text response from streamGenerateContent
   */
  private parseSSEText(text: string): VertexAIResponse {
    const lines = text.split('\n').filter(line => line.startsWith('data: '));
    let fullText = '';
    let usageMetadata = { promptTokenCount: 0, candidatesTokenCount: 0, totalTokenCount: 0 };
    
    for (const line of lines) {
      try {
        const json = JSON.parse(line.replace('data: ', ''));
        if (json.candidates?.[0]?.content?.parts?.[0]?.text) {
          fullText += json.candidates[0].content.parts[0].text;
        }
        if (json.usageMetadata) {
          usageMetadata = json.usageMetadata;
        }
      } catch {
        // Skip malformed lines
      }
    }
    return { text: fullText, usageMetadata };
  }

  /**
   * Parse Gemini API errors
   */
  private parseError(error: any, status: number): Error {
    const code = error.error?.code || status;
    const message = error.error?.message || error.message || 'Unknown Gemini API error';

    let userMessage = `Gemini API Error: ${message}`;

    if (code === 400) {
      if (message.includes('API_KEY_INVALID')) {
        userMessage = 'Vertex AI API Key Invalid. Check that the API key is correct.';
      }
    } else if (code === 429) {
      userMessage = 'Gemini API Rate Limit Exceeded. Please wait and retry.';
    } else if (code === 503) {
      userMessage = 'Gemini API Service Unavailable. Retry in a few seconds.';
    }

    return new Error(userMessage);
  }
}
