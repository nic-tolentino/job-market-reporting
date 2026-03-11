import { createApp } from './api/server';
import { CrawlerService } from './api/CrawlerService';

const PORT = process.env.PORT || 8080;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

async function main() {
  console.log('Starting Crawler Service...');

  // Log API key status (not the actual key for security)
  if (GEMINI_API_KEY) {
    console.log(`Gemini API key: Configured (key starts with ${GEMINI_API_KEY.substring(0, 8)}...)`);
  } else {
    console.warn('WARNING: GEMINI_API_KEY environment variable not set');
    console.warn('The crawler will not be able to extract jobs from career pages');
    console.warn('Check health endpoint for details: /health');
  }

  // Initialize crawler service
  const crawlerService = new CrawlerService(GEMINI_API_KEY);

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
