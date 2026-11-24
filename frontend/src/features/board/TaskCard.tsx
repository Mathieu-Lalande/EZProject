import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { CalendarDays, CheckSquare, MessageSquare, Paperclip } from 'lucide-react';
import clsx from 'clsx';
import type { Priority, Task } from '../../api/types';
import { Avatar } from '../../components/ui/Avatar';
import { formatDate } from '../../lib/format';
import { priorityLabel } from '../../lib/labels';

const priorityStyle: Record<Priority, string> = {
  LOW: 'bg-zinc-300 dark:bg-zinc-600',
  MEDIUM: 'bg-sky-500',
  HIGH: 'bg-amber-500',
  CRITICAL: 'bg-red-500',
};

export function TaskCardView({ task, dragging, onClick }: { task: Task; dragging?: boolean; onClick?: () => void }) {
  return (
    <div
      onClick={onClick}
      className={clsx(
        'group cursor-grab rounded-lg border border-line bg-surface p-3 shadow-card transition select-none hover:border-brand-400 active:cursor-grabbing',
        dragging && 'rotate-2 shadow-pop ring-2 ring-brand-500/40',
      )}
    >
      <div className="flex items-start gap-2">
        <span className={clsx('mt-1.5 size-1.5 shrink-0 rounded-full', priorityStyle[task.priority])} title={`Priorité ${priorityLabel[task.priority]}`} />
        <p className={clsx('flex-1 text-sm leading-snug', task.status === 'DONE' && 'text-ink-3 line-through')}>{task.title}</p>
      </div>
      {task.labels.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1">
          {task.labels.map((l) => <span key={l} className="rounded bg-brand-50 px-1.5 py-0.5 text-[10px] font-medium text-brand-700 dark:bg-brand-500/15 dark:text-brand-200">{l}</span>)}
        </div>
      )}
      <div className="mt-3 flex items-center gap-2">
        {task.counts.checklistTotal > 0 && (
          <span className={clsx('flex items-center gap-0.5 text-[11px] tabular-nums', task.counts.checklistDone === task.counts.checklistTotal ? 'text-emerald-600' : 'text-ink-3')}
            title="Sous-tâches terminées"><CheckSquare className="size-3" />{task.counts.checklistDone}/{task.counts.checklistTotal}</span>
        )}
        {task.counts.comments > 0 && <span className="flex items-center gap-0.5 text-[11px] text-ink-3" title="Commentaires"><MessageSquare className="size-3" />{task.counts.comments}</span>}
        {task.counts.attachments > 0 && <span className="flex items-center gap-0.5 text-[11px] text-ink-3" title="Pièces jointes"><Paperclip className="size-3" />{task.counts.attachments}</span>}
        {task.storyPoints !== null && (
          <span className="rounded-md bg-surface-2 px-1.5 py-0.5 text-[11px] font-semibold tabular-nums text-ink-2">{task.storyPoints} pts</span>
        )}
        {task.dueDate && (
          <span className={clsx('flex items-center gap-1 text-[11px]', task.overdue ? 'font-medium text-red-600' : 'text-ink-3')}>
            <CalendarDays className="size-3" />
            {formatDate(task.dueDate)}
          </span>
        )}
        <span className="ml-auto">{task.assignee && <Avatar name={task.assignee.displayName} size="xs" />}</span>
      </div>
    </div>
  );
}

export function SortableTaskCard({ task, onOpen, disabled }: { task: Task; onOpen: (task: Task) => void; disabled?: boolean }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: task.id, data: { status: task.status }, disabled });
  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={clsx(isDragging && 'opacity-30')}
      {...attributes}
      {...listeners}
    >
      <TaskCardView task={task} onClick={() => onOpen(task)} />
    </div>
  );
}
