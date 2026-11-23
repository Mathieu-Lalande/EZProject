export function Spinner({ label = 'Chargement…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-16 text-sm text-ink-3" role="status">
      <span className="size-4 animate-spin rounded-full border-2 border-brand-500 border-r-transparent" />
      {label}
    </div>
  );
}
