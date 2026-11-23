import { useState, type FormEvent } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useLogin, useMe, useSignup } from '../api/hooks';
import { ApiError } from '../api/client';
import { Button } from '../components/ui/Button';
import { Field } from '../components/ui/Field';

/** Inscription self-service : crée l'organisation (offre Free) et son compte propriétaire. */
export function SignupPage() {
  const { data: me } = useMe();
  const signup = useSignup();
  const login = useLogin();
  const [form, setForm] = useState({ organizationName: '', displayName: '', email: '', password: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);

  // Après inscription, on oriente le nouveau propriétaire vers la configuration de ses équipes
  if (me) return <Navigate to={signup.isSuccess ? '/organization/teams' : '/'} replace />;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setErrors({});
    setError(null);
    signup.mutate(form, {
      onSuccess: () => login.mutate({ email: form.email, password: form.password }),
      onError: (err) => {
        if (err instanceof ApiError) {
          setErrors(err.fieldErrors);
          if (!Object.keys(err.fieldErrors).length) setError(err.message);
        }
      },
    });
  };

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value });

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand-50 via-canvas to-violet-50 p-6 dark:from-canvas dark:via-canvas dark:to-canvas">
      <form onSubmit={submit} className="card w-full max-w-md animate-pop-in p-8">
        <div className="mb-6 flex items-center gap-2.5 font-semibold">
          <span className="flex size-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-violet-600 text-xs font-bold text-white">EZ</span>
          EZProject
        </div>
        <h1 className="text-2xl font-semibold tracking-tight">Créer votre espace</h1>
        <p className="mt-1 mb-6 text-sm text-ink-3">Gratuit jusqu'à 3 projets et 5 utilisateurs. Sans carte bancaire.</p>
        <div className="space-y-4">
          <Field label="Nom de l'organisation" error={errors.organizationName}>
            <input className="input" required maxLength={150} value={form.organizationName} onChange={set('organizationName')} placeholder="ex. Cabinet Dupont & Associés" />
          </Field>
          <Field label="Votre nom" error={errors.displayName}>
            <input className="input" required maxLength={120} autoComplete="name" value={form.displayName} onChange={set('displayName')} />
          </Field>
          <Field label="Email professionnel" error={errors.email}>
            <input className="input" type="email" required autoComplete="username" value={form.email} onChange={set('email')} />
          </Field>
          <Field label="Mot de passe (12 caractères minimum)" error={errors.password}>
            <input className="input" type="password" required minLength={12} autoComplete="new-password" value={form.password} onChange={set('password')} />
          </Field>
          {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">{error}</p>}
          <Button type="submit" className="w-full" loading={signup.isPending || login.isPending}>Créer mon organisation</Button>
        </div>
        <p className="mt-6 text-center text-sm text-ink-3">
          Déjà un compte ? <Link to="/login" className="font-medium text-brand-600 hover:underline">Se connecter</Link>
        </p>
      </form>
    </div>
  );
}
