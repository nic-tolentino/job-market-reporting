# Engineering Tech Market Insights (AU/NZ/ES)

## Objective
A full-stack data analyzer that tracks engineering job trends (technologies, seniority, and location) across **Australia**, **New Zealand**, and **Spain**. The system periodically ingests data from major job boards, stores it in a data warehouse, and visualizes it on a web dashboard.

---

## 🏗 High-Level Architecture

| Component | Technology | Hosting |
| :--- | :--- | :--- |
| **Backend** | Spring Boot 3.x (Kotlin) | Google Cloud Run |
| **Data Warehouse** | Google BigQuery | GCP (Serverless) |
| **Frontend** | React (Vite/TypeScript) + Tailwind | Vercel / static hosting |
| **Data Ingestion** | Apify API | Seek & LinkedIn |
| **Automation** | Cloud Scheduler | Weekly Triggers |

---

## � Project Structure
```text
.
├── backend/               # Spring Boot 3.x (Kotlin) API
│   ├── src/               # Application source code
│   ├── build.gradle.kts   # Build configuration
│   └── Dockerfile         # Multi-stage build for Cloud Run
├── frontend/              # React (Vite/TypeScript) Dashboard (Coming Soon)
└── README.md              # Project documentation
```

---

## �🚀 Component Details

### 🧠 Backend: The "Brain" (Spring Boot + Kotlin)
- **Framework**: Spring Boot 3.x with Kotlin.
- **Build System**: Gradle (Kotlin DSL).
- **Core Responsibilities**:
    - Expose REST API for the frontend dashboard.
    - Fetch job listings from Apify API (JSON).
    - **Data Cleaning**: Normalize seniority levels and extract technology tags (e.g., Kotlin, Python, Kafka).
    - **GCP Integration**: Stream processed data to BigQuery using `spring-cloud-gcp-starter-bigquery`.
- **Deployment**: Multi-stage Docker build, containerized via Jib/Docker, deployed to Cloud Run.

### 📊 Data Layer: The "Warehouse" (BigQuery)
- **Dataset**: `techmarket`
- **Table**: `raw_postings`
- **Schema**:
    - `job_id`: Unique identifier.
    - `source`: Seek/LinkedIn.
    - `country`: AU/NZ/ES.
    - `title`, `company`, `location`.
    - `seniority_level`: Normalized (Senior, Mid, Junior, etc.).
    - `technologies`: Array of extracted tech tags.
    - `salary_min`, `salary_max`.
    - `posted_date`.

### 💻 Frontend: The "Dashboard" (React/Vite)
- **Framework**: React 18+ bootstrapped with Vite and TypeScript.
- **Styling**: Tailwind CSS for a clean, data-heavy, *levels.fyi-inspired* aesthetic.
- **UI Components**: landing page with filters for Country and Tech.
- **Visualizations**: Recharts for line graphs (job volume) and bar charts (tech demand).
- **Communication**: Standard REST calls to the Spring Boot backend.

## 🔮 Future Considerations: Multi-Source Data

As the system scales to ingest data from multiple job boards (e.g., LinkedIn, Workday, Seek) across various regions, we will evolve the data architecture:
- **Composite Identity**: A job's unique identifier will need to be a combination of `jobId` AND `source` (and potentially `country`) to prevent collisions between different job boards using overlapping ID schemes.
- **Normalization Adapters**: Different platforms provide varying levels of data quality. We will introduce an adapter/factory pattern to normalize disparate payloads into our standard `CompanyRecord` and `JobRecord` models.

---

## 📈 Analytical Charting Ideas

To provide maximum value to job hunters and recruiters, the dashboard will support the following visualizations:

- **Companies Using a Given Technology**: List or visualize all companies hiring for a specific tech stack (e.g., "Who's hiring for React?").
- **Company Posting Frequency Timeline**: Line chart showing how many jobs a specific company has advertised each month.
- **Technology Demand Timeline**: Line chart tracking the volume of jobs posted for a given technology over time (e.g., "Is Go increasing or decreasing?").
- **Salary Bands by Technology**: Scatter plot or box plots comparing the min/max salary ranges across different technologies.
- **Top Hiring Companies**: A leaderboard (bar chart) of companies actively posting the most roles in the current week/month.
- **Benefits Cloud**: A word cloud or bar chart highlighting the most common perks (e.g., "Remote", "Health Insurance", "Stock Options").
- **Seniority Demand Distribution**: A donut chart showing the split between Junior, Mid, and Senior roles currently active in the market.

