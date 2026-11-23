const dateFmt = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });
const dateFmtYear = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });

/** Parse une date ISO (yyyy-MM-dd) en date locale, sans décalage de fuseau. */
export function parseDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y ?? 1970, (m ?? 1) - 1, d ?? 1);
}

export function formatDate(iso: string | null | undefined, withYear = false): string {
  if (!iso) return '—';
  return (withYear ? dateFmtYear : dateFmt).format(parseDate(iso));
}

export function toIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function daysBetween(a: Date, b: Date): number {
  return Math.round((b.getTime() - a.getTime()) / 86_400_000);
}

export function relativeDays(days: number | null): string {
  if (days === null) return 'Sans échéance';
  if (days === 0) return "Échéance aujourd'hui";
  if (days < 0) return `Dépassé de ${-days} j`;
  return `J-${days}`;
}

export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase() ?? '')
    .join('');
}
