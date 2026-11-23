import { useState, type FormEvent } from 'react';
import { UserPlus } from 'lucide-react';
import { useChangeUserRole, useCreateUser, useMe, useUsers } from '../../api/hooks';
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
  const toast = useToast();
  const [open, setOpen] = useState(false);
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
              <li key={u.id} className="flex items-center gap-3 px-5 py-3">
                <Avatar name={u.displayName} />
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">{u.displayName}{u.id === me?.id && <span className="ml-1.5 text-xs text-ink-3">(vous)</span>}</div>
                  <div className="truncate text-xs text-ink-3">{u.jobTitle ? `${u.jobTitle} · ` : ''}{u.email}</div>
                </div>
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
