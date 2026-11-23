import { useEffect, useState, type FormEvent } from 'react';
import { DndContext, PointerSensor, closestCenter, useSensor, useSensors, type DragEndEvent } from '@dnd-kit/core';
import { SortableContext, arrayMove, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { AlertTriangle, CalendarClock, Check, Diamond, GripVertical, Plus, Trash2, Wand2 } from 'lucide-react';
import clsx from 'clsx';
import { Link } from 'react-router-dom';
import type { PhaseKind, PhaseState, Project, ProjectType, RetroPhase } from '../../api/types';
import { useRetroMutations, useRetroPlan } from '../../api/hooks';
import { Spinner } from '../../components/ui/Spinner';
import { Button } from '../../components/ui/Button';
import { Badge, type Tone } from '../../components/ui/Badge';
import { EmptyState } from '../../components/ui/EmptyState';
import { formatDate } from '../../lib/format';
import { phaseStateLabel, projectTypeLabel } from '../../lib/labels';
import { GanttChart } from './GanttChart';
import { can } from '../../lib/permissions';

const stateTone: Record<PhaseState, Tone> = { DONE: 'green', LATE: 'red', IN_PROGRESS: 'brand', UPCOMING: 'neutral', UNSCHEDULED: 'neutral' };

export function RetroPlanning({ project }: { project: Project }) {
  const { data: plan, isLoading } = useRetroPlan(project.id);
  const m = useRetroMutations(project.id);
  const [name, setName] = useState('');
  const [kind, setKind] = useState<PhaseKind>('PHASE');
  const [duration, setDuration] = useState(5);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 4 } }));
  const editable = can(project, 'RETRO_EDIT');

  if (isLoading || !plan) return <Spinner />;

  const add = (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    m.add.mutate({ name: name.trim(), kind, durationDays: kind === 'MILESTONE' ? 0 : duration, done: false }, { onSuccess: () => setName('') });
  };

  const onDragEnd = ({ active, over }: DragEndEvent) => {
    if (!over || active.id === over.id) return;
    const ids = plan.phases.map((p) => p.id);
    m.reorder.mutate(arrayMove(ids, ids.indexOf(active.id as number), ids.indexOf(over.id as number)));
  };

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Échéance" value={formatDate(plan.deadline, true)} icon={<CalendarClock className="size-4" />} />
        <Stat label="Démarrage au plus tard" value={formatDate(plan.computedStart, true)} icon={<Diamond className="size-4" />} warn={plan.startConflict} />
        <Stat label="Charge planifiée" value={`${plan.totalWorkingDays} j ouvrés`} icon={<Check className="size-4" />} />
      </div>

      {!plan.deadline && (
        <div className="flex items-center gap-2 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-800 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
          <AlertTriangle className="size-4 shrink-0" />
          Définissez une date de fin dans les <Link className="underline" to={`/projects/${project.id}/settings`}>paramètres du projet</Link> pour calculer le rétro-planning.
        </div>
      )}
      {plan.startConflict && (
        <div className="flex items-center gap-2 rounded-xl border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-800 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">
          <AlertTriangle className="size-4 shrink-0" />
          Le planning impose de démarrer le {formatDate(plan.computedStart, true)}, avant le début prévu ({formatDate(plan.projectStart, true)}). Réduisez des phases ou décalez l'échéance.
        </div>
      )}

      {plan.phases.length === 0 ? (
        <EmptyState
          icon={<Wand2 className="size-5" />}
          title="Aucune phase pour l'instant"
          description="Partez d'un gabarit adapté ou ajoutez vos phases une à une : les dates sont calculées à rebours depuis l'échéance."
          action={editable &&
            <div className="flex flex-wrap justify-center gap-2">
              {(['DEVELOPMENT', 'CONSULTING', 'INTERNAL'] as ProjectType[]).map((t) => (
                <Button key={t} variant={t === project.type ? 'primary' : 'secondary'} size="sm" onClick={() => m.template.mutate(t)} loading={m.template.isPending && m.template.variables === t}>
                  Gabarit {projectTypeLabel[t]}
                </Button>
              ))}
            </div>
          }
        />
      ) : (
        <>
          {plan.deadline && plan.computedStart && (
            <div className="card p-5">
              <h3 className="mb-4 text-sm font-semibold">Diagramme</h3>
              <GanttChart phases={plan.phases} start={plan.computedStart} deadline={plan.deadline} />
            </div>
          )}
          <div className="card overflow-hidden">
            <div className="border-b border-line px-5 py-3 text-sm font-semibold">Phases et jalons {editable && <span className="font-normal text-ink-3">· glisser pour réordonner</span>}</div>
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
              <SortableContext items={plan.phases.map((p) => p.id)} strategy={verticalListSortingStrategy}>
                <ul className="divide-y divide-line">
                  {plan.phases.map((p) => (
                    <PhaseRow
                      key={p.id}
                      phase={p}
                      editable={editable}
                      onChange={(input) => m.update.mutate({ id: p.id, input })}
                      onDelete={() => m.remove.mutate(p.id)}
                    />
                  ))}
                </ul>
              </SortableContext>
            </DndContext>
          </div>
        </>
      )}

      {editable && <form onSubmit={add} className="card flex flex-wrap items-end gap-3 p-4">
        <label className="min-w-48 flex-1">
          <span className="label">Nouvelle phase ou jalon</span>
          <input className="input" placeholder="ex. Recette utilisateur" maxLength={150} value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          <span className="label">Type</span>
          <select className="input" value={kind} onChange={(e) => setKind(e.target.value as PhaseKind)}>
            <option value="PHASE">Phase</option>
            <option value="MILESTONE">Jalon</option>
          </select>
        </label>
        {kind === 'PHASE' && (
          <label className="w-28">
            <span className="label">Durée (j ouvrés)</span>
            <input className="input" type="number" min={1} max={1000} value={duration} onChange={(e) => setDuration(Number(e.target.value))} />
          </label>
        )}
        <Button type="submit" icon={<Plus className="size-4" />} loading={m.add.isPending}>Ajouter à la fin</Button>
      </form>}
    </div>
  );
}

