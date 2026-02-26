import { useParams, Link } from 'react-router-dom';
import { Building2, MapPin, DollarSign, Calendar } from 'lucide-react';
import { mockRecentJobs } from '../lib/mockData';
import { getCompanyById } from '../lib/api';

export default function CompanyProfilePage() {
    const { companyId } = useParams<{ companyId: string }>();
    const company = getCompanyById(companyId || '');
    const companyName = company.name;
    const displayLetter = company.logo;

    return (
        <div className="space-y-8">
            <div className="flex items-start gap-6 border-b border-gray-200 pb-8">
                <div className="h-24 w-24 rounded-2xl bg-white border border-gray-200 shadow-sm flex items-center justify-center flex-shrink-0 text-3xl font-bold text-slate-700">
                    {displayLetter}
                </div>
                <div>
                    <h1 className="text-3xl font-bold text-slate-900">{companyName}</h1>
                    <p className="text-gray-600 mt-2 max-w-2xl text-lg">
                        Engineering-focused organization specializing in web-scale infrastructure and developer tools.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                            <Building2 className="h-3.5 w-3.5" />
                            Technology & Software
                        </span>
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                            Public Company
                        </span>
                        <a href="#" className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 hover:bg-blue-100 transition-colors">
                            website.com
                        </a>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">

                    <div className="rounded-xl border border-gray-200 bg-white shadow-sm overflow-hidden">
                        <div className="border-b border-gray-100 p-6">
                            <h2 className="text-lg font-bold text-slate-900">Tech Stack</h2>
                            <p className="text-sm text-gray-500 mt-1">Extracted from active and historical job postings</p>
                        </div>
                        <div className="p-6">
                            <div className="flex flex-wrap gap-2.5">
                                {['React', 'TypeScript', 'Node.js', 'PostgreSQL', 'AWS', 'Docker', 'Kubernetes', 'GraphQL'].map(tech => (
                                    <Link key={tech} to={"/tech/" + tech.toLowerCase()} className="inline-flex items-center rounded-full bg-white border border-gray-200 px-4 py-1.5 text-sm font-semibold text-slate-700 hover:border-blue-400 hover:text-blue-600 transition-colors shadow-sm">
                                        {tech}
                                    </Link>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="rounded-xl border border-gray-200 bg-white shadow-sm overflow-hidden">
                        <div className="border-b border-gray-100 p-6">
                            <h2 className="text-lg font-bold text-slate-900">Active Roles</h2>
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
                                    {mockRecentJobs.map((job) => (
                                        <tr key={job.id} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-5">
                                                <div className="font-semibold text-slate-900">{job.title}</div>
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
                    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                        <h3 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-5">Extracted Insights</h3>
                        <ul className="space-y-4 text-sm text-gray-600">
                            <li className="flex justify-between items-center border-b border-gray-50 pb-3">
                                <span className="font-medium text-slate-600">Work Model</span>
                                <span className="font-semibold text-slate-900 bg-green-50 text-green-700 px-2 py-0.5 rounded text-xs border border-green-200">Hybrid Friendly</span>
                            </li>
                            <li className="flex justify-between items-center border-b border-gray-50 pb-3">
                                <span className="font-medium text-slate-600">Top Hubs</span>
                                <span className="font-semibold text-slate-900 text-right">Sydney<br />Remote</span>
                            </li>
                            <li className="flex justify-between items-center cursor-pointer group">
                                <span className="font-medium text-slate-600">Common Benefits</span>
                                <span className="font-semibold text-blue-600 group-hover:underline">View 8 tags</span>
                            </li>
                        </ul>
                    </div>
                </div>

            </div>
        </div>
    );
}
