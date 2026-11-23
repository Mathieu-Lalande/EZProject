import { useState, type FormEvent } from 'react';
import { Gavel, Pencil, Plus, ShieldAlert, Trash2 } from 'lucide-react';
import clsx from 'clsx';
import type { DecisionStatus, Project, Risk, RiskInput, RiskStatus } from '../../api/types';
import { useDecisions, useRiskMutations, useRisks, useUsers } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Badge, type Tone } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { Field } from '../../components/ui/Field';
import { Modal } from '../../components/ui/Modal';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { formatDate } from '../../lib/format';
import { decisionStatusLabel, impactLabel, probabilityLabel, riskStatusLabel } from '../../lib/labels';
import { can } from '../../lib/permissions';

/** Niveau de criticité (statut réservé) : toujours accompagné du score écrit. */
function level(score: number): { tone: Tone; label: string; cell: string } {
  if (score >= 12) return { tone: 'red', label: 'Critique', cell: 'bg-red-500 text-white' };
  if (score >= 6) return { tone: 'amber', label: 'Élevé', cell: 'bg-amber-400 text-amber-950' };
  if (score >= 3) return { tone: 'blue', label: 'Modéré', cell: 'bg-sky-200 text-sky-900 dark:bg-sky-500/40 dark:text-white' };
  return { tone: 'neutral', label: 'Faible', cell: 'bg-surface-2 text-ink-2' };
}

const decisionTone: Record<DecisionStatus, Tone> = { PROPOSED: 'amber', APPROVED: 'green', REJECTED: 'neutral' };

