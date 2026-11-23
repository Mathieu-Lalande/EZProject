import { useEffect, useMemo, useState } from 'react';
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useDroppable,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { SortableContext, arrayMove, sortableKeyboardCoordinates, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { Plus, Search } from 'lucide-react';
import clsx from 'clsx';
import type { Project, Task, TaskStatus } from '../../api/types';
import { can } from '../../lib/permissions';
import { useMe, useMoveTask, useSaveTask, useTasks } from '../../api/hooks';
import { Spinner } from '../../components/ui/Spinner';
import { Button } from '../../components/ui/Button';
import { TASK_COLUMNS, taskStatusLabel } from '../../lib/labels';
import { SortableTaskCard, TaskCardView } from './TaskCard';
import { TaskModal } from './TaskModal';

type Columns = Record<TaskStatus, Task[]>;

const columnAccent: Record<TaskStatus, string> = {
  BACKLOG: 'bg-zinc-400',
  TODO: 'bg-sky-500',
  IN_PROGRESS: 'bg-brand-500',
  REVIEW: 'bg-amber-500',
  DONE: 'bg-emerald-500',
};

function group(tasks: Task[]): Columns {
  const cols = Object.fromEntries(TASK_COLUMNS.map((s) => [s, [] as Task[]])) as Columns;
  [...tasks].sort((a, b) => a.position - b.position).forEach((t) => cols[t.status].push(t));
  return cols;
}

function findColumn(columns: Columns, id: number | string): TaskStatus | undefined {
  if (typeof id === 'string' && (TASK_COLUMNS as string[]).includes(id)) return id as TaskStatus;
  return TASK_COLUMNS.find((s) => columns[s].some((t) => t.id === id));
}

export function KanbanBoard({ project }: { project: Project }) {
  const projectId = project.id;
  const canCreate = can(project, 'TASK_CREATE');
  const canEdit = can(project, 'TASK_EDIT');
  const { data: tasks, isLoading } = useTasks(projectId);
  const { data: me } = useMe();
  const move = useMoveTask(projectId);
  const quickCreate = useSaveTask(projectId);
  const [columns, setColumns] = useState<Columns>(() => group([]));
  const [activeTask, setActiveTask] = useState<Task | null>(null);
  const [editing, setEditing] = useState<{ task: Task | null; status: TaskStatus } | null>(null);
  const [search, setSearch] = useState('');
  const [mineOnly, setMineOnly] = useState(false);
  const [label, setLabel] = useState('');
  const allLabels = useMemo(() => [...new Set((tasks ?? []).flatMap((t) => t.labels))].sort((a, b) => a.localeCompare(b)), [tasks]);
  const [quickAdd, setQuickAdd] = useState<{ status: TaskStatus; title: string } | null>(null);

  useEffect(() => {
    if (tasks && !activeTask) setColumns(group(tasks));
  }, [tasks, activeTask]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q && !mineOnly && !label) return columns;
    return Object.fromEntries(
      TASK_COLUMNS.map((s) => [s, columns[s].filter((t) => (!q || t.title.toLowerCase().includes(q)) && (!mineOnly || t.assignee?.id === me?.id) && (!label || t.labels.includes(label)))]),
    ) as Columns;
  }, [columns, search, mineOnly, me, label]);

  if (isLoading) return <Spinner />;

  const onDragStart = ({ active }: DragStartEvent) => {
    const col = findColumn(columns, active.id as number);
    setActiveTask(col ? columns[col].find((t) => t.id === active.id) ?? null : null);
  };

  // Déplacement entre colonnes pendant le survol, pour un retour visuel immédiat
  const onDragOver = ({ active, over }: DragOverEvent) => {
    if (!over) return;
    const from = findColumn(columns, active.id as number);
    const to = findColumn(columns, over.id as number | string);
    if (!from || !to || from === to) return;
    setColumns((prev) => {
      const task = prev[from].find((t) => t.id === active.id);
      if (!task) return prev;
      const overIndex = prev[to].findIndex((t) => t.id === over.id);
      const insertAt = overIndex >= 0 ? overIndex : prev[to].length;
      const target = [...prev[to]];
      target.splice(insertAt, 0, { ...task, status: to });
      return { ...prev, [from]: prev[from].filter((t) => t.id !== active.id), [to]: target };
    });
  };

  const onDragEnd = ({ active, over }: DragEndEvent) => {
    const task = activeTask;
    setActiveTask(null);
    if (!over || !task) return;
    const col = findColumn(columns, active.id as number);
    if (!col) return;
    let list = columns[col];
    const oldIndex = list.findIndex((t) => t.id === active.id);
    const overIndex = list.findIndex((t) => t.id === over.id);
    if (overIndex >= 0 && overIndex !== oldIndex) {
      list = arrayMove(list, oldIndex, overIndex);
      setColumns((prev) => ({ ...prev, [col]: list }));
    }
    const position = list.findIndex((t) => t.id === active.id);
    if (col !== task.status || position !== task.position) {
      move.mutate({ id: task.id, status: col, position });
    }
  };

  const submitQuickAdd = () => {
    if (!quickAdd?.title.trim()) return setQuickAdd(null);
    quickCreate.mutate({ input: { title: quickAdd.title.trim(), status: quickAdd.status } });
    setQuickAdd({ status: quickAdd.status, title: '' });
  };

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center gap-2">
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-ink-3" />
          <input className="input w-60 pl-8" placeholder="Filtrer les tâches…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <button
          onClick={() => setMineOnly((m) => !m)}
          className={clsx('rounded-lg border px-3 py-2 text-sm transition', mineOnly ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200' : 'border-line text-ink-2 hover:bg-surface-2')}
        >
          Mes tâches
        </button>
        {allLabels.length > 0 && (
          <select className="input w-44" value={label} onChange={(e) => setLabel(e.target.value)} aria-label="Filtrer par étiquette">
            <option value="">Toutes les étiquettes</option>
            {allLabels.map((l) => <option key={l} value={l}>{l}</option>)}
          </select>
        )}
        {!canEdit && <span className="ml-auto text-xs text-ink-3">Lecture seule</span>}
        {canCreate && (
          <Button className="ml-auto" icon={<Plus className="size-4" />} onClick={() => setEditing({ task: null, status: 'TODO' })}>
            Nouvelle tâche
          </Button>
        )}
      </div>

      <DndContext sensors={sensors} collisionDetection={closestCorners} onDragStart={onDragStart} onDragOver={onDragOver} onDragEnd={onDragEnd} onDragCancel={() => setActiveTask(null)}>
        <div className="scrollbar-thin -mx-1 flex gap-4 overflow-x-auto px-1 pb-4">
          {TASK_COLUMNS.map((status) => (
            <Column key={status} status={status} tasks={filtered[status]}>
              <SortableContext items={filtered[status].map((t) => t.id)} strategy={verticalListSortingStrategy}>
                <div className="flex min-h-16 flex-col gap-2">
                  {filtered[status].map((t) => (
                    <SortableTaskCard key={t.id} task={t} disabled={!canEdit} onOpen={(task) => setEditing({ task, status: task.status })} />
                  ))}
                </div>
              </SortableContext>
              {!canCreate ? null : quickAdd?.status === status ? (
                <input
                  autoFocus
                  className="input mt-2"
                  placeholder="Titre puis Entrée"
                  value={quickAdd.title}
                  onChange={(e) => setQuickAdd({ status, title: e.target.value })}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') submitQuickAdd();
                    if (e.key === 'Escape') setQuickAdd(null);
                  }}
                  onBlur={submitQuickAdd}
                />
              ) : (
                <button onClick={() => setQuickAdd({ status, title: '' })} className="mt-2 flex w-full items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs text-ink-3 transition hover:bg-surface hover:text-ink">
                  <Plus className="size-3.5" /> Ajouter
                </button>
              )}
            </Column>
          ))}
        </div>
        <DragOverlay>{activeTask && <TaskCardView task={activeTask} dragging />}</DragOverlay>
      </DndContext>

      <TaskModal projectId={projectId} readOnly={!canEdit} canDelete={can(project, 'TASK_DELETE')} open={editing !== null} task={editing?.task ?? null} defaultStatus={editing?.status} onClose={() => setEditing(null)} />
    </div>
  );
}

function Column({ status, tasks, children }: { status: TaskStatus; tasks: Task[]; children: React.ReactNode }) {
  const { setNodeRef, isOver } = useDroppable({ id: status });
  const points = tasks.reduce((sum, t) => sum + (t.storyPoints ?? 0), 0);
  return (
    <div ref={setNodeRef} className={clsx('flex min-w-60 flex-1 flex-col rounded-xl bg-surface-2/70 p-2.5 transition', isOver && 'ring-2 ring-brand-500/40')}>
      <div className="mb-2.5 flex items-center gap-2 px-1">
        <span className={clsx('size-2 rounded-full', columnAccent[status])} />
        <span className="text-sm font-semibold">{taskStatusLabel[status]}</span>
        <span className="rounded-md bg-surface px-1.5 text-xs text-ink-3 tabular-nums">{tasks.length}</span>
        {points > 0 && <span className="ml-auto text-[11px] text-ink-3 tabular-nums">{points} pts</span>}
      </div>
      {children}
    </div>
  );
}
