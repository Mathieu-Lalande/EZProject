import { useState, type FormEvent } from 'react';
import { UserPlus } from 'lucide-react';
import { useCreateUser, useMe, useUsers } from '../api/hooks';
import type { Role } from '../api/types';
import { ApiError } from '../api/client';
import { PageHeader } from '../components/ui/PageHeader';
import { Spinner } from '../components/ui/Spinner';
import { Avatar } from '../components/ui/Avatar';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Field } from '../components/ui/Field';
import { useToast } from '../components/ui/Toast';
import { roleLabel } from '../lib/labels';

export function TeamPage() {
  const { data: users = [], isLoading } = useUsers();
  const { data: me } = useMe();
  const [open, setOpen] = useState(false);

  if (isLoading) return <Spinner />;
  return (
    <div className="animate-fade-in">
      <PageHeader
        title="Équipe"
        subtitle={`${users.length} membre${users.length > 1 ? 's' : ''}`}
        actions={me?.role === 'ADMIN' && <Button icon={<UserPlus className="size-4" />} onClick={() => setOpen(true)}>Ajouter</Button>}
      />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {users.map((u) => (
          <div key={u.id} className="card flex items-center gap-3 p-4">
            <Avatar name={u.displayName} />
            <div className="min-w-0 flex-1">
              <div className="truncate font-medium">{u.displayName}</div>
              <div className="truncate text-xs text-ink-3">{u.jobTitle ?? u.email}</div>
            </div>
            <Badge tone={u.role === 'ADMIN' ? 'brand' : u.role === 'MANAGER' ? 'blue' : 'neutral'}>{roleLabel[u.role]}</Badge>
          </div>
        ))}
      </div>
      <NewUserModal open={open} onClose={() => setOpen(false)} />
    </div>
  );
}

function NewUserModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const create = useCreateUser();
  const toast = useToast();
  const [form, setForm] = useState({ email: '', displayName: '', password: '', role: 'MEMBER' as Role, jobTitle: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const submit = (e: FormEvent) => {
    e.preventDefault();
    create.mutate(form, {
      onSuccess: () => { toast('success', 'Utilisateur créé'); onClose(); setForm({ email: '', displayName: '', password: '', role: 'MEMBER', jobTitle: '' }); },
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
            {(Object.keys(roleLabel) as Role[]).map((r) => <option key={r} value={r}>{roleLabel[r]}</option>)}
          </select>
        </Field>
        <Field label="Mot de passe initial (12 caractères min.)" error={errors.password}><input className="input" type="password" autoComplete="new-password" minLength={12} required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field>
      </form>
    </Modal>
  );
}