function Stat({ label, value, icon, warn }: { label: string; value: string; icon: React.ReactNode; warn?: boolean }) {
  return (
    <div className={clsx('card flex items-center gap-3 p-4', warn && 'border-red-300 dark:border-red-500/40')}>
      <span className={clsx('flex size-9 items-center justify-center rounded-lg', warn ? 'bg-red-50 text-red-600 dark:bg-red-500/15' : 'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-300')}>{icon}</span>
      <div>
        <div className="text-xs text-ink-3">{label}</div>
        <div className="font-semibold">{value}</div>
      </div>
    </div>
  );
}

function PhaseRow({ phase, onChange, onDelete, editable }: { phase: RetroPhase; editable: boolean; onChange: (input: { name: string; kind: PhaseKind; durationDays: number; done: boolean }) => void; onDelete: () => void }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: phase.id, disabled: !editable });
  const [duration, setDuration] = useState(phase.durationDays);
  useEffect(() => setDuration(phase.durationDays), [phase.durationDays]);
  const base = { name: phase.name, kind: phase.kind, durationDays: phase.durationDays, done: phase.done };

  return (
    <li ref={setNodeRef} style={{ transform: CSS.Translate.toString(transform), transition }} className={clsx('group flex items-center gap-3 bg-surface px-4 py-2.5', isDragging && 'relative z-10 shadow-pop')}>
      <button className="cursor-grab text-ink-3 opacity-40 group-hover:opacity-100" aria-label="Déplacer" {...attributes} {...listeners}>
        <GripVertical className="size-4" />
      </button>
      <button
        disabled={!editable}
        onClick={() => onChange({ ...base, done: !phase.done })}
        aria-label={phase.done ? 'Marquer non terminé' : 'Marquer terminé'}
        className={clsx('flex size-5 items-center justify-center rounded-md border transition', phase.done ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-line hover:border-brand-500')}
      >
        {phase.done && <Check className="size-3.5" />}
      </button>
      {phase.kind === 'MILESTONE' ? <Diamond className="size-4 text-brand-500" /> : <span className="size-4" />}
      <span className={clsx('flex-1 text-sm font-medium', phase.done && 'text-ink-3 line-through')}>{phase.name}</span>
      {phase.kind === 'PHASE' && (
        <label className="flex items-center gap-1 text-xs text-ink-3">
          <input
            type="number"
            min={1}
            disabled={!editable}
            className="w-14 rounded-md border border-transparent bg-transparent px-1.5 py-0.5 text-right text-sm text-ink hover:border-line focus:border-brand-500 focus:outline-none"
            value={duration}
            onChange={(e) => setDuration(Number(e.target.value))}
            onBlur={() => duration !== phase.durationDays && duration > 0 && onChange({ ...base, durationDays: duration })}
            aria-label="Durée en jours ouvrés"
          />
          j
        </label>
      )}
      <span className="hidden w-36 text-right text-xs text-ink-3 tabular-nums md:block">
        {phase.start ? (phase.kind === 'MILESTONE' ? formatDate(phase.start) : `${formatDate(phase.start)} → ${formatDate(phase.end)}`) : '—'}
      </span>
      <Badge tone={stateTone[phase.state]} className="w-20 justify-center">{phaseStateLabel[phase.state]}</Badge>
      {editable && <button onClick={onDelete} className="rounded p-1 text-ink-3 opacity-0 transition group-hover:opacity-100 hover:text-red-600 focus:opacity-100" aria-label="Supprimer">
        <Trash2 className="size-4" />
      </button>}
    </li>
  );
}
