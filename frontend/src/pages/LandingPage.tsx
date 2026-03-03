import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Briefcase } from 'lucide-react';
import { fetchLandingPageData, type LandingPageDto } from '../lib/api';
import PageLoader from '../components/common/PageLoader';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import CompanyLogo from '../components/common/CompanyLogo';
import { Card, CardHeader } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';
import SimplePager from '../components/ui/SimplePager';
import SearchBox from '../components/common/SearchBox';

const COMPANIES_PAGE_SIZE = 5;
const MAX_COMPANIES = 20;
const MAX_TECH = 20;

export default function LandingPage() {
    const navigate = useNavigate();
    const [data, setData] = useState<LandingPageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);
    const [companiesPage, setCompaniesPage] = useState(1);

    const loadData = useCallback(async () => {
        setIsLoading(true);
        setError(false);
        try {
            const apiData = await fetchLandingPageData();
            setData(apiData);
        } catch (err) {
            console.error('Failed to load landing page data:', err);
            setError(true);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const topCompanies = useMemo(() => {
        if (!data) return [];
        return data.topCompanies.slice(0, MAX_COMPANIES);
    }, [data]);

    const paginatedCompanies = useMemo(() => {
        const start = (companiesPage - 1) * COMPANIES_PAGE_SIZE;
        return topCompanies.slice(start, start + COMPANIES_PAGE_SIZE);
    }, [topCompanies, companiesPage]);

    const totalCompaniesPages = Math.ceil(topCompanies.length / COMPANIES_PAGE_SIZE);

    const topTech = useMemo(() => {
        if (!data) return [];
        return data.topTech.slice(0, MAX_TECH);
    }, [data]);

    if (isLoading) {
        return <PageLoader />;
    }

    if (error || !data) {
        return <ErrorState title="Couldn't load market data" message="We had trouble fetching the latest insights. This might be a temporary issue." onRetry={loadData} />;
    }

    return (
        <div className="space-y-10">
            {/* Hero Section */}
            <section className="text-center py-12 md:py-20 relative px-4 flex flex-col items-center z-20">
                <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-slate-900 leading-tight">
                    Discover. Grow. Connect.
                </h1>
                <p className="mx-auto mt-6 max-w-2xl text-lg md:text-xl text-gray-600 leading-relaxed">
                    We're here to help you succeed in your tech career 🚀📈❤️
                </p>
                <div className="mt-10 w-full max-w-2xl px-4 md:px-0">
                    <SearchBox
                        className="shadow-2xl hover:shadow-blue-200/50 transition-all duration-300 transform hover:-translate-y-1"
                        inputClassName="py-4 md:py-5 pl-14 text-lg"
                        placeholder="Search for your next role or technology..."
                    />
                </div>
            </section>

            {/* High-Level Stats Cards */}
            <section className="grid grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
                <div className="rounded-2xl border border-gray-200 bg-white p-6 md:p-7 shadow-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-md hover:border-blue-200">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Tracked Vacancies" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 md:mb-3">Tracked Vacancies</p>
                    <p className="text-3xl md:text-4xl font-extrabold text-slate-900 leading-none">{data.globalStats.totalVacancies.toLocaleString()}</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-blue-500 rounded-full opacity-20"></div>
                </div>

                <div className="rounded-2xl border border-gray-200 bg-white p-6 md:p-7 shadow-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-md hover:border-blue-200">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Top Technology" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 md:mb-3">Top Tech</p>
                    <p className="text-2xl md:text-3xl font-extrabold text-slate-900 leading-tight">{data.globalStats.topTech}</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-emerald-500 rounded-full opacity-20"></div>
                </div>

                <div className="rounded-2xl border border-gray-200 bg-white p-6 md:p-7 shadow-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-md hover:border-blue-200">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Remote Roles" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 md:mb-3">Remote</p>
                    <p className="text-3xl md:text-4xl font-extrabold text-slate-900 leading-none">{data.globalStats.remotePercentage}%</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-purple-500 rounded-full opacity-20"></div>
                </div>

                <div className="rounded-2xl border border-gray-200 bg-white p-6 md:p-7 shadow-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-md hover:border-blue-200">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Hybrid Roles" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-gray-400 uppercase tracking-widest mb-2 md:mb-3">Hybrid</p>
                    <p className="text-3xl md:text-4xl font-extrabold text-slate-900 leading-none">{data.globalStats.hybridPercentage}%</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-amber-500 rounded-full opacity-20"></div>
                </div>
            </section>

            {/* Main Content Grid */}
            <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                {/* Top Tech Chart */}
                <Card className="p-0">
                    <CardHeader>
                        <div>
                            <H2>Highest Demand Tech</H2>
                            <p className="text-sm text-gray-500 mt-1">Based on active job postings</p>
                        </div>
                        <FeedbackButton variant="icon" context="Highest Demand Tech Chart" />
                    </CardHeader>
                    <div className="p-4 md:p-6 flex-1 min-h-[350px] md:min-h-[400px] outline-none select-none [&_svg]:outline-none" tabIndex={-1}>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={topTech} layout="vertical" margin={{ top: 0, right: 30, left: 20, bottom: 0 }}>
                                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#E5E7EB" />
                                <XAxis type="number" hide />
                                <YAxis dataKey="name" type="category" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 13, fontWeight: 500 }} width={80} />
                                <Tooltip
                                    cursor={{ fill: '#F1F5F9' }}
                                    contentStyle={{ borderRadius: '8px', border: '1px solid #E5E7EB', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                                <Bar
                                    dataKey="count"
                                    radius={[0, 4, 4, 0]}
                                    barSize={32}
                                    onClick={(chartData) => {
                                        if (chartData && chartData.name) {
                                            navigate("/tech/" + chartData.name.toLowerCase());
                                        }
                                    }}
                                    style={{ cursor: 'pointer', outline: 'none' }}
                                >
                                    {topTech.map((_entry, index) => (
                                        <Cell key={`cell-${index}`} fill={index === 0 ? '#2563EB' : '#94A3B8'} className="hover:opacity-80 transition-opacity" />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </Card>

                {/* Top Companies List */}
                <Card className="p-0">
                    <CardHeader className="flex flex-row items-center justify-between">
                        <div>
                            <H2>Top Hiring Companies</H2>
                            <div className="text-sm font-medium text-slate-500 mt-1">
                                Showing <span className="text-slate-900 font-bold">{topCompanies.length > 0 ? (companiesPage - 1) * COMPANIES_PAGE_SIZE + 1 : 0}</span> to <span className="text-slate-900 font-bold">{Math.min(companiesPage * COMPANIES_PAGE_SIZE, topCompanies.length)}</span> of <span className="text-slate-900 font-bold">{topCompanies.length}</span>
                            </div>
                        </div>
                        <div className="flex items-center gap-4">
                            <SimplePager
                                currentPage={companiesPage}
                                totalPages={totalCompaniesPages}
                                onPageChange={setCompaniesPage}
                            />
                            <FeedbackButton variant="icon" context="Top Hiring Companies List" />
                        </div>
                    </CardHeader>
                    <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-50 text-gray-500 sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {paginatedCompanies.map((company: any) => (
                                    <tr
                                        key={company.id}
                                        onClick={() => navigate("/company/" + company.id)}
                                        className="hover:bg-gray-50 transition-colors group cursor-pointer"
                                    >
                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-3">
                                                <CompanyLogo
                                                    logoUrl={company.logo}
                                                    companyName={company.name}
                                                    className="h-10 w-10 rounded-lg border border-gray-200 shadow-sm group-hover:border-blue-300 group-hover:text-blue-600 transition-colors"
                                                />
                                                <span className="font-semibold text-slate-900 group-hover:text-blue-600 transition-colors flex-1 truncate" title={company.name}>
                                                    {company.name}
                                                </span>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-1 text-sm font-semibold text-blue-700">
                                                <Briefcase className="h-4 w-4" />
                                                {company.activeRoles}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    {totalCompaniesPages > 1 && (
                        <div className="flex justify-center border-t border-gray-100 p-4">
                            <SimplePager
                                currentPage={companiesPage}
                                totalPages={totalCompaniesPages}
                                onPageChange={setCompaniesPage}
                            />
                        </div>
                    )}
                </Card>

            </section>
        </div>
    );
}
