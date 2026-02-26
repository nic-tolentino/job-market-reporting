import { useParams } from 'react-router-dom';

export default function TechDetailsPage() {
    const { techId } = useParams<{ techId: string }>();

    return (
        <div className="space-y-8">
            <div>
                <h1 className="text-3xl font-bold capitalize text-slate-900">{techId || 'Technology'} Jobs</h1>
                <p className="text-gray-600 mt-2 text-lg">Market breakdown and actively hiring companies.</p>
            </div>

            <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                <h2 className="text-lg font-bold text-slate-900 border-b border-gray-100 pb-4 mb-4">Seniority Distribution</h2>
                <div className="flex h-48 items-center justify-center text-sm text-gray-400">Donut Chart Placeholder</div>
            </div>

            <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
                <h2 className="text-lg font-bold text-slate-900 border-b border-gray-100 pb-4 mb-4">Actively Hiring Companies</h2>
                <div className="flex h-48 items-center justify-center text-sm text-gray-400">Data Table Placeholder</div>
            </div>
        </div>
    );
}
