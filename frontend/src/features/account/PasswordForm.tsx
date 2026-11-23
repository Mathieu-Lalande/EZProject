import { useState, type FormEvent } from 'react';
import { useAccountMutations } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Field } from '../../components/ui/Field';

const MIN_LENGTH = 12;

/** Changement de mot de passe : vérifie l'actuel, impose 12 caractères et une confirmation identique. */
export function PasswordForm({ onDone, currentLabel = 'Mot de passe actuel' }: { onDone: () => void; currentLabel?: string }) {
  const { password } = useAccountMutations();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mismatch = confirm.length > 0 && confirm !== next;
  const tooShort = next.length > 0 && next.length < MIN_LENGTH;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    password.mutate(
      { currentPassword: current, newPassword: next },
      {
        onSuccess: () => {
          setCurrent('');
          setNext('');
          setConfirm('');
          onDone();
        },
        onError: (err) => setError(err instanceof ApiError ? Object.values(err.fieldErrors)[0] ?? err.message : 'Action impossible'),
      },
    );
  };

  return (
    <form onSubmit={submit} className="grid gap-4 sm:grid-cols-3">
      <Field label={currentLabel}>
        <input className="input" type="password" autoComplete="current-password" required value={current} onChange={(e) => setCurrent(e.target.value)} />
      </Field>
      <Field label={`Nouveau (${MIN_LENGTH} caractères min.)`} error={tooShort ? 'Trop court' : undefined}>
        <input className="input" type="password" autoComplete="new-password" required minLength={MIN_LENGTH} maxLength={128} value={next} onChange={(e) => setNext(e.target.value)} />
      </Field>
      <Field label="Confirmation" error={mismatch ? 'Ne correspond pas' : undefined}>
        <input className="input" type="password" autoComplete="new-password" required value={confirm} onChange={(e) => setConfirm(e.target.value)} />
      </Field>
      {error && <p role="alert" className="text-sm text-red-600 sm:col-span-3">{error}</p>}
      <div className="sm:col-span-3">
        <Button type="submit" disabled={mismatch || tooShort || !next} loading={password.isPending}>Changer le mot de passe</Button>
      </div>
    </form>
  );
}
