import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PieChart, Pie, Cell, Tooltip } from 'recharts';
import { Briefcase, Loader2 } from 'lucide-react';
import { fetchTechDetails, type TechDetailsPageDto } from '../lib/api';
import { FeedbackButton } from '../components/common/Feedback';
import ErrorState from '../components/common/ErrorState';
import CompanyLogo from '../components/common/CompanyLogo';

const COLORS = ['#f563EB', '#4F46E5', '#10B981', '#F59E0B'];

export default function TechDetailsPage() {
    const { techId } = useParams<{ techId: string }>();

    const [data, setData] = useState<TechDetailsPageDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(false);

    const loadData = useCallback(async () => {
        if (!techId) return;
        setIsLoading(true);
        setError(false);
        try {
            const apiData = await fetchTechDetails(techId);
            setData(apiData);
        } catch (err) {
            console.error(`Failed to load tech details for ${techId}:`, err);
            setError(true);
        } finally {
            setIsLoading(false);
        }
    }, [techId]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[50vh]">
                <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
            </div>
        );
    }

    if (error || !data) {
        return <ErrorState title={`Couldn't load ${techId || 'technology'} data`} message="We had trouble fetching details for this technology. Try again shortly." onRetry={loadData} />;
    }

    const displayLetter = data.techName.charAt(0);

    return (
        <div className="space-y-8">
            <div className="flex items-start gap-6 border-b border-gray-200 pb-8">
                <div className="h-24 w-24 rounded-2xl bg-white border border-gray-200 shadow-sm flex items-center justify-center flex-shrink-0 text-3xl font-bold text-slate-700">
                    {displayLetter}
                </div>
                <div className="flex-1">
                    <div className="flex items-center gap-3">
                        <h1 className="text-3xl font-bold text-slate-900">{data.techName} Jobs</h1>
                        <FeedbackButton variant="icon" context={`${data.techName} Page Overview`} />
                    </div>
                    <p className="text-gray-600 mt-2 text-lg">
                        Global market demand breakdown, hiring hotspots, and seniority requirements for {data.techName} experts.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2">
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 border border-blue-100">
                            High Demand
                        </span>
                        <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                            Engineering
                        </span>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                {/* Seniority Donut Chart */}
                <div className="rounded-xl border border-gray-200 bg-white shadow-sm flex flex-col overflow-hidden">
                    <div className="border-b border-gray-100 p-6 flex items-center justify-between">
                        <h2 className="text-lg font-bold text-slate-900">Seniority Distribution</h2>
                        <FeedbackButton variant="icon" context={`${data.techName} Seniority Breakdown`} />
                    </div>
                    <div className="p-6 flex-1 flex flex-col items-center justify-center min-h-[400px]">
                        <div className="h-64 flex items-center justify-center">
                            <PieChart width={250} height={250}>
                                <Pie
                                    data={data.seniorityDistribution}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={85}
                                    paddingAngle={5}
                                    dataKey="value"
                                    stroke="none"
                                >
                                    {data.seniorityDistribution.map((_entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} style={{ outline: 'none' }} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={{ borderRadius: '8px', border: '1px solid #E5E7EB', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                            </PieChart>
                        </div>
                        <div className="mt-8 grid grid-cols-2 gap-x-6 gap-y-3 text-sm px-2">
                            {data.seniorityDistribution.map((entry, index) => (
                                <div key={entry.name} className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                                    <span className="text-gray-600 font-medium whitespace-nowrap">{entry.name}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Hiring Companies Table */}
                <div className="lg:col-span-2 rounded-xl border border-gray-200 bg-white shadow-sm flex flex-col overflow-hidden">
                    <div className="border-b border-gray-100 p-6 flex items-center justify-between">
                        <h2 className="text-lg font-bold text-slate-900">Actively Hiring Companies</h2>
                        <FeedbackButton variant="icon" context={`${data.techName} Hiring Companies`} />
                    </div>
                    <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-50 text-gray-500 sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active {data.techName} Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {data.hiringCompanies.map((company) => (
                                    <tr key={company.id} className="hover:bg-gray-50 transition-colors group">
                                        <td className="px-6 py-4">
                                            <Link to={"/company/" + company.id} className="flex items-center gap-3">
                                                <CompanyLogo
                                                    logoUrl={company.logo}
                                                    companyName={company.name}
                                                    className="h-10 w-10 rounded-lg border border-gray-200 shadow-sm group-hover:border-blue-300 group-hover:text-blue-600 transition-colors"
                                                />
                                                <span className="font-semibold text-slate-900 group-hover:text-blue-600 transition-colors flex-1 truncate" title={company.name}>
                                                    {company.name}
                                                </span>
                                            </Link>
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
                </div>

            </div>
        </div>
    );
}
