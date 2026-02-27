import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Building2, MapPin, Clock, Calendar, ChevronRight, ExternalLink } from 'lucide-react';
import DOMPurify from 'dompurify';
import { FeedbackButton } from '../components/common/Feedback';
import { Card, CardHeader, CardContent } from '../components/ui/Card';
import { H1, H2, SectionSubtitle } from '../components/ui/Typography';
import { Badge } from '../components/ui/Badge';
import type { JobPageDto } from '../lib/api';
import { fetchJobDetails } from '../lib/api';
import CompanyLogo from '../components/common/CompanyLogo';

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
        <div className="space-y-8">
            {/* Job Header - Aligned with Company/Tech Pages */}
            <div className="flex items-start gap-6 border-b border-gray-200 pb-8 mt-2">
                <CompanyLogo
                    logoUrl={company.logoUrl}
                    companyName={company.name}
                    className="h-24 w-24 rounded-2xl border border-gray-200 shadow-sm flex-shrink-0 text-3xl"
                    imageClassName="p-2"
                />
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <H1>{details.title}</H1>
                        <FeedbackButton variant="icon" context={`Job: ${details.title}`} />
                    </div>
                    <div className="mt-3 flex flex-wrap items-center gap-y-2 gap-x-4 text-gray-600">
                        {details.postedDate && (
                            <div className="flex items-center gap-1.5">
                                <Calendar className="w-4 h-4 text-gray-400" />
                                <span>{new Date(details.postedDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                            </div>
                        )}
                        <div className="flex items-center gap-1.5">
                            <Clock className="w-4 h-4 text-gray-400" />
                            <span>{details.seniorityLevel}</span>
                        </div>
                    </div>

                    <div className="mt-4 flex flex-wrap gap-2">
                        {details.employmentType && (
                            <Badge variant="blue">{details.employmentType}</Badge>
                        )}
                        {details.workModel && (
                            <Badge variant="purple">{details.workModel}</Badge>
                        )}
                        {details.jobFunction && (
                            <Badge variant="slate">{details.jobFunction}</Badge>
                        )}
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Main Content (Left, 2 columns wide on large screens) */}
                <div className="lg:col-span-2 space-y-8">

                    {/* Description Section */}
                    <Card>
                        <CardHeader className="bg-white sticky top-0 z-10">
                            <H2>Job Description</H2>
                            <FeedbackButton variant="icon" context={`Job Description: ${details.title}`} />
                        </CardHeader>
                        <CardContent className="md:p-8">
                            {sanitizedDescription ? (
                                <div
                                    className="prose prose-blue max-w-none text-gray-600 prose-headings:text-slate-900 prose-a:text-blue-600 hover:prose-a:text-blue-500 prose-strong:text-slate-900 prose-li:marker:text-blue-400"
                                    dangerouslySetInnerHTML={{ __html: sanitizedDescription }}
                                />
                            ) : (
                                <div className="text-gray-500 italic py-8 text-center bg-slate-50 rounded-lg border border-dashed border-gray-200">
                                    No detailed description provided by the employer.
                                </div>
                            )}
                        </CardContent>
                    </Card>

                    {/* Locations Section */}
                    <Card>
                        <CardHeader>
                            <H2 className="flex items-center gap-2">
                                <MapPin className="w-5 h-5 text-gray-400" />
                                Available Locations ({locations.length})
                            </H2>
                        </CardHeader>
                        <div className="p-0">
                            <ul className="divide-y divide-gray-100">
                                {locations.map((loc, idx) => (
                                    <li key={idx} className="px-6 py-4 flex items-center justify-between group hover:bg-slate-50/50 transition-colors">
                                        <span className="text-slate-700 font-medium">{loc.location}</span>
                                        {loc.applyUrl ? (
                                            <a
                                                href={loc.applyUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                className="inline-flex items-center gap-1.5 px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold rounded-lg transition-all shadow-sm hover:shadow-md active:scale-95"
                                            >
                                                Apply <ExternalLink className="w-3.5 h-3.5" />
                                            </a>
                                        ) : (
                                            <span className="text-xs text-gray-400 italic">No direct link available</span>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </Card>
                </div>

                {/* Sidebar (Right) */}
                <div className="space-y-6">
                    {/* Company Info Box */}
                    <Card>
                        <CardContent className="border-b border-gray-100 p-6">
                            <SectionSubtitle className="mb-5">Hiring Company</SectionSubtitle>
                            <div className="flex items-center gap-4">
                                <Link to={`/company/${company.companyId}`} className="group relative block">
                                    <div className="w-16 h-16 bg-white rounded-xl border border-gray-100 flex items-center justify-center p-2 flex-shrink-0 shadow-sm group-hover:border-blue-200 transition-colors bg-slate-50/50">
                                        {company.logoUrl ? (
                                            <img src={company.logoUrl} alt={`${company.name} logo`} className="max-w-full max-h-full object-contain" />
                                        ) : (
                                            <Building2 className="w-8 h-8 text-gray-400" />
                                        )}
                                    </div>
                                </Link>
                                <div>
                                    <h3 className="text-lg font-bold text-slate-900 leading-snug">{company.name}</h3>
                                    <Link to={`/company/${company.companyId}`} className="text-blue-600 hover:text-blue-700 text-sm font-semibold flex items-center gap-1 mt-1 transition-colors group">
                                        View Profile <ChevronRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
                                    </Link>
                                </div>
                            </div>
                        </CardContent>

                        <div className="p-6 bg-slate-50/50">
                            <SectionSubtitle className="mb-4">Required Stack</SectionSubtitle>
                            {details.technologies && details.technologies.length > 0 ? (
                                <div className="flex flex-wrap gap-1.5">
                                    {details.technologies.map(tech => (
                                        <Link
                                            key={tech}
                                            to={`/tech/${tech.toLowerCase()}`}
                                            className="inline-flex items-center rounded-full bg-slate-50 border border-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 hover:border-blue-200 hover:text-blue-700 transition-colors"
                                        >
                                            {tech}
                                        </Link>
                                    ))}
                                </div>
                            ) : (
                                <p className="text-sm text-gray-400 italic">Not specified</p>
                            )}
                        </div>

                        {details.benefits && details.benefits.length > 0 && (
                            <div className="p-6 border-t border-gray-100">
                                <SectionSubtitle className="mb-4">Role Benefits</SectionSubtitle>
                                <div className="flex flex-wrap gap-2">
                                    {details.benefits.map((benefit, i) => (
                                        <Badge key={i} variant="emerald">{benefit}</Badge>
                                    ))}
                                </div>
                            </div>
                        )}
                    </Card>

                    {/* Similar Roles */}
                    {similarRoles && similarRoles.length > 0 && (
                        <Card>
                            <CardHeader>
                                <SectionSubtitle>Similar Roles</SectionSubtitle>
                                <FeedbackButton variant="icon" context="Similar Roles Section" />
                            </CardHeader>
                            <div className="divide-y divide-gray-50">
                                {similarRoles.map(role => (
                                    <Link
                                        key={role.id}
                                        to={`/job/${role.id}`}
                                        className="block p-5 hover:bg-slate-50 transition-all group"
                                    >
                                        <div className="font-bold text-slate-900 group-hover:text-blue-600 mb-1 transition-colors leading-snug">
                                            {role.title}
                                        </div>
                                        <div className="text-sm text-gray-500 font-medium">
                                            {role.companyName}
                                        </div>
                                        <div className="flex items-center gap-3 mt-4 text-xs text-gray-400">
                                            <span className="flex items-center gap-1.5 bg-slate-100 px-2 py-0.5 rounded text-slate-500">
                                                <MapPin className="w-3 h-3" /> {role.locations[0]}{role.locations.length > 1 ? ` +${role.locations.length - 1}` : ''}
                                            </span>
                                            {role.postedDate && (
                                                <span className="flex items-center gap-1">
                                                    <Clock className="w-3 h-3" /> {new Date(role.postedDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                                                </span>
                                            )}
                                        </div>
                                    </Link>
                                ))}
                            </div>
                        </Card>
                    )}
                </div>
            </div>
        </div >
    );
};

export default JobPage;
