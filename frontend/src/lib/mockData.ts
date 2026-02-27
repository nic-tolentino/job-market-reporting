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
    seniorityLevel: string;
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
    { name: 'Tailwind CSS', count: 950, percentageChange: 12.4 },
    { name: 'Next.js', count: 850, percentageChange: 15.2 },
    { name: 'Vue', count: 650, percentageChange: -2.1 },
    { name: 'Angular', count: 580, percentageChange: -4.5 },
    { name: 'Svelte', count: 420, percentageChange: 9.8 },
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
        title: 'Senior UI Developer',
        companyId: 'atlassian',
        companyName: 'Atlassian',
        location: 'Sydney, AU (Hybrid)',
        salaryMin: 160000,
        salaryMax: 210000,
        postedDate: '2024-05-15',
        seniorityLevel: 'Senior',
        technologies: ['React', 'TypeScript', 'Tailwind CSS'],
    },
    {
        id: '2',
        title: 'Frontend Engineer',
        companyId: 'canva',
        companyName: 'Canva',
        location: 'Remote, AU',
        salaryMin: 140000,
        salaryMax: 180000,
        postedDate: '2024-05-14',
        seniorityLevel: 'Mid-Level',
        technologies: ['React', 'Next.js', 'Framer Motion'],
    },
    {
        id: '3',
        title: 'Web Specialist',
        companyId: 'xero',
        companyName: 'Xero',
        location: 'Wellington, NZ (Hybrid)',
        salaryMin: 120000,
        salaryMax: 150000,
        postedDate: '2024-05-12',
        seniorityLevel: 'Senior',
        technologies: ['Vue', 'JavaScript', 'CSS'],
    },
];
