import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Code2, 
  Globe, 
  Server, 
  Smartphone, 
  Cloud, 
  Database, 
  Infinity as InfinityIcon, 
  ShieldCheck,
  TrendingUp,
  Briefcase,
  Layers,
  ChevronRight
} from 'lucide-react';
import { api, type DomainSummary } from '../api';
import { useAppStore } from '../store/useAppStore';
import PageLoader from '../components/common/PageLoader';
import ErrorState from '../components/common/ErrorState';
import { Card } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';
import { FeedbackButton } from '../components/common/Feedback';

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

export default function DomainHubsPage() {
  const navigate = useNavigate();
  const { selectedCountry } = useAppStore();
  const [hubs, setHubs] = useState<DomainSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadHubs = useCallback(async () => {
    setIsLoading(true);
    setError(false);
    try {
      const data = await api.getAllDomainHubs(selectedCountry);
      setHubs(data);
    } catch (err) {
      console.error('Failed to load domain hubs:', err);
      setError(true);
    } finally {
      setIsLoading(false);
    }
  }, [selectedCountry]);

  useEffect(() => {
    loadHubs();
  }, [loadHubs]);

  if (isLoading) return <PageLoader />;
  if (error) return <ErrorState title="Couldn't load domains" onRetry={loadHubs} />;

  return (
    <div className="space-y-10 pb-20">
      <header className="space-y-4 max-w-3xl">
        <h1 className="text-4xl font-extrabold tracking-tight text-primary">
          Technology Domains
        </h1>
        <p className="text-xl text-secondary leading-relaxed">
          Explore the job market through specific technology verticals. 
          Discover trends, top companies, and in-demand roles for each domain.
        </p>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {hubs.map((hub) => {
          const Icon = categoryIcons[hub.category.slug] || Layers;
          return (
            <Card 
              key={hub.category.slug}
              className="p-0 border-border group hover:border-accent hover:shadow-theme-md transition-all cursor-pointer overflow-hidden flex flex-col"
              onClick={() => navigate(`/hubs/${hub.category.slug}`)}
            >
              <div className="p-6 flex-1 space-y-4">
                <div className="flex items-start justify-between">
                  <div className="p-3 bg-accent-subtle rounded-xl text-accent">
                    <Icon className="h-8 w-8" />
                  </div>
                  {hub.growthRate !== 0 && (
                    <div className={`flex items-center gap-1 font-bold text-sm px-2 py-1 rounded-full ${
                      hub.growthRate > 0 ? 'text-emerald-500 bg-emerald-500/10' : 'text-rose-500 bg-rose-500/10'
                    }`}>
                      <TrendingUp className={`h-4 w-4 ${hub.growthRate < 0 ? 'rotate-180' : ''}`} />
                      {hub.growthRate > 0 ? '+' : ''}{hub.growthRate.toFixed(1)}%
                    </div>
                  )}
                </div>

                <div>
                  <h2 className="text-2xl font-bold text-primary group-hover:text-accent transition-colors">
                    {hub.category.displayName}
                  </h2>
                  <p className="text-muted text-sm mt-2 line-clamp-2">
                    {hub.category.description}
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-4 pt-2">
                  <div className="space-y-1">
                    <p className="text-[10px] uppercase tracking-widest font-bold text-muted">Active Roles</p>
                    <div className="flex items-center gap-2">
                      <Briefcase className="h-4 w-4 text-secondary" />
                      <span className="font-bold text-primary">{hub.jobCount.toLocaleString()}</span>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <p className="text-[10px] uppercase tracking-widest font-bold text-muted">Hiring Cos</p>
                    <div className="flex items-center gap-2">
                      <Layers className="h-4 w-4 text-secondary" />
                      <span className="font-bold text-primary">{hub.companyCount.toLocaleString()}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="px-6 py-4 bg-elevated border-t border-border-subtle flex items-center justify-between group-hover:bg-accent/5 transition-colors">
                <span className="text-sm font-semibold text-secondary">{hub.techCount} Technologies</span>
                <div className="flex items-center gap-1 text-accent font-bold text-sm">
                  Explore Hub
                  <ChevronRight className="h-4 w-4 group-hover:translate-x-1 transition-transform" />
                </div>
              </div>
            </Card>
          );
        })}
      </div>

      <section className="bg-elevated rounded-3xl p-8 md:p-12 border border-border mt-12">
        <div className="max-w-3xl space-y-6">
          <H2>About Domain Hubs</H2>
          <p className="text-secondary leading-relaxed">
            Technology domains represent groups of related technologies that form cohesive market sectors. 
            By analyzing data at the domain level, we can provide better insights into broader industry shifts 
            and help you understand the requirements for different specialized career paths.
          </p>
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center gap-2 text-sm text-secondary bg-surface px-4 py-2 rounded-full border border-border">
              <span className="w-2 h-2 rounded-full bg-blue-500"></span>
              Real-time Market Share
            </div>
            <div className="flex items-center gap-2 text-sm text-secondary bg-surface px-4 py-2 rounded-full border border-border">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              Monthly Growth Metrics
            </div>
            <div className="flex items-center gap-2 text-sm text-secondary bg-surface px-4 py-2 rounded-full border border-border">
              <span className="w-2 h-2 rounded-full bg-purple-500"></span>
              Top Paying Locations
            </div>
          </div>
        </div>
      </section>

      <div className="flex justify-center mt-8">
        <FeedbackButton context="Domain Hubs Listing Page" />
      </div>
    </div>
  );
}
