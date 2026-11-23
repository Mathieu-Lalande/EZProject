import { useState, type FormEvent } from 'react';
import { CalendarDays, CheckCircle2, ChevronDown, Circle, ListTodo, Plus, Trash2 } from 'lucide-react';
import clsx from 'clsx';
import { useTodoMutations, useTodos } from '../../api/hooks';
import type { Todo } from '../../api/types';
import { Spinner } from '../../components/ui/Spinner';
import { EmptyState } from '../../components/ui/EmptyState';
import { formatDate, toIsoDate } from '../../lib/format';

export function TodoList({ projectId, title, readOnly = false }: { projectId?: number; title?: string; readOnly?: boolean }) {
  const { data: todos = [], isLoading } = useTodos(projectId);
  const { create, update, remove } = useTodoMutations(projectId);
  const [text, setText] = useState('');
  const [due, setDue] = useState('');
  const [showDone, setShowDone] = useState(false);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (!text.trim()) return;
    create.mutate({ title: text.trim(), done: false, dueDate: due || null }, { onSuccess: () => { setText(''); setDue(''); } });
  };
  const toggle = (t: Todo) => update.mutate({ id: t.id, input: { title: t.title, done: !t.done, dueDate: t.dueDate } });

  const open = todos.filter((t) => !t.done);
  const done = todos.filter((t) => t.done);
  const today = toIsoDate(new Date());

  return (
    <div className="card p-5">
      {title && <h3 className="mb-4 text-sm font-semibold">{title}</h3>}
      {!readOnly && <form onSubmit={submit} className="flex gap-2">
        <div className="relative flex-1">
          <Plus className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-3" />
          <input className="input pl-9" placeholder="Ajouter un todo et appuyer sur Entrée" maxLength={300} value={text} onChange={(e) => setText(e.target.value)} />
        </div>
        <input className="input w-40" type="date" aria-label="Échéance" value={due} onChange={(e) => setDue(e.target.value)} />
      </form>}

      {isLoading ? (
        <Spinner />
      ) : todos.length === 0 ? (
        <div className="mt-4"><EmptyState icon={<ListTodo className="size-5" />} title="Rien à faire pour l'instant" description="Ajoutez vos actions, relances et points de suivi." /></div>
      ) : (
        <>
          <ul className="mt-3 divide-y divide-line">
            {open.map((t) => <TodoRow key={t.id} todo={t} today={today} readOnly={readOnly} onToggle={toggle} onDelete={() => remove.mutate(t.id)} showOwner={!!projectId} />)}
          </ul>
          {done.length > 0 && (
            <div className="mt-2">
              <button onClick={() => setShowDone((s) => !s)} className="flex items-center gap-1 py-2 text-xs font-medium text-ink-3 hover:text-ink">
                <ChevronDown className={clsx('size-3.5 transition', !showDone && '-rotate-90')} />
                Terminés ({done.length})
              </button>
              {showDone && (
                <ul className="divide-y divide-line">
                  {done.map((t) => <TodoRow key={t.id} todo={t} today={today} readOnly={readOnly} onToggle={toggle} onDelete={() => remove.mutate(t.id)} showOwner={!!projectId} />)}
                </ul>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}

function TodoRow({ todo, today, onToggle, onDelete, showOwner, readOnly }: { todo: Todo; today: string; onToggle: (t: Todo) => void; onDelete: () => void; showOwner: boolean; readOnly: boolean }) {
  const late = !todo.done && todo.dueDate !== null && todo.dueDate < today;
  return (
    <li className="group flex items-center gap-3 py-2.5">
      <button disabled={readOnly} onClick={() => onToggle(todo)} aria-label={todo.done ? 'Marquer à faire' : 'Marquer comme fait'} className="text-ink-3 transition hover:text-brand-600">
        {todo.done ? <CheckCircle2 className="size-5 text-emerald-500" /> : <Circle className="size-5" />}
      </button>
      <span className={clsx('flex-1 text-sm', todo.done && 'text-ink-3 line-through')}>{todo.title}</span>
      {showOwner && <span className="hidden text-xs text-ink-3 sm:inline">{todo.ownerName}</span>}
      {todo.dueDate && (
        <span className={clsx('flex items-center gap-1 text-xs', late ? 'font-medium text-red-600' : 'text-ink-3')}>
          <CalendarDays className="size-3.5" />
          {todo.dueDate === today ? "Aujourd'hui" : formatDate(todo.dueDate)}
        </span>
      )}
      {!readOnly && <button onClick={onDelete} aria-label="Supprimer" className="rounded p-1 text-ink-3 opacity-0 transition group-hover:opacity-100 hover:text-red-600 focus:opacity-100">
        <Trash2 className="size-4" />
      </button>}
    </li>
  );
}
