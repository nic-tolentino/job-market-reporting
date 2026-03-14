import { useNavigate } from 'react-router-dom';
import { MapPin, Calendar, ShieldCheck } from 'lucide-react';
import { Card, CardHeader } from '../ui/Card';
import SimplePager from '../ui/SimplePager';
import { FeedbackButton } from '../common/Feedback';
import { formatSalaryRange, getConfidenceBadgeClasses, getConfidenceLabel } from '../../lib/salaryFormatter';

/**
 * Formats an ISO timestamp into a human-readable relative time string.
 */
function formatLastUpdated(isoString: string): string {
    if (!isoString || isoString === '1970-01-01T00:00:00Z') return 'today';
    
    const lastUpdated = new Date(isoString);
    const now = new Date();
    const diffMs = now.getTime() - lastUpdated.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffHours < 1) return 'within the last hour';
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays === 1) return 'yesterday';
    if (diffDays < 7) return `${diffDays}d ago`;
    return lastUpdated.toLocaleDateString();
}

const JOBS_PAGE_SIZE = 10;

interface JobListing {
    id: string;
    title: string;
    locations: string[];
    companyName: string;
    salaryMin?: { value: number; currency: string; source: string; disclaimer?: string };
    salaryMax?: { value: number; currency: string; source: string; disclaimer?: string };
    postedDate: string;
    source: string;
    lastUpdatedAt: string;
}

interface JobListProps {
    jobs: JobListing[];
    title: string;
    page?: number;
    setPage?: (val: number) => void;
    totalPages?: number;
    showPagination?: boolean;
    context?: string;
}

export const JobList = ({
    jobs,
    title,
    page = 1,
    setPage,
    totalPages = 1,
    showPagination = true,
    context
}: JobListProps) => {
    const navigate = useNavigate();

    return (
        <Card>
            <CardHeader className="flex flex-row items-center justify-between">
                <div className="text-sm font-medium text-muted">
                    {showPagination ? (
                        <>
                            Showing <span className="text-primary font-bold">{jobs.length > 0 ? (page - 1) * JOBS_PAGE_SIZE + 1 : 0}</span> to <span className="text-primary font-bold">{Math.min(page * JOBS_PAGE_SIZE, jobs.length)}</span> of <span className="text-primary font-bold">{jobs.length}</span> positions
                        </>
                    ) : (
                        <span>Showing <span className="text-primary font-bold">{jobs.length}</span> positions</span>
                    )}
                </div>
                <div className="flex items-center gap-4">
                    {showPagination && setPage && totalPages > 1 && (
                        <SimplePager
                            currentPage={page}
                            totalPages={totalPages}
                            onPageChange={setPage}
                        />
                    )}
                    <FeedbackButton variant="icon" context={context || title} />
                </div>
            </CardHeader>
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead className="table-header-row bg-elevated text-secondary sticky top-0 z-20">
                        <tr>
                            <th className="px-6 py-4 font-medium uppercase tracking-wider">Role</th>
                            <th className="px-6 py-4 font-medium uppercase tracking-wider">Company</th>
                            <th className="px-6 py-4 font-medium uppercase tracking-wider">Compensation</th>
                            <th className="px-6 py-4 font-medium uppercase tracking-wider">Posted</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-border-subtle">
                        {jobs.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="px-6 py-12 text-center text-muted">
                                    No roles found matching your criteria.
                                </td>
                            </tr>
                        ) : (
                            jobs.map((role) => (
                                <tr
                                    key={role.id}
                                    onClick={() => navigate(`/job/${role.id}`)}
                                    className="hover:bg-surface-hover transition-colors cursor-pointer group/row"
                                >
                                    <td className="px-6 py-5">
                                        <span className="font-semibold text-primary group-hover/row:text-accent transition-colors">
                                            {role.title}
                                        </span>
                                        <div className="mt-1 flex items-center gap-1.5 text-muted">
                                            <MapPin className="h-3.5 w-3.5" />
                                            <span>{role.locations.map((loc: string) => loc.replace(/,\s*$/, '')).join(' · ')}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-5">
                                        <div className="flex items-center gap-2">
                                            <span className="font-medium text-secondary">{role.companyName}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-5">
                                        {role.salaryMin && role.salaryMax ? (
                                            <div className="space-y-1">
                                                <div className="font-medium text-secondary">
                                                    {formatSalaryRange(role.salaryMin as any, role.salaryMax as any)}
                                                </div>
                                                <span className={`inline-flex items-center gap-1 rounded border px-2 py-0.5 text-xs font-medium ${getConfidenceBadgeClasses(role.salaryMin.source as any)}`}
                                                        title={role.salaryMin.disclaimer || undefined}>
                                                    <ShieldCheck className="h-3 w-3" />
                                                    {getConfidenceLabel(role.salaryMin.source as any)}
                                                </span>
                                            </div>
                                        ) : (
                                            <span className="text-muted italic">Unlisted</span>
                                        )}
                                    </td>
                                    <td className="px-6 py-5 text-muted whitespace-nowrap">
                                        <div className="flex items-center gap-1.5">
                                            <Calendar className="h-4 w-4" />
                                            {role.postedDate}
                                        </div>
                                        <div className="mt-2 flex flex-wrap items-center gap-1.5">
                                            <span className="inline-flex items-center gap-1 rounded bg-accent-subtle px-2 py-0.5 text-xs font-medium text-accent">
                                                <ShieldCheck className="h-3 w-3" />
                                                {role.source}
                                            </span>
                                            <span className="text-xs text-muted">
                                                Updated {formatLastUpdated(role.lastUpdatedAt)}
                                            </span>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
            {showPagination && setPage && totalPages > 1 && (
                <div className="flex justify-center border-t border-border-subtle p-4">
                    <SimplePager
                        currentPage={page}
                        totalPages={totalPages}
                        onPageChange={setPage}
                    />
                </div>
            )}
        </Card>
    );
};
