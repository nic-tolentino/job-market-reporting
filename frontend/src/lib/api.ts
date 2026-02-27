import { mockCompanyLeaderboard, mockGlobalStats, mockRecentJobs, mockTechLeaderboard } from './mockData';

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
    seniorityDistribution: SeniorityDistributionDto[];
    hiringCompanies: CompanyLeaderboardDto[];
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

// Easy way to toggle mock data on the frontend: 
// 1. Set VITE_FORCE_MOCK_DATA=true in .env
// 2. Or set localStorage.setItem('USE_MOCK_DATA', 'true') in the browser console
const FORCE_MOCK_DATA = import.meta.env.VITE_FORCE_MOCK_DATA === 'true' || localStorage.getItem('USE_MOCK_DATA') === 'true';

export const fetchLandingPageData = async (): Promise<LandingPageDto> => {
    if (!FORCE_MOCK_DATA) {
        try {
            const response = await fetch(`${API_BASE_URL}/landing`);
            if (response.ok) return await response.json();
        } catch (error) {
            console.warn('Backend API failed, falling back to local mock data:', error);
        }
    }

    // Simulate network delay for UI testing
    await new Promise(resolve => setTimeout(resolve, 1000));
    return {
        globalStats: mockGlobalStats,
        topTech: mockTechLeaderboard,
        topCompanies: mockCompanyLeaderboard
    };
};

export const fetchTechDetails = async (techName: string): Promise<TechDetailsPageDto> => {
    if (!FORCE_MOCK_DATA) {
        try {
            const response = await fetch(`${API_BASE_URL}/tech/${techName}`);
            if (response.ok) return await response.json();
        } catch (error) {
            console.warn(`Backend API failed for ${techName}, falling back to local mock data:`, error);
        }
    }

    // Simulate network delay for UI testing
    await new Promise(resolve => setTimeout(resolve, 1000));
    return {
        techName: formatTechName(techName),
        seniorityDistribution: [
            { name: 'Senior', value: 400 },
            { name: 'Mid-Level', value: 300 },
            { name: 'Junior', value: 100 },
            { name: 'Lead/Principal', value: 50 },
        ],
        hiringCompanies: mockCompanyLeaderboard.map(c => ({
            ...c,
            activeRoles: Math.floor(c.activeRoles * 0.3)
        }))
    };
};

export const fetchCompanyProfile = async (companyId: string): Promise<CompanyProfilePageDto> => {
    if (!FORCE_MOCK_DATA) {
        try {
            const response = await fetch(`${API_BASE_URL}/company/${companyId}`);
            if (response.ok) return await response.json();
        } catch (error) {
            console.warn(`Backend API failed for ${companyId}, falling back to local mock data:`, error);
        }
    }

    // Simulate network delay for UI testing
    await new Promise(resolve => setTimeout(resolve, 1000));
    const fallbackCompany = getCompanyById(companyId);
    return {
        companyDetails: {
            id: fallbackCompany.id,
            name: fallbackCompany.name,
            logo: fallbackCompany.logo,
            website: 'https://example.com',
            employeesCount: 1000,
            industry: 'Technology',
            description: 'Engineering-focused organization specializing in web-scale infrastructure and developer tools.'
        },
        techStack: ['React', 'TypeScript', 'Tailwind CSS', 'Next.js', 'Vue', 'Svelte', 'Framer Motion'],
        insights: {
            workModel: 'Hybrid Friendly',
            hiringLocations: ['Sydney, NSW, Australia', 'Remote'],
            commonBenefits: ['Health Insurance', 'Equity', 'Flexible Hours']
        },
        activeRoles: mockRecentJobs.map(job => ({
            ...job,
            locations: [(job as any).location ?? 'Unknown Location'],
            jobIds: [job.id],
            applyUrls: [null],
            companyId: fallbackCompany.id,
            companyName: fallbackCompany.name
        }))
    };
};

export const fetchSearchSuggestions = async (): Promise<SearchSuggestionsResponse> => {
    if (!FORCE_MOCK_DATA) {
        try {
            const response = await fetch(`${API_BASE_URL}/search/suggestions`);
            if (response.ok) return await response.json();
        } catch (error) {
            console.warn('Backend API failed for search suggestions:', error);
        }
    }
    return {
        suggestions: [
            ...mockTechLeaderboard.map(t => ({ type: 'TECHNOLOGY' as const, id: t.name.toLowerCase(), name: t.name })),
            ...mockCompanyLeaderboard.map(c => ({ type: 'COMPANY' as const, id: c.id, name: c.name }))
        ]
    };
};

export const trackSearchMiss = async (term: string): Promise<void> => {
    if (!FORCE_MOCK_DATA) {
        try {
            await fetch(`${API_BASE_URL}/search/suggestions?term=${encodeURIComponent(term)}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
            });
        } catch (error) {
            console.warn('Backend API failed for tracking search miss:', error);
        }
    } else {
        console.log(`[MOCK] Tracked search miss: ${term}`);
    }
};

export const submitFeedback = async (context: string | undefined, message: string): Promise<void> => {
    if (!FORCE_MOCK_DATA) {
        try {
            const response = await fetch(`${API_BASE_URL}/feedback`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ context, message })
            });
            if (!response.ok) throw new Error("Failed to submit");
        } catch (error) {
            console.warn('Backend API failed for submitting feedback:', error);
            throw error;
        }
    } else {
        console.log(`[MOCK] Submitted feedback: [${context}] ${message}`);
        await new Promise(resolve => setTimeout(resolve, 800));
    }
};

// --- Legacy Formatters (to be removed once fully transitioned) ---

export const getCompanyById = (id: string | undefined): CompanyLeaderboardDto => {
    if (!id) return { id: '', name: 'Unknown Company', logo: '?', activeRoles: 0 };

    // Fallback logic for when we haven't integrated the real API yet
    const fallbackId = id.toLowerCase();
    const mockCompany = [
        { id: 'atlassian', name: 'Atlassian', logo: 'A', activeRoles: 145 },
        { id: 'canva', name: 'Canva', logo: 'C', activeRoles: 89 },
        { id: 'google', name: 'Google', logo: 'G', activeRoles: 76 },
        { id: 'amazon', name: 'Amazon', logo: 'Am', activeRoles: 65 },
        { id: 'xero', name: 'Xero', logo: 'X', activeRoles: 54 }
    ].find(c => c.id === fallbackId);

    if (mockCompany) return mockCompany;

    return {
        id: id,
        name: id.split('-').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(' '),
        logo: id.charAt(0).toUpperCase(),
        activeRoles: 0
    };
};

export const formatTechName = (techId: string | undefined): string => {
    if (!techId) return 'Technology';
    return techId.charAt(0).toUpperCase() + techId.slice(1);
};
