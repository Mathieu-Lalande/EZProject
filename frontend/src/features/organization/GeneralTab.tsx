import { useEffect, useState } from 'react';
import { Check, Sparkles } from 'lucide-react';
import clsx from 'clsx';
import { useMe, useOrganization, useOrganizationMutations } from '../../api/hooks';
import type { Plan } from '../../api/types';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { isAdmin } from '../../lib/permissions';
import { planLabel } from '../../lib/labels';

const PLANS: { id: Plan; tagline: string; features: string[] }[] = [
  { id: 'FREE', tagline: 'Pour démarrer', features: ['3 projets', '5 utilisateurs', 'Tous les modules', 'Rôles prédéfinis'] },
  { id: 'TEAM', tagline: 'Pour les équipes et PMO', features: ['50 projets', '50 utilisateurs', 'Équipes illimitées', 'Permissions personnalisables'] },
  { id: 'ENTERPRISE', tagline: 'Pour toute l’organisation', features: ['Projets illimités', 'Utilisateurs illimités', 'Permissions personnalisables', 'Hébergement dédié possible'] },
];

const limit = (n: number) => (n < 0 ? '∞' : String(n));

export function GeneralTab() {
  const { data: org, isLoading } = useOrganization();
  const { data: me } = useMe();
  const m = useOrganizationMutations();
  const toast = useToast();
  const [name, setName] = useState('');
  useEffect(() => setName(org?.name ?? ''), [org?.name]);

  if (isLoading || !org) return <Spinner />;
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  return (
    <div className="space-y-6">
      <section className="card max-w-2xl p-5">
        <h3 className="mb-3 text-sm font-semibold">Organisation</h3>
        <div className="flex gap-2">
          <input className="input" value={name} disabled={!isAdmin(me)} maxLength={150} onChange={(e) => setName(e.target.value)} aria-label="Nom de l'organisation" />
          {isAdmin(me) && (
            <Button disabled={!name.trim() || name === org.name} loading={m.rename.isPending}
              onClick={() => m.rename.mutate(name, { onSuccess: () => toast('success', 'Organisation renommée'), onError })}>
              Enregistrer
            </Button>
          )}
        </div>
        <p className="mt-2 text-xs text-ink-3">Identifiant : {org.slug}</p>
      </section>

      <section className="card max-w-2xl p-5">
        <h3 className="mb-4 text-sm font-semibold">Utilisation</h3>
        <Usage label="Projets" used={org.usage.projects} max={org.usage.maxProjects} />
        <Usage label="Utilisateurs" used={org.usage.users} max={org.usage.maxUsers} />
      </section>

      <section>
        <h3 className="mb-3 text-sm font-semibold">Offre</h3>
        <div className="grid gap-4 md:grid-cols-3">
          {PLANS.map((p) => {
            const current = org.plan === p.id;
            return (
              <div key={p.id} className={clsx('card flex flex-col p-5', current && 'border-brand-500 ring-1 ring-brand-500/40')}>
                <div className="flex items-center justify-between">
                  <span className="text-lg font-semibold">{planLabel[p.id]}</span>
                  {current && <span className="rounded-full bg-brand-600 px-2 py-0.5 text-[11px] font-medium text-white">Offre actuelle</span>}
                </div>
                <p className="mb-4 text-xs text-ink-3">{p.tagline}</p>
                <ul className="mb-5 space-y-1.5 text-sm">
                  {p.features.map((f) => <li key={f} className="flex items-center gap-2"><Check className="size-4 text-emerald-500" />{f}</li>)}
                </ul>
                {me?.role === 'OWNER' && !current && (
                  <Button className="mt-auto" variant={p.id === 'FREE' ? 'secondary' : 'primary'} icon={p.id !== 'FREE' ? <Sparkles className="size-4" /> : undefined}
                    onClick={() => m.changePlan.mutate(p.id, { onSuccess: () => toast('success', `Offre ${planLabel[p.id]} activée`), onError })}>
                    Passer à {planLabel[p.id]}
                  </Button>
                )}
              </div>
            );
          })}
        </div>
        <p className="mt-3 text-xs text-ink-3">La facturation n'est pas encore branchée : le changement d'offre est immédiat et réservé au propriétaire.</p>
      </section>
    </div>
  );
}

function Usage({ label, used, max }: { label: string; used: number; max: number }) {
  const pct = max < 0 ? 5 : (used / Math.max(1, max)) * 100;
  return (
    <div className="mb-3">
      <div className="mb-1 flex justify-between text-sm"><span>{label}</span><span className="tabular-nums text-ink-3">{used} / {limit(max)}</span></div>
      <ProgressBar value={pct} color={pct >= 90 ? '#ef4444' : undefined} />
    </div>
  );
}
