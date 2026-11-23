import { useEffect, useState, type FormEvent } from 'react';
import { Trash2 } from 'lucide-react';
import type { Priority, Task, TaskInput, TaskStatus } from '../../api/types';
import { useDeleteTask, useSaveTask, useUsers } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Modal } from '../../components/ui/Modal';
import { Field } from '../../components/ui/Field';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/Toast';
import { priorityLabel, taskStatusLabel, TASK_COLUMNS } from '../../lib/labels';

interface TaskModalProps {
  projectId: number;
  task: Task | null;
  defaultStatus?: TaskStatus;
  open: boolean;
  onClose: () => void;
  readOnly?: boolean;
  canDelete?: boolean;
}

const empty: TaskInput = { title: '', description: '', priority: 'MEDIUM', assigneeId: null, storyPoints: null, dueDate: null };

export function TaskModal({ projectId, task, defaultStatus = 'TODO', open, onClose, readOnly = false, canDelete = false }: TaskModalProps) {
  const { data: users = [] } = useUsers();
  const save = useSaveTask(projectId);
  const remove = useDeleteTask(projectId);
  const toast = useToast();
  const [form, setForm] = useState<TaskInput>(empty);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setForm(
      task
        ? { title: task.title, description: task.description ?? '', status: task.status, priority: task.priority, assigneeId: task.assignee?.id ?? null, storyPoints: task.storyPoints, dueDate: task.dueDate }
        : { ...empty, status: defaultStatus },
    );
  }, [open, task, defaultStatus]);

  const set = <K extends keyof TaskInput>(key: K, value: TaskInput[K]) => setForm((f) => ({ ...f, [key]: value }));

  const submit = (e: FormEvent) => {
    e.preventDefault();
    save.mutate(
      { id: task?.id, input: { ...form, description: form.description || null, dueDate: form.dueDate || null } },
      {
        onSuccess: () => {
          toast('success', task ? 'Tâche mise à jour' : 'Tâche créée');
          onClose();
        },
        onError: (err) => (err instanceof ApiError ? setErrors(err.fieldErrors) : undefined),
      },
    );
  };

  const onDelete = () => {
    if (!task || !confirm('Supprimer cette tâche ?')) return;
    remove.mutate(task.id, { onSuccess: () => { toast('success', 'Tâche supprimée'); onClose(); } });
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={readOnly ? 'Détail de la tâche' : task ? 'Modifier la tâche' : 'Nouvelle tâche'}
      size="lg"
      footer={
        <>
          {task && canDelete && (
            <Button variant="ghost" className="mr-auto text-red-600" icon={<Trash2 className="size-4" />} onClick={onDelete}>
              Supprimer
            </Button>
          )}
          <Button variant="secondary" onClick={onClose}>{readOnly ? 'Fermer' : 'Annuler'}</Button>
          {!readOnly && <Button type="submit" form="task-form" loading={save.isPending}>Enregistrer</Button>}
        </>
      }
    >
      <form id="task-form" onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <fieldset disabled={readOnly} className="contents">
        <Field label="Titre" error={errors.title} className="sm:col-span-2">
          <input className="input" required maxLength={200} value={form.title} onChange={(e) => set('title', e.target.value)} />
        </Field>
        <Field label="Description" className="sm:col-span-2">
          <textarea className="input min-h-24" maxLength={4000} value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} />
        </Field>
        <Field label="Statut">
          <select className="input" value={form.status} onChange={(e) => set('status', e.target.value as TaskStatus)}>
            {TASK_COLUMNS.map((s) => <option key={s} value={s}>{taskStatusLabel[s]}</option>)}
          </select>
        </Field>
        <Field label="Priorité">
          <select className="input" value={form.priority} onChange={(e) => set('priority', e.target.value as Priority)}>
            {(Object.keys(priorityLabel) as Priority[]).map((p) => <option key={p} value={p}>{priorityLabel[p]}</option>)}
          </select>
        </Field>
        <Field label="Assigné à">
          <select className="input" value={form.assigneeId ?? ''} onChange={(e) => set('assigneeId', e.target.value ? Number(e.target.value) : null)}>
            <option value="">Non assigné</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.displayName}</option>)}
          </select>
        </Field>
        <Field label="Échéance">
          <input className="input" type="date" value={form.dueDate ?? ''} onChange={(e) => set('dueDate', e.target.value || null)} />
        </Field>
        <Field label="Story points" error={errors.storyPoints}>
          <input className="input" type="number" min={0} max={1000} value={form.storyPoints ?? ''} onChange={(e) => set('storyPoints', e.target.value === '' ? null : Number(e.target.value))} />
        </Field>
        </fieldset>
      </form>
    </Modal>
  );
}
