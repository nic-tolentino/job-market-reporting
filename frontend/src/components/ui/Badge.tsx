import React from 'react';

export type BadgeVariant = 'slate' | 'blue' | 'purple' | 'emerald' | 'gray';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
    variant?: BadgeVariant;
    icon?: React.ReactNode;
    children: React.ReactNode;
}

const variantStyles: Record<BadgeVariant, string> = {
    slate: 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800/80 dark:text-slate-200 dark:border-slate-700',
    blue: 'bg-blue-50 text-blue-700 border-blue-100 dark:bg-blue-500/20 dark:text-blue-200 dark:border-blue-500/40',
    purple: 'bg-purple-50 text-purple-700 border-purple-100 dark:bg-purple-500/20 dark:text-purple-200 dark:border-purple-500/40',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-100 dark:bg-emerald-500/20 dark:text-emerald-200 dark:border-emerald-500/40',
    gray: 'bg-gray-100 text-gray-600 border-gray-200 dark:bg-slate-800/80 dark:text-slate-200 dark:border-slate-700',
};

export function Badge({ variant = 'slate', icon, children, className = '', ...props }: BadgeProps) {
    return (
        <span
            className={`pill-badge inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold ${variantStyles[variant]} ${className}`}
            {...props}
        >
            {icon && <span className="flex-shrink-0">{icon}</span>}
            {children}
        </span>
    );
}

export function TechBadge({ tech, onClick, active, deletable }: { tech: string, onClick?: () => void, active?: boolean, deletable?: boolean }) {
    const Component = onClick ? 'button' : 'span';
    return (
        <Component
            onClick={onClick}
            className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold transition-colors
                ${active
                    ? 'border-accent bg-accent-subtle text-accent dark:bg-blue-900/40 dark:text-blue-200'
                    : onClick
                        ? 'bg-elevated border-border-subtle text-secondary hover:border-accent hover:text-accent dark:bg-slate-800/50'
                        : 'bg-elevated border-border-subtle text-secondary dark:bg-slate-800/50'
                }
            `}
        >
            {tech}
            {deletable && active && <span className="text-accent/60 hover:text-accent">×</span>}
        </Component>
    );
}
