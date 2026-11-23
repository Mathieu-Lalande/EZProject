import { Link } from 'react-router-dom';
import type { ProjectSummary } from '../../api/types';
import { formatDate, daysBetween, parseDate } from '../../lib/format';

export function MilestonesWidget({ projects }: { projects: ProjectSummary[] }) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const milestones = projects
    .filter((p) => p.nextMilestone)
    .map((p) => ({ project: p, ...p.nextMilestone!, days: daysBetween(today, parseDate(p.nextMilestone!.date)) }))
    .sort((a, b) => a.days - b.days);

  return (
    <div className="card p-5">
      <h3 className="text-sm font-semibold">Prochains jalons</h3>
      <p className="mb-4 text-xs text-ink-3">Issus des rétro-plannings</p>
      {milestones.length === 0 ? (
        <p className="text-sm text-ink-3">Aucun jalon à venir.</p>
      ) : (
        <ol className="relative space-y-4 border-l border-line pl-5">
          {milestones.map((m) => (
            <li key={m.project.id} className="relative">
              <span className="absolute top-1 -left-[25px] size-2.5 rounded-full ring-4 ring-surface" style={{ backgroundColor: m.project.color }} />
              <div className="flex items-baseline justify-between gap-2">
                <span className="text-sm font-medium">{m.name}</span>
                <span className="shrink-0 text-xs tabular-nums text-ink-3">{m.days === 0 ? "Aujourd'hui" : `J-${m.days}`}</span>
              </div>
              <Link to={`/projects/${m.project.id}/retro`} className="text-xs text-ink-3 hover:text-brand-600">
                {m.project.name} · {formatDate(m.date)}
              </Link>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
