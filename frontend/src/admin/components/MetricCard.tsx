interface MetricCardProps {
  label: string;
  value: string | number | null;
  sub?: string;
  highlight?: 'green' | 'red' | 'yellow' | 'neutral';
}

const highlightMap = {
  green:   'text-green-600',
  red:     'text-red-600',
  yellow:  'text-yellow-600',
  neutral: 'text-gray-900',
};

export function MetricCard({ label, value, sub, highlight = 'neutral' }: MetricCardProps) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4">
      <p className="text-xs text-gray-500 uppercase tracking-wide mb-1">{label}</p>
      <p className={`text-2xl font-bold ${highlightMap[highlight]}`}>
        {value ?? '—'}
      </p>
      {sub && <p className="text-xs text-gray-400 mt-0.5">{sub}</p>}
    </div>
  );
}
