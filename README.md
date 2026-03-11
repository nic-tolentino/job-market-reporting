# DevAssembly (AU/NZ/ES)

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

## 📂 Project Structure
```text
.
├── backend/               # Spring Boot 3.x (Kotlin) API & Business Logic [→ README](backend/README.md)
│   └── DEPLOY.md          # GCP Deployment Guide [→ DEPLOYMENT](backend/DEPLOY.md)
├── frontend/              # React (Vite/TypeScript) Dashboard [→ README](frontend/README.md)
├── scripts/               # Automation & Utility Scripts
│   └── deployment/        # Production Deployment Helpers
└── README.md              # Main project documentation
```

---

## 🚀 Component Details

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

---

## 🚀 Deployment & Operations

This project is optimized for serverless hosting on **Google Cloud Run** and **Vercel**.

### Backend (GCP)
For full deployment instructions, see the **[Backend Deployment Guide](backend/DEPLOY.md)**.
We provide several helper scripts to simplify production tasks:
- `scripts/deployment/setup-env.sh`: Initialize your local `.env` configuration.
- `scripts/deployment/deploy.sh`: Push updates to Cloud Run.
- `scripts/deployment/db-reprocess.sh`: Run historical data migrations.
- `scripts/deployment/run-tests.sh`: Run all backend unit tests.


### Frontend (Vercel)
The frontend is hosted on Vercel and connects to the GCP backend via the `VITE_API_URL` environment variable.

---

## 💰 Cost & Constraints
- **Budget**: Targeting $0/month (staying within Free Tiers).
- **Volume**: ~100 jobs per week across 3 countries.
- **Environment**: Development via Docker Desktop.

---

## 💡 Future ideas

Now:

- TOP PRIORITY! Last I saw in production, the job page wasn't loading jobs, and there were multiple duplicating job listings in the company and tech pages. Investigate this issue once we're done with the current feature development.

- Next I need to figure out a staged deployment process - I can't keep breaking and testing in production for frontend and backend services.

- We need to make the landing page better reflect the Discover. Grow. Connect. philosophy.

Later:

- I was thinking: does it make sense to save every ingestion into a separte cold storage file - afterall they shouldn't be accessed often at all, and it also allows us to more easily mamage the data, reduces costs, and we don't need to maintain that table. The cost is performance but does it matter in practice?

- Company level tech stack has a lot of potentail for improvement refer to company-tech-stack-fix.md

- Add a location filter to the job table on the company page???

- An admin panel would be very useful, I'm not sure exactly how it would be accessed though. Nor exactly what to add to it. I feel it could get bloated very quickly. It would be good to get suggestions on what would be good to add, and how to structure the functionality.

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

Costs:
- Apify: USD2.5 + USD29
- Domain: USD7
- Hosting: USD0
- Qwen Code Lite Subscription USD11.5
- Gemini Pro - Free (GDG Manager)
- Claude Code - AUD34
- Macbook Air M5 32GB - AUD2,399

Time:
Estimated time between 23 Feb and Friday 6th March: 40 hours
Friday - 5 hours
Sat - 3 hours
Sun - 5 hours
Mon 9 March - 10 hours
Tue - 13 hrs
Wed - 6 hrs
