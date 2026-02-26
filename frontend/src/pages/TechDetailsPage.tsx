import { useParams, Link } from 'react-router-dom';
import { PieChart, Pie, Cell, Tooltip } from 'recharts';
import { Briefcase } from 'lucide-react';
import { mockCompanyLeaderboard } from '../lib/mockData';
import { formatTechName } from '../lib/api';

const seniorityData = [
    { name: 'Senior', value: 400 },
    { name: 'Mid-Level', value: 300 },
    { name: 'Junior', value: 100 },
    { name: 'Lead/Principal', value: 50 },
];

const COLORS = ['#f563EB', '#4F46E5', '#10B981', '#F59E0B'];

export default function TechDetailsPage() {
    const { techId } = useParams<{ techId: string }>();

    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold text-slate-900">{formatTechName(techId)} Jobs</h1>
                <p className="text-gray-600 mt-2 text-lg">Market demand breakdown and hiring hotspots.</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">

                {/* Seniority Donut Chart */}
                <div className="rounded-xl border border-gray-200 bg-white shadow-sm flex flex-col">
                    <div className="border-b border-gray-100 p-6">
                        <h2 className="text-lg font-bold text-slate-900">Seniority Distribution</h2>
                    </div>
                    <div className="p-6 flex-1 flex flex-col items-center justify-center min-h-[400px]">
                        <div className="h-64 flex items-center justify-center">
                            <PieChart width={250} height={250}>
                                <Pie
                                    data={seniorityData}
                                    cx="50%"
                                    cy="50%"
                                    innerRadius={60}
                                    outerRadius={85}
                                    paddingAngle={5}
                                    dataKey="value"
                                    stroke="none"
                                >
                                    {seniorityData.map((_entry, index) => (
                                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} style={{ outline: 'none' }} />
                                    ))}
                                </Pie>
                                <Tooltip
                                    contentStyle={{ borderRadius: '8px', border: '1px solid #E5E7EB', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                            </PieChart>
                        </div>
                        <div className="mt-8 grid grid-cols-2 gap-x-6 gap-y-3 text-sm px-2">
                            {seniorityData.map((entry, index) => (
                                <div key={entry.name} className="flex items-center gap-2">
                                    <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: COLORS[index % COLORS.length] }}></div>
                                    <span className="text-gray-600 font-medium whitespace-nowrap">{entry.name}</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Hiring Companies Table */}
                <div className="lg:col-span-2 rounded-xl border border-gray-200 bg-white shadow-sm flex flex-col">
                    <div className="border-b border-gray-100 p-6">
                        <h2 className="text-lg font-bold text-slate-900">Actively Hiring Companies</h2>
                    </div>
                    <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-50 text-gray-500 sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active {formatTechName(techId)} Roles</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {mockCompanyLeaderboard.map((company) => (
                                    <tr key={company.id} className="hover:bg-gray-50 transition-colors group">
                                        <td className="px-6 py-4">
                                            <Link to={"/company/" + company.id} className="flex items-center gap-3">
                                                <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-gray-200 bg-white font-bold text-slate-700 shadow-sm group-hover:border-blue-300 group-hover:text-blue-600 transition-colors">
                                                    {company.logo}
                                                </div>
                                                <span className="font-semibold text-slate-900 group-hover:text-blue-600 transition-colors">{company.name}</span>
                                            </Link>
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-1 text-sm font-semibold text-blue-700">
                                                <Briefcase className="h-4 w-4" />
                                                {Math.floor(company.activeRoles * 0.3)} {/* Fake number for demo */}
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
