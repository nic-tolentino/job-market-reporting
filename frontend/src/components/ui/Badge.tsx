import React from 'react';

export type BadgeVariant = 'slate' | 'blue' | 'purple' | 'emerald' | 'gray';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
    variant?: BadgeVariant;
    icon?: React.ReactNode;
    children: React.ReactNode;
}

const variantStyles: Record<BadgeVariant, string> = {
    slate: 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-700/30 dark:text-slate-300 dark:border-slate-600/40',
    blue: 'bg-blue-50 text-blue-700 border-blue-100 dark:bg-blue-500/10 dark:text-blue-300 dark:border-blue-500/20',
    purple: 'bg-purple-50 text-purple-700 border-purple-100 dark:bg-purple-500/10 dark:text-purple-300 dark:border-purple-500/20',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/20',
    gray: 'bg-gray-100 text-gray-600 border-gray-200 dark:bg-gray-700/30 dark:text-gray-300 dark:border-gray-600/40',
};

export function Badge({ variant = 'slate', icon, children, className = '', ...props }: BadgeProps) {
    return (
        <span
            className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold ${variantStyles[variant]} ${className}`}
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
                    ? 'border-accent bg-accent-subtle text-accent dark:text-blue-300'
                    : onClick
                        ? 'bg-elevated border-border-subtle text-secondary hover:border-accent hover:text-accent'
                        : 'bg-elevated border-border-subtle text-secondary'
                }
            `}
        >
            {tech}
            {deletable && active && <span className="text-accent/60 hover:text-accent">×</span>}
        </Component>
    );
}
