import clsx from 'clsx';
import type { PhaseState, RetroPhase } from '../../api/types';
import { daysBetween, formatDate, parseDate } from '../../lib/format';

const barColor: Record<PhaseState, string> = {
  DONE: 'bg-emerald-500',
  LATE: 'bg-red-500',
  IN_PROGRESS: 'bg-brand-500',
  UPCOMING: 'bg-brand-300 dark:bg-brand-400/60',
  UNSCHEDULED: 'bg-zinc-300',
};

/** Diagramme de Gantt léger en CSS : positions en % de la fenêtre [début calculé, échéance]. */
export function GanttChart({ phases, start, deadline }: { phases: RetroPhase[]; start: string; deadline: string }) {
  const from = parseDate(start);
  const to = parseDate(deadline);
  const span = Math.max(1, daysBetween(from, to) + 1);
  const pct = (iso: string) => (daysBetween(from, parseDate(iso)) / span) * 100;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayPct = (daysBetween(from, today) / span) * 100;

  // Graduations mensuelles
  const months: { label: string; left: number }[] = [];
  const cursor = new Date(from.getFullYear(), from.getMonth() + 1, 1);
  while (cursor <= to) {
    months.push({ label: new Intl.DateTimeFormat('fr-FR', { month: 'short' }).format(cursor), left: (daysBetween(from, cursor) / span) * 100 });
    cursor.setMonth(cursor.getMonth() + 1);
  }

  return (
    <div className="relative">
      <div className="relative mb-2 h-5 text-[10px] text-ink-3">
        <span className="absolute left-0">{formatDate(start)}</span>
        {months.map((m) => (
          <span key={m.left} className="absolute -translate-x-1/2 uppercase" style={{ left: `${m.left}%` }}>{m.label}</span>
        ))}
        <span className="absolute right-0">{formatDate(deadline)}</span>
      </div>
      <div className="relative space-y-2">
        {months.map((m) => (
          <div key={m.left} className="pointer-events-none absolute inset-y-0 border-l border-dashed border-line" style={{ left: `${m.left}%` }} />
        ))}
        {todayPct >= 0 && todayPct <= 100 && (
          <div className="pointer-events-none absolute inset-y-0 z-10 border-l-2 border-red-500" style={{ left: `${todayPct}%` }}>
            <span className="absolute -top-1 -left-[5px] size-2 rounded-full bg-red-500" />
          </div>
        )}
        {phases.map((p) =>
          p.start && p.end ? (
            <div key={p.id} className="relative h-8">
              {p.kind === 'MILESTONE' ? (
                <div className="absolute top-1/2 flex -translate-x-1/2 -translate-y-1/2 items-center" style={{ left: `${pct(p.start) + 100 / span / 2}%` }}>
                  <span className={clsx('size-3.5 rotate-45 rounded-[3px] ring-2 ring-surface', barColor[p.state])} title={`${p.name} · ${formatDate(p.start)}`} />
                </div>
              ) : (
                <div
                  className={clsx('absolute top-1 flex h-6 items-center overflow-hidden rounded-md px-2 text-[11px] font-medium text-white shadow-sm', barColor[p.state])}
                  style={{ left: `${pct(p.start)}%`, width: `${Math.max(((daysBetween(parseDate(p.start), parseDate(p.end)) + 1) / span) * 100, 1.5)}%` }}
                  title={`${p.name} · ${formatDate(p.start)} → ${formatDate(p.end)}`}
                >
                  <span className="truncate">{p.name}</span>
                </div>
              )}
            </div>
          ) : null,
        )}
      </div>
    </div>
  );
}
