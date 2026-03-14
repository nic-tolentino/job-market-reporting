import axios from 'axios';

async function testTargetedCrawl() {
    const url = 'http://localhost:8083/crawl';
    const payload = {
        companyId: 'xero',
        url: 'https://www.xero.com', 
        seedData: {
            url: 'https://careers.xero.com/jobs',
            category: 'general',
            lastKnownJobCount: 10,
            lastKnownPageCount: 1
        },
        crawlConfig: {
            maxPages: 2
        }
    };

    console.log('Sending Targeted Mode crawl request for Xero...');
    try {
        const response = await axios.post(url, payload);
        console.log('Crawl Meta:', JSON.stringify(response.data.crawlMeta, null, 2));
        console.log(`Jobs Found: ${response.data.jobs.length}`);
    } catch (error: any) {
        console.error('Crawl failed:', error.response?.data || error.message);
    }
}

testTargetedCrawl();
