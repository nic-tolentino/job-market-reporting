import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import PageLoader from '../components/common/PageLoader';
import { fetchTechDetails, type TechDetailsPageDto } from '../lib/api';
import { useAppStore } from '../store/useAppStore';
import { useCountryUrlSync } from '../hooks/useCountryUrlSync';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import { H1 } from '../components/ui/Typography';
import { Badge } from '../components/ui/Badge';
import { MarketTab } from '../components/tech/MarketTab';
import { LearnTab } from '../components/tech/LearnTab';
import { CommunityTab } from '../components/tech/CommunityTab';

import { TechIcon } from '../components/common/TechIcon';

const COMPANIES_PAGE_SIZE = 5;
const JOBS_PAGE_SIZE = 10;

type Tab = 'Market' | 'Learn' | 'Community';

export default function TechDetailsPage() {
    const { techId } = useParams<{ techId: string }>();
    const { selectedCountry } = useAppStore();
    const [searchParams, setSearchParams] = useSearchParams();
    
    // Global country sync
    useCountryUrlSync();

    const [data, setData] = useState<TechDetailsPageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);

    // Sync active tab with URL
    const activeTab = (searchParams.get('tab') as Tab) || 'Market';
    const setActiveTab = (tab: Tab) => {
        const newParams = new URLSearchParams(searchParams);
        newParams.set('tab', tab);
        setSearchParams(newParams);
    };

    // Mirror default tab to URL if not present
    useEffect(() => {
        if (!searchParams.get('tab')) {
            const newParams = new URLSearchParams(searchParams);
            newParams.set('tab', 'Market');
            setSearchParams(newParams, { replace: true });
        }
    }, [searchParams, setSearchParams]);

    // Filter states
    const [selectedSeniority, setSelectedSeniority] = useState<string>('All');
    const [selectedLocation, setSelectedLocation] = useState<string>('All');

    // Pagination states
    const [companiesPage, setCompaniesPage] = useState(1);
    const [jobsPage, setJobsPage] = useState(1);

    const loadData = useCallback(async () => {
        if (!techId) return;
        setIsLoading(true);
        setError(false);
        setError(false);
        setSelectedSeniority('All');
        setSelectedLocation('All');
        setCompaniesPage(1);
        setJobsPage(1);
        try {
            const apiData = await fetchTechDetails(techId, selectedCountry);
            setData(apiData);
        } catch (err) {
            console.error(`Failed to load tech details for ${techId}:`, err);
            setError(true);
        } finally {
            setIsLoading(false);
        }
    }, [techId, selectedCountry]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    // Derived filter options
    const seniorityOptions = useMemo(() => {
        if (!data) return [];
        // Use the seniorityDistribution from the chart for display names
        return data.seniorityDistribution.map(d => d.name).sort();
    }, [data]);

    const locationOptions = useMemo(() => {
        if (!data) return [];
        const locations = new Set<string>();
        data.roles.forEach(role => {
            role.locations.forEach(loc => {
                const parts = loc.split(',').map(p => p.trim());
                parts.forEach(part => {
                    if (part) locations.add(part);
                });
            });
        });
        return Array.from(locations).sort();
    }, [data]);

    // Filtered roles
    const filteredRoles = useMemo(() => {
        if (!data) return [];
        return data.roles.filter(role => {
            const matchesSeniority = selectedSeniority === 'All' ||
                role.seniorityLevel === selectedSeniority;

            const matchesLocation = selectedLocation === 'All' ||
                role.locations.some(loc => loc.toLowerCase().includes(selectedLocation.toLowerCase()));

            return matchesSeniority && matchesLocation;
        });
    }, [data, selectedSeniority, selectedLocation]);

    // Filtered hiring companies
    const filteredHiringCompanies = useMemo(() => {
        if (!data) return [];

        // If no filter, use the top companies provided by the backend
        if (selectedSeniority === 'All' && selectedLocation === 'All') {
            return data.hiringCompanies;
        }

        // Otherwise, group filtered roles by company
        const companyMap = new Map<string, { id: string, name: string, logo: string, activeRoles: number }>();

        filteredRoles.forEach(role => {
            const existing = companyMap.get(role.companyId);
            if (existing) {
                existing.activeRoles++;
            } else {
                // Find logo from hiringCompanies list if possible, or fallback
                const originalCompany = data.hiringCompanies.find(c => c.id === role.companyId);
                companyMap.set(role.companyId, {
                    id: role.companyId,
                    name: role.companyName,
                    logo: originalCompany?.logo || '',
                    activeRoles: 1
                });
            }
        });

        return Array.from(companyMap.values())
            .sort((a, b) => b.activeRoles - a.activeRoles);
    }, [data, filteredRoles, selectedSeniority, selectedLocation]);

    // Paginated roles
    const paginatedRoles = useMemo(() => {
        const start = (jobsPage - 1) * JOBS_PAGE_SIZE;
        return filteredRoles.slice(start, start + JOBS_PAGE_SIZE);
    }, [filteredRoles, jobsPage]);

    // Paginated companies
    const paginatedCompanies = useMemo(() => {
        const start = (companiesPage - 1) * COMPANIES_PAGE_SIZE;
        return filteredHiringCompanies.slice(start, start + COMPANIES_PAGE_SIZE);
    }, [filteredHiringCompanies, companiesPage]);

    const totalJobsPages = Math.ceil(filteredRoles.length / JOBS_PAGE_SIZE);
    const totalCompaniesPages = Math.ceil(filteredHiringCompanies.length / COMPANIES_PAGE_SIZE);

    if (isLoading) {
        return <PageLoader />;
    }

    if (error || !data) {
        return <ErrorState title={`Couldn't load ${techId || 'technology'} data`} message="We had trouble fetching details for this technology. Try again shortly." onRetry={loadData} />;
    }


    return (
        <div className="space-y-8">
            <div className="flex flex-col md:flex-row md:items-start gap-6">
                <div className="h-24 w-24 rounded-2xl bg-card border border-border shadow-theme-sm flex items-center justify-center flex-shrink-0 text-3xl font-bold text-secondary overflow-hidden">
                    <TechIcon techId={techId || ''} className="w-12 h-12" />
                </div>
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <H1>{data.techName}</H1>
                        <FeedbackButton variant="icon" context={`${data.techName} Page Overview`} />
                    </div>
                    <p className="text-secondary mt-2 text-lg leading-relaxed max-w-2xl">
                        Comprehensive market insights, curated learning paths, and local community connections to help you succeed as a professional in the {data.techName} ecosystem.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <Badge variant="blue">
                            High Demand
                        </Badge>
                        <Badge variant="blue">
                            {(data.totalJobs || 0).toLocaleString()} Active Roles
                        </Badge>
                        <Badge variant="slate">
                            Engineering
                        </Badge>
                    </div>
                </div>
            </div>

            {/* Tab Navigation */}
            <div className="flex border-b border-border relative overflow-x-auto no-scrollbar">
                {(['Market', 'Learn', 'Community'] as Tab[]).map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`px-8 py-4 font-semibold text-sm transition-all relative z-10 whitespace-nowrap ${activeTab === tab
                            ? 'text-accent'
                            : 'text-muted hover:text-secondary'
                            }`}
                    >
                        {tab}
                        {activeTab === tab && (
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-accent rounded-t-full shadow-[0_0_8px_rgba(37,99,235,0.4)]" />
                        )}
                    </button>
                ))}
            </div>

            {/* Tab Content */}
            <div className="min-h-[400px]">
                {activeTab === 'Market' && (
                    <MarketTab
                        data={data}
                        selectedSeniority={selectedSeniority}
                        setSelectedSeniority={setSelectedSeniority}
                        selectedLocation={selectedLocation}
                        setSelectedLocation={setSelectedLocation}
                        companiesPage={companiesPage}
                        setCompaniesPage={setCompaniesPage}
                        jobsPage={jobsPage}
                        setJobsPage={setJobsPage}
                        seniorityOptions={seniorityOptions}
                        locationOptions={locationOptions}
                        paginatedRoles={paginatedRoles}
                        paginatedCompanies={paginatedCompanies}
                        totalJobsPages={totalJobsPages}
                        totalCompaniesPages={totalCompaniesPages}
                        filteredHiringCompanies={filteredHiringCompanies}
                    />
                )}
                {activeTab === 'Learn' && (
                    <LearnTab techId={techId || ''} techName={data.techName} />
                )}
                {activeTab === 'Community' && (
                    <CommunityTab techId={techId || ''} techName={data.techName} />
                )}
            </div>
        </div>
    );
}
