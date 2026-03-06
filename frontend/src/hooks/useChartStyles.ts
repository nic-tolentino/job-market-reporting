import { useMemo } from 'react';
import { useTheme } from 'next-themes';

export function useChartStyles() {
    const { resolvedTheme } = useTheme();

    return useMemo(() => {
        const style = getComputedStyle(document.documentElement);
        const get = (v: string) => style.getPropertyValue(v).trim();

        return {
            tooltipStyle: {
                backgroundColor: resolvedTheme === 'dark' ? '#1E293B' : '#FFFFFF',
                borderColor: resolvedTheme === 'dark' ? '#475569' : '#E2E8F0',
                borderRadius: '8px',
                border: `1px solid ${resolvedTheme === 'dark' ? '#475569' : '#E2E8F0'}`,
                boxShadow: get('--theme-shadow-md'),
                color: resolvedTheme === 'dark' ? '#F1F5F9' : '#1E293B',
            },
            tooltipItemStyle: {
                color: resolvedTheme === 'dark' ? '#F1F5F9' : '#1E293B',
            },
            gridStroke: resolvedTheme === 'dark' ? '#334155' : '#F1F5F9',
            axisTickFill: resolvedTheme === 'dark' ? '#94A3B8' : '#94A3B8',
            // Brand-aware chart colors
            barColors: {
                primary: resolvedTheme === 'dark' ? '#3B82F6' : '#2563EB',
                secondary: resolvedTheme === 'dark' ? '#CBD5E1' : '#64748B', // Adjusted for contrast
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
