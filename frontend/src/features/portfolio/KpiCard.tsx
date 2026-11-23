import type { ReactNode } from 'react';
import clsx from 'clsx';

export function KpiCard({ label, value, icon, tone = 'brand', hint }: { label: string; value: ReactNode; icon: ReactNode; tone?: 'brand' | 'red' | 'amber' | 'green'; hint?: string }) {
  const toneClass = {
    brand: 'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-300',
    red: 'bg-red-50 text-red-600 dark:bg-red-500/15 dark:text-red-300',
    amber: 'bg-amber-50 text-amber-600 dark:bg-amber-500/15 dark:text-amber-300',
    green: 'bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15 dark:text-emerald-300',
  }[tone];
  return (
    <div className="card p-4">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-ink-3">{label}</span>
        <span className={clsx('flex size-7 items-center justify-center rounded-lg', toneClass)}>{icon}</span>
      </div>
      <div className="mt-3 text-2xl font-semibold tracking-tight tabular-nums">{value}</div>
      {hint && <div className="mt-0.5 text-xs text-ink-3">{hint}</div>}
    </div>
  );
}
