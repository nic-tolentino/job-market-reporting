export default function LandingPage() {
    return (
        <div className="space-y-8">
            {/* Hero Section */}
            <section className="text-center py-10">
                <h1 className="text-4xl font-extrabold tracking-tight text-slate-900 sm:text-5xl">
                    Engineering Job Market Insights
                </h1>
                <p className="mx-auto mt-4 max-w-2xl text-lg text-gray-600">
                    Discover who is hiring, what tech they use, and how the market is trending across AU, NZ, and ES.
                </p>
            </section>

            {/* High-Level Stats Cards */}
            <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                    <p className="text-sm font-medium text-gray-500">Tracked Vacancies</p>
                    <p className="mt-2 text-3xl font-bold text-slate-900">4,281</p>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                    <p className="text-sm font-medium text-gray-500">Top Technology</p>
                    <p className="mt-2 text-3xl font-bold text-slate-900">React</p>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                    <p className="text-sm font-medium text-gray-500">Remote Percentage</p>
                    <p className="mt-2 text-3xl font-bold text-slate-900">34%</p>
                </div>
            </section>

            {/* Placeholder for Leaderboards */}
            <section className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm min-h-[400px]">
                    <h2 className="text-lg font-bold text-slate-900 border-b border-gray-100 pb-4 mb-4">Top Technologies</h2>
                    <div className="flex h-64 items-center justify-center text-sm text-gray-400">Chart Placeholder</div>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm min-h-[400px]">
                    <h2 className="text-lg font-bold text-slate-900 border-b border-gray-100 pb-4 mb-4">Top Companies</h2>
                    <div className="flex h-64 items-center justify-center text-sm text-gray-400">List Placeholder</div>
                </div>
            </section>
        </div>
    );
}
