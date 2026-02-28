import { useState, useRef, useEffect, type ReactNode } from 'react';
import { ChevronDown, Filter } from 'lucide-react';

interface Option {
    value: string;
    label: string;
}

interface DropdownProps {
    value: string;
    onChange: (value: string) => void;
    options: Option[];
    labelPrefix?: string;
    icon?: ReactNode;
    selectedLabel?: string;
    className?: string;
}

export default function Dropdown({ value, onChange, options, labelPrefix, icon, selectedLabel, className = "" }: DropdownProps) {
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    const selectedOption = options.find(opt => opt.value === value) || options[0];

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div className={`relative ${className}`} ref={dropdownRef}>
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                className={`flex items-center gap-2.5 bg-white border border-gray-200 rounded-lg px-3.5 py-2 hover:border-blue-400 hover:shadow-sm transition-all group text-left ${className.includes('min-w-') ? '' : 'min-w-[140px]'}`}
            >
                <div className="flex-shrink-0">
                    {icon || <Filter className="h-4 w-4 text-gray-400 group-hover:text-blue-500 transition-colors" />}
                </div>
                <div className="flex flex-col items-start overflow-hidden">
                    {labelPrefix && (
                        <span className="text-[9px] uppercase tracking-wider text-gray-400 font-bold leading-none mb-0.5">
                            {labelPrefix}
                        </span>
                    )}
                    <span className="text-sm font-semibold text-slate-700 leading-tight truncate w-full">
                        {selectedLabel || selectedOption?.label}
                    </span>
                </div>
                <ChevronDown className={`ml-auto h-4 w-4 text-gray-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {isOpen && (
                <div className="absolute right-0 lg:left-0 mt-2 w-56 origin-top-right lg:origin-top-left rounded-xl border border-gray-200 bg-white shadow-2xl ring-1 ring-black/5 focus:outline-none overflow-hidden z-50 animate-in fade-in zoom-in duration-200">
                    <div className="py-1.5 max-h-80 overflow-y-auto">
                        {options.map((opt) => (
                            <button
                                key={opt.value}
                                onClick={() => {
                                    onChange(opt.value);
                                    setIsOpen(false);
                                }}
                                className={`flex w-full items-center px-4 py-2.5 text-left transition-colors ${value === opt.value
                                    ? 'bg-blue-50 text-blue-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                    }`}
                            >
                                <span className="text-[13px] font-semibold">{opt.label}</span>
                                {value === opt.value && (
                                    <div className="ml-auto w-1.5 h-1.5 rounded-full bg-blue-600 shadow-[0_0_8px_rgba(37,99,235,0.4)]" />
                                )}
                            </button>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );
}
