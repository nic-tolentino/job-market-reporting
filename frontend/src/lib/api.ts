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
        'aws': 'AWS',
        'google cloud': 'GCP',
        'gcp': 'GCP',
        'azure': 'Azure',
        'dotnet': '.NET',
        '.net': '.NET',
        'react': 'React',
        'nodejs': 'Node.js',
        'node.js': 'Node.js',
        'typescript': 'TypeScript',
        'javascript': 'JavaScript',
        'kotlin': 'Kotlin',
        'ios': 'iOS',
        'android': 'Android',
        'sql server': 'SQL Server',
        'mysql': 'MySQL',
        'postgresql': 'PostgreSQL',
        'mongodb': 'MongoDB'
    };

    const lower = techId.toLowerCase();
    return techMap[lower] || techId.charAt(0).toUpperCase() + techId.slice(1);
};
