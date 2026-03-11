import { useState, useEffect, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { 
    Briefcase, 
    Globe, 
    Server, 
    Smartphone, 
    Database, 
    ChevronRight,
    Code2,
    Cloud,
    Infinity as InfinityIcon,
    ShieldCheck
} from 'lucide-react';
import { fetchLandingPageData, type LandingPageDto } from '../lib/api';
import { api, type DomainSummary } from '../api';
import { useAppStore } from '../store/useAppStore';
import PageLoader from '../components/common/PageLoader';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import CompanyLogo from '../components/common/CompanyLogo';
import { Card, CardHeader } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';
import SimplePager from '../components/ui/SimplePager';
import SearchBox from '../components/common/SearchBox';
import { useChartStyles } from '../hooks/useChartStyles';
import AsstonKick from '../assets/asston-kick.png';

const categoryIcons: Record<string, any> = {
  'languages': Code2,
  'frontend': Globe,
  'backend': Server,
  'mobile': Smartphone,
  'cloud-infra': Cloud,
  'data-ai': Database,
  'devops': InfinityIcon,
  'security': ShieldCheck
};

const categoryColors: Record<string, string> = {
  'languages': 'blue',
  'frontend': 'blue',
  'backend': 'emerald',
  'mobile': 'purple',
  'cloud-infra': 'sky',
  'data-ai': 'amber',
  'devops': 'indigo',
  'security': 'rose'
};

const COMPANIES_PAGE_SIZE = 5;
const MAX_COMPANIES = 20;
const MAX_TECH = 20;

export default function LandingPage() {
    const navigate = useNavigate();
    const { selectedCountry } = useAppStore();
    const [data, setData] = useState<LandingPageDto | null>(null);
    const [hubs, setHubs] = useState<DomainSummary[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);
    const [companiesPage, setCompaniesPage] = useState(1);

    const { tooltipStyle, tooltipItemStyle, gridStroke, axisTickFill, barColors } = useChartStyles();

    const loadData = useCallback(async () => {
        setIsLoading(true);
        setError(false);
        try {
            const [apiData, hubData] = await Promise.all([
                fetchLandingPageData(selectedCountry),
                api.getAllDomainHubs(selectedCountry)
            ]);
            setData(apiData);
            setHubs(hubData);
        } catch (err) {
            console.error('Failed to load landing page data:', err);
            setError(true);
        } finally {
            setIsLoading(false);
        }
    }, [selectedCountry]);

    useEffect(() => {
        loadData();
    }, [loadData, selectedCountry]);

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

    const topDomains = [...hubs]
        .sort((a, b) => b.jobCount - a.jobCount)
        .slice(0, 4);

    return (
        <div className="space-y-10">
            <section className="text-center py-12 md:py-20 relative px-4 flex flex-col items-center z-20">
                <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-primary leading-tight">
                    Discover. Grow. Connect.
                </h1>
                
                <div className="mx-auto mt-8 flex flex-col md:flex-row items-center justify-center gap-1 md:gap-2">
                    <div className="transition-all duration-500 hover:scale-110 hover:-rotate-6 cursor-pointer group flex-shrink-0">
                        <img 
                            src={AsstonKick} 
                            alt="Asston the friendly donkey mascot" 
                            className="h-20 w-20 md:h-28 md:w-28 lg:h-32 lg:w-32 object-contain drop-shadow-lg group-hover:drop-shadow-xl transition-all"
                        />
                    </div>
                    <div className="flex flex-col items-center md:items-start">
                        <p className="max-w-2xl text-xl md:text-2xl text-secondary font-medium leading-[1.4] text-center md:text-left">
                            Stop grazing. Start kicking.<br/>
                            <span className="text-muted text-lg md:text-xl font-normal opacity-90">
                                The open-source hub for your next big career move 💥🚀📈❤️
                            </span>
                        </p>
                    </div>
                </div>
                <div className="mt-10 w-full max-w-2xl px-4 md:px-0">
                    <SearchBox
                        className="shadow-2xl hover:shadow-blue-200/50 dark:hover:shadow-blue-500/10 transition-all duration-300 transform hover:-translate-y-1"
                        inputClassName="py-4 md:py-5 pl-14 text-lg"
                        placeholder="Search for your next role or technology..."
                    />
                </div>
            </section>

            {/* High-Level Stats Cards */}
            <section className="grid grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
                <div className="rounded-2xl border border-border bg-card p-6 md:p-7 shadow-theme-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-theme-md hover:border-accent/30">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Tracked Vacancies" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-muted uppercase tracking-widest mb-2 md:mb-3">Tracked Vacancies</p>
                    <p className="text-3xl md:text-4xl font-extrabold text-primary leading-none">{data.globalStats.totalVacancies.toLocaleString()}</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-blue-500 rounded-full opacity-20"></div>
                </div>

                <div className="rounded-2xl border border-border bg-card p-6 md:p-7 shadow-theme-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-theme-md hover:border-accent/30">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Top Technology" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-muted uppercase tracking-widest mb-2 md:mb-3">Top Tech</p>
                    <p className="text-2xl md:text-3xl font-extrabold text-primary leading-tight">{data.globalStats.topTech}</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-emerald-500 rounded-full opacity-20"></div>
                </div>

                <div className="rounded-2xl border border-border bg-card p-6 md:p-7 shadow-theme-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-theme-md hover:border-accent/30">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Remote Roles" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-muted uppercase tracking-widest mb-2 md:mb-3">Remote</p>
                    <p className="text-3xl md:text-4xl font-extrabold text-primary leading-none">{data.globalStats.remotePercentage}%</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-purple-500 rounded-full opacity-20"></div>
                </div>

                <div className="rounded-2xl border border-border bg-card p-6 md:p-7 shadow-theme-sm flex flex-col items-center justify-center text-center group relative aspect-square md:aspect-auto transition-all hover:shadow-theme-md hover:border-accent/30">
                    <div className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <FeedbackButton variant="icon" context="Stat: Hybrid Roles" />
                    </div>
                    <p className="text-[10px] md:text-xs font-bold text-muted uppercase tracking-widest mb-2 md:mb-3">Hybrid</p>
                    <p className="text-3xl md:text-4xl font-extrabold text-primary leading-none">{data.globalStats.hybridPercentage}%</p>
                    <div className="mt-2 md:mt-3 w-8 h-1 bg-amber-500 rounded-full opacity-20"></div>
                </div>
            </section>
            
            {/* Tech Domains Section */}
            <section className="space-y-6">
                <div className="flex items-center justify-between">
                    <div>
                        <H2>Technology Domains</H2>
                        <p className="text-sm text-secondary mt-1">Specialized market hubs by sector</p>
                    </div>
                    <button 
                        onClick={() => navigate('/hubs')}
                        className="text-sm font-bold text-accent hover:underline flex items-center gap-1"
                    >
                        Explore all <ChevronRight className="h-4 w-4" />
                    </button>
                </div>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    {topDomains.map((hub) => {
                        const Icon = categoryIcons[hub.category.slug] || Briefcase;
                        const color = categoryColors[hub.category.slug] || 'blue';
                        return (
                            <button 
                                key={hub.category.slug}
                                onClick={() => navigate(`/hubs/${hub.category.slug}`)}
                                className="p-6 rounded-2xl border border-border bg-card hover:border-accent hover:shadow-theme-md transition-all group flex flex-col items-center text-center gap-3"
                            >
                                <div className={`p-3 bg-${color}-500/10 text-${color}-500 rounded-xl group-hover:scale-110 transition-transform`}>
                                    <Icon className="h-6 w-6" />
                                </div>
                                <div className="space-y-1">
                                    <span className="font-bold text-primary block">{hub.category.displayName}</span>
                                    <span className="text-[10px] text-muted font-bold uppercase tracking-tight">{hub.jobCount.toLocaleString()} Jobs</span>
                                </div>
                            </button>
                        );
                    })}
                </div>
            </section>

            {/* Main Content Grid */}
            <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                {/* Top Tech Chart */}
                <Card className="p-0">
                    <CardHeader>
                        <div>
                            <H2>Highest Demand Tech</H2>
                            <p className="text-sm text-muted mt-1">Based on active job postings</p>
                        </div>
                        <FeedbackButton variant="icon" context="Highest Demand Tech Chart" />
                    </CardHeader>
                    <div className="p-4 md:p-6 flex-1 min-h-[350px] md:min-h-[400px] outline-none select-none [&_svg]:outline-none" tabIndex={-1}>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={topTech} layout="vertical" margin={{ top: 0, right: 30, left: 20, bottom: 0 }}>
                                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={gridStroke} />
                                <XAxis type="number" hide />
                                <YAxis dataKey="name" type="category" axisLine={false} tickLine={false} tick={{ fill: axisTickFill, fontSize: 13, fontWeight: 500 }} width={80} />
                                <Tooltip
                                    cursor={{ fill: 'var(--theme-hover)' }}
                                    contentStyle={tooltipStyle}
                                    itemStyle={tooltipItemStyle}
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
                                    {topTech.map((_entry: any, index: number) => (
                                        <Cell key={`cell-${index}`} fill={index === 0 ? barColors.primary : barColors.secondary} className="hover:opacity-80 transition-opacity" />
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
                            <div className="text-sm font-medium text-muted mt-1">
                                Showing <span className="text-primary font-bold">{topCompanies.length > 0 ? (companiesPage - 1) * COMPANIES_PAGE_SIZE + 1 : 0}</span> to <span className="text-primary font-bold">{Math.min(companiesPage * COMPANIES_PAGE_SIZE, topCompanies.length)}</span> of <span className="text-primary font-bold">{topCompanies.length}</span>
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
                            <thead className="table-header-row bg-elevated text-secondary sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-border-subtle">
                                {paginatedCompanies.map((company: any) => (
                                    <tr
                                        key={company.id}
                                        onClick={() => navigate("/company/" + company.id)}
                                        className="hover:bg-surface-hover transition-colors group cursor-pointer"
                                    >
                                        <td className="px-6 py-4">
                                            <div className="flex items-center gap-3">
                                                <CompanyLogo
                                                    logoUrl={company.logo}
                                                    companyName={company.name}
                                                    className="h-10 w-10 rounded-lg border border-border shadow-theme-sm group-hover:border-accent group-hover:text-accent transition-colors"
                                                />
                                                <span className="font-semibold text-primary group-hover:text-accent transition-colors flex-1 truncate" title={company.name}>
                                                    {company.name}
                                                </span>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="inline-flex items-center gap-1.5 rounded-full bg-accent-subtle px-2.5 py-1 text-sm font-semibold text-accent">
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
                        <div className="flex justify-center border-t border-border-subtle p-4">
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
