import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Briefcase } from 'lucide-react';
import Dropdown from '../ui/Dropdown';
import SearchBox from '../common/SearchBox';

const countries = [
    { code: 'AU', name: 'Australia', flag: '🇦🇺' },
    { code: 'NZ', name: 'New Zealand', flag: '🇳🇿' },
    { code: 'ES', name: 'Spain', flag: '🇪🇸' },
];

export default function Navbar() {
    const [selectedCountry, setSelectedCountry] = useState('NZ');
    const currentCountry = countries.find(c => c.code === selectedCountry);

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
                    <SearchBox
                        className="max-w-lg"
                        inputClassName="py-2 pl-10 pr-4 text-sm"
                    />
                </div>

                <div className="flex items-center gap-4 sm:gap-6 text-sm font-semibold text-gray-600">
                    <Dropdown
                        value={selectedCountry}
                        onChange={setSelectedCountry}
                        options={countries.map(c => ({ value: c.code, label: c.name }))}
                        selectedLabel={currentCountry?.code}
                        icon={<span className="text-xl leading-none">{currentCountry?.flag}</span>}
                        className="min-w-0"
                    />
                    <Link to="/transparency" className="hover:text-blue-600 transition-colors hidden sm:block">About</Link>
                </div>
            </div>
        </nav>
    );
}
