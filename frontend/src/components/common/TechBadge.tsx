import React from 'react';

interface TechBadgeProps {
    name: string;
    size?: 'sm' | 'md' | 'lg';
    className?: string;
    onClick?: () => void;
    selected?: boolean;
}

const TechBadge: React.FC<TechBadgeProps> = ({
    name,
    size = 'md',
    className = '',
    onClick,
    selected = false
}) => {
    const sizeClasses = {
        sm: 'text-xs px-2 py-1',
        md: 'text-sm px-3 py-1.5',
        lg: 'text-base px-4 py-2'
    };

    const interactiveClasses = onClick
        ? 'cursor-pointer transition-colors ' + (selected ? 'bg-accent text-inverted border-accent' : 'bg-inset text-secondary border-border hover:bg-surface-hover hover:text-primary')
        : 'bg-inset text-secondary border-border';

    return (
        <span
            onClick={onClick}
            className={`inline-flex items-center justify-center font-medium rounded-md border ${interactiveClasses} ${sizeClasses[size]} ${className}`}
        >
            {name}
        </span>
    );
};

export default TechBadge;
