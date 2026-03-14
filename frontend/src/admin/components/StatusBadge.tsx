interface StatusBadgeProps {
  status: string | null | undefined;
  size?: 'sm' | 'md';
}

const statusConfig: Record<string, { bg: string; text: string; label: string }> = {
  ACTIVE:  { bg: 'bg-green-100',  text: 'text-green-800',  label: 'Active'   },
  STALE:   { bg: 'bg-yellow-100', text: 'text-yellow-800', label: 'Stale'    },
  BLOCKED: { bg: 'bg-red-100',    text: 'text-red-800',    label: 'Blocked'  },
  TIMEOUT: { bg: 'bg-orange-100', text: 'text-orange-800', label: 'Timeout'  },
  FAILED:  { bg: 'bg-red-100',    text: 'text-red-800',    label: 'Failed'   },
  NONE:    { bg: 'bg-gray-100',   text: 'text-gray-500',   label: 'No seed'  },
};

export function StatusBadge({ status, size = 'sm' }: StatusBadgeProps) {
  const cfg = statusConfig[status ?? 'NONE'] ?? statusConfig['NONE'];
  const padding = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm';
  return (
    <span className={`inline-flex items-center rounded-full font-medium ${cfg.bg} ${cfg.text} ${padding}`}>
      {cfg.label}
    </span>
  );
}
