import React from 'react';

export function H1({ children, className = '' }: { children: React.ReactNode; className?: string }) {
    return (
        <h1 className={`text-3xl font-bold text-slate-900 ${className}`}>
            {children}
        </h1>
    );
}

export function H2({ children, className = '', id }: { children: React.ReactNode; className?: string; id?: string }) {
    return (
        <h2 id={id} className={`text-lg font-bold text-slate-900 ${className}`}>
            {children}
        </h2>
    );
}

export function SectionSubtitle({ children, className = '' }: { children: React.ReactNode; className?: string }) {
    return (
        <h3 className={`text-xs font-bold text-gray-400 uppercase tracking-widest ${className}`}>
            {children}
        </h3>
    );
}
