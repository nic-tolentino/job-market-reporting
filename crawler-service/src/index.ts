import { createApp } from './api/server';
import { CrawlerService } from './api/CrawlerService';

const PORT = process.env.PORT || 8080;
const GCP_PROJECT_ID = process.env.GCP_PROJECT_ID;
const GCP_REGION = process.env.GCP_REGION || 'us-central1';
const GEMINI_MODEL = process.env.GEMINI_MODEL || 'gemini-2.0-flash';

async function main() {
  console.log('Starting Crawler Service...');

  // Check Vertex AI configuration
  if (GCP_PROJECT_ID) {
    console.log(`Vertex AI configured: Project=${GCP_PROJECT_ID}, Region=${GCP_REGION}, Model=${GEMINI_MODEL}`);
  } else {
    console.warn('WARNING: GCP_PROJECT_ID environment variable not set');
    console.warn('The crawler will not be able to extract jobs from career pages');
    console.warn('Set GCP_PROJECT_ID to enable Vertex AI');
  }

  // Initialize crawler service with Vertex AI
  const crawlerService = new CrawlerService(GCP_PROJECT_ID, GCP_REGION, GEMINI_MODEL);

  // Create Express app
  const app = createApp(crawlerService);

  // Start server
  app.listen(PORT, () => {
    console.log(`Crawler Service listening on port ${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health`);
    console.log(`Crawl endpoint: POST http://localhost:${PORT}/crawl`);
  });

  // Graceful shutdown
  const shutdown = () => {
    console.log('Shutting down gracefully...');
    process.exit(0);
  };

  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
}

main().catch(console.error);
