interface MetricCardProps {
  label: string;
  value: string | number | null;
  sub?: string;
  icon?: React.ReactNode;
  highlight?: 'green' | 'red' | 'yellow' | 'neutral';
}

const highlightMap = {
  green:   'text-green-600 dark:text-green-400',
  red:     'text-red-600 dark:text-red-400',
  yellow:  'text-yellow-600 dark:text-yellow-400',
  neutral: 'text-primary',
};

export function MetricCard({ label, value, sub, icon, highlight = 'neutral' }: MetricCardProps) {
  return (
    <div className="bg-card rounded-lg border border-border p-4">
      <div className="flex items-center justify-between mb-1">
        <p className="text-xs text-muted uppercase tracking-wide">{label}</p>
        {icon && <div className="text-muted">{icon}</div>}
      </div>
      <p className={`text-2xl font-bold ${highlightMap[highlight]}`}>
        {value ?? '—'}
      </p>
      {sub && <p className="text-xs text-muted mt-0.5">{sub}</p>}
    </div>
  );
}
