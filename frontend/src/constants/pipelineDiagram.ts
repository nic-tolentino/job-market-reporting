export const pipelineDiagram = `flowchart TB
    subgraph DataSources["📥 Data Sources (External)"]
        direction TB
        LinkedIn["LinkedIn Job Postings"]
        ApifyScraper["Apify Scraper API"]
        ATS["Company ATS Systems<br/>(Greenhouse, Lever, etc.)"]
        ManualData["📄 companies.json<br/>Master Manifest"]
    end

    subgraph Ingestion["🔄 Ingestion Layer"]
        direction TB
        Webhook["ApifyWebhookController<br/>/api/webhook/apify/data-changed"]
        WebhookNZ["🇳🇿 /api/webhook/apify/nz/data-changed"]
        WebhookAU["🇦🇺 /api/webhook/apify/au/data-changed"]
        ATSController["AtsSyncController<br/>/api/internal/ats-sync"]
        AdminController["AdminController<br/>/api/admin/trigger-sync"]
    end

    subgraph Orchestration["⚙️ Orchestration Services"]
        direction TB
        JobDataSync["JobDataSyncService<br/>runDataSync"]
        AtsSync["AtsJobDataSyncService<br/>syncCompany"]
        CompanySync["CompanySyncService<br/>syncFromManifest"]
        Reprocess["JobDataSyncService<br/>reprocessHistoricalData"]
    end

    subgraph Bronze["🥉 Bronze Layer (Raw Storage)"]
        direction TB
        RawIngestion[("raw_ingestions<br/>Table")]
        RawIngestionDesc["Immutable JSON payloads<br/>with datasetId tracking"]
    end

    subgraph Silver["🥈 Silver Layer (Structured Data)"]
        direction TB
        RawJobs[("raw_jobs<br/>Table")]
        RawCompanies[("raw_companies<br/>Table")]
        JobsDesc["Deduplicated job roles<br/>with extracted technologies"]
        CompaniesDesc["Verified + Unverified<br/>company records"]
    end

    subgraph Processing["🔧 Data Processing"]
        direction TB
        ApifyClient["ApifyClient<br/>Fetch recent jobs"]
        RawJobMapper["RawJobDataMapper<br/>Map & Deduplicate"]
        RawJobParser["RawJobDataParser<br/>Extract: Tech, Location,<br/>Seniority, Work Model"]
        UnifiedMapper["UnifiedJobDataMapper<br/>ATS Data Mapping"]
        AtsNormalizer["AtsNormalizer<br/>ATS-specific parsing"]
        TechClassifier["TechRoleClassifier<br/>Filter tech roles only"]
        SilverMerger["SilverDataMerger<br/>Merge with existing data"]
        PiiSanitizer["PiiSanitizer<br/>Remove sensitive data"]
    end

    subgraph Persistence["💾 Persistence Layer"]
        direction TB
        JobRepo["JobRepository<br/>BigQuery Streaming"]
        CompanyRepo["CompanyRepository<br/>BigQuery CRUD"]
        IngestionRepo["IngestionRepository<br/>Bronze Layer Access"]
        AtsConfigRepo["AtsConfigRepository<br/>Sync Status Tracking"]
    end

    subgraph API["🌐 API Layer (BFF)"]
        direction TB
        LandingAPI["LandingController<br/>/api/landing"]
        TechAPI["TechController<br/>/api/tech/{techName}"]
        CompanyAPI["CompanyController<br/>/api/company/{companyId}"]
        JobAPI["JobController<br/>/api/job/{jobId}"]
        SearchAPI["SearchController<br/>/api/search/suggestions"]
        AnalyticsRepo["AnalyticsRepository<br/>Aggregation Queries"]
        TechRepo["TechRepository<br/>Tech-specific queries"]
    end

    subgraph Cache["💨 Application Cache"]
        direction TB
        CacheLanding["@Cacheable('landing')"]
        CacheTech["@Cacheable('tech')"]
        CacheCompany["@Cacheable('company')"]
        CacheSearch["@Cacheable('search')"]
    end

    subgraph Frontend["💻 Frontend (React/Vite)"]
        direction TB
        LandingPage["Landing Page<br/>Global Stats, Top Tech,<br/>Top Companies"]
        TechPage["Tech Details Page<br/>Role distribution,<br/>Hiring companies"]
        CompanyPage["Company Profile Page<br/>Tech stack, Insights,<br/>Active roles"]
        JobPage["Job Details Page<br/>Full description,<br/>Apply links, Similar roles"]
        SearchUI["Search UI<br/>Tech & Company suggestions"]
        CountrySelector["Country Selector<br/>(NZ/AU/ES)"]
    end

    subgraph Automation["⏰ Automation"]
        direction TB
        CloudScheduler["Google Cloud Scheduler<br/>Weekly Triggers"]
        ApifyWebhook["Apify Webhook<br/>On data change"]
    end

    %% Data Source Flows
    LinkedIn -->|Scraped via API| ApifyScraper
    ApifyScraper -->|JSON Payload| Webhook
    ApifyScraper -->|JSON Payload| WebhookNZ
    ApifyScraper -->|JSON Payload| WebhookAU
    
    ATS -->|Direct API| AtsSync
    ManualData -->|File Read| CompanySync

    %% Webhook to Orchestration
    Webhook -->|Trigger| JobDataSync
    WebhookNZ -->|Trigger + Country=NZ| JobDataSync
    WebhookAU -->|Trigger + Country=AU| JobDataSync
    ATSController -->|Trigger| AtsSync
    AdminController -->|Manual Trigger| JobDataSync
    AdminController -->|Manual Trigger| CompanySync

    %% Orchestration to Processing
    JobDataSync -->|1. Fetch| ApifyClient
    JobDataSync -->|2. Bronze Save| IngestionRepo
    JobDataSync -->|3. Company Refresh| CompanySync
    JobDataSync -->|4. Map Jobs| RawJobMapper
    JobDataSync -->|5. Merge| SilverMerger
    
    AtsSync -->|1. Fetch| AtsNormalizer
    AtsSync -->|2. Bronze Save| IngestionRepo
    AtsSync -->|3. Map| UnifiedMapper
    AtsSync -->|4. Merge| SilverMerger

    %% Processing Details
    ApifyClient -->|Raw DTOs| RawJobMapper
    RawJobMapper -->|Group by Role| RawJobParser
    RawJobMapper -->|Extract Tech| RawJobParser
    RawJobMapper -->|Parse Location| RawJobParser
    RawJobMapper -->|Sanitize| PiiSanitizer
    RawJobMapper -->|Filter| TechClassifier
    
    AtsNormalizer -->|Normalized Jobs| UnifiedMapper
    UnifiedMapper -->|Tech Filter| TechClassifier

    %% Bronze Layer
    IngestionRepo -->|Stream JSON| RawIngestion
    RawIngestion --- RawIngestionDesc

    %% Silver Layer
    SilverMerger -->|Upsert| RawJobs
    SilverMerger -->|Upsert| RawCompanies
    RawJobs --- JobsDesc
    RawCompanies --- CompaniesDesc

    %% Repository Connections
    RawJobs -.->|Read/Write| JobRepo
    RawCompanies -.->|Read/Write| CompanyRepo
    RawIngestion -.->|Read/Write| IngestionRepo

    %% Reprocessing Flow
    Reprocess -->|Read All| IngestionRepo
    Reprocess -->|Re-map| RawJobMapper
    Reprocess -->|Delete All| JobRepo
    Reprocess -->|Delete All| RawCompanies
    Reprocess -->|Re-insert| SilverMerger

    %% API to Repository
    LandingAPI -->|Aggregated Stats| AnalyticsRepo
    TechAPI -->|Tech Jobs| TechRepo
    CompanyAPI -->|Company Data| CompanyRepo
    JobAPI -->|Job Details| JobRepo
    SearchAPI -->|Suggestions| TechRepo
    SearchAPI -->|Suggestions| CompanyRepo

    AnalyticsRepo -.->|Query| RawJobs
    TechRepo -.->|Query| RawJobs
    CompanyRepo -.->|Query| RawCompanies
    JobRepo -.->|Query| RawJobs

    %% Caching
    LandingAPI -->|Cache Result| CacheLanding
    TechAPI -->|Cache Result| CacheTech
    CompanyAPI -->|Cache Result| CacheCompany
    SearchAPI -->|Cache Result| CacheSearch

    %% Frontend Connections
    LandingPage -->|GET /api/landing| LandingAPI
    TechPage -->|"GET /api/tech/{tech}"| TechAPI
    CompanyPage -->|"GET /api/company/{id}"| CompanyAPI
    JobPage -->|"GET /api/job/{id}"| JobAPI
    SearchUI -->|GET /api/search/suggestions| SearchAPI

    %% Cache to API
    CacheLanding -.->|Serve Cached| LandingAPI
    CacheTech -.->|Serve Cached| TechAPI
    CacheCompany -.->|Serve Cached| CompanyAPI
    CacheSearch -.->|Serve Cached| SearchAPI

    %% Country Context
    CountrySelector -.->|Filters All Queries| Frontend

    %% Automation Triggers
    CloudScheduler -->|Trigger| ApifyWebhook
    ApifyWebhook -->|POST webhook| Webhook

    %% Styling
    classDef dataSource fill:#e1f5fe,stroke:#01579b,stroke-width:2px,color:#01579b
    classDef ingestion fill:#fff3e0,stroke:#e65100,stroke-width:2px,color:#e65100
    classDef orchestration fill:#f3e5f5,stroke:#4a148c,stroke-width:2px,color:#4a148c
    classDef bronze fill:#ffebee,stroke:#b71c1c,stroke-width:2px,color:#b71c1c
    classDef silver fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px,color:#1b5e20
    classDef processing fill:#fff8e1,stroke:#ff6f00,stroke-width:2px,color:#ff6f00
    classDef persistence fill:#eceff1,stroke:#263238,stroke-width:2px,color:#263238
    classDef api fill:#e3f2fd,stroke:#0d47a1,stroke-width:2px,color:#0d47a1
    classDef cache fill:#f1f8e9,stroke:#33691e,stroke-width:2px,color:#33691e
    classDef frontend fill:#fce4ec,stroke:#880e4f,stroke-width:2px,color:#880e4f
    classDef automation fill:#e0f7fa,stroke:#006064,stroke-width:2px,color:#006064

    class LinkedIn,ApifyScraper,ATS,ManualData dataSource
    class Webhook,WebhookNZ,WebhookAU,ATSController,AdminController ingestion
    class JobDataSync,AtsSync,CompanySync,Reprocess orchestration
    class RawIngestion,RawIngestionDesc bronze
    class RawJobs,RawCompanies,JobsDesc,CompaniesDesc silver
    class ApifyClient,RawJobMapper,RawJobParser,UnifiedMapper,AtsNormalizer,TechClassifier,SilverMerger,PiiSanitizer processing
    class JobRepo,CompanyRepo,IngestionRepo,AtsConfigRepo persistence
    class LandingAPI,TechAPI,CompanyAPI,JobAPI,SearchAPI,AnalyticsRepo,TechRepo api
    class CacheLanding,CacheTech,CacheCompany,CacheSearch cache
    class LandingPage,TechPage,CompanyPage,JobPage,SearchUI,CountrySelector frontend
    class CloudScheduler,ApifyWebhook automation`;
