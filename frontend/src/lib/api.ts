// --- DTO Types matching Backend BFF ---

export interface GlobalStatsDto {
    totalVacancies: number;
    remotePercentage: number;
    hybridPercentage: number;
    topTech: string;
}

export interface TechTrendAggregatedDto {
    name: string;
    count: number;
    percentageChange: number;
}

export interface CompanyLeaderboardDto {
    id: string;
    name: string;
    logo: string;
    activeRoles: number;
}

export interface LandingPageDto {
    globalStats: GlobalStatsDto;
    topTech: TechTrendAggregatedDto[];
    topCompanies: CompanyLeaderboardDto[];
}

export interface SeniorityDistributionDto {
    name: string;
    value: number;
}

export interface TechDetailsPageDto {
    techName: string;
    totalJobs: number;
    seniorityDistribution: SeniorityDistributionDto[];
    hiringCompanies: CompanyLeaderboardDto[];
    roles: JobRoleDto[];
}

export interface JobRoleDto {
    id: string;
    title: string;
    companyId: string;
    companyName: string;
    locations: string[];
    jobIds: string[];
    applyUrls: (string | null)[];
    salaryMin: number | null;
    salaryMax: number | null;
    postedDate: string;
    seniorityLevel: string;
    technologies: string[];
}

export interface CompanyDetailsDto {
    id: string;
    name: string;
    logo: string;
    website: string;
    employeesCount: number;
    industry: string;
    description: string;
    hqCountry: string | null;
    verificationLevel: string;
}

export interface CompanyInsightsDto {
    workModel: string;
    hiringLocations: string[];
    commonBenefits: string[];
}

export interface CompanyProfilePageDto {
    companyDetails: CompanyDetailsDto;
    techStack: string[];
    insights: CompanyInsightsDto;
    activeRoles: JobRoleDto[];
}

export interface JobDetailsDto {
    title: string;
    description: string | null;
    seniorityLevel: string;
    employmentType: string | null;
    workModel: string | null;
    postedDate: string | null;
    jobFunction: string | null;
    technologies: string[];
    benefits: string[] | null;
}

export interface JobLocationDto {
    location: string;
    applyUrl: string | null;
    jobId: string;
}

export interface JobCompanyDto {
    companyId: string;
    name: string;
    logoUrl: string;
    description: string;
    website: string;
    hiringLocations: string[];
}

export interface JobPageDto {
    details: JobDetailsDto;
    locations: JobLocationDto[];
    company: JobCompanyDto;
    similarRoles: JobRoleDto[];
}

export interface SearchSuggestionDto {
    type: 'TECHNOLOGY' | 'COMPANY';
    id: string;
    name: string;
}

export interface SearchSuggestionsResponse {
    suggestions: SearchSuggestionDto[];
}

// --- API Client Fetchers ---

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

export const fetchLandingPageData = async (): Promise<LandingPageDto> => {
    const response = await fetch(`${API_BASE_URL}/landing`);
    if (!response.ok) throw new Error(`Failed to fetch landing page: ${response.statusText}`);
    return await response.json();
};

export const fetchTechDetails = async (techName: string): Promise<TechDetailsPageDto> => {
    const response = await fetch(`${API_BASE_URL}/tech/${techName}`);
    if (!response.ok) throw new Error(`Failed to fetch tech details: ${response.statusText}`);
    return await response.json();
};

export const fetchCompanyProfile = async (companyId: string): Promise<CompanyProfilePageDto> => {
    const response = await fetch(`${API_BASE_URL}/company/${companyId}`);
    if (!response.ok) throw new Error(`Failed to fetch company profile: ${response.statusText}`);
    return await response.json();
};

export const fetchJobDetails = async (jobId: string): Promise<JobPageDto | null> => {
    const response = await fetch(`${API_BASE_URL}/job/${jobId}`);
    if (response.status === 404) return null;
    if (!response.ok) throw new Error(`Failed to fetch job details: ${response.statusText}`);
    return await response.json();
};

export const fetchSearchSuggestions = async (): Promise<SearchSuggestionsResponse> => {
    const response = await fetch(`${API_BASE_URL}/search/suggestions`);
    if (!response.ok) throw new Error(`Failed to fetch search suggestions: ${response.statusText}`);
    return await response.json();
};

