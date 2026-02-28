import { Card, CardHeader } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';

export default function TransparencyPage() {
    return (
        <div className="max-w-4xl mx-auto space-y-10 py-8 md:py-12">
            <section className="text-center px-4">
                <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight text-slate-900 leading-tight">
                    Transparency & Mission
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-600">
                    Our commitment to the AU/NZ engineering community.
                </p>
            </section>

            <Card className="p-0 overflow-hidden">
                <CardHeader className="bg-gray-50 border-b border-gray-100">
                    <H2>1. Source Disclosure</H2>
                </CardHeader>
                <div className="p-6 md:p-8 prose prose-slate max-w-none text-gray-600 leading-relaxed">
                    <p>
                        TechMarket is a search and discovery utility. We aggregate data from publicly available job listings on major platforms including <strong>LinkedIn</strong> and <strong>Seek</strong>.
                    </p>
                    <p className="mt-4">
                        We don't "own" the listings; we provide a centralized, insights-driven lens to help developers find high-quality opportunities in the Australia, New Zealand, and Spain regions.
                    </p>
                </div>
            </Card>

            <Card className="p-0 overflow-hidden">
                <CardHeader className="bg-gray-50 border-b border-gray-100">
                    <H2>2. Processing Ethics & Privacy</H2>
                </CardHeader>
                <div className="p-6 md:p-8 text-gray-600 leading-relaxed">
                    <p>
                        Your privacy, and the privacy of recruiters, is paramount. Our automated ingestion pipeline is designed with "Sanitization First" principles:
                    </p>
                    <ul className="mt-4 space-y-3 list-disc pl-5">
                        <li><strong>Personal Data Scrubbing:</strong> Recruiter names, individual email addresses, and phone numbers are stripped from descriptions before they ever reach our primary database.</li>
                        <li><strong>Business Facts Only:</strong> We only store professional data points—Job Titles, Tech Stacks, Salary Bands, and Company Metrics.</li>
                        <li><strong>No Data Sales:</strong> We will never sell your browsing history or contact information to third-party data brokers.</li>
                    </ul>
                </div>
            </Card>

            <Card className="p-0 overflow-hidden">
                <CardHeader className="bg-gray-50 border-b border-gray-100">
                    <H2>3. Live Financial Ledger</H2>
                </CardHeader>
                <div className="p-6 md:p-8">
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm border-collapse">
                            <thead>
                                <tr className="border-b border-gray-200">
                                    <th className="py-3 font-semibold text-slate-900">Item</th>
                                    <th className="py-3 font-semibold text-slate-900">Description</th>
                                    <th className="py-3 text-right font-semibold text-slate-900">Monthly Cost (EST)</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                <tr>
                                    <td className="py-4 font-medium text-slate-700">GCP Hosting</td>
                                    <td className="py-4 text-gray-500">Cloud Run & BigQuery storage</td>
                                    <td className="py-4 text-right text-slate-900">$0.00</td>
                                </tr>
                                <tr>
                                    <td className="py-4 font-medium text-slate-700">Data Pipeline</td>
                                    <td className="py-4 text-gray-500">Apify usage & Credits</td>
                                    <td className="py-4 text-right text-slate-900">$40.00 (Free Tier)</td>
                                </tr>
                                <tr>
                                    <td className="py-4 font-medium text-slate-700">Domains</td>
                                    <td className="py-4 text-gray-500">Project domains & SSL</td>
                                    <td className="py-4 text-right text-slate-900">$2.00</td>
                                </tr>
                                <tr className="bg-blue-50/50">
                                    <td className="py-4 font-bold text-blue-900" colSpan={2}>Total Operating Expense</td>
                                    <td className="py-4 text-right font-bold text-blue-900">$42.00</td>
                                </tr>
                                <tr className="bg-emerald-50/50">
                                    <td className="py-4 font-bold text-emerald-900" colSpan={2}>Current Revenue</td>
                                    <td className="py-4 text-right font-bold text-emerald-900">$0.00</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <p className="mt-4 text-xs text-gray-400 italic text-center">
                        Last updated: February 2026. This ledger is updated monthly.
                    </p>
                </div>
            </Card>

            <Card className="p-0 overflow-hidden border-blue-200 shadow-blue-50 shadow-lg">
                <CardHeader className="bg-blue-600 border-b border-blue-700">
                    <H2 className="text-white">4. Community Reinvestment Plan</H2>
                </CardHeader>
                <div className="p-6 md:p-8 text-slate-700 leading-relaxed bg-blue-50/30">
                    <p className="font-bold text-lg text-blue-900 mb-4">
                        "100% of future surplus funds go back to the developers."
                    </p>
                    <p>
                        As TechMarket grows, if we ever generate more revenue than it costs to keep the lights on, that money belongs to the community. Our plan includes:
                    </p>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
                        <div className="bg-white p-4 rounded-xl border border-blue-100 shadow-sm">
                            <h3 className="font-bold text-blue-900 mb-1">Scholarships</h3>
                            <p className="text-sm text-gray-500">Helping junior devs in AU/NZ attend international tech conferences or bootcamps.</p>
                        </div>
                        <div className="bg-white p-4 rounded-xl border border-blue-100 shadow-sm">
                            <h3 className="font-bold text-blue-900 mb-1">Local Events</h3>
                            <p className="text-sm text-gray-500">Sponsoring GDG (Google Developer Groups) and local meetups across Auckland, Sydney, and Melbourne.</p>
                        </div>
                        <div className="bg-white p-4 rounded-xl border border-blue-100 shadow-sm">
                            <h3 className="font-bold text-blue-900 mb-1">Interview Support</h3>
                            <p className="text-sm text-gray-500">Bulk LeetCode subscriptions or prep materials for devs transitioning roles.</p>
                        </div>
                        <div className="bg-white p-4 rounded-xl border border-blue-100 shadow-sm">
                            <h3 className="font-bold text-blue-900 mb-1">Open Source</h3>
                            <p className="text-sm text-gray-500">Bounties for fixes or features on AU/NZ-led open source projects.</p>
                        </div>
                    </div>
                </div>
            </Card>
        </div>
    );
}
