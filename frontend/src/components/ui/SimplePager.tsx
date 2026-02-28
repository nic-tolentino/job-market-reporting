import { ChevronLeft, ChevronRight } from 'lucide-react';

interface SimplePagerProps {
    currentPage: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    className?: string;
}

export default function SimplePager({ currentPage, totalPages, onPageChange, className = "" }: SimplePagerProps) {
    if (totalPages <= 1) return null;

    return (
        <div className={`flex items-center gap-4 ${className}`}>
            <button
                onClick={() => onPageChange(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="p-2 rounded-full border border-gray-200 hover:bg-white hover:border-blue-400 hover:text-blue-600 disabled:opacity-30 disabled:hover:border-gray-200 disabled:hover:text-gray-400 transition-all group"
            >
                <ChevronLeft className="h-4 w-4" />
            </button>
            <div className="flex items-center gap-1.5 min-w-[3rem] justify-center">
                <span className="text-sm font-bold text-slate-700">{currentPage}</span>
                <span className="text-gray-400 font-medium text-sm">/</span>
                <span className="text-sm font-medium text-gray-500">{totalPages}</span>
            </div>
            <button
                onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="p-2 rounded-full border border-gray-200 hover:bg-white hover:border-blue-400 hover:text-blue-600 disabled:opacity-30 disabled:hover:border-gray-200 disabled:hover:text-gray-400 transition-all group"
            >
                <ChevronRight className="h-4 w-4" />
            </button>
        </div>
    );
}
