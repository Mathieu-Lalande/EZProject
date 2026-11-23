import { useState, type FormEvent } from 'react';
import { Plus, Trash2, Users } from 'lucide-react';
import { useMe, useTeamMutations, useTeams, useUsers } from '../../api/hooks';
import type { ProjectRole, Team } from '../../api/types';
import { ApiError } from '../../api/client';
import { Avatar } from '../../components/ui/Avatar';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { projectRoleLabel } from '../../lib/labels';
import { isManager } from '../../lib/permissions';

const ROLES: ProjectRole[] = ['LEAD', 'CONTRIBUTOR', 'VIEWER'];
const COLORS = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#0ea5e9', '#8b5cf6', '#64748b'];

export function TeamsTab() {
  const { data: teams = [], isLoading } = useTeams();
  const { data: me } = useMe();
  const m = useTeamMutations();
  const toast = useToast();
  const [name, setName] = useState('');
  const [color, setColor] = useState(COLORS[0]!);

  if (isLoading) return <Spinner />;
  const create = (e: FormEvent) => {
    e.preventDefault();
    m.save.mutate({ input: { name: name.trim(), color } }, {
      onSuccess: () => { setName(''); toast('success', 'Équipe créée'); },
      onError: (err) => toast('error', err instanceof ApiError ? err.message : 'Action impossible'),
    });
  };

  return (
    <div className="space-y-6">
      <p className="max-w-3xl text-sm text-ink-3">
        Chaque projet appartient à une équipe. Le rôle d'équipe (Responsable, Contributeur, Lecteur) s'applique à tous ses projets,
        selon la matrice de permissions. Un responsable d'équipe peut créer des projets et gérer ses membres.
      </p>
      {isManager(me) && (
        <form onSubmit={create} className="card flex flex-wrap items-end gap-3 p-4">
          <label className="min-w-56 flex-1">
            <span className="label">Nouvelle équipe</span>
            <input className="input" placeholder="ex. Delivery Data, PMO, Conseil Cloud" maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <div className="flex gap-1.5 pb-1.5">
            {COLORS.map((c) => (
              <button key={c} type="button" aria-label={`Couleur ${c}`} onClick={() => setColor(c)} className="size-6 rounded-md" style={{ backgroundColor: c, boxShadow: color === c ? `0 0 0 2px var(--color-surface), 0 0 0 4px ${c}` : undefined }} />
            ))}
          </div>
          <Button type="submit" icon={<Plus className="size-4" />} disabled={!name.trim()} loading={m.save.isPending}>Créer</Button>
        </form>
      )}
      {teams.length === 0 ? (
        <EmptyState icon={<Users className="size-5" />} title="Aucune équipe" />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2 2xl:grid-cols-3">
          {teams.map((t) => <TeamCard key={t.id} team={t} canDelete={isManager(me)} />)}
        </div>
      )}
    </div>
  );
}

function TeamCard({ team, canDelete }: { team: Team; canDelete: boolean }) {
  const { data: users = [] } = useUsers();
  const m = useTeamMutations();
  const toast = useToast();
  const [userId, setUserId] = useState('');
  const [role, setRole] = useState<ProjectRole>('CONTRIBUTOR');
  const onError = (err: unknown) => toast('error', err instanceof ApiError ? err.message : 'Action impossible');
  const memberIds = new Set(team.members.map((x) => x.userId));
  const candidates = users.filter((u) => !memberIds.has(u.id) && u.role !== 'GUEST');

  return (
    <div className="card flex flex-col overflow-hidden">
      <div className="h-1" style={{ backgroundColor: team.color }} />
      <div className="flex items-start gap-3 p-5 pb-3">
        <span className="flex size-9 items-center justify-center rounded-lg text-sm font-bold text-white" style={{ backgroundColor: team.color }}>{team.name.slice(0, 1)}</span>
        <div className="min-w-0 flex-1">
          <div className="font-semibold">{team.name}</div>
          <div className="text-xs text-ink-3">{team.description ?? `${team.members.length} membre${team.members.length > 1 ? 's' : ''}`}</div>
        </div>
        {canDelete && (
          <button aria-label={`Supprimer l'équipe ${team.name}`} className="rounded p-1 text-ink-3 hover:text-red-600"
            onClick={() => confirm(`Supprimer l'équipe « ${team.name} » ? Ses projets resteront, sans équipe.`) && m.remove.mutate(team.id, { onError })}>
            <Trash2 className="size-4" />
          </button>
        )}
      </div>
      <ul className="flex-1 space-y-2 px-5 pb-4">
        {team.members.map((u) => (
          <li key={u.userId} className="flex items-center gap-2.5 text-sm">
            <Avatar name={u.displayName} size="sm" />
            <span className="min-w-0 flex-1 truncate">{u.displayName}</span>
            {team.canManage ? (
              <>
                <select className="rounded-md border border-line bg-surface px-1.5 py-0.5 text-xs" value={u.role} aria-label={`Rôle de ${u.displayName}`}
                  onChange={(e) => m.putMember.mutate({ teamId: team.id, userId: u.userId, role: e.target.value as ProjectRole }, { onError })}>
                  {ROLES.map((r) => <option key={r} value={r}>{projectRoleLabel[r]}</option>)}
                </select>
                <button aria-label={`Retirer ${u.displayName}`} className="rounded p-0.5 text-ink-3 hover:text-red-600"
                  onClick={() => m.removeMember.mutate({ teamId: team.id, userId: u.userId }, { onError })}>
                  <Trash2 className="size-3.5" />
                </button>
              </>
            ) : (
              <Badge>{projectRoleLabel[u.role]}</Badge>
            )}
          </li>
        ))}
      </ul>
      {team.canManage && candidates.length > 0 && (
        <div className="flex gap-2 border-t border-line p-3">
          <select className="input min-w-0 flex-1 py-1.5" value={userId} onChange={(e) => setUserId(e.target.value)} aria-label="Membre à ajouter">
            <option value="">Ajouter un membre…</option>
            {candidates.map((u) => <option key={u.id} value={u.id}>{u.displayName}</option>)}
          </select>
          <select className="input w-32 py-1.5" value={role} onChange={(e) => setRole(e.target.value as ProjectRole)} aria-label="Rôle">
            {ROLES.map((r) => <option key={r} value={r}>{projectRoleLabel[r]}</option>)}
          </select>
          <Button size="sm" disabled={!userId} onClick={() => m.putMember.mutate({ teamId: team.id, userId: Number(userId), role }, { onSuccess: () => setUserId(''), onError })}>
            <Plus className="size-4" />
          </Button>
        </div>
      )}
    </div>
  );
}
