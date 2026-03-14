import axios from 'axios';

const companies = [
  { id: 'atlassian', url: 'https://www.atlassian.com/company/careers' },
  { id: 'canva', url: 'https://www.canva.com' },
  { id: 'salesforce', url: 'https://www.salesforce.com' },
  { id: 'fphcare', url: 'https://www.fphcare.com' },
  { id: 'rocketlab', url: 'https://www.rocketlabusa.com' },
  { id: 'lightspeed', url: 'https://www.lightspeedhq.com' },
  { id: 'pushpay', url: 'https://pushpay.com' },
  { id: 'mainfreight', url: 'https://www.mainfreight.com' },
  { id: 'halter', url: 'https://www.halterhq.com' },
  { id: 'tracksuit', url: 'https://www.gettracksuit.com' }
];

async function runBatch() {
  for (const company of companies) {
    console.log(`\n=== Starting Crawl: ${company.id} (${company.url}) ===`);
    try {
      const response = await axios.post('http://localhost:8083/crawl', {
        companyId: company.id,
        url: company.url,
        crawlConfig: {
          maxPages: 15,
          followJobLinks: true,
          isDiscoveryMode: true,
          timeout: 60000
        }
      });
      console.log(`Result for ${company.id}:`, JSON.stringify(response.data.crawlMeta, null, 2));
      console.log(`Jobs found: ${response.data.jobs.length}`);
    } catch (error: any) {
      console.error(`Failed to crawl ${company.id}:`, error.message);
    }
    // Small delay between companies
    await new Promise(resolve => setTimeout(resolve, 5000));
  }
}

runBatch();
