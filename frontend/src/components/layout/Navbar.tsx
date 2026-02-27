import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Search, Briefcase, ChevronDown } from 'lucide-react';
import { fetchSearchSuggestions, trackSearchMiss } from '../../lib/api';
import { useQuery } from '@tanstack/react-query';

const countries = [
    { code: 'AU', name: 'Australia', flag: '🇦🇺' },
    { code: 'NZ', name: 'New Zealand', flag: '🇳🇿' },
    { code: 'ES', name: 'Spain', flag: '🇪🇸' },
];

export default function Navbar() {
    const navigate = useNavigate();
    const [selectedCountry, setSelectedCountry] = useState('NZ');
    const [isOpen, setIsOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearchFocused, setIsSearchFocused] = useState(false);

    const dropdownRef = useRef<HTMLDivElement>(null);
    const searchRef = useRef<HTMLDivElement>(null);
    const currentCountry = countries.find(c => c.code === selectedCountry);

    // Fetch master list of search suggestions globally
    const { data: searchData } = useQuery({
        queryKey: ['searchSuggestions'],
        queryFn: fetchSearchSuggestions,
        staleTime: 1000 * 60 * 60 * 24 // 24 hours
    });

    // Compute filtered suggestions based on typing
    const suggestions = (searchData?.suggestions || []).filter(s =>
        s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        s.id.toLowerCase().includes(searchQuery.toLowerCase())
    ).slice(0, 8); // Top 8 results max

    // Close dropdowns when clicking outside
    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
            if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
                setIsSearchFocused(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSearchSubmit = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' && searchQuery.trim() !== '') {
            // Check if exact match exists
            const exactMatch = suggestions.find(s => s.name.toLowerCase() === searchQuery.toLowerCase() || s.id.toLowerCase() === searchQuery.toLowerCase());

            if (exactMatch) {
                handleSuggestionClick(exactMatch);
            } else if (suggestions.length > 0) {
                // Navigate to top suggestion
                handleSuggestionClick(suggestions[0]);
            } else {
                // No match anywhere, fire a tracking miss request
                trackSearchMiss(searchQuery.trim());
                setIsSearchFocused(false);
                setSearchQuery('');
            }
        }
    };

    const handleSuggestionClick = (suggestion: any) => {
        setIsSearchFocused(false);
        setSearchQuery('');
        if (suggestion.type === 'COMPANY') {
            navigate(`/company/${suggestion.id}`);
        } else {
            navigate(`/tech/${suggestion.id}`);
        }
    };

    return (
        <nav className="sticky top-0 z-50 w-full border-b border-gray-200 bg-white shadow-sm">
            <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
                <div className="flex items-center gap-2">
                    <Link to="/" className="flex items-center gap-2 text-xl font-bold tracking-tight text-slate-900 hover:text-blue-600 transition-colors">
                        <Briefcase className="h-6 w-6 text-blue-600 flex-shrink-0" />
                        <span className="hidden sm:inline">TechMarket</span>
                    </Link>
                </div>

                <div className="flex flex-1 items-center justify-center px-4 md:px-6">
                    <div className="relative w-full max-w-lg" ref={searchRef}>
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                            <Search className="h-4 w-4 text-gray-400" />
                        </div>
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            onFocus={() => setIsSearchFocused(true)}
                            onKeyDown={handleSearchSubmit}
                            className="block w-full rounded-full border-0 py-2 pl-10 pr-4 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 bg-gray-50 hover:bg-white transition-all outline-none"
                            placeholder="Search companies, tech..."
                        />

                        {isSearchFocused && searchQuery.length > 0 && (
                            <div className="absolute left-0 right-0 mt-2 rounded-2xl border border-gray-200 bg-white shadow-xl overflow-hidden z-50 animate-in fade-in zoom-in duration-200">
                                {suggestions.length > 0 ? (
                                    <div className="py-2">
                                        {suggestions.map((s, i) => (
                                            <button
                                                key={`${s.type}-${s.id}-${i}`}
                                                onClick={() => handleSuggestionClick(s)}
                                                className="flex w-full items-center justify-between px-4 py-2.5 text-left text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                                            >
                                                <span className="font-semibold">{s.name}</span>
                                                <span className="text-xs font-medium text-gray-400 uppercase tracking-wider bg-gray-100 px-2 py-0.5 rounded-full">
                                                    {s.type === 'COMPANY' ? 'Company' : 'Tech'}
                                                </span>
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="px-4 py-8 text-center">
                                        <p className="text-sm font-semibold text-gray-900">No matching companies or tech</p>
                                        <p className="text-xs text-gray-500 mt-1">Press Enter to search anyway (we track these to add new ones!)</p>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                <div className="flex items-center gap-4 sm:gap-6 text-sm font-semibold text-gray-600">
                    <div className="relative" ref={dropdownRef}>
                        <button
                            onClick={() => setIsOpen(!isOpen)}
                            className="flex items-center gap-2 bg-gray-50 border border-gray-300 rounded-full pl-2 pr-2.5 py-1 hover:bg-white hover:border-blue-400 hover:shadow-sm transition-all group"
                        >
                            <span className="text-xl leading-none">{currentCountry?.flag}</span>
                            <span className="text-xs font-bold text-slate-700">{selectedCountry}</span>
                            <ChevronDown className={`h-3 w-3 text-gray-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
                        </button>

                        {isOpen && (
                            <div className="absolute right-0 mt-2 w-48 origin-top-right rounded-2xl border border-gray-300 bg-white shadow-xl ring-1 ring-black/5 focus:outline-none overflow-hidden z-50 animate-in fade-in zoom-in duration-200">
                                <div className="py-1.5">
                                    <div className="px-3 py-1.5 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                                        Select Region
                                    </div>
                                    {countries.map((c) => (
                                        <button
                                            key={c.code}
                                            onClick={() => {
                                                setSelectedCountry(c.code);
                                                setIsOpen(false);
                                            }}
                                            className={`flex w-full items-center gap-3 px-4 py-2.5 text-left transition-colors ${selectedCountry === c.code
                                                ? 'bg-blue-50 text-blue-700'
                                                : 'text-gray-700 hover:bg-gray-50'
                                                }`}
                                        >
                                            <span className="text-xl leading-none">{c.flag}</span>
                                            <span className="text-sm font-semibold">{c.name}</span>
                                            {selectedCountry === c.code && (
                                                <div className="ml-auto w-1.5 h-1.5 rounded-full bg-blue-600" />
                                            )}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                    <a href="#" className="hover:text-blue-600 transition-colors hidden sm:block">About</a>
                </div>
            </div>
        </nav>
    );
}
