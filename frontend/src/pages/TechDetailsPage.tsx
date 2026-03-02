import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import PageLoader from '../components/common/PageLoader';
import { fetchTechDetails, type TechDetailsPageDto } from '../lib/api';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import { H1 } from '../components/ui/Typography';
import { Badge } from '../components/ui/Badge';
import { MarketTab } from '../components/tech/MarketTab';
import { LearnTab } from '../components/tech/LearnTab';
import { CommunityTab } from '../components/tech/CommunityTab';

const COMPANIES_PAGE_SIZE = 5;
const JOBS_PAGE_SIZE = 10;

type Tab = 'Market' | 'Learn' | 'Community';

export default function TechDetailsPage() {
    const { techId } = useParams<{ techId: string }>();

    const [data, setData] = useState<TechDetailsPageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);
    const [activeTab, setActiveTab] = useState<Tab>('Market');

    // Filter states
    const [selectedSeniority, setSelectedSeniority] = useState<string>('All');
    const [selectedCity, setSelectedCity] = useState<string>('All');

    // Pagination states
    const [companiesPage, setCompaniesPage] = useState(1);
    const [jobsPage, setJobsPage] = useState(1);

    const loadData = useCallback(async () => {
        if (!techId) return;
        setIsLoading(true);
        setError(false);
        setSelectedSeniority('All');
        setSelectedCity('All');
        setCompaniesPage(1);
        setJobsPage(1);
        setActiveTab('Market');
        try {
            const apiData = await fetchTechDetails(techId);
            setData(apiData);
        } catch (err) {
            console.error(`Failed to load tech details for ${techId}:`, err);
            setError(true);
        } finally {
            setIsLoading(false);
        }
    }, [techId]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    // Derived filter options
    const seniorityOptions = useMemo(() => {
        if (!data) return [];
        // Use the seniorityDistribution from the chart for display names
        return data.seniorityDistribution.map(d => d.name).sort();
    }, [data]);

    const cityOptions = useMemo(() => {
        if (!data) return [];
        const cities = data.roles.map(r => r.locations[0]?.split(',')[0]?.trim()).filter(Boolean);
        return [...new Set(cities)].sort() as string[];
    }, [data]);

    // Filtered roles
    const filteredRoles = useMemo(() => {
        if (!data) return [];
        return data.roles.filter(role => {
            const matchesSeniority = selectedSeniority === 'All' ||
                role.seniorityLevel === selectedSeniority;

            const matchesCity = selectedCity === 'All' ||
                role.locations.some(loc => loc.startsWith(selectedCity));

            return matchesSeniority && matchesCity;
        });
    }, [data, selectedSeniority, selectedCity]);

    // Filtered hiring companies
    const filteredHiringCompanies = useMemo(() => {
        if (!data) return [];

        // If no filter, use the top companies provided by the backend
        if (selectedSeniority === 'All' && selectedCity === 'All') {
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
    }, [data, filteredRoles, selectedSeniority, selectedCity]);

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

    const displayLetter = data.techName.charAt(0);

    return (
        <div className="space-y-8">
            <div className="flex flex-col md:flex-row md:items-start gap-6">
                <div className="h-24 w-24 rounded-2xl bg-white border border-gray-200 shadow-sm flex items-center justify-center flex-shrink-0 text-3xl font-bold text-slate-700">
                    {displayLetter}
                </div>
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <H1>{data.techName}</H1>
                        <FeedbackButton variant="icon" context={`${data.techName} Page Overview`} />
                    </div>
                    <p className="text-gray-600 mt-2 text-lg leading-relaxed max-w-2xl">
                        Comprehensive market insights, curated learning paths, and local community connections to help you succeed as a {data.techName} professional.
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
            <div className="flex border-b border-gray-200 relative overflow-x-auto no-scrollbar">
                {(['Market', 'Learn', 'Community'] as Tab[]).map((tab) => (
                    <button
                        key={tab}
                        onClick={() => setActiveTab(tab)}
                        className={`px-8 py-4 font-semibold text-sm transition-all relative z-10 whitespace-nowrap ${activeTab === tab
                            ? 'text-blue-600'
                            : 'text-gray-500 hover:text-gray-700'
                            }`}
                    >
                        {tab}
                        {activeTab === tab && (
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600 rounded-t-full shadow-[0_0_8px_rgba(37,99,235,0.4)]" />
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
                        selectedCity={selectedCity}
                        setSelectedCity={setSelectedCity}
                        companiesPage={companiesPage}
                        setCompaniesPage={setCompaniesPage}
                        jobsPage={jobsPage}
                        setJobsPage={setJobsPage}
                        seniorityOptions={seniorityOptions}
                        cityOptions={cityOptions}
                        filteredRoles={filteredRoles}
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
