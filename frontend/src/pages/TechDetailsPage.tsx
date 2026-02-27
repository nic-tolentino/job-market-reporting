import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { PieChart, Pie, Cell, Tooltip } from 'recharts';
import { Briefcase, MapPin, DollarSign, Calendar, Filter } from 'lucide-react';
import PageLoader from '../components/common/PageLoader';
import { fetchTechDetails, type TechDetailsPageDto } from '../lib/api';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import CompanyLogo from '../components/common/CompanyLogo';
import { Card, CardHeader, CardContent } from '../components/ui/Card';
import { H1, H2 } from '../components/ui/Typography';
import { Badge } from '../components/ui/Badge';

const COLORS = ['#f563EB', '#4F46E5', '#10B981', '#F59E0B'];

export default function TechDetailsPage() {
    const { techId } = useParams<{ techId: string }>();
    const navigate = useNavigate();

    const [data, setData] = useState<TechDetailsPageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);

    // Filter states
    const [selectedSeniority, setSelectedSeniority] = useState<string>('All');
    const [selectedCity, setSelectedCity] = useState<string>('All');

    const loadData = useCallback(async () => {
        if (!techId) return;
        setIsLoading(true);
        setError(false);
        setSelectedSeniority('All');
        setSelectedCity('All');
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

    // Derive hiring companies from filtered roles for a dynamic UI
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

    if (isLoading) {
        return <PageLoader />;
    }

    if (error || !data) {
        return <ErrorState title={`Couldn't load ${techId || 'technology'} data`} message="We had trouble fetching details for this technology. Try again shortly." onRetry={loadData} />;
    }

    const displayLetter = data.techName.charAt(0);

    return (
        <div className="space-y-8">
            <div className="flex items-start gap-6 border-b border-gray-200 pb-8">
                <div className="h-24 w-24 rounded-2xl bg-white border border-gray-200 shadow-sm flex items-center justify-center flex-shrink-0 text-3xl font-bold text-slate-700">
                    {displayLetter}
                </div>
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <H1>{data.techName} Jobs</H1>
                        <FeedbackButton variant="icon" context={`${data.techName} Page Overview`} />
                    </div>
                    <p className="text-gray-600 mt-2 text-lg">
                        Global market demand breakdown, hiring hotspots, and seniority requirements for {data.techName} experts.
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

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                {/* Seniority Donut Chart */}
                <Card>
                    <CardHeader>
                        <H2>Seniority Distribution</H2>
                        <FeedbackButton variant="icon" context={`${data.techName} Seniority Breakdown`} />
                    </CardHeader>
                    <CardContent className="flex-1 flex flex-col items-center justify-center min-h-[400px]">
                        <div className="h-64 flex items-center justify-center">
                            <PieChart width={250} height={250}>
                                <Pie
                                    data={data.seniorityDistribution}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={85}
                                    paddingAngle={5}
                                    dataKey="value"
                                    stroke="none"
                                >
                                    {data.seniorityDistribution.map((_entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} style={{ outline: 'none' }} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={{ borderRadius: '8px', border: '1px solid #E5E7EB', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                            </PieChart>
                        </div>
                        <div className="mt-8 grid grid-cols-2 gap-x-6 gap-y-3 text-sm px-2">
                            {data.seniorityDistribution.map((entry, index) => (
                                <div key={entry.name} className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                                    <span className="text-gray-600 font-medium whitespace-nowrap">{entry.name}</span>
                                </div>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                {/* Hiring Companies Table */}
                <Card className="lg:col-span-2">
                    <CardHeader>
                        <H2>Actively Hiring Companies</H2>
                        <FeedbackButton variant="icon" context={`${data.techName} Hiring Companies`} />
                    </CardHeader>
                    <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-50 text-gray-500 sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active {data.techName} Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {filteredHiringCompanies.map((company) => (
                                    <tr key={company.id} className="hover:bg-gray-50 transition-colors group">
                                        <td className="px-6 py-4">
                                            <Link to={"/company/" + company.id} className="flex items-center gap-3">
                                                <CompanyLogo
                                                    logoUrl={company.logo}
                                                    companyName={company.name}
                                                    className="h-10 w-10 rounded-lg border border-gray-200 shadow-sm group-hover:border-blue-300 group-hover:text-blue-600 transition-colors"
                                                />
                                                <span className="font-semibold text-slate-900 group-hover:text-blue-600 transition-colors flex-1 truncate" title={company.name}>
                                                    {company.name}
                                                </span>
                                            </Link>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-1 text-sm font-semibold text-blue-700">
                                                <Briefcase className="h-4 w-4" />
                                                {company.activeRoles}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                {filteredHiringCompanies.length === 0 && (
                                    <tr>
                                        <td colSpan={2} className="px-6 py-12 text-center text-gray-400 italic">
                                            No companies found for this filter combination.
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </Card>

            </div>

            {/* Job Listings Section */}
            <div className="space-y-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <H2>Active {data.techName} Roles</H2>

                    <div className="flex flex-wrap items-center gap-3">
                        <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg px-3 py-1.5 shadow-sm">
                            <Filter className="h-4 w-4 text-gray-400" />
                            <select
                                value={selectedSeniority}
                                onChange={(e) => setSelectedSeniority(e.target.value)}
                                className="text-sm font-medium text-slate-700 bg-transparent border-none focus:ring-0 cursor-pointer"
                            >
                                <option value="All">All Seniorities</option>
                                {seniorityOptions.map(opt => (
                                    <option key={opt} value={opt}>{opt}</option>
                                ))}
                            </select>
                        </div>

                        <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg px-3 py-1.5 shadow-sm">
                            <Filter className="h-4 w-4 text-gray-400" />
                            <select
                                value={selectedCity}
                                onChange={(e) => setSelectedCity(e.target.value)}
                                className="text-sm font-medium text-slate-700 bg-transparent border-none focus:ring-0 cursor-pointer"
                            >
                                <option value="All">All Cities</option>
                                {cityOptions.map(city => (
                                    <option key={city} value={city}>{city}</option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>

                <Card>
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-50 text-gray-500">
                                <tr>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Role</th>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Compensation</th>
                                    <th className="px-6 py-4 font-medium uppercase tracking-wider">Posted</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {filteredRoles.length === 0 ? (
                                    <tr>
                                        <td colSpan={4} className="px-6 py-12 text-center text-gray-400">
                                            No {data.techName} roles found matching your criteria.
                                        </td>
                                    </tr>
                                ) : (
                                    filteredRoles.map((role) => (
                                        <tr
                                            key={role.id}
                                            onClick={() => navigate(`/job/${role.id}`)}
                                            className="hover:bg-gray-50 transition-colors cursor-pointer group/row"
                                        >
                                            <td className="px-6 py-5">
                                                <span className="font-semibold text-slate-900 group-hover/row:text-blue-600 transition-colors">
                                                    {role.title}
                                                </span>
                                                <div className="mt-1 flex items-center gap-1.5 text-gray-500">
                                                    <MapPin className="h-3.5 w-3.5" />
                                                    <span>{role.locations.join(' · ')}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-5">
                                                <div className="flex items-center gap-2">
                                                    <span className="font-medium text-slate-700">{role.companyName}</span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-5">
                                                {role.salaryMin && role.salaryMax ? (
                                                    <div className="flex items-center gap-1 font-medium text-slate-700">
                                                        <DollarSign className="h-4 w-4 text-gray-400" />
                                                        ${(role.salaryMin / 1000)}k - ${(role.salaryMax / 1000)}k
                                                    </div>
                                                ) : (
                                                    <span className="text-gray-400 italic">Unlisted</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-5 text-gray-500 whitespace-nowrap">
                                                <div className="flex items-center gap-1.5">
                                                    <Calendar className="h-4 w-4" />
                                                    {role.postedDate}
                                                </div>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </Card>
            </div>
        </div>
    );
}
