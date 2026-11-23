import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Health, Project, ProjectInput, ProjectPriority, ProjectStatus, ProjectType, Visibility } from '../../api/types';
import { useMe, useSaveProject, useTeams, useUsers } from '../../api/hooks';
import { isManager } from '../../lib/permissions';
import { ApiError } from '../../api/client';
import { Modal } from '../../components/ui/Modal';
import { Field } from '../../components/ui/Field';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/Toast';
import { healthLabel, projectPriorityLabel, projectStatusLabel, projectTypeLabel, visibilityLabel } from '../../lib/labels';

const COLORS = ['#6366f1', '#8b5cf6', '#ec4899', '#ef4444', '#f59e0b', '#10b981', '#14b8a6', '#0ea5e9', '#64748b'];

function toInput(p: Project | null): ProjectInput {
  if (!p) return { code: '', name: '', type: 'DEVELOPMENT', status: 'PLANNED', priority: 'P3', health: 'GREEN', visibility: 'TEAM', color: COLORS[0], description: '', client: '', ownerId: null, teamId: null, startDate: null, endDate: null, budgetDays: null };
  return { code: p.code, name: p.name, type: p.type, status: p.status, statusReason: p.statusReason, priority: p.priority, health: p.health, visibility: p.visibility, color: p.color, description: p.description ?? '', client: p.client ?? '', ownerId: p.owner?.id ?? null, teamId: p.team?.id ?? null, startDate: p.startDate, endDate: p.endDate, budgetDays: p.budgetDays };
}

