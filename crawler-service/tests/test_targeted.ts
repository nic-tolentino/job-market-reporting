import axios from 'axios';

async function testTargetedCrawl() {
    const url = 'http://localhost:8083/crawl';
    const payload = {
        companyId: 'atlassian',
        url: 'https://www.atlassian.com', // Original homepage
        seedData: {
            url: 'https://www.atlassian.com/company/careers/all-jobs?team=Engineering%2CData%2C%20Analytics%20%26%20Research%2CDesign&location=&search=',
            category: 'tech-filtered'
        },
        crawlConfig: {
            maxPages: 3
        }
    };

    console.log('Sending Targeted Mode crawl request for Atlassian...');
    try {
        const response = await axios.post(url, payload);
        console.log('Crawl Meta:', JSON.stringify(response.data.crawlMeta, null, 2));
        console.log(`Jobs Found: ${response.data.jobs.length}`);
        if (response.data.jobs.length > 0) {
            console.log('First Job:', response.data.jobs[0].title);
        }
    } catch (error: any) {
        console.error('Crawl failed:', error.response?.data || error.message);
    }
}

testTargetedCrawl();