export const trackSearchMiss = async (term: string): Promise<void> => {
    try {
        await fetch(`${API_BASE_URL}/search/suggestions?term=${encodeURIComponent(term)}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
        });
    } catch (error) {
        console.warn('Failed to track search miss:', error);
    }
};

export const submitFeedback = async (context: string | undefined, message: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ context, message })
    });
    if (!response.ok) throw new Error("Failed to submit feedback");
};

// --- Formatters ---

export const formatTechName = (techId: string | undefined): string => {
    if (!techId) return 'Technology';

    const techMap: Record<string, string> = {
        'kotlin': 'Kotlin',
        'java': 'Java',
        'python': 'Python',
        'go': 'Go',
        'golang': 'Go',
        'rust': 'Rust',
        'c++': 'C++',
        'c#': 'C#',
        'javascript': 'JavaScript',
        'typescript': 'TypeScript',
        'ruby': 'Ruby',
        'php': 'PHP',
        'swift': 'Swift',
        'objective-c': 'Objective-C',
        'scala': 'Scala',
        'dart': 'Dart',
        'elixir': 'Elixir',
        'clojure': 'Clojure',
        'haskell': 'Haskell',
        'lua': 'Lua',
        'perl': 'Perl',
        'r': 'R',
        'shell': 'Shell',
        'bash': 'Bash',
        'react': 'React',
        'angular': 'Angular',
        'vue': 'Vue.js',
        'nextjs': 'Next.js',
        'next.js': 'Next.js',
        'svelte': 'Svelte',
        'ember': 'Ember.js',
        'backbone': 'Backbone.js',
        'html': 'HTML',
        'css': 'CSS',
        'sass': 'Sass',
        'less': 'Less',
        'tailwind': 'Tailwind CSS',
        'bootstrap': 'Bootstrap',
        'material-ui': 'Material UI',
        'redux': 'Redux',
        'graphql': 'GraphQL',
        'spring': 'Spring',
        'spring boot': 'Spring Boot',
        'django': 'Django',
        'flask': 'Flask',
        'fastapi': 'FastAPI',
        'node': 'Node.js',
        'nodejs': 'Node.js',
        'node.js': 'Node.js',
        'express': 'Express',
        'nest': 'NestJS',
        'nestjs': 'NestJS',
        'ruby on rails': 'Ruby on Rails',
        'laravel': 'Laravel',
        'asp.net': 'ASP.NET',
        'dotnet': '.NET',
        '.net': '.NET',
        'android': 'Android',
        'ios': 'iOS',
        'flutter': 'Flutter',
        'react native': 'React Native',
        'xamarin': 'Xamarin',
        'ionic': 'Ionic',
        'kotlin multiplatform': 'Kotlin Multiplatform',
        'aws': 'AWS',
        'google cloud': 'GCP',
        'gcp': 'GCP',
        'azure': 'Azure',
        'docker': 'Docker',
        'kubernetes': 'Kubernetes',
        'k8s': 'Kubernetes',
        'terraform': 'Terraform',
        'ansible': 'Ansible',
        'chef': 'Chef',
        'puppet': 'Puppet',
        'jenkins': 'Jenkins',
        'github actions': 'GitHub Actions',
        'gitlab ci': 'GitLab CI',
        'circleci': 'CircleCI',
        'travis ci': 'Travis CI',
        'linux': 'Linux',
        'ubuntu': 'Ubuntu',
        'serverless': 'Serverless',
        'lambda': 'AWS Lambda',
        'cloudformation': 'CloudFormation',
        'sql': 'SQL',
        'postgresql': 'PostgreSQL',
        'postgres': 'PostgreSQL',
        'mysql': 'MySQL',
        'mongodb': 'MongoDB',
        'mongo': 'MongoDB',
        'redis': 'Redis',
        'elasticsearch': 'Elasticsearch',
        'cassandra': 'Cassandra',
        'dynamodb': 'DynamoDB',
        'mariadb': 'MariaDB',
        'oracle': 'Oracle',
        'sql server': 'SQL Server',
        'sqlite': 'SQLite',
        'couchbase': 'Couchbase',
        'neo4j': 'Neo4j',
        'bigquery': 'BigQuery',
        'snowflake': 'Snowflake',
        'redshift': 'Redshift',
        'hadoop': 'Hadoop',
        'spark': 'Apache Spark',
        'kafka': 'Apache Kafka',
        'rabbitmq': 'RabbitMQ',
        'activemq': 'ActiveMQ',
        'airflow': 'Apache Airflow',
        'dbt': 'dbt',
        'databricks': 'Databricks',
        'pandas': 'Pandas',
        'numpy': 'NumPy',
        'scikit-learn': 'scikit-learn',
        'tensorflow': 'TensorFlow',
        'pytorch': 'PyTorch'
    };

    const lower = techId.toLowerCase();
    return techMap[lower] || techId.charAt(0).toUpperCase() + techId.slice(1);
};
