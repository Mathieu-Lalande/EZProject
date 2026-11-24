import { KeyRound } from 'lucide-react';
import { useLogout } from '../api/hooks';
import { Button } from '../components/ui/Button';
import { PasswordForm } from '../features/account/PasswordForm';

/** Affiché tant qu'un mot de passe provisoire (défini par un administrateur) n'a pas été remplacé. */
export function ForcePasswordChange() {
  const logout = useLogout();
  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="card w-full max-w-2xl animate-pop-in p-8">
        <div className="mb-4 flex size-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-300">
          <KeyRound className="size-5" />
        </div>
        <h1 className="text-xl font-semibold">Choisissez votre mot de passe</h1>
        <p className="mt-1 mb-6 text-sm text-ink-3">
          Un administrateur vous a attribué un mot de passe provisoire. Remplacez-le pour accéder à EZProject.
        </p>
        <PasswordForm currentLabel="Mot de passe provisoire" onDone={() => undefined} />
        <Button variant="ghost" className="mt-4" onClick={() => logout.mutate()}>Se déconnecter</Button>
      </div>
    </div>
  );
}