---

## ⚡ Data Caching & Optimization

Since historical job data does not change frequently (ingestion happens daily/weekly), we will implement targeted caching strategies to keep the Dashboard API responsive and minimize BigQuery scan costs:

1. **BigQuery Materialized Views / Scheduled Queries**:
   - Rather than querying the raw job entries every time a chart loads, we can create aggregated tables (e.g., `monthly_tech_demand`, `monthly_company_postings`).
   - These tables are refreshed via Scheduled Queries after each Apify ingestion run.
2. **Application-Level Caching (Spring Boot)**:
   - Use Spring Cache (e.g., `@Cacheable` with caffeine or Redis) on the `DashboardController` endpoints.
   - Cache TTL can be set to 12-24 hours or invalidated programmatically whenever the webhook indicates a new sync has completed.
3. **Frontend Static Generation (Next.js)**:
   - For high-level, slow-moving charts (like "Top Tech of the Year"), use Next.js ISR (Incremental Static Regeneration) to serve pre-rendered charts, pulling from the API only periodically.

---

## 🗺 Implementation Roadmap

- [ ] **Phase 1: Scaffolding** - Set up Spring Boot, GCP dependencies, and Docker configuration.
- [ ] **Phase 2: Ingestion Logic** - Implement Apify integration and BigQuery streaming.
- [ ] **Phase 3: Data Dashboard API** - Build aggregate query endpoints.
- [ ] **Phase 4: Frontend** - Initialize Vite application and build the 3 core pages (Landing, Tech Details, Company Profile).

---

## 💰 Cost & Constraints
- **Budget**: Targeting $0/month (staying within Free Tiers).
- **Volume**: ~100 jobs per week across 3 countries.
- **Environment**: Development via Docker Desktop.

---

## 💡 Future ideas

Now:

- Improve the tech page layout - how can we show companies and jobs, and other insights, without resorting to giant lists? The main challenge is showing long lists of content. Perhaps a locally driven pager approach is suitable?
- Update the current drop down filters in tech page for jobs to look similar to the country drop down in the navbar. Does it make sense to make this a standard component?

Implement similar paging on the home screen, show 5 per page, but only load the top 20 companies? Maybe also only show a page size of 5 companies in the tech page?
- Add the ability to see more than just the top 10 technologies and companies on landing screen

- Find out if the Westpac iOS role is in the dataset

Later:

- Locations in company, tech, and job pages appear with city shown twice. Eg Auckland, Auckland. Countries are only shown once. Eg "New Zealand". Please check first whether this is a backend data or frontend display issue.

- Add a location job filter to the company page?
- Ability to filter companies by seniority in tech page. 

- Technologies need to retain their capitalisation - e.g. AWS, .NET, Go, iOS, etc

- The ability to group technologies by category, e.g. cloud, server, database, web, mobile, backend, etc in the landing, and company pages

- Add a soft skills (leadership, communication, etc) and capabilities (agile, devops, etc) leaderboard, also show it on the job page
- Extend appropriate unit tests, both backend and frontend.

- How to handle when there's no apply to job link?
- List trending jobs, companies, and technologies (most visited)

- The salary data is very messy. See what we can do to clean it up.
- Which companies provide Visa sponsorship?

- Create a Tech record with the pre-computed data for each tech to reduce computation? or just rely on cache?
- Can we rid of some of the nullable fields on the company and jobs tables?
- Test error scenarios
- Tidy up titles, figure out how to handle 'Mid-Senior level', maybe multiple levels are allowed for each job?

- Mobile design is still pretty meh in places especially rows of information like jobs, and page headers.
- Launch to the third party url where the data was obtained instead of the apply url.

Images:
- Manually search for logo urls for each of the technologies (and companies?). Host the images locally so they are more stable? Maybe use them as backups if the url isn't provided?
- We should store the company images for when the companies stop advertising roles. Use them as backups if no updated url is available? Because companies may change their url over time

