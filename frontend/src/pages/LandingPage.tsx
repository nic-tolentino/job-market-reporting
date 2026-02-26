import { Link, useNavigate } from 'react-router-dom';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Briefcase } from 'lucide-react';
import { mockGlobalStats, mockTechLeaderboard, mockCompanyLeaderboard } from '../lib/mockData';

export default function LandingPage() {
    const navigate = useNavigate();

    return (
        <div className="space-y-10">
            {/* Hero Section */}
            <section className="text-center py-12">
                <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl">
                    Engineering Job Market Insights
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-600">
                    Discover who is hiring, what tech they use, and how the market is trending across AU, NZ, and ES.
                </p>
            </section>

            {/* High-Level Stats Cards */}
            <section className="grid grid-cols-1 gap-4 sm:grid-cols-4">
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm flex flex-col justify-between">
                    <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Tracked Vacancies</p>
                    <p className="mt-4 text-3xl font-bold text-slate-900">{mockGlobalStats.totalVacancies.toLocaleString()}</p>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm flex flex-col justify-between">
                    <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Top Technology</p>
                    <p className="mt-4 text-3xl font-bold text-slate-900">{mockGlobalStats.topTech}</p>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm flex flex-col justify-between">
                    <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Remote Roles</p>
                    <p className="mt-4 text-3xl font-bold text-slate-900">{mockGlobalStats.remotePercentage}%</p>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm flex flex-col justify-between">
                    <p className="text-sm font-medium text-gray-500 uppercase tracking-wider">Hybrid Roles</p>
                    <p className="mt-4 text-3xl font-bold text-slate-900">{mockGlobalStats.hybridPercentage}%</p>
                </div>
            </section>

            {/* Main Content Grid */}
            <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                {/* Top Tech Chart */}
                <div className="rounded-xl border border-gray-200 bg-white p-0 shadow-sm flex flex-col overflow-hidden">
                    <div className="border-b border-gray-100 p-6">
                        <h2 className="text-lg font-bold text-slate-900">Highest Demand Tech</h2>
                        <p className="text-sm text-gray-500 mt-1">Based on active job postings</p>
                    </div>
                    <div className="p-6 flex-1 min-h-[400px] outline-none select-none [&_svg]:outline-none" tabIndex={-1}>
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={mockTechLeaderboard} layout="vertical" margin={{ top: 0, right: 30, left: 20, bottom: 0 }}>
                                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#E5E7EB" />
                                <XAxis type="number" hide />
                                <YAxis dataKey="name" type="category" axisLine={false} tickLine={false} tick={{ fill: '#475569', fontSize: 13, fontWeight: 500 }} width={80} />
                                <Tooltip
                                    cursor={{ fill: '#F1F5F9' }}
                                    contentStyle={{ borderRadius: '8px', border: '1px solid #E5E7EB', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                                <Bar
                                    dataKey="count"
                                    radius={[0, 4, 4, 0]}
                                    barSize={32}
                                    onClick={(data) => {
                                        if (data && data.name) {
                                            navigate("/tech/" + data.name.toLowerCase());
                                        }
                                    }}
                                    style={{ cursor: 'pointer', outline: 'none' }}
                                >
                                    {mockTechLeaderboard.map((_entry, index) => (
                                        <Cell key={`cell-${index}`} fill={index === 0 ? '#2563EB' : '#94A3B8'} className="hover:opacity-80 transition-opacity" />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Top Companies List */}
                <div className="rounded-xl border border-gray-200 bg-white p-0 shadow-sm flex flex-col overflow-hidden">
                    <div className="border-b border-gray-100 p-6 flex justify-between items-end">
                        <div>
                            <h2 className="text-lg font-bold text-slate-900">Top Hiring Companies</h2>
                            <p className="text-sm text-gray-500 mt-1">Companies with the most active vacancies</p>
                        </div>
                    </div>
                    <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-sm">
                            <thead className="bg-gray-50 text-gray-500 sticky top-0">
                                <tr>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider">Company</th>
                                    <th className="px-6 py-3 font-medium uppercase tracking-wider text-right">Active Roles</th>
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
                                                {company.activeRoles}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>

            </section>
        </div>
    );
}
