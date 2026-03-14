/**
 * Model configuration for Gemini API
 *
 * Centralized configuration for model selection
 * Change DEFAULT_MODEL to switch the default model across the entire service
 */

export interface ModelConfig {
  modelId: string;
  displayName: string;
  inputCostPerMillion: number;
  outputCostPerMillion: number;
  maxTokens: number;
  description: string;
}

export const AVAILABLE_MODELS: Record<string, ModelConfig> = {
  'gemini-2.5-flash-lite': {
    modelId: 'gemini-2.5-flash-lite',
    displayName: 'Gemini 2.5 Flash Lite',
    inputCostPerMillion: 0.075,
    outputCostPerMillion: 0.30,
    maxTokens: 8192,
    description: 'Ultra-fast lite model for high-volume extraction.'
  },
  'gemini-2.0-flash': {
    modelId: 'gemini-2.0-flash',
    displayName: 'Gemini 2.0 Flash',
    inputCostPerMillion: 0.075,
    outputCostPerMillion: 0.30,
    maxTokens: 8192,
    description: 'Excellent quality for extraction tasks.'
  },
  'gemini-1.5-flash': {
    modelId: 'gemini-1.5-flash',
    displayName: 'Gemini 1.5 Flash',
    inputCostPerMillion: 0.075,
    outputCostPerMillion: 0.30,
    maxTokens: 8192,
    description: 'Previous generation Flash model.'
  }
};

export const DEFAULT_MODEL = 'gemini-2.5-flash-lite';

export function getModelConfig(modelId: string): ModelConfig | undefined {
  return AVAILABLE_MODELS[modelId];
}

export function getAvailableModelIds(): string[] {
  return Object.keys(AVAILABLE_MODELS);
}

export function estimateCost(modelId: string, inputTokens: number, outputTokens: number): number {
  const config = getModelConfig(modelId);
  if (!config) return 0;
  const inputCost = (inputTokens / 1_000_000) * config.inputCostPerMillion;
  const outputCost = (outputTokens / 1_000_000) * config.outputCostPerMillion;
  return inputCost + outputCost;
}
