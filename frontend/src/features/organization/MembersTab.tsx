import { useState, type FormEvent } from 'react';
import { Ban, Copy, KeyRound, MoreHorizontal, RotateCcw, Trash2, UserPlus } from 'lucide-react';
import { Dropdown, MenuItem } from '../../components/ui/Dropdown';
import { useChangeCapacity, useChangeUserRole, useCreateUser, useMe, useUserAdmin, useUsers } from '../../api/hooks';
import { isManager } from '../../lib/permissions';
import type { Role } from '../../api/types';
import { ApiError } from '../../api/client';
import { Avatar } from '../../components/ui/Avatar';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Field } from '../../components/ui/Field';
import { Modal } from '../../components/ui/Modal';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { roleDescription, roleLabel } from '../../lib/labels';
import { isAdmin } from '../../lib/permissions';

const ROLES: Role[] = ['OWNER', 'ADMIN', 'MANAGER', 'MEMBER', 'GUEST'];
const roleTone = { OWNER: 'brand', ADMIN: 'brand', MANAGER: 'blue', MEMBER: 'neutral', GUEST: 'amber' } as const;

export function MembersTab() {
  const { data: users = [], isLoading } = useUsers();
  const { data: me } = useMe();
  const changeRole = useChangeUserRole();
  const admin$ = useUserAdmin();
  const changeCapacity = useChangeCapacity();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [temporary, setTemporary] = useState<{ name: string; password: string } | null>(null);
  const onError = (err: unknown) => toast('error', err instanceof ApiError ? err.message : 'Action impossible');
  const admin = isAdmin(me);
  const assignable = ROLES.filter((r) => r !== 'OWNER' || me?.role === 'OWNER');

  if (isLoading) return <Spinner />;
  return (
    <div className="grid gap-6 xl:grid-cols-[1fr_320px]">
      <div className="card overflow-hidden">
        <div className="flex items-center justify-between border-b border-line px-5 py-3">
          <h3 className="text-sm font-semibold">{users.length} membre{users.length > 1 ? 's' : ''}</h3>
          {admin && <Button size="sm" icon={<UserPlus className="size-4" />} onClick={() => setOpen(true)}>Ajouter</Button>}
        </div>
        <ul className="divide-y divide-line">
          {users.map((u) => {
            const editable = admin && u.id !== me?.id && (u.role !== 'OWNER' || me?.role === 'OWNER');
            return (
              <li key={u.id} className={`flex items-center gap-3 px-5 py-3 ${u.active ? '' : 'opacity-60'}`}>
                <Avatar name={u.displayName} />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">
                    {u.displayName}{u.id === me?.id && <span className="ml-1.5 text-xs text-ink-3">(vous)</span>}
                    {!u.active && <Badge tone="red" className="ml-2">Désactivé</Badge>}
                  </div>
                  <div className="truncate text-xs text-ink-3">{u.jobTitle ? `${u.jobTitle} · ` : ''}{u.email}</div>
                </div>
                {isManager(me) && u.role !== 'GUEST' ? (
                  <select className="rounded-lg border border-line bg-surface px-1.5 py-1 text-xs tabular-nums" title="Temps de travail (jours / semaine)"
                    aria-label={`Temps de travail de ${u.displayName}`} value={u.capacityDaysPerWeek}
                    onChange={(e) => changeCapacity.mutate({ userId: u.id, daysPerWeek: Number(e.target.value) }, { onSuccess: () => toast('success', 'Temps de travail mis à jour'), onError })}>
                    {[5, 4.5, 4, 3.5, 3, 2.5, 2, 1.5, 1, 0.5].map((d) => <option key={d} value={d}>{d} j/sem.</option>)}
                  </select>
                ) : u.role !== 'GUEST' && <span className="text-xs text-ink-3 tabular-nums">{u.capacityDaysPerWeek} j/sem.</span>}
                {editable ? (
                  <select
                    className="rounded-lg border border-line bg-surface px-2 py-1 text-sm"
                    value={u.role}
                    aria-label={`Rôle de ${u.displayName}`}
                    onChange={(e) => changeRole.mutate({ userId: u.id, role: e.target.value as Role }, {
                      onSuccess: () => toast('success', 'Rôle mis à jour'),
                      onError: (err) => toast('error', err instanceof ApiError ? err.message : 'Action impossible'),
                    })}
                  >
                    {assignable.map((r) => <option key={r} value={r}>{roleLabel[r]}</option>)}
                  </select>
                ) : (
                  <Badge tone={roleTone[u.role]}>{roleLabel[u.role]}</Badge>
                )}
                {editable && (
                  <Dropdown align="right" trigger={({ toggle }) => (
                    <button onClick={toggle} className="rounded-md p-1.5 text-ink-3 hover:bg-surface-2" aria-label={`Actions pour ${u.displayName}`}>
                      <MoreHorizontal className="size-4" />
                    </button>
                  )}>
                    {(close) => (
                      <>
                        <MenuItem onClick={() => { close(); admin$.resetPassword.mutate(u.id, { onSuccess: (r) => setTemporary({ name: u.displayName, password: r.temporaryPassword }), onError }); }}>
                          <KeyRound className="size-4 text-ink-3" />Réinitialiser le mot de passe
                        </MenuItem>
                        <MenuItem onClick={() => { close(); admin$.setActive.mutate({ userId: u.id, active: !u.active }, { onSuccess: () => toast('success', u.active ? 'Compte désactivé' : 'Compte réactivé'), onError }); }}>
                          {u.active ? <><Ban className="size-4 text-ink-3" />Désactiver le compte</> : <><RotateCcw className="size-4 text-ink-3" />Réactiver le compte</>}
                        </MenuItem>
                        <MenuItem onClick={() => {
                          close();
                          if (confirm(`Supprimer le compte de ${u.displayName} ? Ses données personnelles seront effacées (nom anonymisé dans l'historique). Action irréversible.`)) {
                            admin$.remove.mutate(u.id, { onSuccess: () => toast('success', 'Compte supprimé'), onError });
                          }
                        }}>
                          <Trash2 className="size-4 text-red-500" /><span className="text-red-600">Supprimer le compte</span>
                        </MenuItem>
                      </>
                    )}
                  </Dropdown>
                )}
              </li>
            );
          })}
        </ul>
      </div>
      <aside className="card h-fit p-5">
        <h3 className="mb-3 text-sm font-semibold">Rôles dans l'organisation</h3>
        <dl className="space-y-3 text-sm">
          {ROLES.map((r) => (
            <div key={r}>
              <dt><Badge tone={roleTone[r]}>{roleLabel[r]}</Badge></dt>
              <dd className="mt-1 text-xs text-ink-3">{roleDescription[r]}</dd>
            </div>
          ))}
        </dl>
      </aside>
      <NewUserModal open={open} onClose={() => setOpen(false)} roles={assignable} />
      <Modal open={temporary !== null} onClose={() => setTemporary(null)} title="Mot de passe provisoire"
        footer={<Button onClick={() => setTemporary(null)}>J'ai transmis le mot de passe</Button>}>
        <p className="mb-3 text-sm text-ink-2">
          Transmettez ce mot de passe à <strong>{temporary?.name}</strong> par un canal sûr. Il ne sera plus affiché,
          et devra être changé à la prochaine connexion. Les sessions en cours restent actives jusqu'à ce changement.
        </p>
        <div className="flex items-center gap-2 rounded-lg border border-line bg-surface-2 px-3 py-2 font-mono text-base">
          <span className="flex-1 select-all">{temporary?.password}</span>
          <button className="rounded p-1 text-ink-3 hover:text-ink" aria-label="Copier"
            onClick={() => temporary && navigator.clipboard?.writeText(temporary.password).then(() => toast('success', 'Copié'))}>
            <Copy className="size-4" />
          </button>
        </div>
      </Modal>
    </div>
  );
}

function NewUserModal({ open, onClose, roles }: { open: boolean; onClose: () => void; roles: Role[] }) {
  const create = useCreateUser();
  const toast = useToast();
  const empty = { email: '', displayName: '', password: '', role: 'MEMBER' as Role, jobTitle: '' };
  const [form, setForm] = useState(empty);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const submit = (e: FormEvent) => {
    e.preventDefault();
    create.mutate(form, {
      onSuccess: () => { toast('success', 'Utilisateur créé'); onClose(); setForm(empty); setErrors({}); },
      onError: (err) => err instanceof ApiError && setErrors(Object.keys(err.fieldErrors).length ? err.fieldErrors : { email: err.message }),
    });
  };

  return (
    <Modal open={open} onClose={onClose} title="Nouvel utilisateur" footer={<><Button variant="secondary" onClick={onClose}>Annuler</Button><Button type="submit" form="user-form" loading={create.isPending}>Créer</Button></>}>
      <form id="user-form" onSubmit={submit} className="space-y-4">
        <Field label="Nom complet" error={errors.displayName}><input className="input" required value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} /></Field>
        <Field label="Email" error={errors.email}><input className="input" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
        <Field label="Fonction"><input className="input" value={form.jobTitle} onChange={(e) => setForm({ ...form, jobTitle: e.target.value })} /></Field>
        <Field label="Rôle">
          <select className="input" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
            {roles.map((r) => <option key={r} value={r}>{roleLabel[r]} — {roleDescription[r]}</option>)}
          </select>
        </Field>
        <Field label="Mot de passe initial (12 caractères min.)" error={errors.password}><input className="input" type="password" autoComplete="new-password" minLength={12} required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field>
      </form>
    </Modal>
  );
}
