import { useState } from 'react';
import { Building2, Lock, Plus, Trash2, Users } from 'lucide-react';
import clsx from 'clsx';
import type { Project, ProjectRole, Visibility } from '../../api/types';
import { useProjectMemberMutations, useProjectMembers, useSaveProject, useTeams, useUsers } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Avatar } from '../../components/ui/Avatar';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { projectRoleLabel, roleLabel, visibilityLabel } from '../../lib/labels';
import { can } from '../../lib/permissions';
import { toProjectInput } from './projectInput';

const visibilityIcon: Record<Visibility, React.ReactNode> = {
  ORGANIZATION: <Building2 className="size-4" />,
  TEAM: <Users className="size-4" />,
  RESTRICTED: <Lock className="size-4" />,
};

const ROLES: ProjectRole[] = ['LEAD', 'CONTRIBUTOR', 'VIEWER'];

/** Qui voit et modifie ce projet : visibilité, équipe propriétaire, membres hérités et ajoutés. */
export function ProjectAccess({ project }: { project: Project }) {
  const { data: members, isLoading } = useProjectMembers(project.id);
  const { data: users = [] } = useUsers();
  const { data: teams = [] } = useTeams();
  const m = useProjectMemberMutations(project.id);
  const save = useSaveProject();
  const toast = useToast();
  const editable = can(project, 'PROJECT_MEMBERS');
  const [newUser, setNewUser] = useState('');
  const [newRole, setNewRole] = useState<ProjectRole>('VIEWER');

  if (isLoading || !members) return <Spinner />;

  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');
  const updateProject = (overrides: { visibility?: Visibility; teamId?: number | null }) =>
    save.mutate({ id: project.id, input: toProjectInput(project, overrides) }, { onSuccess: () => toast('success', 'Accès mis à jour'), onError });

  const explicitIds = new Set(members.explicitMembers.map((x) => x.userId));
  const candidates = users.filter((u) => !explicitIds.has(u.id) && u.role !== 'OWNER' && u.role !== 'ADMIN');
  const selectedGuest = users.find((u) => String(u.id) === newUser)?.role === 'GUEST';

  return (
    <div className="grid max-w-5xl gap-6 lg:grid-cols-2">
      <section className="card p-5 lg:col-span-2">
        <h3 className="text-sm font-semibold">Visibilité</h3>
        <p className="mb-4 text-xs text-ink-3">Administrateurs et managers (vision portefeuille) voient toujours tous les projets.</p>
        <div className="grid gap-3 sm:grid-cols-3">
          {(Object.keys(visibilityLabel) as Visibility[]).map((v) => (
            <button
              key={v}
              disabled={!editable || save.isPending}
              onClick={() => v !== project.visibility && updateProject({ visibility: v })}
              className={clsx(
                'rounded-xl border p-3.5 text-left transition',
                project.visibility === v ? 'border-brand-500 bg-brand-50 ring-1 ring-brand-500/30 dark:bg-brand-500/10' : 'border-line hover:border-brand-300',
                !editable && 'cursor-default',
              )}
            >
              <span className="flex items-center gap-2 text-sm font-semibold">{visibilityIcon[v]}{visibilityLabel[v].label}</span>
              <span className="mt-1 block text-xs text-ink-3">{visibilityLabel[v].description}</span>
            </button>
          ))}
        </div>
        <div className="mt-4 flex flex-wrap items-center gap-3 text-sm">
          <span className="text-ink-3">Équipe propriétaire</span>
          <select
            className="input w-60"
            disabled={!editable || save.isPending}
            value={project.team?.id ?? ''}
            onChange={(e) => updateProject({ teamId: e.target.value ? Number(e.target.value) : null })}
          >
            <option value="">Aucune équipe</option>
            {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>
      </section>

      <section className="card p-5">
        <h3 className="text-sm font-semibold">Membres de l'équipe {members.teamName ?? ''}</h3>
        <p className="mb-3 text-xs text-ink-3">
          {project.visibility === 'RESTRICTED' ? 'Projet confidentiel : l’appartenance à l’équipe ne donne pas accès.' : 'Rôle hérité de l’équipe.'}
        </p>
        {members.teamMembers.length === 0 ? <p className="text-sm text-ink-3">Aucun membre.</p> : (
          <ul className={clsx('space-y-2.5', project.visibility === 'RESTRICTED' && 'opacity-50')}>
            {members.teamMembers.map((u) => (
              <li key={u.userId} className="flex items-center gap-2.5 text-sm">
                <Avatar name={u.displayName} size="sm" />
                <span className="flex-1 truncate">{u.displayName}</span>
                <Badge>{projectRoleLabel[u.role]}</Badge>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="card p-5">
        <h3 className="text-sm font-semibold">Membres ajoutés au projet</h3>
        <p className="mb-3 text-xs text-ink-3">Contributeurs d'autres équipes, clients ou partenaires invités.</p>
        <ul className="space-y-2.5">
          {members.explicitMembers.length === 0 && <li className="text-sm text-ink-3">Aucun membre ajouté.</li>}
          {members.explicitMembers.map((u) => (
            <li key={u.userId} className="group flex items-center gap-2.5 text-sm">
              <Avatar name={u.displayName} size="sm" />
              <span className="min-w-0 flex-1">
                <span className="block truncate">{u.displayName}</span>
                {u.orgRole === 'GUEST' && <span className="text-[11px] text-amber-600">{roleLabel.GUEST}</span>}
              </span>
              {editable ? (
                <>
                  <select
                    className="rounded-md border border-line bg-surface px-1.5 py-1 text-xs"
                    value={u.role}
                    onChange={(e) => m.put.mutate({ userId: u.userId, role: e.target.value as ProjectRole }, { onError })}
                    aria-label={`Rôle de ${u.displayName}`}
                  >
                    {ROLES.filter((r) => u.orgRole !== 'GUEST' || r !== 'LEAD').map((r) => <option key={r} value={r}>{projectRoleLabel[r]}</option>)}
                  </select>
                  <button onClick={() => m.remove.mutate(u.userId, { onError })} className="rounded p-1 text-ink-3 hover:text-red-600" aria-label={`Retirer ${u.displayName}`}>
                    <Trash2 className="size-4" />
                  </button>
                </>
              ) : (
                <Badge>{projectRoleLabel[u.role]}</Badge>
              )}
            </li>
          ))}
        </ul>
        {editable && (
          <div className="mt-4 flex flex-wrap gap-2 border-t border-line pt-4">
            <select className="input min-w-0 flex-1" value={newUser} onChange={(e) => setNewUser(e.target.value)} aria-label="Personne à ajouter">
              <option value="">Ajouter une personne…</option>
              {candidates.map((u) => <option key={u.id} value={u.id}>{u.displayName}{u.role === 'GUEST' ? ' (invité)' : ''}</option>)}
            </select>
            <select className="input w-36" value={newRole} onChange={(e) => setNewRole(e.target.value as ProjectRole)} aria-label="Rôle">
              {ROLES.filter((r) => !selectedGuest || r !== 'LEAD').map((r) => <option key={r} value={r}>{projectRoleLabel[r]}</option>)}
            </select>
            <Button
              icon={<Plus className="size-4" />}
              disabled={!newUser}
              loading={m.put.isPending}
              onClick={() => m.put.mutate({ userId: Number(newUser), role: newRole }, { onSuccess: () => setNewUser(''), onError })}
            >
              Ajouter
            </Button>
          </div>
        )}
      </section>
    </div>
  );
}
