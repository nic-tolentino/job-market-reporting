import { Card, CardHeader } from '../components/ui/Card';
import { H2 } from '../components/ui/Typography';

export default function TransparencyPage() {
    return (
        <div className="max-w-4xl mx-auto space-y-10 py-8 md:py-12">
            <section className="text-center px-4">
                <h1 className="text-3xl md:text-5xl font-extrabold tracking-tight text-primary leading-tight">
                    Transparency & Mission
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-secondary">
                    Our commitment to the AU/NZ engineering community.
                </p>
            </section>

            <Card className="p-0 overflow-hidden">
                <CardHeader className="bg-elevated border-b border-border-subtle">
                    <H2>1. Source Disclosure</H2>
                </CardHeader>
                <div className="p-6 md:p-8 prose prose-slate dark:prose-invert max-w-none text-secondary leading-relaxed">
                    <p>
                        DevAssembly is a search and discovery utility. We aggregate data from publicly available job listings on major platforms including <strong>LinkedIn</strong> and <strong>Seek</strong>.
                    </p>
                    <p className="mt-4">
                        We don't "own" the listings; we provide a centralized, insights-driven lens to help developers find high-quality opportunities in the Australia, New Zealand, and Spain regions.
                    </p>
                </div>
            </Card>

            <Card className="p-0 overflow-hidden">
                <CardHeader className="bg-elevated border-b border-border-subtle">
                    <H2>2. Processing Ethics & Privacy</H2>
                </CardHeader>
                <div className="p-6 md:p-8 text-secondary leading-relaxed">
                    <p>
                        Your privacy, and the privacy of recruiters, is paramount. Our automated ingestion pipeline is designed with "Sanitization First" principles:
                    </p>
                    <ul className="mt-4 space-y-3 list-disc pl-5">
                        <li><strong className="text-primary">Personal Data Scrubbing:</strong> Recruiter names, individual email addresses, and phone numbers are stripped from descriptions before they ever reach our primary database.</li>
                        <li><strong className="text-primary">Business Facts Only:</strong> We only store professional data points—Job Titles, Tech Stacks, Salary Bands, and Company Metrics.</li>
                        <li><strong className="text-primary">No Data Sales:</strong> We will never sell your browsing history or contact information to third-party data brokers.</li>
                    </ul>
                </div>
            </Card>

            <Card className="p-0 overflow-hidden">
                <CardHeader className="bg-elevated border-b border-border-subtle">
                    <H2>3. Live Financial Ledger</H2>
                </CardHeader>
                <div className="p-6 md:p-8">
                    <div className="overflow-x-auto">
                        <table className="w-full text-left text-sm border-collapse">
                            <thead>
                                <tr className="border-b border-border">
                                    <th className="py-3 font-semibold text-primary">Item</th>
                                    <th className="py-3 font-semibold text-primary">Description</th>
                                    <th className="py-3 text-right font-semibold text-primary">Monthly Cost (EST)</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-border-subtle">
                                <tr>
                                    <td className="py-4 font-medium text-secondary">GCP Hosting</td>
                                    <td className="py-4 text-muted">Cloud Run & BigQuery storage</td>
                                    <td className="py-4 text-right text-primary">$0.00</td>
                                </tr>
                                <tr>
                                    <td className="py-4 font-medium text-secondary">Data Pipeline</td>
                                    <td className="py-4 text-muted">Apify usage & Credits</td>
                                    <td className="py-4 text-right text-primary">$40.00 (Free Tier)</td>
                                </tr>
                                <tr>
                                    <td className="py-4 font-medium text-secondary">Domains</td>
                                    <td className="py-4 text-muted">Project domains & SSL</td>
                                    <td className="py-4 text-right text-primary">$2.00</td>
                                </tr>
                                <tr className="bg-blue-50/50 dark:bg-blue-500/10">
                                    <td className="py-4 font-bold text-blue-900 dark:text-blue-300" colSpan={2}>Total Operating Expense</td>
                                    <td className="py-4 text-right font-bold text-blue-900 dark:text-blue-300">$42.00</td>
                                </tr>
                                <tr className="bg-emerald-50/50 dark:bg-emerald-500/10">
                                    <td className="py-4 font-bold text-emerald-900 dark:text-emerald-300" colSpan={2}>Current Revenue</td>
                                    <td className="py-4 text-right font-bold text-emerald-900 dark:text-emerald-300">$0.00</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <p className="mt-4 text-xs text-muted italic text-center">
                        Last updated: February 2026. This ledger is updated monthly.
                    </p>
                </div>
            </Card>

            <Card className="p-0 overflow-hidden border-blue-200 dark:border-blue-500/30 shadow-blue-50 dark:shadow-blue-500/5 shadow-lg">
                <CardHeader className="bg-blue-600 border-b border-blue-700">
                    <H2 className="text-white">4. Community Reinvestment Plan</H2>
                </CardHeader>
                <div className="p-6 md:p-8 text-secondary leading-relaxed bg-blue-50/30 dark:bg-blue-500/5">
                    <p className="font-bold text-lg text-blue-900 dark:text-blue-300 mb-4">
                        "100% of future surplus funds go back to the developers."
                    </p>
                    <p>
                        As DevAssembly grows, if we ever generate more revenue than it costs to keep the lights on, that money belongs to the community. Our plan includes:
                    </p>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
                        <div className="bg-card p-4 rounded-xl border border-blue-100 dark:border-blue-500/20 shadow-theme-sm">
                            <h3 className="font-bold text-blue-900 dark:text-blue-300 mb-1">Scholarships</h3>
                            <p className="text-sm text-muted">Helping junior devs in AU/NZ attend international tech conferences or bootcamps.</p>
                        </div>
                        <div className="bg-card p-4 rounded-xl border border-blue-100 dark:border-blue-500/20 shadow-theme-sm">
                            <h3 className="font-bold text-blue-900 dark:text-blue-300 mb-1">Local Events</h3>
                            <p className="text-sm text-muted">Sponsoring GDG (Google Developer Groups) and local meetups across Auckland, Sydney, and Melbourne.</p>
                        </div>
                        <div className="bg-card p-4 rounded-xl border border-blue-100 dark:border-blue-500/20 shadow-theme-sm">
                            <h3 className="font-bold text-blue-900 dark:text-blue-300 mb-1">Interview Support</h3>
                            <p className="text-sm text-muted">Bulk LeetCode subscriptions or prep materials for devs transitioning roles.</p>
                        </div>
                        <div className="bg-card p-4 rounded-xl border border-blue-100 dark:border-blue-500/20 shadow-theme-sm">
                            <h3 className="font-bold text-blue-900 dark:text-blue-300 mb-1">Open Source</h3>
                            <p className="text-sm text-muted">Bounties for fixes or features on AU/NZ-led open source projects.</p>
                        </div>
                    </div>
                </div>
            </Card>
        </div>
    );
}
