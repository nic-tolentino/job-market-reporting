import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { fetchSearchSuggestions, trackSearchMiss } from '../../lib/api';

interface SearchBoxProps {
    placeholder?: string;
    className?: string;
    inputClassName?: string;
}

export default function SearchBox({
    placeholder = "Search companies, tech...",
    className = "",
    inputClassName = ""
}: SearchBoxProps) {
    const navigate = useNavigate();
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearchFocused, setIsSearchFocused] = useState(false);
    const searchRef = useRef<HTMLDivElement>(null);

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
            if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
                setIsSearchFocused(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleSuggestionClick = (suggestion: any) => {
        setIsSearchFocused(false);
        setSearchQuery('');
        if (suggestion.type === 'COMPANY') {
            navigate(`/company/${suggestion.id}`);
        } else {
            navigate(`/tech/${suggestion.id}`);
        }
    };

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

    return (
        <div className={`relative w-full rounded-full bg-white ${className}`} ref={searchRef}>
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4">
                <Search className="h-5 w-5 text-gray-400" />
            </div>
            <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => setIsSearchFocused(true)}
                onKeyDown={handleSearchSubmit}
                className={`block w-full rounded-full border-0 py-3 pl-12 pr-4 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 bg-transparent hover:bg-white transition-all outline-none ${inputClassName}`}
                placeholder={placeholder}
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
    );
}