Bugs?:
- Wait, the landing page is now loading for an instant and then the whole page goes blank. Wait, now when I navigate to the landing page it loads fine, but if I navigate to a compage page the screen goes blank and navigating back doesn't fix the blank screen. Hmm, I wonder if it has to do with the backend taking too long or returning an error. I notice that upon loading a page, after a few seconds if I refresh. I'm struggling to reproduce the issue. Perhaps it has to do with the server cache and spinning up data?

Major:
- Add support for different countries
- Add user accounts and authentication + saved companies / technologies + email notifications
- Add interview preparation content
- Add links to local tech communities (eg meetups) for each technology/city/country
- look into legality of using scrapped data, also check if there's a better way to get this data, also do i need to add any disclaimers, links, or legal a stuff?

Nice to have:
- ideally we'd log when a location can't be properly parsed
- The "Market Sentiment" Feedback: Since you don't have historical data yet, add a simple "Is this salary range accurate for [City]?" button. It crowdsources "The Now" and builds a high-trust relationship with local devs who know the market.
- The "Remote-from-NZ" Tag: Many AU companies hire NZ-based devs as contractors. Highlighting "Remote (NZ/AU Wide)" is a huge value-add for the local community that larger platforms often miss.
- generate sitemap.xml for SEO

Meh?:
- Filters for people lead roles like managers?
- Add a locations with most jobs, and locations for a given tech. Does this make sense? we have location filters for tech and companies. Maybe total jobs per city, but it's not very useful unless you're a migrant? Even then you can just filter jobs for a technology and location?

Funding ideas:
- Make the related companies and related roles show sponsored companies and roles first, based on the browsing history of the user (look at the technologies they've visited). Perhaps these 'related' sections need to be separated into their own APIs.
- Show sponsor companies at the top of the website???
- Sponsored companies get badges and are listed at the top of lists?
- 

IMPORTANT:
- Clean all data of personal information, including descriptions. 
- Add a transparency page explaining where the data comes from, how it's processed, and how it's used. Also include a link to the privacy policy. Also include any costs and revenues and how excess money will be used.
- Add an about, terms & condition, robots.txt, and contact page.
- If you prefer to keep it under your name for your portfolio (especially important for your upcoming move to Spain!), you can frame it as a "Public Interest Tech" project.

📋 Project Launch Checklist: AU/NZ Focus
1. Data Sanitization & Privacy
[x] Scrub Personal Identifiers: Ensure your scraping pipeline removes jobPosterName, recruiter emails, and phone numbers from the descriptionText before saving to BigQuery.
[x] Fact-Only Storage: Verify that the database only contains "Business Facts" (Job Title, Stack, Salary, Location).
[x] Regulatory Alignment: Ensure data handling aligns with the NZ Privacy Act 2020 and the Australian Privacy Act 1988.

2. Transparency & Mission Page (/about or /transparency)
[x] Source Disclosure: Clearly state: "Data aggregated from public job listings (e.g., LinkedIn, Seek)."
[x] Processing Ethics: Explain that personal recruiter/poster data is discarded immediately to protect individual privacy.
[x] Live Financial Ledger: Add a simple table showing Monthly Hosting Costs vs. Revenue.
[x] Community Reinvestment Plan: State clearly: "100% of future surplus will fund local GDG scholarships, community events, and LeetCode subscriptions for AU/NZ devs."

3. The "Legal Four" Essential Files
[x] Privacy Policy: Tailored to the NZ Privacy Act and Australian Privacy Principles (APP). Include a "Right to Removal" contact.
[x] Terms & Conditions: Include a standard "As-Is" clause (you are a search utility, not a recruitment agency).
[x] Robots.txt: A standard file to prevent other bots from scraping your aggregated data.
[x] Contact Page: Provide a simple email or form to ensure you are reachable by the community.

4. Branding & Portfolio Positioning
[x] Public Interest Tech Framing: Frame the project as a personal contribution to the ANZ tech ecosystem.
[x] Updated Footer:
"Built with ❤️ by Nic Tolentino. This is an open-source, non-profit project dedicated to helping the NZ and AU engineering communities. All proceeds are reinvested into local developer scholarships and community events."
