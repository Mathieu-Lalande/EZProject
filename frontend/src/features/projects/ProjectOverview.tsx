import { CalendarDays, Clock, User, Wallet } from 'lucide-react';
import type { Project } from '../../api/types';
import { useRetroPlan, useTasks } from '../../api/hooks';
import { Avatar } from '../../components/ui/Avatar';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Badge } from '../../components/ui/Badge';
import { formatDate } from '../../lib/format';
import { TASK_COLUMNS, phaseStateLabel, taskStatusLabel } from '../../lib/labels';
import { TodoList } from '../todos/TodoList';

const statusColor = { BACKLOG: '#a1a1aa', TODO: '#0ea5e9', IN_PROGRESS: '#6366f1', REVIEW: '#f59e0b', DONE: '#10b981' } as const;

export function ProjectOverview({ project }: { project: Project }) {
  const { data: tasks = [] } = useTasks(project.id);
  const hasRetro = project.modules.includes('RETRO_PLANNING');
  const { data: plan } = useRetroPlan(project.id);

  const done = tasks.filter((t) => t.status === 'DONE').length;
  const progress = tasks.length ? Math.round((done / tasks.length) * 100) : 0;
  const overdue = tasks.filter((t) => t.overdue);
  const team = [...new Map(tasks.filter((t) => t.assignee).map((t) => [t.assignee!.id, t.assignee!])).values()];

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div className="min-w-0 space-y-6">
        <div className="card p-5">
          <h3 className="text-sm font-semibold">Avancement</h3>
          <div className="mt-3 flex items-end gap-3">
            <span className="text-3xl font-semibold tabular-nums">{progress}%</span>
            <span className="mb-1 text-sm text-ink-3">{done} / {tasks.length} tâches terminées</span>
          </div>
          <div className="mt-4 flex h-2.5 overflow-hidden rounded-full bg-surface-2">
            {TASK_COLUMNS.map((s) => {
              const count = tasks.filter((t) => t.status === s).length;
              return count ? <div key={s} style={{ width: `${(count / tasks.length) * 100}%`, backgroundColor: statusColor[s] }} title={`${taskStatusLabel[s]} : ${count}`} /> : null;
            })}
          </div>
          <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-ink-3">
            {TASK_COLUMNS.map((s) => (
              <span key={s} className="flex items-center gap-1.5"><span className="size-2 rounded-full" style={{ backgroundColor: statusColor[s] }} />{taskStatusLabel[s]} {tasks.filter((t) => t.status === s).length}</span>
            ))}
          </div>
        </div>

        {overdue.length > 0 && (
          <div className="card p-5">
            <h3 className="mb-3 text-sm font-semibold text-red-600">Tâches en retard ({overdue.length})</h3>
            <ul className="divide-y divide-line">
              {overdue.map((t) => (
                <li key={t.id} className="flex items-center gap-3 py-2 text-sm">
                  <span className="flex-1">{t.title}</span>
                  {t.assignee && <Avatar name={t.assignee.displayName} size="xs" />}
                  <span className="text-xs text-red-600">{formatDate(t.dueDate)}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {hasRetro && plan && plan.phases.length > 0 && (
          <div className="card p-5">
            <h3 className="mb-3 text-sm font-semibold">Jalons et phases</h3>
            <ul className="space-y-2">
              {plan.phases.map((p) => (
                <li key={p.id} className="flex items-center gap-3 text-sm">
                  <span className="flex-1">{p.name}</span>
                  <span className="text-xs text-ink-3">{p.start ? formatDate(p.kind === 'MILESTONE' ? p.start : p.end) : '—'}</span>
                  <Badge tone={p.state === 'DONE' ? 'green' : p.state === 'LATE' ? 'red' : p.state === 'IN_PROGRESS' ? 'brand' : 'neutral'}>{phaseStateLabel[p.state]}</Badge>
                </li>
              ))}
            </ul>
          </div>
        )}

        {project.modules.includes('TODOS') && <TodoList projectId={project.id} title="Checklist de l'équipe" />}
      </div>

      <div className="space-y-6">
        <div className="card space-y-4 p-5 text-sm">
          {project.description && <p className="leading-relaxed text-ink-2">{project.description}</p>}
          <InfoRow icon={<User className="size-4" />} label="Responsable" value={project.owner?.displayName ?? '—'} />
          <InfoRow icon={<CalendarDays className="size-4" />} label="Période" value={`${formatDate(project.startDate, true)} → ${formatDate(project.endDate, true)}`} />
          <InfoRow icon={<Wallet className="size-4" />} label="Budget" value={project.budgetDays ? `${project.budgetDays} jours` : '—'} />
          <InfoRow icon={<Clock className="size-4" />} label="Points estimés" value={`${tasks.reduce((s, t) => s + (t.storyPoints ?? 0), 0)} pts`} />
          {project.endDate && project.startDate && (
            <div>
              <div className="mb-1 flex justify-between text-xs text-ink-3"><span>Temps écoulé</span><span>{elapsed(project.startDate, project.endDate)}%</span></div>
              <ProgressBar value={elapsed(project.startDate, project.endDate)} color={project.color} />
            </div>
          )}
        </div>
        <div className="card p-5">
          <h3 className="mb-3 text-sm font-semibold">Équipe</h3>
          {team.length === 0 ? <p className="text-sm text-ink-3">Aucune tâche assignée.</p> : (
            <ul className="space-y-2.5">
              {team.map((u) => (
                <li key={u.id} className="flex items-center gap-2.5 text-sm">
                  <Avatar name={u.displayName} size="sm" />
                  <span className="flex-1">{u.displayName}</span>
                  <span className="text-xs text-ink-3">{tasks.filter((t) => t.assignee?.id === u.id && t.status !== 'DONE').length} ouvertes</span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function elapsed(start: string, end: string): number {
  const s = new Date(start).getTime();
  const e = new Date(end).getTime();
  return Math.round(Math.max(0, Math.min(1, (Date.now() - s) / Math.max(1, e - s))) * 100);
}

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-3">
      <span className="text-ink-3">{icon}</span>
      <span className="text-ink-3">{label}</span>
      <span className="ml-auto text-right font-medium">{value}</span>
    </div>
  );
}
