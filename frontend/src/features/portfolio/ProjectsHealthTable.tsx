import { Link } from 'react-router-dom';
import { AlertTriangle, Flag, PauseCircle } from 'lucide-react';
import { PriorityBadge } from '../../components/ui/PriorityBadge';
import { StatusBadge } from '../../components/ui/StatusBadge';
import clsx from 'clsx';
import type { ProjectSummary } from '../../api/types';
import { HealthBadge } from '../../components/ui/HealthBadge';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Badge } from '../../components/ui/Badge';
import { formatDate, relativeDays } from '../../lib/format';
import { projectTypeLabel } from '../../lib/labels';

export function ProjectsHealthTable({ projects }: { projects: ProjectSummary[] }) {
  return (
    <div className="card overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full min-w-[880px] text-sm">
          <thead>
            <tr className="border-b border-line bg-surface-2/60 text-left text-xs text-ink-3">
              <th className="w-14 px-4 py-2.5 font-medium">Prio.</th>
              <th className="min-w-64 px-3 py-2.5 font-medium">Projet</th>
              <th className="px-3 py-2.5 font-medium">Statut</th>
              <th className="px-3 py-2.5 font-medium">Responsable</th>
              <th className="px-3 py-2.5 font-medium">Météo</th>
              <th className="w-44 px-3 py-2.5 font-medium">Avancement</th>
              <th className="px-3 py-2.5 font-medium">Retards</th>
              <th className="px-3 py-2.5 font-medium">Prochain jalon</th>
              <th className="px-4 py-2.5 text-right font-medium">Échéance</th>
            </tr>
          </thead>
          <tbody>
            {projects.map((p) => (
              <tr key={p.id} className={clsx('group border-b border-line last:border-0 hover:bg-surface-2/50', p.status === 'ON_HOLD' && 'bg-amber-50/40 dark:bg-amber-500/5')}>
                <td className="px-4 py-3"><PriorityBadge priority={p.priority} short /></td>
                <td className="px-3 py-3">
                  <Link to={`/projects/${p.id}`} className="flex items-center gap-3">
                    <span className="size-8 shrink-0 rounded-lg text-[10px] font-bold text-white flex items-center justify-center" style={{ backgroundColor: p.color }}>
                      {p.code.slice(0, 2)}
                    </span>
                    <span className="min-w-0">
                      <span className="flex items-center gap-1.5 font-medium whitespace-nowrap group-hover:text-brand-600">
                        {p.name}
                        {p.atRisk && <AlertTriangle className="size-3.5 text-amber-500" aria-label="À risque" />}
                      </span>
                      <span className="block max-w-72 truncate text-xs text-ink-3">
                        {p.teamName ?? 'Sans équipe'} · {projectTypeLabel[p.type]}{p.client ? ` · ${p.client}` : ''}
                      </span>
                    </span>
                  </Link>
                </td>
                <td className="px-3 py-3">
                  <StatusBadge status={p.status} reason={p.statusReason} />
                  {p.status === 'ON_HOLD' && p.statusReason && (
                    <span className="mt-1 flex max-w-44 items-start gap-1 text-[11px] text-amber-700 dark:text-amber-300" title={p.statusReason}>
                      <PauseCircle className="mt-px size-3 shrink-0" /><span className="line-clamp-2">{p.statusReason}</span>
                    </span>
                  )}
                </td>
                <td className="px-3 py-3 whitespace-nowrap text-ink-2">{p.ownerName ?? '—'}</td>
                <td className="px-3 py-3"><HealthBadge health={p.health} /></td>
                <td className="px-3 py-3">
                  <div className="flex items-center gap-2">
                    <ProgressBar value={p.progress} color={p.color} />
                    <span className="w-9 text-right text-xs tabular-nums text-ink-2">{p.progress}%</span>
                  </div>
                  <div className="mt-1 text-[11px] text-ink-3">{p.tasksByStatus.DONE}/{p.tasksTotal} tâches</div>
                </td>
                <td className="px-3 py-3">
                  <div className="flex flex-wrap gap-1">
                    {p.overdueTasks > 0 && <Badge tone="red">{p.overdueTasks} tâche{p.overdueTasks > 1 ? 's' : ''}</Badge>}
                    {p.latePhases > 0 && <Badge tone="amber">{p.latePhases} phase{p.latePhases > 1 ? 's' : ''}</Badge>}
                    {p.overdueTasks === 0 && p.latePhases === 0 && <span className="text-xs text-ink-3">—</span>}
                  </div>
                </td>
                <td className="px-3 py-3">
                  {p.nextMilestone ? (
                    <span className="flex items-center gap-1.5 whitespace-nowrap text-ink-2">
                      <Flag className="size-3.5 text-brand-500" />
                      <span className="truncate">{p.nextMilestone.name}</span>
                      <span className="text-xs text-ink-3">{formatDate(p.nextMilestone.date)}</span>
                    </span>
                  ) : (
                    <span className="text-xs text-ink-3">—</span>
                  )}
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="whitespace-nowrap text-ink-2">{formatDate(p.endDate, true)}</div>
                  <div className={clsx('text-xs', p.daysRemaining !== null && p.daysRemaining < 0 ? 'text-red-600' : 'text-ink-3')}>
                    {relativeDays(p.daysRemaining)}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
