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

- I need to make ingestion of datasets idempotent. 

Later:

- Locations in company, tech, and job pages appear with city shown twice. Eg Auckland, Auckland. Countries are only shown once. Eg "New Zealand". Please check first whether this is a backend data or frontend display issue.

- Add a location job filter to the company page???

- The ability to group technologies by category, e.g. cloud, server, database, web, mobile, backend, etc in the landing, and company pages

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

- other country selector option, to get feedback on demand for other countries
- label and show the source of the data
- does our pii filter out "If you would like to find out more about this amazing opportunity, please feel free to call our Head of Talent Acquisitions, Bob Bloblob, mobile 021 999 111, she loves to chat about TVNZ careers."
- allow users to contribute to the company data by creating a public repo to host that data. The backend will pull the data from the repo and upsert it into the database.
- rename AnalyticsBigQueryRepository and related classes from Analytics to Insights? To avoid confusion with actual analytics?

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


Meh?:
- Filters for people lead roles like managers?
- Add a locations with most jobs, and locations for a given tech. Does this make sense? we have location filters for tech and companies. Maybe total jobs per city, but it's not very useful unless you're a migrant? Even then you can just filter jobs for a technology and location?

Funding ideas:
- Make the related companies and related roles show sponsored companies and roles first, based on the browsing history of the user (look at the technologies they've visited). Perhaps these 'related' sections need to be separated into their own APIs.
- Show sponsor companies at the top of the website???
- Sponsored companies get badges and are listed at the top of lists?
- donations

Costs:
- Apify: USD2.5
- Domain: USD7
- Hosting: USD1

Time:
Estimated time between 23 Feb and Friday 6th March: 40 hours
Friday 6th March - 5 hours