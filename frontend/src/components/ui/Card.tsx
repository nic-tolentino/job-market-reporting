import React from 'react';

export function Card({ children, className = '', id, ...props }: { children: React.ReactNode; className?: string; id?: string;[key: string]: any }) {
    return (
        <div id={id} className={`bg-card rounded-xl border border-border shadow-theme-sm overflow-hidden flex flex-col ${className}`} {...props}>
            {children}
        </div>
    );
}

export function CardHeader({ children, className = '' }: { children: React.ReactNode; className?: string }) {
    return (
        <div className={`border-b border-border-subtle p-6 flex flex-wrap items-center justify-between gap-4 ${className}`}>
            {children}
        </div>
    );
}

export function CardContent({ children, className = '' }: { children: React.ReactNode; className?: string }) {
    return (
        <div className={`p-6 ${className}`}>
            {children}
        </div>
    );
}
