import clsx from 'clsx';

export function ProgressBar({ value, color, className }: { value: number; color?: string; className?: string }) {
  const pct = Math.max(0, Math.min(100, value));
  return (
    <div
      className={clsx('h-1.5 w-full overflow-hidden rounded-full bg-surface-2', className)}
      role="progressbar"
      aria-valuenow={pct}
      aria-valuemin={0}
      aria-valuemax={100}
    >
      <div
        className="h-full rounded-full bg-brand-500 transition-[width] duration-500"
        style={{ width: `${pct}%`, backgroundColor: color }}
      />
    </div>
  );
}
