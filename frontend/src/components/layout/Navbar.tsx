import { Link } from 'react-router-dom';
import { Search, Briefcase } from 'lucide-react';

export default function Navbar() {
    return (
        <nav className="sticky top-0 z-50 w-full border-b border-gray-200 bg-white shadow-sm">
            <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6 lg:px-8">
                <div className="flex items-center gap-2">
                    <Link to="/" className="flex items-center gap-2 text-xl font-bold tracking-tight text-slate-900 hover:text-blue-600 transition-colors">
                        <Briefcase className="h-6 w-6 text-blue-600" />
                        JobMarket Pulse
                    </Link>
                </div>

                <div className="flex flex-1 items-center justify-center px-6">
                    <div className="relative w-full max-w-lg hidden md:block">
                        <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                            <Search className="h-4 w-4 text-gray-400" />
                        </div>
                        <input
                            type="text"
                            className="block w-full rounded-full border-0 py-2 pl-10 pr-4 text-sm text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 bg-gray-50 hover:bg-white transition-all outline-none"
                            placeholder="Search companies, tech..."
                        />
                    </div>
                </div>

                <div className="flex items-center gap-6 text-sm font-semibold text-gray-600">
                    <Link to="/" className="hover:text-blue-600 transition-colors">Home</Link>
                    <a href="#" className="hover:text-blue-600 transition-colors">About</a>
                </div>
            </div>
        </nav>
    );
}