export function ProjectFormModal({ open, onClose, project }: { open: boolean; onClose: () => void; project: Project | null }) {
  const { data: users = [] } = useUsers();
  const { data: teams = [] } = useTeams();
  const { data: me } = useMe();
  // Équipes où l'utilisateur peut créer : toutes pour un manager, sinon celles dont il est responsable
  const allowedTeams = isManager(me) ? teams : teams.filter((t) => me?.teams.some((mt) => mt.teamId === t.id && mt.role === 'LEAD'));
  const canManageAccess = !project || project.myPermissions.includes('PROJECT_MEMBERS');
  const save = useSaveProject();
  const toast = useToast();
  const navigate = useNavigate();
  const [form, setForm] = useState<ProjectInput>(toInput(project));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [globalError, setGlobalError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      const initial = toInput(project);
      setForm(!project && allowedTeams.length === 1 ? { ...initial, teamId: allowedTeams[0]!.id } : initial);
      setErrors({});
      setGlobalError(null);
    }
  }, [open, project]);

  const set = <K extends keyof ProjectInput>(key: K, value: ProjectInput[K]) => setForm((f) => ({ ...f, [key]: value }));

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const input: ProjectInput = { ...form, description: form.description || null, client: form.client || null, startDate: form.startDate || null, endDate: form.endDate || null };
    save.mutate(
      { id: project?.id, input },
      {
        onSuccess: (saved) => {
          toast('success', project ? 'Projet mis à jour' : 'Projet créé');
          onClose();
          if (!project) navigate(`/projects/${saved.id}`);
        },
        onError: (err) => {
          if (err instanceof ApiError) {
            setErrors(err.fieldErrors);
            if (Object.keys(err.fieldErrors).length === 0) setGlobalError(err.message);
          }
        },
      },
    );
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={project ? 'Modifier le projet' : 'Nouveau projet'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Annuler</Button>
          <Button type="submit" form="project-form" loading={save.isPending}>{project ? 'Enregistrer' : 'Créer le projet'}</Button>
        </>
      }
    >
      <form id="project-form" onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field label="Nom" error={errors.name}>
          <input className="input" required maxLength={150} value={form.name} onChange={(e) => set('name', e.target.value)} />
        </Field>
        <Field label="Code (2-12 car.)" error={errors.code}>
          <input className="input uppercase" required disabled={!!project} maxLength={12} value={form.code} onChange={(e) => set('code', e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''))} />
        </Field>
        <Field label="Type">
          <select className="input" value={form.type} onChange={(e) => set('type', e.target.value as ProjectType)}>
            {(Object.keys(projectTypeLabel) as ProjectType[]).map((t) => <option key={t} value={t}>{projectTypeLabel[t]}</option>)}
          </select>
        </Field>
        <Field label="Client">
          <input className="input" maxLength={150} value={form.client ?? ''} onChange={(e) => set('client', e.target.value)} />
        </Field>
        <Field label="Statut">
          <select className="input" value={form.status ?? 'PLANNED'} onChange={(e) => set('status', e.target.value as ProjectStatus)}>
            {(Object.keys(projectStatusLabel) as ProjectStatus[]).map((s) => <option key={s} value={s}>{projectStatusLabel[s]}</option>)}
          </select>
        </Field>
        <Field label="Priorité">
          <select className="input" value={form.priority} onChange={(e) => set('priority', e.target.value as ProjectPriority)}>
            {(Object.keys(projectPriorityLabel) as ProjectPriority[]).map((pr) => <option key={pr} value={pr}>{projectPriorityLabel[pr]}</option>)}
          </select>
        </Field>
        {(form.status === 'ON_HOLD' || form.status === 'CANCELLED') && (
          <Field label="Motif du statut (obligatoire)" className="sm:col-span-2">
            <input className="input" required maxLength={500} value={form.statusReason ?? ''} onChange={(e) => set('statusReason', e.target.value)} />
          </Field>
        )}
        <Field label="Équipe">
          <select className="input" disabled={!canManageAccess} value={form.teamId ?? ''} onChange={(e) => set('teamId', e.target.value ? Number(e.target.value) : null)}>
            {isManager(me) && <option value="">Aucune équipe</option>}
            {(project ? teams : allowedTeams).map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </Field>
        <Field label="Visibilité">
          <select className="input" disabled={!canManageAccess} value={form.visibility} onChange={(e) => set('visibility', e.target.value as Visibility)}>
            {(Object.keys(visibilityLabel) as Visibility[]).map((v) => <option key={v} value={v}>{visibilityLabel[v].label} — {visibilityLabel[v].description}</option>)}
          </select>
        </Field>
        <Field label="Météo">
          <select className="input" value={form.health} onChange={(e) => set('health', e.target.value as Health)}>
            {(Object.keys(healthLabel) as Health[]).map((h) => <option key={h} value={h}>{healthLabel[h]}</option>)}
          </select>
        </Field>
        <Field label="Responsable">
          <select className="input" value={form.ownerId ?? ''} onChange={(e) => set('ownerId', e.target.value ? Number(e.target.value) : null)}>
            <option value="">—</option>
            {users.map((u) => <option key={u.id} value={u.id}>{u.displayName}</option>)}
          </select>
        </Field>
        <Field label="Budget (jours)" error={errors.budgetDays}>
          <input className="input" type="number" min={0} value={form.budgetDays ?? ''} onChange={(e) => set('budgetDays', e.target.value === '' ? null : Number(e.target.value))} />
        </Field>
        <Field label="Début">
          <input className="input" type="date" value={form.startDate ?? ''} onChange={(e) => set('startDate', e.target.value)} />
        </Field>
        <Field label="Échéance">
          <input className="input" type="date" value={form.endDate ?? ''} onChange={(e) => set('endDate', e.target.value)} />
        </Field>
        <Field label="Description" className="sm:col-span-2">
          <textarea className="input min-h-20" maxLength={4000} value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} />
        </Field>
        <div className="sm:col-span-2">
          <span className="label">Couleur</span>
          <div className="flex gap-2">
            {COLORS.map((c) => (
              <button key={c} type="button" aria-label={`Couleur ${c}`} onClick={() => set('color', c)} className="size-7 rounded-lg ring-offset-2 ring-offset-surface transition" style={{ backgroundColor: c, boxShadow: form.color === c ? `0 0 0 2px ${c}` : undefined }} />
            ))}
          </div>
        </div>
        {!project && <p className="text-xs text-ink-3 sm:col-span-2">Les modules (Kanban, checklist, rétro-planning, poker) sont pré-activés selon le type, et modifiables ensuite.</p>}
        {globalError && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 sm:col-span-2 dark:bg-red-500/10 dark:text-red-300">{globalError}</p>}
      </form>
    </Modal>
  );
}
