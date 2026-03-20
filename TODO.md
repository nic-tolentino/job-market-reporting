## 💡 Future ideas

Now:

- We should update the admin dashboard to show the % of manifests which have an ATS, and also a graph showing % of companies have been crawled in the past 7 days?

- I need to understand how we handle various ingress and update processes. Eg, if we ingest a new company from LinkedIn, how or when do we create a new company manifest / profile? Then when and how do we flesh out that company's detauls including ATS identification? Then how do we keep the seeds up to date, or other data up to date, like num employees or visa support?

- Use https://stackshare.io/ to verify the tech that companies use
- Check builtin.com and https://stackshare.io/ for ATS identification

- Ok I really need an admin panel now - I need a way to understand information like: status of company crawls and seed urls including last run (and ideally analytics like run time, number of jobs found, % of tech jobs out of all jobs processed per company, etc. - we need information to understand the status of the crawling process, as well as stats that can help us detect optimisation opportunities, enable/disable scheduled crawling, feedback submissions, current state of ingestion requests (plus any dead letters), ), status of active crawls, list of datset IDs that have been consumed. A bunch of convenience stuff like: re-process datasets, ingest dataset ID, trigger a crawl for a seed url/company, etc. We should do some brainsotrming on what would be useful to have in an admin panel. 

- Next I need to figure out a staged deployment process - I can't keep breaking and testing in production for frontend and backend services.

- We need to make the landing page better reflect the Discover. Grow. Connect. philosophy.

