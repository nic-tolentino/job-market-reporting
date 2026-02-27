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

- Also show jobs in tech page???
- Show all jobs for a given tech, filtered by location and seniority

- [x] Split location into city and state/region and country

- [x] Can we get rid of rawSeniorityLevel and rawLocation? in the job record? 


- Technologies need to retain their capitalisation - e.g. AWS, .NET, Go, iOS, etc

- Add a soft skills (leadership, communication, etc) and capabilities (agile, devops, etc) leaderboard, also show it on the job page
- Add a locations with most jobs, and locations for a given tech
-  Extend appropriate unit tests, both backend and frontend.
- Add support for different countries
- Add the ability to see more than just the top 10 technologies and companies
- Filters for people lead roles like managers?
- The ability to group technologies by category, e.g. cloud, server, database, web, mobile, backend, etc in the landing, and company pages
- Ability to filter companies by seniority in tech page. 
- Test error scenarios
- Create a Tech record with the pre-computed data for each tech to reduce computation? or just rely on cache?
- Can we rid of some of the nullable fields on the company and jobs tables?
- ideally we'd log when a location can't be properly parsed
