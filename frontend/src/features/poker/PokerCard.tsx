import clsx from 'clsx';

interface PokerCardProps {
  value: string;
  selected?: boolean;
  onSelect?: (value: string) => void;
  disabled?: boolean;
}

/** Carte du deck, sélectionnable. */
export function PokerCard({ value, selected, onSelect, disabled }: PokerCardProps) {
  return (
    <button
      type="button"
      disabled={disabled}
      aria-pressed={selected}
      onClick={() => onSelect?.(value)}
      className={clsx(
        'flex h-20 w-14 items-center justify-center rounded-xl border-2 text-lg font-bold transition-all duration-150',
        selected
          ? '-translate-y-2 border-brand-500 bg-brand-600 text-white shadow-lg shadow-brand-600/30'
          : 'border-line bg-surface text-ink hover:-translate-y-1 hover:border-brand-400',
        disabled && 'cursor-not-allowed opacity-50 hover:translate-y-0',
      )}
    >
      {value}
    </button>
  );
}

/** Carte d'un participant : face cachée tant que le tour n'est pas révélé. */
export function ParticipantCard({ name, card, revealed }: { name: string; card: string | null; revealed: boolean }) {
  return (
    <div className="flex flex-col items-center gap-2">
      <div
        key={revealed ? 'front' : 'back'}
        className={clsx(
          'flex h-24 w-16 animate-flip items-center justify-center rounded-xl text-xl font-bold',
          revealed ? 'border-2 border-brand-500 bg-surface text-brand-600' : 'bg-gradient-to-br from-brand-500 to-violet-600 text-white/80 shadow-md',
        )}
      >
        {revealed ? card : '✓'}
      </div>
      <span className="max-w-24 truncate text-xs text-ink-2">{name}</span>
    </div>
  );
}