- How does the manifest sync with the backend BQ tables? Is it additive / merge this existing content? Eg, if we remove a seed url from the manifest, does it remove the company from the BQ table (probably shouldn't be removed, but then how should they be removed / disabled)? 

- Write crawled data into a new persisted dataset at the end of the day: Admin-triggered crawls (triggerCrawl) persist to crawl_runs in BigQuery and update crawler_seeds, but they don't currently write to the jobs ingestion pipeline — the extracted jobs are returned in the API response but not stored anywhere. The "Recent Ingestions" on the Analytics page shows rows from a separate ingestion/BigQuery dataset table, not from admin crawl runs.

- Similar roles in the job screen should only include jobs in the same country.

- The search box breaks after pressing enter / making a custom search
- We also need to search over the job descriptions and show search results? Also search over tech domains. Should also search over learning / community resources.

- we have orphaned jobs due to company id changes.

- build gdg pages for each city? So we can find companies currently hiring in that space.

- We can use https://www.immigration.govt.nz/work/requirements-for-work-visas/approved-employers/accredited-employer-list/?page=1 to find companies that sponsor visas for NZ companies only, but there may be other websites for AU and Spain.

Later:

- Melbourne tech events calendar: https://calendar.google.com/calendar/u/0/embed?src=741714b060754779a29f37566919b7921ec1133990e4c4021d013e72204f38f9@group.calendar.google.com&ctz=Australia/Melbourne

- Make all constant values in the manifest schema file into ALL CAPS?

- Company level tech stack has a lot of potentail for improvement refer to company-tech-stack-fix.md

- Add a location filter to the job table on the company page???


- Add testing, design, project management, product management???
- Add a link to the relevant Tech Domain to the tech page, (and Job page?)
- Identify what the core technology (or multiple??) are for a role - we can use this to sort the roles and other information. May need NLP.
- The ability to group technologies by category, e.g. cloud, server, database, web, mobile, backend, etc in the landing, and company pages

- Add currency detection and support
- Add salary analysis on a per country basis, comparing industries and seniorities
- Add details to the job salary indicatig the source of the info (job listing, vs market data, vs AI estimate) so we can make accurate analysis
- Add language detection for job listings

- Add a soft skills (leadership, communication, etc) and capabilities (agile, devops, etc) leaderboard, also show it on the job page
- Add pages for high level domains: Web, Mobile, Backend, Full stack (?), security, SRE, etc.
- Extend appropriate unit tests, both backend and frontend.

- There are jobs from mid-2025 in the database. We should probably do something about them. Remove them???

- How to handle when there's no apply to job link?
- List trending jobs, companies, and technologies (most visited)

- The salary data is very messy. See what we can do to clean it up.
- Which companies provide Visa sponsorship?

- Create a Tech record with the pre-computed data for each tech to reduce computation? or just rely on cache?
- Can we rid of some of the nullable fields on the company and jobs tables?
- Test error scenarios
- Tidy up titles, figure out how to handle 'Mid-Senior level', maybe multiple levels are allowed for each job?
- Add a tooltip to the company verified status explaining what the status means (eg, if it's unverified, explain it's lacking data)
- Data issue - how we can limit certain jobs from showing up in the wrong technologies? For example, native iOS and Android roles shouldn't show Xamarin or ReactNative jobs. Or perhaps we need a way to optionally filter out or exclude other technologies
- Also, estimate salary range for a job but provide the confidence level (LOW AI market estimate, vs HIGH job posting range) - always with a disclaimer that it's just an estimate.

- Mobile design is still pretty meh in places especially rows of information like jobs, and page headers.
- Mobile bug: refresh on company page results in 404 Not Found
- Launch to the third party url where the data was obtained instead of the apply url.

- Make it clear what the sources of jobs are, and how often the data is updated so people can make an informed decision on how to use this site.

- Discover tech jobs, insights, and resources: we're here to help you succeed in your tech career 🚀📈❤️

- jobs per capita comparison
- salary per job over time
- overall market job seniority (how many juniors and mids are there)

- smoothly show/hide the top navbar when scrolling up/down
- left align section headings in tech page

- does the current system scale to handle different languages - especially as we've now started supporting Spain / Spanish?

- other country selector option, to get feedback on demand for other countries
- label and show the source of the data
- does our pii filter out "If you would like to find out more about this amazing opportunity, please feel free to call our Head of Talent Acquisitions, Bob Bloblob, mobile 021 999 111, she loves to chat about TVNZ careers."
- allow users to contribute to the company data by creating a public repo to host that data. The backend will pull the data from the repo and upsert it into the database.
- rename AnalyticsBigQueryRepository and related classes from Analytics to Insights? To avoid confusion with actual analytics?

- Filters for people lead roles like managers? Actually I probably need this to help me find a role in Spain
- Ability to 'follow' a job and mark it 'applied' with a date?
- Ability to 'hide' a job or company (though perhaps it's more about sorting them to the bottom and visibly marking them in some way?)
- Ability to 'log in' to view my jobs, and see the success of my suggested resources? Also the ability to show a personal dashboard with the companies and tech that I follow as well as any roles that I saved or applied for which have since closed? Ability to mark applied jobs as rejected so we can show how long it takes to get a rejection on average. 
- Ability to define job application process for each company (role?)
- record which platforms have been used to get job data for a company - this way we can record that there's no careers platform for a company, and that historically their roles have only appeared on LinkedIn or X or Y platform. Which can help inform our own data intestion approaches.

- for company IDs, how do we deal with international companies having the same name? I imagine we could end up with many conflicts - or worse, if we're not careful we may end up with multiple companies merged into one! Perhaps we can use the company home page and countries which a company operates in as part of the identification? Hmm. Likewise, how do we distinguish the companies during search? eg: Company Name (NZ)?
- Also, how do we store more semi-dynamic information like what technologies a company uses, or how many jobs they've had for a particular role? Perhaps we can use the job data to inform what technologies are most commonly asked for at a company - how could that be setup? And how do we backup that data (or do we back it up?) or do we just re-calculate it every so often based on historic data?

- we should find a way to prioritise updating certain companies, and technologies which are most popular / high value

- Add a CONTRIBUTING with AI guidelines
- Refactor documentation, arrange it, update it, distinguish between ADRs and Feature Specifications. Arrange by function like the codebase


- Search entire codebase looking for opportunities to reduce magic numbers and magic strings
- Rename analytics to insights (package name)
- Standardize DB column names to be consistent either way (camelCase or snake_case)
- Improve Remove/Hybrid % data by processing the job description, and searching for online data about the company's work model.
- Why does the Hubs data take so long to load? Is it not being cached? https://www.devassembly.org/hubs/mobile

Atul feedback:
8
- Top search is broken 
- Main search by location 
- Auto assign country I 
- Filter by city or region 
- Love the Tech Hubs 
- companies roles/ 
- Add activity indicator 
- interest into roles or companies? Check for seniority filter usage 
- Atul: Market maybe learn? 
- Domain hubs explore all not visible 
- Rather put one hub - with highest at the front, or personalised - once you visit it. 
- Link to domain hub from technology hub 
- Remember scroll position (and don't reload data)
- Improve 'unlisted' as pay label, it's confusing replace with blank


Images:
- Manually search for logo urls for each of the technologies (and companies?). Host the images locally so they are more stable? Maybe use them as backups if the url isn't provided?
- We should store the company images for when the companies stop advertising roles. Use them as backups if no updated url is available? Because companies may change their url over time

Major:
- Add user accounts and authentication + saved companies / technologies + email notifications
- Add interview preparation content
- look into legality of using scrapped data, also check if there's a better way to get this data, also do i need to add any disclaimers, links, or legal a stuff?

Nice to have:
- ideally we'd log when a location can't be properly parsed
- The "Market Sentiment" Feedback: Since you don't have historical data yet, add a simple "Is this salary range accurate for [City]?" button. It crowdsources "The Now" and builds a high-trust relationship with local devs who know the market.
- The "Remote-from-NZ" Tag: Many AU companies hire NZ-based devs as contractors. Highlighting "Remote (NZ/AU Wide)" is a huge value-add for the local community that larger platforms often miss.
- generate sitemap.xml for SEO

Admin/stats
- Breakdown of ATS provider counts / %s over the companies
- List of all companies and their current sync status - highlighting issues
- Background job sync logs?


Meh?:
- Filters for people lead roles like managers?
- Add a locations with most jobs, and locations for a given tech. Does this make sense? we have location filters for tech and companies. Maybe total jobs per city, but it's not very useful unless you're a migrant? Even then you can just filter jobs for a technology and location?

Funding ideas:
- Make the related companies and related roles show sponsored companies and roles first, based on the browsing history of the user (look at the technologies they've visited). Perhaps these 'related' sections need to be separated into their own APIs.
- Show sponsor companies at the top of the website???
- Sponsored companies get badges and are listed at the top of lists?
- donations



- should the crawler-service/storage folder be added to gitignore?
- https://www.devassembly.org/company/xero lists 8 jobs, but it's actually just 4 jobs which weren't de-duplicated successfully. We should understand why and fix this issue. 
- Consider adding a calculation for the total number of jobs listed this month, and this year for each company, and on average how long a job listing exists for (do we ever remove jobs from the database - should we?).
- Add new scrawler seed categories: homepage?, careers, and reserve tech-filtered for urls that are highly tech focused.

- Xero careers with Engineering + Data Science filter (page 1 of x): https://careers.xero.com/jobs/?team=Data,%20AI%20%26%20Machine%20Learning&team=Engineering&pagesize=20#results 
- Trade Me careers with Engineering filter: https://trademe.csod.com/ux/ats/careersite/1/home?c=trademe&cfdd[0][id]=241&cfdd[0][options][0]=512
- atlassian with Engineering + Data + Design filters: https://www.atlassian.com/company/careers/all-jobs?team=Engineering%2CData%2C%20Analytics%20%26%20Research%2CDesign&location=&search=

Trade Me: https://trademe.csod.com/ux/ats/careersite/6/home?c=trademe (Cornerstone Integration)
Xero: https://careers.xero.com (Engineering specific findings also available)
Atlassian: https://www.atlassian.com/company/careers/all-jobs (Workday)
Canva: https://www.canva.com/careers/ (SmartRecruiters)
Salesforce: https://www.salesforce.com/company/careers/ (Workday)
Fisher & Paykel: https://www.fphcare.com/us/careers/
Rocket Lab: https://www.rocketlabusa.com/careers/
Mainfreight: https://www.mainfreight.com/careers
Pushpay: https://pushpay.com/company/careers
Halter: https://halter.com/careers/
Tracksuit: https://www.tracksuit.com/careers/jobs
Vend/Lightspeed: https://www.lightspeedhq.com/careers/

---

Company website identification outcome:

Uncertain Cases to Review

REGENERA: Energy vs Health ambiguity preserved.
Great Value Hiring: Website found but content generic.
onthaast en geniet: Skipped.
Verse: Closed.
Evolution Australia: Evolution Jobs.
EPI Company: Epi-interactive.
Breaker: Skipped.
KD: Updated as KD Semiconductor (kd.tech) in Madrid.
ParallaxOS: Skipped.
Generic/Undisclosed: Skipped files like undisclosed-employer.json, manufacturing.json, and confidential-careers.json.

Statistics
Total companies missing websites identified: 847
Companies processed and updated: 839
Companies skipped (Generic/Unactionable): 8
Status: Completion Achieved
I am ready to process more companies in batches. Please let me know if you have a specific list or if I should continue through the alphabetical list.

---

From the 145 SQL companies, we extracted 1 new valid ATS config
  (macquarie-group → LEVER/macquarietechnologygroup). The 2 remaining
  "candidates" (pet-circle LEVER and open→openai match) are rejected by
  live API validation.

- Consider: Build probe_search_api.py using Brave Search free tier
  (2,000/month) — highest ROI, fully automated, reusable every month as
  new companies are added.

[Pasted text #6 +4 lines] can we please resolve this? you mentioned doing a one off BQ fix previously. Then we can update the ATS of those companies confidently. 

 - Seeding — already handled by CompanySyncService.syncFromManifest().
  Run POST /api/admin/pipeline/sync-companies to push all 153 configs to
   BigQuery, then POST /api/internal/ats-sync-all to kick off the first
  extraction.


  pull jobs from??? https://www.ycombinator.com/companies/rever/jobs/CjCpj0t-backend-engineer?utm_source=syn_li

---

LinkedIn url analysis which may allow us to easily ingest jobs from comapnies that don't have career pages. The &f_C=10424439 parameter appears to match to a specific company. We should be able to extract this parameter from the url and use it to find jobs for that company.

https://www.linkedin.com/jobs/search?keywords=Alan&location=Spain&geoId=105646813
&f_TPR=
&f_C=10424439
&position=1
&pageNum=0

https://www.linkedin.com/jobs/search?keywords=Alan&location=Spain&geoId=105646813
&f_C=11016997
&f_TPR=
&position=1
&pageNum=0


Au cxVx5TgHwARQ8f79g
nz kcDYvHh5956pKCjb7
sp 6dWGtKSBpgZA9va8Q