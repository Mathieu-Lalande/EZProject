import { useEffect, useState, type FormEvent } from 'react';
import QRCode from 'qrcode';
import { ShieldCheck, ShieldOff } from 'lucide-react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError, request } from '../../api/client';
import { keys, useMe } from '../../api/hooks';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Field } from '../../components/ui/Field';
import { useToast } from '../../components/ui/Toast';
import { isAdmin } from '../../lib/permissions';

/** Double authentification : enrôlement par QR code, confirmation, codes de secours, désactivation. */
export function MfaSettings() {
  const { data: me } = useMe();
  const qc = useQueryClient();
  const toast = useToast();
  const [setup, setSetup] = useState<{ secret: string; otpauthUri: string } | null>(null);
  const [qr, setQr] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [codes, setCodes] = useState<string[] | null>(null);
  const [password, setPassword] = useState('');
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  const start = useMutation({ mutationFn: () => api.post<{ secret: string; otpauthUri: string }>('/api/account/mfa/setup') });
  const enable = useMutation({ mutationFn: (c: string) => api.post<{ recoveryCodes: string[] }>('/api/account/mfa/enable', { code: c }) });
  const disable = useMutation({ mutationFn: (p: string) => request('DELETE', '/api/account/mfa', { password: p }) });

  useEffect(() => {
    // QR code généré localement : le secret ne transite vers aucun service tiers
    if (setup) void QRCode.toDataURL(setup.otpauthUri, { margin: 1, width: 192 }).then(setQr);
  }, [setup]);

  if (!me) return null;

  const confirm = (e: FormEvent) => {
    e.preventDefault();
    enable.mutate(code.trim(), {
      onSuccess: (r) => { setCodes(r.recoveryCodes); setSetup(null); setCode(''); void qc.invalidateQueries({ queryKey: keys.me }); },
      onError,
    });
  };

  return (
    <section className="card p-5">
      <h2 className="mb-1 flex items-center gap-2 text-sm font-semibold">
        <ShieldCheck className="size-4 text-ink-3" />Double authentification
        {me.mfaEnabled ? <Badge tone="green">Activée</Badge> : <Badge tone={isAdmin(me) ? 'amber' : 'neutral'}>Désactivée</Badge>}
      </h2>
      <p className="mb-4 text-xs text-ink-3">
        Un code à 6 chiffres généré par une application (Google Authenticator, Microsoft Authenticator, 1Password…) est demandé à chaque connexion.
        {!me.mfaEnabled && isAdmin(me) && ' Fortement recommandé pour les comptes administrateurs.'}
      </p>

      {codes && (
        <div className="mb-4 rounded-lg border border-amber-300 bg-amber-50 p-4 dark:border-amber-500/30 dark:bg-amber-500/10">
          <p className="mb-2 text-sm font-medium">Codes de secours — notez-les maintenant, ils ne seront plus affichés.</p>
          <p className="mb-3 text-xs text-ink-3">Chacun permet une connexion si vous perdez votre téléphone (usage unique).</p>
          <ul className="grid grid-cols-2 gap-1 font-mono text-sm sm:grid-cols-4">{codes.map((c) => <li key={c}>{c}</li>)}</ul>
          <Button size="sm" className="mt-3" onClick={() => setCodes(null)}>C'est noté</Button>
        </div>
      )}

      {!me.mfaEnabled && !setup && (
        <Button icon={<ShieldCheck className="size-4" />} loading={start.isPending}
          onClick={() => start.mutate(undefined, { onSuccess: setSetup, onError })}>Activer la double authentification</Button>
      )}

      {setup && (
        <div className="flex flex-wrap items-start gap-6">
          {qr && <img src={qr} alt="QR code à scanner avec votre application d'authentification" className="size-48 rounded-lg border border-line bg-white p-1" />}
          <form onSubmit={confirm} className="min-w-64 flex-1 space-y-3">
            <ol className="list-decimal space-y-1 pl-4 text-sm text-ink-2">
              <li>Scannez le QR code avec votre application.</li>
              <li>Ou saisissez la clé : <code className="font-mono text-xs break-all select-all">{setup.secret}</code></li>
              <li>Entrez le code affiché pour confirmer.</li>
            </ol>
            <Field label="Code à 6 chiffres">
              <input className="input w-40 font-mono tracking-widest" inputMode="numeric" autoComplete="one-time-code" pattern="\d{6}" maxLength={6} required
                value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} />
            </Field>
            <div className="flex gap-2">
              <Button type="submit" loading={enable.isPending}>Confirmer</Button>
              <Button variant="ghost" onClick={() => setSetup(null)}>Annuler</Button>
            </div>
          </form>
        </div>
      )}

      {me.mfaEnabled && (
        <form className="flex flex-wrap items-end gap-2" onSubmit={(e) => {
          e.preventDefault();
          disable.mutate(password, {
            onSuccess: () => { setPassword(''); toast('success', 'Double authentification désactivée'); void qc.invalidateQueries({ queryKey: keys.me }); },
            onError,
          });
        }}>
          <Field label="Mot de passe pour désactiver" className="min-w-56">
            <input className="input" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
          </Field>
          <Button type="submit" variant="secondary" icon={<ShieldOff className="size-4" />} loading={disable.isPending}>Désactiver</Button>
        </form>
      )}
    </section>
  );
}
