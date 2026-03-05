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
        };
    }, [resolvedTheme]);
}
