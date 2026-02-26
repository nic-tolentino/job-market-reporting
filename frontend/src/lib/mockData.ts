export interface TechTrend {
    name: string;
    count: number;
    percentageChange: number; // positive or negative
}

export interface CompanyLeaderboard {
    id: string;
    name: string;
    logo: string;
    activeRoles: number;
}

export interface JobRole {
    id: string;
    title: string;
    companyId: string;
    companyName: string;
    location: string;
    salaryMin: number | null;
    salaryMax: number | null;
    postedDate: string;
    technologies: string[];
}

export const mockGlobalStats = {
    totalVacancies: 4281,
    remotePercentage: 34,
    hybridPercentage: 45,
    inPersonPercentage: 21,
    topTech: 'React',
};

export const mockTechLeaderboard: TechTrend[] = [
    { name: 'React', count: 1205, percentageChange: 5.2 },
    { name: 'TypeScript', count: 1150, percentageChange: 8.1 },
    { name: 'Node.js', count: 850, percentageChange: -2.4 },
    { name: 'Python', count: 820, percentageChange: 1.2 },
    { name: 'Kotlin', count: 450, percentageChange: 12.5 },
    { name: 'Java', count: 410, percentageChange: -5.0 },
    { name: 'Go', count: 380, percentageChange: 4.8 },
];

export const mockCompanyLeaderboard: CompanyLeaderboard[] = [
    { id: 'atlassian', name: 'Atlassian', logo: 'A', activeRoles: 145 },
    { id: 'canva', name: 'Canva', logo: 'C', activeRoles: 89 },
    { id: 'google', name: 'Google', logo: 'G', activeRoles: 76 },
    { id: 'amazon', name: 'Amazon', logo: 'Am', activeRoles: 65 },
    { id: 'xero', name: 'Xero', logo: 'X', activeRoles: 54 },
];

export const mockRecentJobs: JobRole[] = [
    {
        id: '1',
        title: 'Senior Frontend Engineer',
        companyId: 'atlassian',
        companyName: 'Atlassian',
        location: 'Sydney, AU (Hybrid)',
        salaryMin: 160000,
        salaryMax: 210000,
        postedDate: '2024-05-15',
        technologies: ['React', 'TypeScript', 'GraphQL'],
    },
    {
        id: '2',
        title: 'Backend Developer',
        companyId: 'canva',
        companyName: 'Canva',
        location: 'Remote, AU',
        salaryMin: 140000,
        salaryMax: 180000,
        postedDate: '2024-05-14',
        technologies: ['Java', 'Kotlin', 'Spring Boot'],
    },
    {
        id: '3',
        title: 'Full Stack Engineer',
        companyId: 'xero',
        companyName: 'Xero',
        location: 'Wellington, NZ (Hybrid)',
        salaryMin: 120000,
        salaryMax: 150000,
        postedDate: '2024-05-12',
        technologies: ['C#', '.NET', 'React'],
    },
];
