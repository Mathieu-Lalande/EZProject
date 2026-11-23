import { Link } from 'react-router-dom';
import { CalendarDays, CheckCircle2, Inbox } from 'lucide-react';
import clsx from 'clsx';
import { useMe, useMyTasks } from '../api/hooks';
import type { Task } from '../api/types';
import { PageHeader } from '../components/ui/PageHeader';
import { Spinner } from '../components/ui/Spinner';
import { Badge } from '../components/ui/Badge';
import { EmptyState } from '../components/ui/EmptyState';
import { formatDate, toIsoDate } from '../lib/format';
import { priorityLabel, taskStatusLabel } from '../lib/labels';
import { TodoList } from '../features/todos/TodoList';

function greeting(): string {
  const h = new Date().getHours();
  return h < 12 ? 'Bonjour' : h < 18 ? 'Bon après-midi' : 'Bonsoir';
}

export function MySpacePage() {
  const { data: me } = useMe();
  const { data: tasks = [], isLoading } = useMyTasks();
  const today = toIsoDate(new Date());
  const inAWeek = toIsoDate(new Date(Date.now() + 7 * 86_400_000));

  const groups: { title: string; tone?: string; items: Task[] }[] = [
    { title: 'En retard', tone: 'text-red-600', items: tasks.filter((t) => t.overdue) },
    { title: 'Cette semaine', items: tasks.filter((t) => !t.overdue && t.dueDate && t.dueDate <= inAWeek) },
    { title: 'Plus tard', items: tasks.filter((t) => !t.overdue && t.dueDate && t.dueDate > inAWeek) },
    { title: 'Sans échéance', items: tasks.filter((t) => !t.dueDate) },
  ];

  return (
    <div className="animate-fade-in">
      <PageHeader title={`${greeting()} ${me?.displayName.split(' ')[0] ?? ''} 👋`} subtitle={`${tasks.length} tâche${tasks.length > 1 ? 's' : ''} ouverte${tasks.length > 1 ? 's' : ''} vous sont assignées.`} />
      <div className="grid gap-6 lg:grid-cols-[1fr_420px]">
        <div className="min-w-0 space-y-6">
          {isLoading ? (
            <Spinner />
          ) : tasks.length === 0 ? (
            <EmptyState icon={<CheckCircle2 className="size-5" />} title="Rien d'assigné" description="Profitez-en, ou piochez dans un backlog !" />
          ) : (
            groups.filter((g) => g.items.length).map((g) => (
              <section key={g.title}>
                <h2 className={clsx('mb-2 text-xs font-semibold tracking-wider uppercase', g.tone ?? 'text-ink-3')}>{g.title} · {g.items.length}</h2>
                <div className="card divide-y divide-line">
                  {g.items.map((t) => (
                    <Link key={t.id} to={`/projects/${t.projectId}/board`} className="flex items-center gap-3 px-4 py-3 transition hover:bg-surface-2/50">
                      <span className="size-2.5 shrink-0 rounded-[4px]" style={{ backgroundColor: t.projectColor }} />
                      <div className="min-w-0 flex-1">
                        <div className="truncate text-sm font-medium">{t.title}</div>
                        <div className="text-xs text-ink-3">{t.projectName} · {taskStatusLabel[t.status]}</div>
                      </div>
                      {(t.priority === 'HIGH' || t.priority === 'CRITICAL') && <Badge tone={t.priority === 'CRITICAL' ? 'red' : 'amber'}>{priorityLabel[t.priority]}</Badge>}
                      {t.dueDate && (
                        <span className={clsx('flex items-center gap-1 text-xs', t.overdue ? 'font-medium text-red-600' : 'text-ink-3')}>
                          <CalendarDays className="size-3.5" />{t.dueDate === today ? "Aujourd'hui" : formatDate(t.dueDate)}
                        </span>
                      )}
                    </Link>
                  ))}
                </div>
              </section>
            ))
          )}
        </div>
        <div>
          <TodoList title="Mes todos personnels" />
          <p className="mt-2 flex items-center gap-1.5 px-1 text-xs text-ink-3"><Inbox className="size-3.5" />Visibles uniquement par vous.</p>
        </div>
      </div>
    </div>
  );
}
