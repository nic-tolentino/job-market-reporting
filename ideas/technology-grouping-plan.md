# Refined Technology Grouping & Insights Plan

This document outlines a high-granularity strategy for categorizing the 100+ technologies tracked by the platform. This enables multi-dimensional market analysis and "sector-level" reporting.

## 1. Updated Category Hierarchy
To achieve "refined insights," we move beyond flat categories into a tiered structure.

| Top-Level Category | Sub-Categories (Internal) | Core Purpose |
| :--- | :--- | :--- |
| **Languages** | Systems, Web, Scripting, Functional | Foundation of all tech roles. |
| **Frontend** | Frameworks, Styling, State Management | UI and Client-side experience. |
| **Backend** | Frameworks, Runtimes, Tooling | Server-side logic and APIs. |
| **Mobile** | Native, Cross-Platform | iOS and Android ecosystems. |
| **Cloud & Infra** | Cloud Providers, IaC, Serverless | Modern infrastructure hosting. |
| **Data & AI** | Databases, Engineering, ML/Science | Data lifecycle and intelligence. |
| **Platform & DevOps** | Containerization, CI/CD, OS | Delivery and reliability. |
| **Security** | AppSec, InfraSec, Identity | Protecting the stack. |

---

## 2. Comprehensive Mapping (Initial)
Below is the proposed mapping for all current technologies in `TechFormatter.kt`:

| Category | Technologies |
| :--- | :--- |
| **Languages** | Kotlin, Java, Python, Go, Rust, C++, C#, JavaScript, TypeScript, Ruby, PHP, Swift, Objective-C, Scala, Dart, Elixir, Clojure, Haskell, Lua, Perl, R, Shell, Bash |
| **Frontend** | React, Angular, Vue.js, Next.js, Svelte, Ember.js, Backbone.js, HTML, CSS, Sass, Less, Tailwind CSS, Bootstrap, Material UI, Redux |
| **Backend** | Spring, Spring Boot, Django, Flask, FastAPI, Node.js, Express, NestJS, Ruby on Rails, Laravel, ASP.NET, .NET, GraphQL |
| **Mobile** | Android, iOS, Flutter, React Native, Xamarin, Ionic, Kotlin Multiplatform |
| **Cloud & Infra**| AWS, GCP, Azure, Terraform, Ansible, Chef, Puppet, Serverless, AWS Lambda, CloudFormation |
| **Data & AI** | SQL, PostgreSQL, MySQL, MongoDB, Redis, Elasticsearch, Cassandra, DynamoDB, MariaDB, Oracle, SQL Server, SQLite, Couchbase, Neo4j, BigQuery, Snowflake, Redshift, Hadoop, Spark, Kafka, RabbitMQ, ActiveMQ, Airflow, dbt, Databricks, Pandas, NumPy, scikit-learn, TensorFlow, PyTorch |
| **Platform/DevOps**| Docker, Kubernetes, Jenkins, GitHub Actions, GitLab CI, CircleCI, Travis CI, Linux, Ubuntu |

---

## 3. Targeted "Refined Insights"
With these groups, we can generate the following high-value reports:

### A. Sector Growth Analysis
Compare the relative growth of entire sectors. 
- *Insight:* "The **Data & AI** sector has grown 40% faster than **Mobile** in the last 12 months."

### B. Tech Stack Affinity (Co-occurrence)
Analyze which technologies are most frequently paired within a category.
- *Insight:* "90% of **Frontend** roles (React) also require at least one **Backend** framework (Node.js/Spring)."

### C. Salary Benchmarking by Sector
Aggregate salary data by top-level category.
- *Insight:* "Roles categorized under **Cloud & Infra** carry a 15% salary premium over general **Backend** roles."

---

## 4. Implementation Roadmap

### Phase 1: Backend Type-Safety
1. **Define `TechCategory` Enum**: Add to a new `com.techmarket.model` package.
2. **Refactor `TechFormatter.kt`**: Change the map from `String -> String` to `String -> TechMetadata`.
   ```kotlin
   data class TechMetadata(val displayName: String, val category: TechCategory)
   ```

### Phase 2: Analytics Data Enrichment
1. **BigQuery View**: Create a view `v_jobs_with_categories` that joins extracted technologies with their categories.
2. **Repository Update**: Update `AnalyticsBigQueryRepository` to support `groupByCategory`.

### Phase 3: Frontend "Discovery" UI
1. **Sector-Level Dashboards**: A high-level view showing the "Health" of each category.
2. **Category Filters**: Allow users to filter jobs/tech pages by "Backend", "Cloud", etc.
3. **Tech Cards**: Add category badges to tech icon components.

## 5. Next Steps
1. [ ] Finalize category assignment for Edge Cases (e.g., Is Python "Language" or "Data & AI"? *Decision: It is a Language, but its usage in Data roles can be tracked separately.*)
2. [ ] Backend: Implement `TechCategory` and update `TechFormatter`.
3. [ ] Analytics: Update BigQuery schemas.
