import { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Building2, MapPin, DollarSign, Calendar, X, ShieldCheck, Globe, CircleDashed } from 'lucide-react';
import PageLoader from '../components/common/PageLoader';
import { fetchCompanyProfile, type CompanyProfilePageDto } from '../lib/api';
import { useAppStore } from '../store/useAppStore';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import CompanyLogo from '../components/common/CompanyLogo';
import { Card, CardContent } from '../components/ui/Card';
import { H1, H2, SectionSubtitle } from '../components/ui/Typography';
import { Badge } from '../components/ui/Badge';
import SimplePager from '../components/ui/SimplePager';

const ROLES_PAGE_SIZE = 10;

export default function CompanyProfilePage() {
    const navigate = useNavigate();
    const { companyId } = useParams<{ companyId: string }>();
    const { selectedCountry } = useAppStore();
    const [data, setData] = useState<CompanyProfilePageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);
    const [selectedTechs, setSelectedTechs] = useState<Set<string>>(new Set());
    const [rolesPage, setRolesPage] = useState(1);

    const [isDescriptionExpanded, setIsDescriptionExpanded] = useState(false);

    const loadData = useCallback(async () => {
        if (!companyId) return;
        setIsLoading(true);
        setError(false);
        setSelectedTechs(new Set());
        setIsDescriptionExpanded(false);
        setRolesPage(1);
        try {
            const apiData = await fetchCompanyProfile(companyId, selectedCountry);
            setData(apiData);
        } catch (err) {
            console.error(`Failed to load company profile for ${companyId}:`, err);
            setError(true);
        } finally {
            setIsLoading(false);
        }
    }, [companyId, selectedCountry]);

    useEffect(() => {
        loadData();
    }, [loadData, companyId, selectedCountry]);

    const toggleTech = (tech: string) => {
        setSelectedTechs(prev => {
            const next = new Set(prev);
            if (next.has(tech)) next.delete(tech);
            else next.add(tech);
            return next;
        });
        setRolesPage(1);
    };

    const filterableTechs = useMemo(() => {
        if (!data) return [];
        const all = data.activeRoles.flatMap(r => r.technologies);
        return [...new Set(all)].sort();
    }, [data]);

    // Filtered roles based on selected tech pills (inclusive OR)
    const filteredRoles = useMemo(() => {
        if (!data) return [];
        if (selectedTechs.size === 0) return data.activeRoles;
        return data.activeRoles.filter(role =>
            role.technologies.some(t => selectedTechs.has(t))
        );
    }, [data, selectedTechs]);

    const paginatedRoles = useMemo(() => {
        const start = (rolesPage - 1) * ROLES_PAGE_SIZE;
        return filteredRoles.slice(start, start + ROLES_PAGE_SIZE);
    }, [filteredRoles, rolesPage]);

    const totalPages = Math.ceil(filteredRoles.length / ROLES_PAGE_SIZE);

    if (isLoading) {
        return <PageLoader />;
    }

    if (error || !data) {
        return <ErrorState title="Couldn't load company profile" message="We had trouble fetching this company's data. Try again shortly." onRetry={loadData} />;
    }

    const companyName = data.companyDetails.name;

    return (
        <div className="space-y-8">
            {/* Company Header */}
            <div className="flex items-start gap-6 border-b border-border pb-8">
                <CompanyLogo
                    logoUrl={data.companyDetails.logo}
                    companyName={data.companyDetails.name}
                    className="h-24 w-24 rounded-2xl border border-border shadow-theme-sm flex-shrink-0 text-3xl"
                    imageClassName="p-2"
                />
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <H1>{companyName}</H1>
                        <FeedbackButton variant="icon" context={`${companyName} Profile Info`} />
                    </div>
                    <div>
                        <p className={`text-secondary mt-2 max-w-2xl text-lg ${!isDescriptionExpanded ? 'line-clamp-3' : ''}`}>
                            {data.companyDetails.description}
                        </p>
                        {data.companyDetails.description.length > 200 && (
                            <button
                                onClick={() => setIsDescriptionExpanded(!isDescriptionExpanded)}
                                className="text-accent font-semibold text-sm mt-1 hover:underline"
                            >
                                {isDescriptionExpanded ? 'Show less' : 'Show more'}
                            </button>
                        )}
                    </div>
                    {/* Meta pills row */}
                    <div className="mt-4 flex flex-wrap gap-2">
                        {data.companyDetails.verificationLevel === 'verified' ? (
                            <Badge variant="emerald" icon={<ShieldCheck className="h-3.5 w-3.5" />}>
                                Verified
                            </Badge>
                        ) : data.companyDetails.verificationLevel === 'unverified' ? (
                            <Badge variant="slate" icon={<CircleDashed className="h-3.5 w-3.5" />} className="opacity-70">
                                Unverified
                            </Badge>
                        ) : null}

                        {data.companyDetails.hqCountry && (
                            <Badge variant={data.companyDetails.hqCountry === 'NZ' ? 'emerald' : 'slate'} icon={data.companyDetails.hqCountry === 'NZ' ? <MapPin className="h-3.5 w-3.5" /> : <Globe className="h-3.5 w-3.5" />}>
                                {data.companyDetails.hqCountry === 'NZ' ? 'NZ Local' : `HQ: ${data.companyDetails.hqCountry}`}
                            </Badge>
                        )}
                        <Badge variant="slate" icon={<Building2 className="h-3.5 w-3.5" />}>
                            {data.companyDetails.industry}
                        </Badge>
                        <Badge variant="slate">
                            ~{data.companyDetails.employeesCount.toLocaleString()} Employees
                        </Badge>
                        <a href={data.companyDetails.website} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1.5 rounded-full border border-accent/20 bg-accent-subtle px-3 py-1 text-xs font-semibold text-accent hover:bg-accent/10 transition-colors">
                            Website
                        </a>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">

                    {/* Active Roles */}
                    <Card>
                        <div className="border-b border-border-subtle p-6">
                            <div className="flex items-center justify-between mb-1">
                                <div className="flex items-center gap-2">
                                    <H2>Active Roles</H2>
                                    <span className="text-sm text-muted font-normal">
                                        {selectedTechs.size > 0
                                            ? `(${filteredRoles.length} of ${data.activeRoles.length})`
                                            : `(${data.activeRoles.length})`}
                                    </span>
                                </div>
                                <div className="flex items-center gap-4">
                                    <SimplePager
                                        currentPage={rolesPage}
                                        totalPages={totalPages}
                                        onPageChange={setRolesPage}
                                    />
                                    <FeedbackButton variant="icon" context={`${companyName} Active Roles`} />
                                </div>
                            </div>

                            {/* Tech filter pills */}
                            {filterableTechs.length > 0 && (
                                <div className="mt-3 flex flex-wrap gap-2">
                                    {/* "All" pill */}
                                    <button
                                        onClick={() => {
                                            setSelectedTechs(new Set());
                                            setRolesPage(1);
                                        }}
                                        className={`inline-flex items-center rounded-full border px-4 py-1.5 text-sm font-semibold transition-colors ${selectedTechs.size === 0
                                            ? 'border-accent bg-accent-subtle text-accent'
                                            : 'border-border bg-card text-muted hover:border-secondary'
                                            }`}
                                    >
                                        All
                                    </button>
                                    {filterableTechs.map(tech => {
                                        const active = selectedTechs.has(tech);
                                        return (
                                            <button
                                                key={tech}
                                                onClick={() => toggleTech(tech)}
                                                className={`inline-flex items-center gap-1.5 rounded-full border px-4 py-1.5 text-sm font-semibold transition-colors ${active
                                                    ? 'border-accent bg-accent-subtle text-accent'
                                                    : 'border-border bg-card text-secondary hover:border-accent/40 hover:text-accent'
                                                    }`}
                                            >
                                                {tech}
                                                {active && <X className="h-3.5 w-3.5" />}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-elevated text-muted">
                                    <tr>
                                        <th className="px-6 py-4 font-medium uppercase tracking-wider">Role</th>
                                        <th className="px-6 py-4 font-medium uppercase tracking-wider">Compensation</th>
                                        <th className="px-6 py-4 font-medium uppercase tracking-wider">Posted</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-border-subtle">
                                    {filteredRoles.length === 0 ? (
                                        <tr>
                                            <td colSpan={3} className="px-6 py-12 text-center text-muted">
                                                No roles match the selected filters.
                                            </td>
                                        </tr>
                                    ) : paginatedRoles.map((job) => (
                                        <tr
                                            key={job.id}
                                            onClick={() => navigate(`/job/${job.id}`)}
                                            className="hover:bg-surface-hover transition-colors group/row cursor-pointer"
                                        >
                                            <td className="px-6 py-5">
                                                <div className="flex items-center gap-2">
                                                    <Link to={`/job/${job.id}`} className="font-semibold text-primary hover:text-accent hover:underline transition-colors block">
                                                        {job.title}
                                                    </Link>
                                                    <div className="opacity-0 group-hover/row:opacity-100 transition-opacity">
                                                        <FeedbackButton variant="icon" context={`Job Role: ${job.title} at ${companyName}`} />
                                                    </div>
                                                </div>
                                                <div className="mt-1 flex items-center gap-1.5 text-muted">
                                                    <MapPin className="h-3.5 w-3.5 flex-shrink-0" />
                                                    <span>{job.locations.join(' · ')}</span>
                                                </div>
                                                <div className="mt-2 flex flex-wrap gap-1.5">
                                                    {job.technologies.map(tech => (
                                                        <span
                                                            key={tech}
                                                            className={`inline-flex rounded px-2 text-xs font-medium py-0.5 ${selectedTechs.has(tech)
                                                                ? 'bg-accent-subtle text-accent'
                                                                : 'bg-inset text-secondary'
                                                                }`}
                                                        >
                                                            {tech}
                                                        </span>
                                                    ))}
                                                </div>
                                            </td>
                                            <td className="px-6 py-5 align-top">
                                                {job.salaryMin && job.salaryMax ? (
                                                    <div className="flex items-center gap-1 font-medium text-secondary">
                                                        <DollarSign className="h-4 w-4 text-muted" />
                                                        ${(job.salaryMin / 1000)}k - ${(job.salaryMax / 1000)}k
                                                    </div>
                                                ) : (
                                                    <span className="text-muted italic">Unlisted</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-5 align-top text-muted whitespace-nowrap">
                                                <div className="flex items-center gap-1.5">
                                                    <Calendar className="h-4 w-4" />
                                                    {job.postedDate}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {totalPages > 1 && (
                            <div className="flex justify-center border-t border-border-subtle p-4">
                                <SimplePager
                                    currentPage={rolesPage}
                                    totalPages={totalPages}
                                    onPageChange={setRolesPage}
                                />
                            </div>
                        )}
                    </Card>
                </div>

                <div className="space-y-6">
                    <Card className="relative">
                        <CardContent>
                            <div className="absolute top-4 right-4">
                                <FeedbackButton variant="icon" context={`${companyName} Insights`} />
                            </div>
                            <SectionSubtitle className="mb-5">Insights</SectionSubtitle>
                            <ul className="space-y-4 text-sm text-secondary">
                                <li className="flex justify-between items-center border-b border-border-subtle pb-3">
                                    <span className="font-medium text-secondary">Work Model</span>
                                    <Badge variant="emerald">{data.insights.workModel}</Badge>
                                </li>

                                <li className="flex justify-between items-center border-b border-border-subtle pb-3 group cursor-pointer">
                                    <span className="font-medium text-secondary">Common Benefits</span>
                                    <span className="font-semibold text-accent group-hover:underline">View {data.insights.commonBenefits.length} tags</span>
                                </li>
                                {data.insights.hiringLocations.length > 0 && (
                                    <li className="pt-2 border-b border-border-subtle pb-4">
                                        <span className="block font-medium text-secondary mb-3">Hiring Locations</span>
                                        <div className="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto">
                                            {[...new Set(data.insights.hiringLocations)].sort().map(loc => (
                                                <span
                                                    key={loc}
                                                    className="inline-flex items-center rounded-full bg-inset border border-border-subtle px-2.5 py-1 text-xs font-medium text-secondary"
                                                >
                                                    {loc}
                                                </span>
                                            ))}
                                        </div>
                                    </li>
                                )}
                                <li className="pt-2">
                                    <span className="block font-medium text-secondary mb-3">Tech Stacks</span>
                                    <div className="flex flex-wrap gap-1.5">
                                        {data.techStack.map(tech => (
                                            <Link
                                                key={tech}
                                                to={`/tech/${tech.toLowerCase()}`}
                                                className="inline-flex items-center rounded-full bg-inset border border-border-subtle px-2.5 py-1 text-xs font-semibold text-secondary hover:border-accent/40 hover:text-accent transition-colors"
                                            >
                                                {tech}
                                            </Link>
                                        ))}
                                    </div>
                                </li>
                            </ul>
                        </CardContent>
                    </Card>
                </div>

            </div>
        </div>
    );
}
