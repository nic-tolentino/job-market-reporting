import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { fetchSearchSuggestions, trackSearchMiss } from '../../lib/api';
import { useAppStore } from '../../store/useAppStore';

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
    const { selectedCountry } = useAppStore();
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearchFocused, setIsSearchFocused] = useState(false);
    const searchRef = useRef<HTMLDivElement>(null);

    // Fetch master list of search suggestions globally
    const { data: searchData } = useQuery({
        queryKey: ['searchSuggestions', selectedCountry],
        queryFn: () => fetchSearchSuggestions(selectedCountry),
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
        <div className={`relative w-full rounded-full bg-card ${className}`} ref={searchRef}>
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-4">
                <Search className="h-5 w-5 text-muted" />
            </div>
            <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => setIsSearchFocused(true)}
                onKeyDown={handleSearchSubmit}
                className={`block w-full rounded-full border-0 py-3 pl-12 pr-4 text-primary ring-1 ring-inset ring-border placeholder:text-muted focus:ring-2 focus:ring-inset focus:ring-accent/20 bg-transparent hover:bg-card transition-all outline-none ${inputClassName}`}
                placeholder={placeholder}
            />

            {isSearchFocused && searchQuery.length > 0 && (
                <div className="absolute left-0 right-0 mt-2 rounded-2xl border border-border bg-card shadow-xl overflow-hidden z-50 animate-in fade-in zoom-in duration-200">
                    {suggestions.length > 0 ? (
                        <div className="py-2">
                            {suggestions.map((s, i) => (
                                <button
                                    key={`${s.type}-${s.id}-${i}`}
                                    onClick={() => handleSuggestionClick(s)}
                                    className="flex w-full items-center justify-between px-4 py-2.5 text-left text-sm text-secondary hover:bg-surface-hover transition-colors"
                                >
                                    <span className="font-semibold">{s.name}</span>
                                    <span className="text-xs font-medium text-muted uppercase tracking-wider bg-inset px-2 py-0.5 rounded-full">
                                        {s.type === 'COMPANY' ? 'Company' : 'Tech'}
                                    </span>
                                </button>
                            ))}
                        </div>
                    ) : (
                        <div className="px-4 py-8 text-center">
                            <p className="text-sm font-semibold text-primary">No matching companies or tech</p>
                            <p className="text-xs text-muted mt-1">Press Enter to search anyway (we track these to add new ones!)</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
