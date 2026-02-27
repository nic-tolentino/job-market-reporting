import React from 'react';

export type BadgeVariant = 'slate' | 'blue' | 'purple' | 'emerald' | 'gray';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
    variant?: BadgeVariant;
    icon?: React.ReactNode;
    children: React.ReactNode;
}

const variantStyles: Record<BadgeVariant, string> = {
    slate: 'bg-slate-100 text-slate-700 border-slate-200',
    blue: 'bg-blue-50 text-blue-700 border-blue-100',
    purple: 'bg-purple-50 text-purple-700 border-purple-100',
    emerald: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    gray: 'bg-gray-100 text-gray-600 border-gray-200',
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
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : onClick
                        ? 'bg-slate-50 border-slate-100 text-slate-600 hover:border-blue-200 hover:text-blue-700'
                        : 'bg-slate-50 border-slate-100 text-slate-600'
                }
            `}
        >
            {tech}
            {deletable && active && <span className="text-blue-400 hover:text-blue-600">×</span>}
        </Component>
    );
}
