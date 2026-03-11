import { createApp } from './api/server';
import { CrawlerService } from './api/CrawlerService';

const PORT = process.env.PORT || 8080;

async function main() {
  console.log('Starting Crawler Service...');
  
  // Initialize crawler service
  const crawlerService = new CrawlerService();
  
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
