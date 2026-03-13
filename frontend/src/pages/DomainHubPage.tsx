import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCountryUrlSync } from '../hooks/useCountryUrlSync';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell,
  AreaChart, Area
} from 'recharts';
import { 
  Briefcase, 
  Layers, 
  TrendingUp, 
  ChevronRight,
  ArrowRight,
  Building2,
  Euro,
  Navigation
} from 'lucide-react';
import { api, type DomainHub } from '../api';
import { useAppStore } from '../store/useAppStore';
import PageLoader from '../components/common/PageLoader';
import ErrorState from '../components/common/ErrorState';
import { Card, CardHeader } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';
import { FeedbackButton } from '../components/common/Feedback';
import CompanyLogo from '../components/common/CompanyLogo';
import { useChartStyles } from '../hooks/useChartStyles';
import TechBadge from '../components/common/TechBadge';

export default function DomainHubPage() {
  const { category: categorySlug } = useParams<{ category: string }>();
  const navigate = useNavigate();
  const { selectedCountry } = useAppStore();
  
  // Global country sync
  useCountryUrlSync();
  const [data, setData] = useState<DomainHub | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  const { tooltipStyle, tooltipItemStyle, gridStroke, axisTickFill, barColors } = useChartStyles();

  const loadHub = useCallback(async () => {
    if (!categorySlug) return;
    setIsLoading(true);
    setError(false);
    try {
      const hubData = await api.getDomainHub(categorySlug, selectedCountry);
      setData(hubData);
    } catch (err) {
      console.error('Failed to load domain hub:', err);
      setError(true);
    } finally {
      setIsLoading(false);
    }
  }, [categorySlug, selectedCountry]);

  useEffect(() => {
    loadHub();
  }, [loadHub]);

  if (isLoading) return <PageLoader />;
  if (error || !data) return <ErrorState title="Couldn't load hub" onRetry={loadHub} />;

  const { category, trends, technologies, topCompanies, recentJobs } = data;

  return (
    <div className="space-y-10 pb-20">
      {/* breadcrumbs */}
      <nav className="flex items-center gap-2 text-sm text-muted">
        <span className="cursor-pointer hover:text-accent" onClick={() => navigate('/hubs')}>Domains</span>
        <ChevronRight className="h-4 w-4" />
        <span className="text-secondary font-semibold">{category.displayName}</span>
      </nav>

      {/* Hero Header */}
      <header className="space-y-6">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="space-y-4 max-w-3xl">
            <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-primary">
              {category.displayName} Market Hub
            </h1>
            <p className="text-xl text-secondary leading-relaxed">
              {category.description}
            </p>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-[10px] uppercase tracking-widest font-bold text-muted">Market Share</p>
              <p className="text-3xl font-extrabold text-primary">{data.marketShare.toFixed(1)}%</p>
            </div>
            <div className="h-12 w-px bg-border"></div>
            <div className="text-right">
              <p className="text-[10px] uppercase tracking-widest font-bold text-muted">Hiring Growth</p>
              <div className={`flex items-center gap-1 text-3xl font-extrabold ${data.growthRate >= 0 ? 'text-emerald-500' : 'text-rose-500'}`}>
                <TrendingUp className={`h-6 w-6 ${data.growthRate < 0 ? 'rotate-180' : ''}`} />
                {data.growthRate > 0 ? '+' : ''}{data.growthRate.toFixed(1)}%
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Stats Row */}
      <section className="grid grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6">
        <StatCard 
          label="Tracked Vacancies" 
          value={data.totalJobs.toLocaleString()} 
          icon={Briefcase} 
          color="blue" 
        />
        <StatCard 
          label="Hiring Companies" 
          value={data.activeCompanies.toLocaleString()} 
          icon={Building2} 
          color="emerald" 
        />
        <StatCard 
          label="Core Technologies" 
          value={technologies.length.toLocaleString()} 
          icon={Layers} 
          color="purple" 
        />
        <StatCard 
          label="Recent Postings" 
          value={trends.last6MonthsJobs.toLocaleString()} 
          icon={Navigation} 
          color="amber" 
          subtext="Last 6 months"
        />
      </section>

      {/* Charts Section */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Technology Mix Chart */}
        <Card className="p-0">
          <CardHeader>
            <div>
              <H2>Top Technologies</H2>
              <p className="text-sm text-muted mt-1">Market distribution in this domain</p>
            </div>
            <FeedbackButton variant="icon" context="Domain Tech Distribution" />
          </CardHeader>
          <div className="p-4 md:p-6 h-[400px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={technologies.slice(0, 10)} layout="vertical" margin={{ left: 20 }}>
                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke={gridStroke} />
                <XAxis type="number" hide />
                <YAxis dataKey="name" type="category" axisLine={false} tickLine={false} tick={{ fill: axisTickFill, fontSize: 13 }} width={100} />
                <Tooltip cursor={{ fill: 'var(--theme-hover)' }} contentStyle={tooltipStyle} itemStyle={tooltipItemStyle} />
                <Bar dataKey="jobCount" radius={[0, 4, 4, 0]} barSize={24}>
                  {technologies.slice(0, 10).map((_entry: any, index: number) => (
                    <Cell key={`cell-${index}`} fill={barColors.primary} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* Demand Trend Chart */}
        <Card className="p-0">
          <CardHeader>
            <div>
              <H2>Demand Trend</H2>
              <p className="text-sm text-muted mt-1">Monthly hiring volume</p>
            </div>
            <FeedbackButton variant="icon" context="Domain Demand Trend" />
          </CardHeader>
          <div className="p-4 md:p-6 h-[400px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trends.monthlyData}>
                <defs>
                  <linearGradient id="colorJobs" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={barColors.primary} stopOpacity={0.1}/>
                    <stop offset="95%" stopColor={barColors.primary} stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={gridStroke} />
                <XAxis dataKey="month" axisLine={false} tickLine={false} tick={{ fill: axisTickFill, fontSize: 11 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: axisTickFill, fontSize: 11 }} />
                <Tooltip contentStyle={tooltipStyle} />
                <Area type="monotone" dataKey="jobCount" stroke={barColors.primary} fillOpacity={1} fill="url(#colorJobs)" strokeWidth={3} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </section>

      {/* Detail Lists */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Top Companies */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <H2>Top Hiring Companies</H2>
            <span className="text-sm font-medium text-muted">{topCompanies.length} tracked</span>
          </div>
          <Card className="p-0 border-border overflow-hidden">
            <table className="w-full text-left text-sm">
              <thead className="bg-elevated text-secondary">
                <tr>
                  <th className="px-6 py-3 font-medium">Company</th>
                  <th className="px-6 py-3 font-medium text-right">Domain Roles</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-subtle">
                {topCompanies.map((company: any) => (
                  <tr key={company.id} className="hover:bg-surface-hover transition-colors cursor-pointer" onClick={() => navigate(`/company/${company.id}`)}>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <CompanyLogo logoUrl="" companyName={company.name} className="h-8 w-8 rounded border border-border" />
                        <span className="font-semibold text-primary">{company.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right font-bold text-accent">
                      {company.jobCount}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
        </div>

        {/* Recent Jobs */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <H2>Recent Opportunities</H2>
            <button className="text-sm font-bold text-accent hover:underline flex items-center gap-1" onClick={() => navigate('/')}>
              View all
              <ArrowRight className="h-4 w-4" />
            </button>
          </div>
          <div className="space-y-3">
            {recentJobs.slice(0, 6).map((job: any) => (
              <Card 
                key={job.id} 
                className="p-4 border-border hover:border-accent transition-all cursor-pointer group"
                onClick={() => navigate(`/job/${job.id}`)}
              >
                <div className="flex justify-between items-start">
                  <div className="space-y-1">
                    <h3 className="font-bold text-primary group-hover:text-accent transition-colors">{job.title}</h3>
                    <p className="text-sm text-secondary font-medium">{job.companyName}</p>
                    <div className="flex items-center gap-3 text-xs text-muted">
                      <span className="flex items-center gap-1"><Navigation className="h-3 w-3" /> {job.location}</span>
                      {job.salaryMax > 0 && <span className="flex items-center gap-1"><Euro className="h-3 w-3" /> {(job.salaryMax / 1000).toFixed(0)}k</span>}
                    </div>
                  </div>
                  <div className="p-2 rounded-lg bg-surface border border-border group-hover:bg-accent/10 group-hover:text-accent transition-all">
                    <ChevronRight className="h-5 w-5" />
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Technologies Directory */}
      <section className="space-y-6">
        <H2>Technology Directory</H2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-4">
          {technologies.map((tech: any) => (
            <div 
              key={tech.name}
              className="p-4 rounded-xl border border-border bg-card hover:border-accent hover:shadow-theme-sm transition-all cursor-pointer group"
              onClick={() => navigate(`/tech/${tech.name.toLowerCase()}`)}
            >
              <div className="flex flex-col items-center text-center space-y-2">
                <TechBadge name={tech.name} className="scale-125 mb-1" />
                <span className="font-bold text-sm text-primary group-hover:text-accent transition-colors">{tech.name}</span>
                <span className="text-[10px] text-muted font-bold uppercase">{tech.jobCount} roles</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      <div className="flex justify-center mt-12">
        <FeedbackButton context={`Domain Hub Page: ${category.displayName}`} />
      </div>
    </div>
  );
}

function StatCard({ label, value, icon: Icon, color, subtext }: { label: string, value: string, icon: any, color: string, subtext?: string }) {
  const colorClasses: Record<string, string> = {
    blue: "bg-blue-500/10 text-blue-500",
    emerald: "bg-emerald-500/10 text-emerald-500",
    purple: "bg-purple-500/10 text-purple-500",
    amber: "bg-amber-500/10 text-amber-500",
  };

  return (
    <Card className="p-6 md:p-7 border-border hover:shadow-theme-md transition-all group">
      <div className="flex items-start justify-between">
        <div className="space-y-4 flex-1">
          <p className="text-[10px] md:text-xs font-bold text-muted uppercase tracking-widest">{label}</p>
          <div className="space-y-1">
            <p className="text-3xl md:text-4xl font-extrabold text-primary leading-none group-hover:text-accent transition-colors">{value}</p>
            {subtext && <p className="text-[10px] text-muted font-medium">{subtext}</p>}
          </div>
        </div>
        <div className={`p-3 rounded-xl ${colorClasses[color] || colorClasses.blue}`}>
          <Icon className="h-6 w-6" />
        </div>
      </div>
    </Card>
  );
}
