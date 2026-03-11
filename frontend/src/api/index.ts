export interface Category {
  slug: string;
  displayName: string;
  description: string;
}

export interface Technology {
  name: string;
  jobCount: number;
  companyCount: number;
  avgSalary?: number;
}

export interface Company {
  id: string;
  name: string;
  jobCount: number;
  technologies: string[];
}

export interface JobRole {
  id: string;
  title: string;
  companyId: string;
  companyName: string;
  location: string;
  country: string;
  salaryMin: number;
  salaryMax: number;
  postedDate: string;
  url: string;
}

export interface MonthlyTrend {
  month: string;
  jobCount: number;
  companyCount: number;
}

export interface CategoryTrends {
  totalJobs: number;
  totalCompanies: number;
  growthRate: number;
  marketShare: number;
  last6MonthsJobs: number;
  monthlyData: MonthlyTrend[];
}

export interface DomainHub {
  category: Category;
  totalJobs: number;
  activeCompanies: number;
  technologies: Technology[];
  topCompanies: Company[];
  recentJobs: JobRole[];
  trends: CategoryTrends;
  marketShare: number;
  growthRate: number;
}

export interface DomainSummary {
  category: Category;
  jobCount: number;
  companyCount: number;
  techCount: number;
  growthRate: number;
  marketShare: number;
}

const API_BASE = (import.meta as any).env?.VITE_API_URL || '/api';

export const api = {
  /**
   * Fetches complete domain hub data for a category.
   */
  getDomainHub: async (category: string, country?: string): Promise<DomainHub> => {
    const url = new URL(`${API_BASE}/hubs/${category}`, window.location.origin);
    if (country) url.searchParams.append('country', country);
    
    const response = await fetch(url.toString());
    if (!response.ok) throw new Error(`Failed to fetch domain hub: ${response.statusText}`);
    return await response.json();
  },

  /**
   * Fetches summary data for all domain hubs.
   */
  getAllDomainHubs: async (country?: string): Promise<DomainSummary[]> => {
    const url = new URL(`${API_BASE}/hubs`, window.location.origin);
    if (country) url.searchParams.append('country', country);
    
    const response = await fetch(url.toString());
    if (!response.ok) throw new Error(`Failed to fetch domain hubs: ${response.statusText}`);
    return await response.json();
  }
};
