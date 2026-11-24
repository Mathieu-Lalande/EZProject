import { useEffect, useState, type FormEvent } from 'react';
import { Download, KeyRound, ShieldAlert, UserRound } from 'lucide-react';
import { useAccountMutations, useDeleteAccount, useMe } from '../api/hooks';
import { ApiError } from '../api/client';
import { PageHeader } from '../components/ui/PageHeader';
import { Button } from '../components/ui/Button';
import { Field } from '../components/ui/Field';
import { Badge } from '../components/ui/Badge';
import { useToast } from '../components/ui/Toast';
import { roleLabel } from '../lib/labels';
import { PasswordForm } from '../features/account/PasswordForm';
import { MfaSettings } from '../features/account/MfaSettings';

/** Espace personnel : profil, sécurité, données personnelles (RGPD). */
export function AccountPage() {
  const { data: me } = useMe();
  const m = useAccountMutations();
  const toast = useToast();
  const [displayName, setDisplayName] = useState('');
  const [jobTitle, setJobTitle] = useState('');

  useEffect(() => {
    setDisplayName(me?.displayName ?? '');
    setJobTitle(me?.jobTitle ?? '');
  }, [me?.displayName, me?.jobTitle]);

  if (!me) return null;

  const saveProfile = (e: FormEvent) => {
    e.preventDefault();
    m.profile.mutate(
      { displayName: displayName.trim(), jobTitle: jobTitle.trim() || null },
      {
        onSuccess: () => toast('success', 'Profil mis à jour'),
        onError: (err) => toast('error', err instanceof ApiError ? err.message : 'Action impossible'),
      },
    );
  };

  return (
    <div className="max-w-3xl animate-fade-in space-y-6">
      <PageHeader
        title="Mon compte"
        subtitle={<span className="flex items-center gap-2">{me.email}<Badge tone="brand">{roleLabel[me.role]}</Badge></span>}
      />

      <section className="card p-5">
        <h2 className="mb-4 flex items-center gap-2 text-sm font-semibold"><UserRound className="size-4 text-ink-3" />Profil</h2>
        <form onSubmit={saveProfile} className="grid gap-4 sm:grid-cols-2">
          <Field label="Nom affiché">
            <input className="input" required maxLength={120} value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </Field>
          <Field label="Fonction">
            <input className="input" maxLength={120} value={jobTitle} onChange={(e) => setJobTitle(e.target.value)} />
          </Field>
          <div className="sm:col-span-2">
            <Button type="submit" loading={m.profile.isPending}>Enregistrer</Button>
          </div>
        </form>
      </section>

      <section className="card p-5">
        <h2 className="mb-4 flex items-center gap-2 text-sm font-semibold"><KeyRound className="size-4 text-ink-3" />Mot de passe</h2>
        <PasswordForm onDone={() => toast('success', 'Mot de passe modifié')} />
      </section>

      <MfaSettings />

      <section className="card p-5">
        <h2 className="mb-1 flex items-center gap-2 text-sm font-semibold"><Download className="size-4 text-ink-3" />Mes données</h2>
        <p className="mb-4 text-xs text-ink-3">
          Téléchargez toutes vos données personnelles au format JSON (droit à la portabilité).
        </p>
        <a href="/api/account/export" download className="inline-flex h-9 items-center gap-2 rounded-lg border border-line bg-surface px-3.5 text-sm font-medium hover:bg-surface-2">
          <Download className="size-4" />Exporter mes données
        </a>
      </section>

      <DeleteAccount />
    </div>
  );
}

function DeleteAccount() {
  const remove = useDeleteAccount();
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');

  return (
    <section className="card border-red-200 p-5 dark:border-red-500/30">
      <h2 className="mb-1 flex items-center gap-2 text-sm font-semibold text-red-600"><ShieldAlert className="size-4" />Supprimer mon compte</h2>
      <p className="mb-4 text-xs text-ink-3">
        Vos données personnelles sont effacées et votre nom est remplacé par « Utilisateur supprimé » dans l'historique des projets.
        Action irréversible.
      </p>
      {!open ? (
        <Button variant="secondary" className="text-red-600" onClick={() => setOpen(true)}>Supprimer mon compte…</Button>
      ) : (
        <form
          className="flex flex-wrap items-end gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            remove.mutate(password, { onError: (err) => toast('error', err instanceof ApiError ? err.message : 'Action impossible') });
          }}
        >
          <Field label="Confirmez avec votre mot de passe" className="min-w-64 flex-1">
            <input className="input" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </Field>
          <Button variant="ghost" onClick={() => setOpen(false)}>Annuler</Button>
          <Button type="submit" variant="danger" loading={remove.isPending}>Supprimer définitivement</Button>
        </form>
      )}
    </section>
  );
}
