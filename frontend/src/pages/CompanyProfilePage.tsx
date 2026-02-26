import { useParams } from 'react-router-dom';

export default function CompanyProfilePage() {
    const { companyId } = useParams<{ companyId: string }>();

    return (
        <div className="space-y-8">
            <div className="flex items-start gap-6 border-b border-gray-200 pb-8">
                <div className="h-24 w-24 rounded-lg bg-gray-100 border border-gray-200 flex items-center justify-center flex-shrink-0">
                    <span className="text-gray-400 text-sm">Logo</span>
                </div>
                <div>
                    <h1 className="text-3xl font-bold capitalize text-slate-900">{companyId || 'Company'}</h1>
                    <p className="text-gray-600 mt-2 max-w-2xl">
                        Software company making amazing products. We're looking for great engineers to join us.
                    </p>
                    <div className="mt-4 flex gap-2">
                        <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700 ring-1 ring-inset ring-blue-700/10">Enterprise Software</span>
                        <span className="inline-flex items-center rounded-full bg-gray-50 px-2.5 py-0.5 text-xs font-medium text-gray-600 ring-1 ring-inset ring-gray-500/10">1,000+ Employees</span>
                    </div>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                <div className="md:col-span-2 space-y-8">
                    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                        <h2 className="text-lg font-bold text-slate-900 border-b border-gray-100 pb-4 mb-4">Tech Stack</h2>
                        <div className="flex flex-wrap gap-2">
                            <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-800">React</span>
                            <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-800">TypeScript</span>
                            <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-800">Kotlin</span>
                            <span className="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-800">PostgreSQL</span>
                        </div>
                    </div>

                    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                        <h2 className="text-lg font-bold text-slate-900 border-b border-gray-100 pb-4 mb-4">Active Roles</h2>
                        <div className="flex h-32 items-center justify-center text-sm text-gray-400">Data Table Placeholder</div>
                    </div>
                </div>

                <div className="space-y-4">
                    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                        <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider mb-4">Insights</h3>
                        <ul className="space-y-3 text-sm text-gray-600">
                            <li className="flex justify-between"><span>Work Model</span> <span className="font-semibold text-slate-900">Hybrid</span></li>
                            <li className="flex justify-between"><span>Locations</span> <span className="font-semibold text-slate-900">Sydney, Remote</span></li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    );
}
