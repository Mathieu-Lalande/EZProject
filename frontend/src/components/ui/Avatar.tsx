import clsx from 'clsx';
import { initials } from '../../lib/format';

const palette = ['bg-indigo-500', 'bg-emerald-500', 'bg-amber-500', 'bg-rose-500', 'bg-sky-500', 'bg-violet-500', 'bg-teal-500'];

export function Avatar({ name, size = 'md', className }: { name: string; size?: 'xs' | 'sm' | 'md'; className?: string }) {
  const color = palette[[...name].reduce((acc, c) => acc + c.charCodeAt(0), 0) % palette.length];
  return (
    <span
      title={name}
      className={clsx(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white ring-2 ring-surface',
        color,
        size === 'xs' && 'size-5 text-[9px]',
        size === 'sm' && 'size-6 text-[10px]',
        size === 'md' && 'size-8 text-xs',
        className,
      )}
    >
      {initials(name)}
    </span>
  );
}
