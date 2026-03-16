interface StatusBadgeProps {
  status: string | null | undefined;
  size?: 'sm' | 'md';
}

const statusConfig: Record<string, { bg: string; text: string; label: string }> = {
  ACTIVE:       { bg: 'bg-green-100 dark:bg-green-900/30',   text: 'text-green-800 dark:text-green-300',   label: 'Active'   },
  STALE:        { bg: 'bg-yellow-100 dark:bg-yellow-900/30', text: 'text-yellow-800 dark:text-yellow-300', label: 'Stale'    },
  BLOCKED:      { bg: 'bg-red-100 dark:bg-red-900/30',       text: 'text-red-800 dark:text-red-300',       label: 'Blocked'  },
  TIMEOUT:      { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-800 dark:text-orange-300', label: 'Timeout'  },
  FAILED:       { bg: 'bg-red-100 dark:bg-red-900/30',       text: 'text-red-800 dark:text-red-300',       label: 'Failed'   },
  COMPLETED:    { bg: 'bg-green-100 dark:bg-green-900/30',   text: 'text-green-800 dark:text-green-300',   label: 'Done'     },
  NONE:         { bg: 'bg-elevated',                         text: 'text-muted',                           label: 'No seed'  },
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