export function RisksTab({ project }: { project: Project }) {
  const { data: risks = [], isLoading } = useRisks(project.id);
  const editable = can(project, 'RISK_EDIT') && project.modules.includes('RISKS');
  const [editing, setEditing] = useState<Risk | null | undefined>(undefined);
  const active = risks.filter((r) => r.status === 'OPEN' || r.status === 'MITIGATING');

  if (isLoading) return <Spinner />;
  return (
    <div className="space-y-6">
      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <section className="card p-5">
          <h3 className="mb-1 text-sm font-semibold">Matrice des risques actifs</h3>
          <p className="mb-4 text-xs text-ink-3">Probabilité × impact (1 à 4)</p>
          <RiskMatrix risks={active} />
        </section>
        <section className="card overflow-hidden">
          <div className="flex items-center justify-between border-b border-line px-5 py-3">
            <h3 className="text-sm font-semibold">Registre des risques</h3>
            {editable && <Button size="sm" icon={<Plus className="size-4" />} onClick={() => setEditing(null)}>Nouveau risque</Button>}
          </div>
          {risks.length === 0 ? (
            <div className="p-5"><EmptyState icon={<ShieldAlert className="size-5" />} title="Aucun risque identifié" /></div>
          ) : (
            <ul className="divide-y divide-line">
              {risks.map((r) => {
                const lv = level(r.score);
                const closed = r.status === 'CLOSED' || r.status === 'OCCURRED';
                return (
                  <li key={r.id} className={clsx('group flex items-start gap-3 px-5 py-3', closed && 'opacity-60')}>
                    <span className={clsx('mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg text-xs font-bold tabular-nums', lv.cell)}
                      title={`Criticité ${r.score} (${lv.label})`}>{r.score}</span>
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="text-sm font-medium">{r.title}</span>
                        <Badge tone={lv.tone}>{lv.label}</Badge>
                        <Badge>{riskStatusLabel[r.status]}</Badge>
                      </div>
                      {r.mitigation && <p className="mt-0.5 text-xs text-ink-2">Plan d'action : {r.mitigation}</p>}
                      <p className="mt-0.5 text-xs text-ink-3">
                        {r.ownerName ?? 'Sans responsable'}{r.dueDate ? ` · échéance ${formatDate(r.dueDate)}` : ''}
                      </p>
                    </div>
                    {editable && (
                      <button aria-label={`Modifier ${r.title}`} className="rounded p-1 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-ink focus:opacity-100" onClick={() => setEditing(r)}>
                        <Pencil className="size-4" />
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>
      <Decisions project={project} editable={editable} />
      <RiskModal project={project} risk={editing} onClose={() => setEditing(undefined)} />
    </div>
  );
}

function RiskMatrix({ risks }: { risks: Risk[] }) {
  return (
    <div className="flex gap-2">
      <div className="flex w-5 items-center justify-center">
        <span className="-rotate-90 text-[10px] whitespace-nowrap text-ink-3">Probabilité →</span>
      </div>
      <div className="flex-1">
        <div className="grid grid-cols-4 gap-0.5">
          {[4, 3, 2, 1].map((p) =>
            [1, 2, 3, 4].map((i) => {
              const inCell = risks.filter((r) => r.probability === p && r.impact === i);
              const lv = level(p * i);
              return (
                <div key={`${p}-${i}`} title={`${probabilityLabel[p]} × ${impactLabel[i]} : ${inCell.length} risque(s)${inCell.length ? ' — ' + inCell.map((r) => r.title).join(', ') : ''}`}
                  className={clsx('flex aspect-square items-center justify-center rounded-md text-sm font-semibold tabular-nums', inCell.length ? lv.cell : 'bg-surface-2/60 text-transparent')}>
                  {inCell.length || '·'}
                </div>
              );
            }),
          )}
        </div>
        <div className="mt-1 text-center text-[10px] text-ink-3">Impact →</div>
      </div>
    </div>
  );
}

function RiskModal({ project, risk, onClose }: { project: Project; risk: Risk | null | undefined; onClose: () => void }) {
  const { data: users = [] } = useUsers();
  const m = useRiskMutations(project.id);
  const toast = useToast();
  const open = risk !== undefined;
  const initial: RiskInput = risk
    ? { title: risk.title, description: risk.description, probability: risk.probability, impact: risk.impact, status: risk.status, ownerId: risk.ownerId, mitigation: risk.mitigation, dueDate: risk.dueDate }
    : { title: '', probability: 2, impact: 2, status: 'OPEN', ownerId: null, mitigation: '', dueDate: null };
  const [form, setForm] = useState<RiskInput>(initial);
  const [key, setKey] = useState<number | null | undefined>(undefined);
  if (open && key !== (risk?.id ?? null)) {
    setKey(risk?.id ?? null);
    setForm(initial);
  }
  if (!open && key !== undefined) setKey(undefined);
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    m.saveRisk.mutate({ id: risk?.id, input: { ...form, dueDate: form.dueDate || null, mitigation: form.mitigation || null } },
      { onSuccess: () => { toast('success', 'Risque enregistré'); onClose(); }, onError });
  };

  return (
    <Modal open={open} onClose={onClose} title={risk ? 'Modifier le risque' : 'Nouveau risque'} size="lg"
      footer={<>
        {risk && <Button variant="ghost" className="mr-auto text-red-600" icon={<Trash2 className="size-4" />}
          onClick={() => confirm('Supprimer ce risque ?') && m.removeRisk.mutate(risk.id, { onSuccess: onClose, onError })}>Supprimer</Button>}
        <Button variant="secondary" onClick={onClose}>Annuler</Button>
        <Button type="submit" form="risk-form" loading={m.saveRisk.isPending}>Enregistrer</Button>
      </>}>
      <form id="risk-form" onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
        <Field label="Risque" className="sm:col-span-2"><input className="input" required maxLength={200} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></Field>
        <Field label="Probabilité">
          <select className="input" value={form.probability} onChange={(e) => setForm({ ...form, probability: Number(e.target.value) })}>
            {[1, 2, 3, 4].map((v) => <option key={v} value={v}>{v} — {probabilityLabel[v]}</option>)}
          </select>
        </Field>
        <Field label="Impact">
          <select className="input" value={form.impact} onChange={(e) => setForm({ ...form, impact: Number(e.target.value) })}>
            {[1, 2, 3, 4].map((v) => <option key={v} value={v}>{v} — {impactLabel[v]}</option>)}
          </select>
        </Field>
        <Field label="Statut">
          <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as RiskStatus })}>
            {(Object.keys(riskStatusLabel) as RiskStatus[]).map((s) => <option key={s} value={s}>{riskStatusLabel[s]}</option>)}
          </select>
        </Field>
        <Field label="Responsable">
          <select className="input" value={form.ownerId ?? ''} onChange={(e) => setForm({ ...form, ownerId: e.target.value ? Number(e.target.value) : null })}>
            <option value="">—</option>
            {users.filter((u) => u.active).map((u) => <option key={u.id} value={u.id}>{u.displayName}</option>)}
          </select>
        </Field>
        <Field label="Plan d'action" className="sm:col-span-2"><textarea className="input min-h-20" maxLength={2000} value={form.mitigation ?? ''} onChange={(e) => setForm({ ...form, mitigation: e.target.value })} /></Field>
        <Field label="Échéance du plan"><input className="input" type="date" value={form.dueDate ?? ''} onChange={(e) => setForm({ ...form, dueDate: e.target.value || null })} /></Field>
        <p className="self-end text-sm text-ink-3">Criticité : <strong className="text-ink">{form.probability * form.impact}</strong> ({level(form.probability * form.impact).label})</p>
      </form>
    </Modal>
  );
}

function Decisions({ project, editable }: { project: Project; editable: boolean }) {
  const { data: decisions = [] } = useDecisions(project.id);
  const m = useRiskMutations(project.id);
  const toast = useToast();
  const [form, setForm] = useState({ title: '', decidedBy: '', decidedOn: '', status: 'PROPOSED' as DecisionStatus });
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  return (
    <section className="card overflow-hidden">
      <div className="flex items-center gap-2 border-b border-line px-5 py-3 text-sm font-semibold"><Gavel className="size-4 text-ink-3" />Relevé de décisions</div>
      <ul className="divide-y divide-line">
        {decisions.length === 0 && <li className="px-5 py-4 text-sm text-ink-3">Aucune décision enregistrée.</li>}
        {decisions.map((d) => (
          <li key={d.id} className="group flex items-center gap-3 px-5 py-2.5 text-sm">
            <span className="flex-1">{d.title}</span>
            <span className="text-xs text-ink-3">{[d.decidedBy, d.decidedOn && formatDate(d.decidedOn, true)].filter(Boolean).join(' · ')}</span>
            {editable ? (
              <select className="rounded-md border border-line bg-surface px-1.5 py-0.5 text-xs" value={d.status} aria-label={`Statut de ${d.title}`}
                onChange={(e) => m.saveDecision.mutate({ id: d.id, input: { title: d.title, description: d.description, decidedOn: d.decidedOn, decidedBy: d.decidedBy, status: e.target.value as DecisionStatus } }, { onError })}>
                {(Object.keys(decisionStatusLabel) as DecisionStatus[]).map((s) => <option key={s} value={s}>{decisionStatusLabel[s]}</option>)}
              </select>
            ) : <Badge tone={decisionTone[d.status]}>{decisionStatusLabel[d.status]}</Badge>}
            {editable && (
              <button aria-label={`Supprimer ${d.title}`} className="rounded p-1 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                onClick={() => m.removeDecision.mutate(d.id, { onError })}><Trash2 className="size-4" /></button>
            )}
          </li>
        ))}
      </ul>
      {editable && (
        <form className="flex flex-wrap items-end gap-3 border-t border-line bg-surface-2/40 px-5 py-4"
          onSubmit={(e) => {
            e.preventDefault();
            m.saveDecision.mutate({ input: { title: form.title.trim(), decidedBy: form.decidedBy || null, decidedOn: form.decidedOn || null, status: form.status } },
              { onSuccess: () => setForm({ title: '', decidedBy: '', decidedOn: '', status: 'PROPOSED' }), onError });
          }}>
          <Field label="Décision" className="min-w-56 flex-1"><input className="input" required maxLength={200} value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></Field>
          <Field label="Instance"><input className="input w-40" maxLength={150} placeholder="COPIL, client…" value={form.decidedBy} onChange={(e) => setForm({ ...form, decidedBy: e.target.value })} /></Field>
          <Field label="Date"><input className="input w-40" type="date" value={form.decidedOn} onChange={(e) => setForm({ ...form, decidedOn: e.target.value })} /></Field>
          <Field label="Statut">
            <select className="input w-36" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as DecisionStatus })}>
              {(Object.keys(decisionStatusLabel) as DecisionStatus[]).map((s) => <option key={s} value={s}>{decisionStatusLabel[s]}</option>)}
            </select>
          </Field>
          <Button type="submit" icon={<Plus className="size-4" />} loading={m.saveDecision.isPending}>Ajouter</Button>
        </form>
      )}
    </section>
  );
}
