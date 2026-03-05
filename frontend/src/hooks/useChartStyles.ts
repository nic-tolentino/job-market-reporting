import { useMemo } from 'react';
import { useTheme } from 'next-themes';

export function useChartStyles() {
    const { resolvedTheme } = useTheme();

    return useMemo(() => {
        const style = getComputedStyle(document.documentElement);
        const get = (v: string) => style.getPropertyValue(v).trim();

        return {
            tooltipStyle: {
                backgroundColor: get('--theme-card'),
                borderColor: get('--theme-border'),
                borderRadius: '8px',
                border: `1px solid ${get('--theme-border')}`,
                boxShadow: get('--theme-shadow-md'),
                color: get('--theme-text-primary'),
            },
            gridStroke: get('--theme-border-subtle'),
            axisTickFill: get('--theme-text-muted'),
            // Brand-aware chart colors
            barColors: {
                primary: resolvedTheme === 'dark' ? '#3B82F6' : '#2563EB',
                secondary: resolvedTheme === 'dark' ? '#94A3B8' : '#64748B', // Adjusted for contrast
            },
            pieColors: [
                '#6366F1', // Indigo 500
                '#8B5CF6', // Violet 500
                '#10B981', // Emerald 500
                '#F59E0B', // Amber 500
                '#3B82F6', // Blue 500
            ],
        };
    }, [resolvedTheme]);
}
