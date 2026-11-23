import { Link } from 'react-router-dom';
import type { ProjectSummary } from '../../api/types';
import { HealthBadge } from '../../components/ui/HealthBadge';
import { PriorityBadge } from '../../components/ui/PriorityBadge';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { relativeDays } from '../../lib/format';
import { projectTypeLabel } from '../../lib/labels';

export function ProjectCardsGrid({ projects }: { projects: ProjectSummary[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {projects.map((p) => (
        <Link key={p.id} to={`/projects/${p.id}`} className="card group relative overflow-hidden p-5 transition hover:-translate-y-0.5 hover:shadow-pop">
          <span className="absolute inset-x-0 top-0 h-1" style={{ backgroundColor: p.color }} />
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <div className="flex items-center gap-1.5 text-xs text-ink-3"><PriorityBadge priority={p.priority} short />{p.code} · {projectTypeLabel[p.type]}</div>
              <div className="truncate font-semibold group-hover:text-brand-600">{p.name}</div>
              <div className="truncate text-xs text-ink-3">{p.client ?? 'Interne'}</div>
            </div>
            <div className="flex flex-col items-end gap-1">
              <HealthBadge health={p.health} />
              {p.status !== 'ACTIVE' && <StatusBadge status={p.status} reason={p.statusReason} />}
            </div>
          </div>
          <div className="mt-5 flex items-center gap-2">
            <ProgressBar value={p.progress} color={p.color} />
            <span className="text-xs tabular-nums text-ink-2">{p.progress}%</span>
          </div>
          <div className="mt-3 grid grid-cols-5 gap-1">
            {(['BACKLOG', 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'] as const).map((s) => (
              <div key={s} className="rounded-md bg-surface-2 py-1 text-center">
                <div className="text-sm font-semibold tabular-nums">{p.tasksByStatus[s]}</div>
                <div className="text-[9px] tracking-wide text-ink-3 uppercase">{{ BACKLOG: 'Back.', TODO: 'À f.', IN_PROGRESS: 'Cours', REVIEW: 'Revue', DONE: 'Fini' }[s]}</div>
              </div>
            ))}
          </div>
          <div className="mt-4 flex justify-between text-xs text-ink-3">
            <span>{p.teamName ?? 'Sans équipe'} · {p.ownerName ?? 'Sans responsable'}</span>
            <span className={p.daysRemaining !== null && p.daysRemaining < 0 ? 'text-red-600' : ''}>{relativeDays(p.daysRemaining)}</span>
          </div>
        </Link>
      ))}
    </div>
  );
}
