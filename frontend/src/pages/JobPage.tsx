import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Building2, MapPin, Briefcase, Clock, Calendar, ChevronRight, ExternalLink } from 'lucide-react';
import DOMPurify from 'dompurify';
import TechBadge from '../components/common/TechBadge';
import type { JobPageDto } from '../lib/api';
import { fetchJobDetails } from '../lib/api';

const JobPage: React.FC = () => {
    const { jobId } = useParams<{ jobId: string }>();
    const [data, setData] = useState<JobPageDto | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadData = async () => {
            if (!jobId) return;
            setLoading(true);
            window.scrollTo(0, 0);
            try {
                const result = await fetchJobDetails(jobId);
                setData(result);
            } catch (error) {
                console.error('Failed to fetch job details:', error);
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, [jobId]);

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[50vh]">
                <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    if (!data) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[50vh]">
                <h1 className="text-2xl font-bold text-slate-900 mb-4">Job Not Found</h1>
                <p className="text-gray-500 mb-6">The job you're looking for doesn't exist or has been removed.</p>
                <Link to="/" className="text-blue-600 hover:text-blue-500 font-medium">Return to Home</Link>
            </div>
        );
    }

    const { details, locations, company, similarRoles } = data;

    // Sanitize the HTML description if it exists
    const sanitizedDescription = details.description ? DOMPurify.sanitize(details.description) : null;

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 font-sans">
            {/* Breadcrumbs */}
            <div className="flex items-center text-sm text-gray-500 mb-8 overflow-x-auto whitespace-nowrap">
                <Link to="/" className="hover:text-blue-600 transition-colors">Home</Link>
                <ChevronRight className="w-4 h-4 mx-2 flex-shrink-0" />
                <Link to={`/company/${company.companyId}`} className="hover:text-blue-600 transition-colors">{company.name}</Link>
                <ChevronRight className="w-4 h-4 mx-2 flex-shrink-0" />
                <span className="text-slate-900 font-medium truncate">{details.title}</span>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Main Content (Left, 2 columns wide on large screens) */}
                <div className="lg:col-span-2 space-y-8">
                    {/* Header Section */}
                    <div className="bg-white rounded-xl border border-gray-200 p-6 md:p-8 relative overflow-hidden shadow-sm">
                        <div className="absolute top-0 right-0 p-8 opacity-[0.03] pointer-events-none">
                            <Briefcase className="w-32 h-32" />
                        </div>

                        <h1 className="text-3xl md:text-4xl font-bold text-slate-900 mb-4">{details.title}</h1>

                        {/* Meta details */}
                        <div className="flex flex-wrap gap-4 text-sm text-gray-600 mb-6">
                            <div className="flex items-center gap-1.5">
                                <Building2 className="w-4 h-4 text-blue-500" />
                                <Link to={`/company/${company.companyId}`} className="hover:text-blue-600 font-medium transition-colors">
                                    {company.name}
                                </Link>
                            </div>
                            {details.postedDate && (
                                <div className="flex items-center gap-1.5">
                                    <Calendar className="w-4 h-4 text-blue-500" />
                                    <span>Posted: {new Date(details.postedDate).toLocaleDateString()}</span>
                                </div>
                            )}
                            <div className="flex items-center gap-1.5">
                                <Briefcase className="w-4 h-4 text-blue-500" />
                                <span>{details.seniorityLevel}</span>
                            </div>
                        </div>

                        {/* Key Attributes Pills */}
                        <div className="flex flex-wrap gap-2 mb-8">
                            {details.employmentType && (
                                <span className="px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-xs font-semibold border border-blue-100">
                                    {details.employmentType}
                                </span>
                            )}
                            {details.workModel && (
                                <span className="px-3 py-1 bg-purple-50 text-purple-700 rounded-full text-xs font-semibold border border-purple-100">
                                    {details.workModel}
                                </span>
                            )}
                            {details.jobFunction && (
                                <span className="px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-xs font-semibold border border-emerald-100">
                                    {details.jobFunction}
                                </span>
                            )}
                        </div>

                        {/* Locations and Apply Actions */}
                        <div className="border border-gray-200 rounded-lg overflow-hidden bg-white">
                            <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 mb-0 flex items-center justify-between">
                                <h3 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                                    <MapPin className="w-4 h-4 text-gray-500" /> Available Locations ({locations.length})
                                </h3>
                            </div>
                            <ul className="divide-y divide-gray-100">
                                {locations.map((loc, idx) => (
                                    <li key={idx} className="px-4 py-3 flex items-center justify-between group hover:bg-blue-50/50 transition-colors">
                                        <span className="text-slate-700 font-medium">{loc.location}</span>
                                        {loc.applyUrl ? (
                                            <a
                                                href={loc.applyUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                className="inline-flex items-center gap-1.5 px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-md transition-colors shadow-sm"
                                            >
                                                Apply <ExternalLink className="w-3.5 h-3.5" />
                                            </a>
                                        ) : (
                                            <span className="text-xs text-gray-400 italic">No link available</span>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </div>

                    {/* Description Section */}
                    <div className="bg-white rounded-xl border border-gray-200 p-6 md:p-8 shadow-sm">
                        <h2 className="text-xl font-bold text-slate-900 mb-6 flex items-center gap-2">
                            Job Description
                        </h2>

                        {sanitizedDescription ? (
                            <div
                                className="prose prose-blue max-w-none text-gray-600 prose-headings:text-slate-900 prose-a:text-blue-600 hover:prose-a:text-blue-500 prose-strong:text-slate-900"
                                dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
                            />
                        ) : (
                            <div className="text-gray-500 italic">No detailed description provided.</div>
                        )}
                    </div>
                </div>

                {/* Sidebar (Right) */}
                <div className="space-y-6">
                    {/* Company Info Box */}
                    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
                        <div className="p-6 border-b border-gray-100">
                            <div className="flex items-center gap-4 mb-4">
                                <div className="w-16 h-16 bg-white rounded-lg border border-gray-100 flex items-center justify-center p-2 flex-shrink-0 shadow-sm">
                                    {company.logoUrl ? (
                                        <img src={company.logoUrl} alt={`${company.name} logo`} className="max-w-full max-h-full object-contain" />
                                    ) : (
                                        <Building2 className="w-8 h-8 text-gray-400" />
                                    )}
                                </div>
                                <div>
                                    <h3 className="text-lg font-bold text-slate-900">{company.name}</h3>
                                    <Link to={`/company/${company.companyId}`} className="text-blue-600 hover:text-blue-700 text-sm font-medium flex items-center gap-1 mt-1 transition-colors">
                                        View Profile <ChevronRight className="w-3 h-3" />
                                    </Link>
                                </div>
                            </div>
                        </div>

                        <div className="p-6 bg-gray-50">
                            <h4 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-4">Stack for this Role</h4>
                            {details.technologies && details.technologies.length > 0 ? (
                                <div className="flex flex-wrap gap-2">
                                    {details.technologies.map(tech => (
                                        <TechBadge key={tech} name={tech} size="sm" />
                                    ))}
                                </div>
                            ) : (
                                <p className="text-sm text-gray-500">Not specified</p>
                            )}
                        </div>

                        {details.benefits && details.benefits.length > 0 && (
                            <div className="p-6 border-t border-gray-100">
                                <h4 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-4">Benefits listed</h4>
                                <ul className="space-y-2">
                                    {details.benefits.map((benefit, i) => (
                                        <li key={i} className="text-sm text-gray-600 flex items-start gap-2">
                                            <div className="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1.5 flex-shrink-0"></div>
                                            <span>{benefit}</span>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>

                    {/* Similar Roles */}
                    {similarRoles && similarRoles.length > 0 && (
                        <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
                            <h3 className="text-lg font-bold text-slate-900 mb-6">Similar Roles</h3>
                            <div className="space-y-4">
                                {similarRoles.map(role => (
                                    <Link
                                        key={role.id}
                                        to={`/job/${role.id}`}
                                        className="block p-4 rounded-lg bg-white border border-gray-100 hover:border-blue-200 hover:shadow-md transition-all group"
                                    >
                                        <div className="font-medium text-slate-900 group-hover:text-blue-600 mb-1 transition-colors">{role.title}</div>
                                        <div className="text-sm text-gray-500 font-medium">{role.companyName}</div>
                                        <div className="flex items-center gap-3 mt-3 text-xs text-gray-400">
                                            <span className="flex items-center gap-1">
                                                <MapPin className="w-3 h-3 text-gray-400" /> {role.locations[0]}{role.locations.length > 1 ? ` +${role.locations.length - 1}` : ''}
                                            </span>
                                            {role.postedDate && (
                                                <span className="flex items-center gap-1">
                                                    <Clock className="w-3 h-3 text-gray-400" /> {new Date(role.postedDate).toLocaleDateString()}
                                                </span>
                                            )}
                                        </div>
                                    </Link>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default JobPage;
