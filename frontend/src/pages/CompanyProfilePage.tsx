import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Building2, MapPin, DollarSign, Calendar, Loader2 } from 'lucide-react';
import { fetchCompanyProfile, type CompanyProfilePageDto } from '../lib/api';
import { FeedbackButton } from '../components/common/Feedback';

export default function CompanyProfilePage() {
    const { companyId } = useParams<{ companyId: string }>();
    const [data, setData] = useState<CompanyProfilePageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const loadData = async () => {
            if (!companyId) return;
            setIsLoading(true);
            try {
                const apiData = await fetchCompanyProfile(companyId);
                setData(apiData);
            } catch (error) {
                console.error(`Failed to load company profile for ${companyId}:`, error);
            } finally {
                setIsLoading(false);
            }
        };

        loadData();
    }, [companyId]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[50vh]">
                <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
            </div>
        );
    }

    if (!data) return null;

    const companyName = data.companyDetails.name;

    return (
        <div className="space-y-8">
            <div className="flex items-start gap-6 border-b border-gray-200 pb-8">
                <div className="h-24 w-24 rounded-2xl bg-white border border-gray-200 shadow-sm flex items-center justify-center flex-shrink-0 text-3xl font-bold text-slate-700">
                    {data.companyDetails.logo}
                </div>
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <h1 className="text-3xl font-bold text-slate-900">{companyName}</h1>
                        <FeedbackButton variant="icon" context={`${companyName} Profile Info`} />
                    </div>
                    <p className="text-gray-600 mt-2 max-w-2xl text-lg">
                        {data.companyDetails.description}
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                            <Building2 className="h-3.5 w-3.5" />
                            {data.companyDetails.industry}
                        </span>
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                            ~{data.companyDetails.employeesCount.toLocaleString()} Employees
                        </span>
                        <a href={data.companyDetails.website} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 hover:bg-blue-100 transition-colors">
                            Website
                        </a>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">

                    <div className="rounded-xl border border-gray-200 bg-white shadow-sm overflow-hidden">
                        <div className="border-b border-gray-100 p-6 flex items-center justify-between">
                            <div>
                                <h2 className="text-lg font-bold text-slate-900">Tech Stack</h2>
                                <p className="text-sm text-gray-500 mt-1">Extracted from active and historical job postings</p>
                            </div>
                            <FeedbackButton variant="icon" context={`${companyName} Tech Stack`} />
                        </div>
                        <div className="p-6">
                            <div className="flex flex-wrap gap-2.5">
                                {data.techStack.map(tech => (
                                    <Link key={tech} to={"/tech/" + tech.toLowerCase()} className="inline-flex items-center rounded-full bg-white border border-gray-200 px-4 py-1.5 text-sm font-semibold text-slate-700 hover:border-blue-400 hover:text-blue-600 transition-colors shadow-sm">
                                        {tech}
                                    </Link>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="rounded-xl border border-gray-200 bg-white shadow-sm overflow-hidden">
                        <div className="border-b border-gray-100 p-6 flex items-center justify-between">
                            <h2 className="text-lg font-bold text-slate-900">Active Roles</h2>
                            <FeedbackButton variant="icon" context={`${companyName} Active Roles`} />
                        </div>
                        <div className="overflow-x-auto">
                            <table className="w-full text-left text-sm">
                                <thead className="bg-gray-50 text-gray-500">
                                    <tr>
                                        <th className="px-6 py-4 font-medium uppercase tracking-wider">Role</th>
                                        <th className="px-6 py-4 font-medium uppercase tracking-wider">Compensation</th>
                                        <th className="px-6 py-4 font-medium uppercase tracking-wider">Posted</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {data.activeRoles.map((job) => (
                                        <tr key={job.id} className="hover:bg-gray-50 transition-colors group/row">
                                            <td className="px-6 py-5">
                                                <div className="flex items-center gap-2">
                                                    <div className="font-semibold text-slate-900">{job.title}</div>
                                                    <div className="opacity-0 group-hover/row:opacity-100 transition-opacity">
                                                        <FeedbackButton variant="icon" context={`Job Role: ${job.title} at ${companyName}`} />
                                                    </div>
                                                </div>
                                                <div className="mt-1 flex items-center gap-1.5 text-gray-500">
                                                    <MapPin className="h-3.5 w-3.5" />
                                                    {job.location}
                                                </div>
                                                <div className="mt-2 flex flex-wrap gap-1.5">
                                                    {job.technologies.map(tech => (
                                                        <span key={tech} className="inline-flex bg-gray-100 text-gray-600 rounded px-2 text-xs font-medium py-0.5">{tech}</span>
                                                    ))}
                                                </div>
                                            </td>
                                            <td className="px-6 py-5 align-top">
                                                {job.salaryMin && job.salaryMax ? (
                                                    <div className="flex items-center gap-1 font-medium text-slate-700">
                                                        <DollarSign className="h-4 w-4 text-gray-400" />
                                                        ${(job.salaryMin / 1000)}k - ${(job.salaryMax / 1000)}k
                                                    </div>
                                                ) : (
                                                    <span className="text-gray-400 italic">Unlisted</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-5 align-top text-gray-500 whitespace-nowrap">
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
                    </div>
                </div>

                <div className="space-y-6">
                    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm relative">
                        <div className="absolute top-4 right-4">
                            <FeedbackButton variant="icon" context={`${companyName} Insights`} />
                        </div>
                        <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-5">Extracted Insights</h3>
                        <ul className="space-y-4 text-sm text-gray-600">
                            <li className="flex justify-between items-center border-b border-gray-50 pb-3">
                                <span className="font-medium text-slate-600">Work Model</span>
                                <span className="font-semibold text-slate-900 bg-green-50 text-green-700 px-2 py-0.5 rounded text-xs border border-green-200">{data.insights.workModel}</span>
                            </li>
                            <li className="flex justify-between items-center border-b border-gray-50 pb-3">
                                <span className="font-medium text-slate-600">Top Hubs</span>
                                <span className="font-semibold text-slate-900 text-right whitespace-pre-line">
                                    {data.insights.topHubs.replace(', ', '\n')}
                                </span>
                            </li>
                            <li className="flex justify-between items-center cursor-pointer group">
                                <span className="font-medium text-slate-600">Common Benefits</span>
                                <span className="font-semibold text-blue-600 group-hover:underline">View {data.insights.commonBenefits.length} tags</span>
                            </li>
                        </ul>
                    </div>
                </div>

            </div>
        </div>
    );
}
