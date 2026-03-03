import { useState, useMemo } from 'react';
import { Search, X, Users, Star, Calendar, ArrowUpDown, Filter, ChevronUp } from 'lucide-react';
import { type Resource } from '../../constants/techResources';

interface ResourceModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    items: Resource[];
    icon: any;
}

const ResourceModalItem = ({ item }: { item: Resource }) => {
    const [upvoted, setUpvoted] = useState(false);
    // Same mock logic for stable upvotes
    const baseUpvotes = item.title.length * 3 + 12;

    return (
        <div className="group/item border-b border-slate-100 last:border-0 hover:bg-slate-50 transition-colors flex items-stretch">
            {/* Upvote Column */}
            <button
                onClick={(e) => { e.preventDefault(); setUpvoted(!upvoted); }}
                className={`flex flex-col items-center justify-start p-4 pr-2 border-r border-transparent transition-colors ${upvoted ? 'bg-blue-50/30 border-r-blue-100' : 'hover:bg-slate-100/50'}`}
            >
                <ChevronUp className={`h-6 w-6 -mb-1 transition-transform ${upvoted ? 'text-blue-600 scale-110' : 'text-slate-400'}`} />
                <span className={`text-xs font-bold ${upvoted ? 'text-blue-600' : 'text-slate-500'}`}>
                    {baseUpvotes + (upvoted ? 1 : 0)}
                </span>
            </button>

            <a
                href={item.url}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 flex items-start gap-4 p-4 pl-1"
            >
                <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                        <h4 className="font-bold text-slate-900 group-hover/item:text-blue-600 transition-colors">
                            {item.title}
                        </h4>
                        <div className="flex items-center gap-2">
                            {item.subscribers && (
                                <span className="flex items-center gap-1 text-[10px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-full border border-red-100">
                                    <Users className="h-2.5 w-2.5" />
                                    {item.subscribers}
                                </span>
                            )}
                            {item.stars && (
                                <span className="flex items-center gap-1 text-[10px] font-bold text-amber-600 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-100">
                                    <Star className="h-2.5 w-2.5 fill-amber-500" />
                                    {item.stars}
                                </span>
                            )}
                        </div>
                    </div>
                    <p className="text-sm text-slate-500 mt-1 line-clamp-2 md:line-clamp-none leading-relaxed">
                        {item.description}
                    </p>
                    {item.date && (
                        <div className="mt-2 flex items-center gap-1.5 text-xs text-indigo-600 font-semibold bg-indigo-50/50 w-fit px-2 py-1 rounded-md border border-indigo-100/50">
                            <Calendar className="h-3 w-3" />
                            {item.date}
                        </div>
                    )}
                </div>
            </a>
        </div>
    );
};

export const ResourceModal = ({ isOpen, onClose, title, items, icon: Icon }: ResourceModalProps) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [sortBy, setSortBy] = useState<'default' | 'title' | 'metric'>('default');

    const filteredAndSortedItems = useMemo(() => {
        let result = items.filter(item =>
            item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
            item.description.toLowerCase().includes(searchQuery.toLowerCase())
        );

        if (sortBy === 'title') {
            result.sort((a, b) => a.title.localeCompare(b.title));
        } else if (sortBy === 'metric') {
            const getVal = (s?: string) => {
                if (!s) return 0;
                let val = parseFloat(s);
                if (s.includes('M')) val *= 1000000;
                else if (s.includes('k')) val *= 1000;
                return val;
            };
            result.sort((a, b) => (getVal(b.stars || b.subscribers)) - (getVal(a.stars || a.subscribers)));
        }

        return result;
    }, [items, searchQuery, sortBy]);

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 md:p-6 animate-in fade-in duration-200">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
                onClick={onClose}
            />

            {/* Modal Content */}
            <div className="relative bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[85vh] overflow-hidden flex flex-col animate-in zoom-in-95 duration-300">
                {/* Header */}
                <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-600 text-white rounded-xl">
                            <Icon className="h-5 w-5" />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-slate-900">{title}</h2>
                            <p className="text-xs text-slate-400 font-medium">Directory • {filteredAndSortedItems.length} items</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-slate-200 rounded-full transition-colors group"
                    >
                        <X className="h-5 w-5 text-slate-400 group-hover:text-slate-600" />
                    </button>
                </div>

                {/* Filters */}
                <div className="p-4 border-b border-slate-100 bg-white grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Find resource..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all font-medium"
                        />
                    </div>
                    <div className="flex items-center gap-2">
                        <ArrowUpDown className="h-4 w-4 text-slate-400" />
                        <select
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value as any)}
                            className="flex-1 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/20 transition-all font-medium text-slate-600 appearance-none bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20fill%3D%22none%22%20viewBox%3D%220%200%2020%2020%22%3E%3Cpath%20stroke%3D%22%236b7280%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%20stroke-width%3D%221.5%22%20d%3D%22m6%208%204%204%204-4%22%2F%3E%3C%2Fsvg%3E')] bg-[length:1.25em_1.25em] bg-[right_0.5rem_center] bg-no-repeat pr-10"
                        >
                            <option value="default">Release Relevance</option>
                            <option value="title">Alphabetical</option>
                            <option value="metric">Top Metric (Subscribers/Stars)</option>
                        </select>
                    </div>
                </div>

                {/* List Area */}
                <div className="flex-1 overflow-y-auto bg-white custom-scrollbar">
                    {filteredAndSortedItems.length > 0 ? (
                        <div className="divide-y divide-slate-50">
                            {filteredAndSortedItems.map((item, idx) => (
                                <ResourceModalItem key={idx} item={item} />
                            ))}
                        </div>
                    ) : (
                        <div className="py-20 flex flex-col items-center justify-center text-slate-400">
                            <Filter className="h-12 w-12 opacity-20 mb-4" />
                            <p className="font-medium">No resources match your search</p>
                            <button
                                onClick={() => setSearchQuery('')}
                                className="mt-2 text-blue-600 font-bold hover:underline text-sm"
                            >
                                Clear search
                            </button>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="px-6 py-4 border-t border-slate-100 bg-slate-50/50 flex items-center justify-between">
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest">TechMarket Index • {items.length} Fully Vetted Resources</p>
                    <button
                        onClick={onClose}
                        className="px-4 py-2 bg-slate-900 text-white rounded-xl text-xs font-bold shadow-lg shadow-slate-200 hover:shadow-xl transition-all active:scale-95"
                    >
                        Close Directory
                    </button>
                </div>
            </div>
        </div>
    );
};
