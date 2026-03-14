import 'dotenv/config';
import { createApp } from './api/server';
import { CrawlerService } from './api/CrawlerService';
import { DEFAULT_MODEL, getModelConfig } from './config/model-config';

const PORT = process.env.PORT || 8081;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const GEMINI_MODEL = process.env.GEMINI_MODEL || DEFAULT_MODEL;

async function main() {
  const modelConfig = getModelConfig(GEMINI_MODEL);
  
  console.log('Starting Crawler Service (Vertex AI)...');
  console.log(`Model: ${modelConfig?.displayName || GEMINI_MODEL}`);
  console.log(`Cost: $${modelConfig?.inputCostPerMillion}/1M input, $${modelConfig?.outputCostPerMillion}/1M output`);

  if (GEMINI_API_KEY && GEMINI_API_KEY.startsWith('AQ.')) {
    console.log(`✓ Vertex AI API Key: Configured`);
  } else if (GEMINI_API_KEY) {
    console.warn(`⚠️  API key format unexpected (starts with ${GEMINI_API_KEY.substring(0, 6)}...)`);
  } else {
    console.warn('⚠️  WARNING: GEMINI_API_KEY not set');
  }

  // Initialize crawler service - pass BOTH key and model
  const crawlerService = new CrawlerService(GEMINI_API_KEY || '', GEMINI_MODEL);

  // Create Express app
  const app = createApp(crawlerService);

  // Start server
  app.listen(PORT, () => {
    console.log(`Crawler Service listening on port ${PORT}`);
    console.log(`Health: http://localhost:${PORT}/health`);
    console.log(`Crawl: POST http://localhost:${PORT}/crawl`);
  });

  const shutdown = () => {
    console.log('Shutting down gracefully...');
    process.exit(0);
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

main().catch(console.error);
