import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Briefcase, Sun, Moon, Monitor } from 'lucide-react';
import { useTheme } from 'next-themes';
import Dropdown from '../ui/Dropdown';
import SearchBox from '../common/SearchBox';

const countries = [
    { code: 'AU', name: 'Australia', flag: '🇦🇺' },
    { code: 'NZ', name: 'New Zealand', flag: '🇳🇿' },
    { code: 'ES', name: 'Spain', flag: '🇪🇸' },
];

function ThemeToggle() {
    const { theme, setTheme } = useTheme();
    const next = theme === 'light' ? 'dark' : theme === 'dark' ? 'system' : 'light';
    const Icon = theme === 'dark' ? Moon : theme === 'light' ? Sun : Monitor;

    return (
        <button
            onClick={() => setTheme(next)}
            className="p-2 rounded-lg text-secondary hover:bg-surface-hover hover:text-primary transition-colors"
            aria-label={`Switch to ${next} theme`}
        >
            <Icon className="h-4 w-4" />
        </button>
    );
}

export default function Navbar() {
    const [selectedCountry, setSelectedCountry] = useState('NZ');
    const currentCountry = countries.find(c => c.code === selectedCountry);

    return (
        <nav className="sticky top-0 z-50 w-full border-b border-border bg-card shadow-theme-sm">
            <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
                <div className="flex items-center gap-2">
                    <Link to="/" className="flex items-center gap-2 text-xl font-bold tracking-tight text-primary hover:text-accent transition-colors">
                        <Briefcase className="h-6 w-6 text-accent flex-shrink-0" />
                        <span className="hidden sm:inline">DevAssembly</span>
                    </Link>
                </div>

                <div className="flex flex-1 items-center justify-center px-4 md:px-6">
                    <SearchBox
                        className="max-w-lg"
                        inputClassName="py-2 pl-10 pr-4 text-sm"
                    />
                </div>

                <div className="flex items-center gap-4 sm:gap-6 text-sm font-semibold text-secondary">
                    <Dropdown
                        value={selectedCountry}
                        onChange={setSelectedCountry}
                        options={countries.map(c => ({ value: c.code, label: c.name }))}
                        selectedLabel={currentCountry?.code}
                        icon={<span className="text-xl leading-none">{currentCountry?.flag}</span>}
                        className="min-w-0"
                    />
                    <ThemeToggle />
                    <Link to="/transparency" className="hover:text-accent transition-colors hidden sm:block">About</Link>
                </div>
            </div>
        </nav>
    );
}
