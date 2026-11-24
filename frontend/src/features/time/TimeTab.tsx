import { useState, type FormEvent } from 'react';
import { AlertTriangle, ChevronLeft, ChevronRight, Plus, Trash2, UserPlus } from 'lucide-react';
import clsx from 'clsx';
import type { Project } from '../../api/types';
import { useAllocationMutations, useAllocations, useTasks, useTimeEntries, useTimeMutations, useTimeSummary, useUsers } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Field } from '../../components/ui/Field';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { Avatar } from '../../components/ui/Avatar';
import { formatDate, toIsoDate } from '../../lib/format';
import { can } from '../../lib/permissions';

const num = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
const monthFmt = new Intl.DateTimeFormat('fr-FR', { month: 'short', year: '2-digit' });

/** Temps passés, consommation du budget (jauge + rythme) et staffing du projet. */
export function TimeTab({ project }: { project: Project }) {
  const { data: summary, isLoading } = useTimeSummary(project.id);
  if (isLoading || !summary) return <Spinner />;

  const pct = summary.consumptionPct;
  const maxUser = Math.max(1, ...summary.byUser.map((u) => u.days));
  const months = summary.byMonth.slice(-12);
  const maxMonth = Math.max(1, ...months.map((m) => m.days));
  const tone = pct === null ? 'brand' : pct > 100 ? 'critical' : pct > 85 ? 'warning' : 'brand';

  return (
    <div className="space-y-6">
      <div className="grid gap-6 lg:grid-cols-3">
        <section className="card p-5 lg:col-span-1">
          <h3 className="text-sm font-semibold">Budget</h3>
          {summary.budgetDays === null ? (
            <p className="mt-3 text-sm text-ink-3">Aucun budget défini. Renseignez « Budget (jours) » dans les informations du projet.</p>
          ) : (
            <>
              <div className="mt-3 flex items-end gap-2">
                <span className="text-3xl font-semibold tabular-nums">{pct}%</span>
                <span className="mb-1 text-sm text-ink-3">consommé</span>
              </div>
              <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-surface-2" role="progressbar" aria-valuenow={pct ?? 0} aria-valuemin={0} aria-valuemax={100}
                title={`${num.format(summary.consumedDays)} j sur ${summary.budgetDays} j`}>
                <div className={clsx('h-full rounded-full', tone === 'critical' ? 'bg-red-500' : tone === 'warning' ? 'bg-amber-500' : 'bg-brand-500')}
                  style={{ width: `${Math.min(100, pct ?? 0)}%` }} />
              </div>
              <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                <Stat label="Consommé" value={`${num.format(summary.consumedDays)} j`} />
                <Stat label="Reste" value={`${num.format(summary.remainingDays ?? 0)} j`} />
                <Stat label="Rythme (4 sem.)" value={`${num.format(summary.weeklyBurn)} j/sem.`} />
                <Stat label="Épuisement prévu" value={summary.projectedExhaustion ? formatDate(summary.projectedExhaustion, true) : '—'} />
              </dl>
              {summary.overBudget && (
                <p className="mt-4 flex items-center gap-1.5 text-sm font-medium text-red-600"><AlertTriangle className="size-4" />Budget dépassé</p>
              )}
              {!summary.overBudget && summary.projectedExhaustion && project.endDate && summary.projectedExhaustion < project.endDate && (
                <p className="mt-4 flex items-center gap-1.5 text-sm text-amber-700 dark:text-amber-300">
                  <AlertTriangle className="size-4" />À ce rythme, le budget sera épuisé avant l'échéance ({formatDate(project.endDate)}).
                </p>
              )}
            </>
          )}
        </section>

        <section className="card p-5">
          <h3 className="mb-4 text-sm font-semibold">Jours passés par personne</h3>
          {summary.byUser.length === 0 ? <p className="text-sm text-ink-3">Aucun temps saisi.</p> : (
            <ul className="space-y-3">
              {summary.byUser.map((u) => (
                <li key={u.userId} title={`${u.name} : ${num.format(u.days)} j`}>
                  <div className="mb-1 flex justify-between text-xs"><span className="font-medium">{u.name}</span><span className="tabular-nums text-ink-3">{num.format(u.days)} j</span></div>
                  <div className="h-1.5 rounded-full bg-surface-2"><div className="h-full rounded-full bg-brand-500" style={{ width: `${(u.days / maxUser) * 100}%` }} /></div>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card p-5">
          <h3 className="mb-4 text-sm font-semibold">Jours passés par mois</h3>
          {months.length === 0 ? <p className="text-sm text-ink-3">Aucun temps saisi.</p> : (
            <div className="flex h-36 items-end gap-0.5">
              {months.map((m) => (
                <div key={m.month} className="group flex h-full max-w-12 flex-1 flex-col justify-end" title={`${monthLabel(m.month)} : ${num.format(m.days)} j`}>
                  <span className="mb-1 text-center text-[10px] text-ink-3 opacity-0 tabular-nums group-hover:opacity-100">{num.format(m.days)}</span>
                  <div className="rounded-t-[4px] bg-brand-500 transition group-hover:bg-brand-600" style={{ height: `${Math.max(2, (m.days / maxMonth) * 100)}%` }} />
                  <span className="mt-1 truncate text-center text-[10px] text-ink-3">{monthLabel(m.month)}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <Staffing project={project} />
      <Entries project={project} />
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-ink-3">{label}</dt>
      <dd className="font-semibold tabular-nums">{value}</dd>
    </div>
  );
}

function monthLabel(ym: string) {
  const [y, m] = ym.split('-').map(Number);
  return monthFmt.format(new Date(y ?? 2000, (m ?? 1) - 1, 1));
}

function Entries({ project }: { project: Project }) {
  const [page, setPage] = useState(0);
  const { data } = useTimeEntries(project.id, page);
  const { data: tasks = [] } = useTasks(project.id, can(project, 'TASK_VIEW'));
  const { data: users = [] } = useUsers();
  const m = useTimeMutations(project.id);
  const toast = useToast();
  const today = toIsoDate(new Date());
  const [form, setForm] = useState({ date: today, days: '1', taskId: '', userId: '', comment: '' });
  const canLog = can(project, 'TIME_LOG') && project.modules.includes('TIME_TRACKING');
  const canManage = can(project, 'TIME_MANAGE');
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    m.log.mutate(
      { date: form.date, days: Number(form.days), taskId: form.taskId ? Number(form.taskId) : null, userId: form.userId ? Number(form.userId) : null, comment: form.comment || undefined },
      { onSuccess: () => { toast('success', 'Temps enregistré'); setForm({ ...form, comment: '' }); }, onError },
    );
  };

  return (
    <section className="card overflow-hidden">
      <div className="border-b border-line px-5 py-3 text-sm font-semibold">Temps saisis</div>
      {canLog && (
        <form onSubmit={submit} className="flex flex-wrap items-end gap-3 border-b border-line bg-surface-2/40 px-5 py-4">
          <Field label="Date"><input className="input w-40" type="date" max={today} required value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
          <Field label="Durée">
            <select className="input w-32" value={form.days} onChange={(e) => setForm({ ...form, days: e.target.value })}>
              {[0.125, 0.25, 0.5, 0.75, 1].map((d) => <option key={d} value={d}>{d === 0.125 ? '1 h' : `${num.format(d)} j`}</option>)}
            </select>
          </Field>
          {tasks.length > 0 && (
            <Field label="Tâche (optionnel)">
              <select className="input w-56" value={form.taskId} onChange={(e) => setForm({ ...form, taskId: e.target.value })}>
                <option value="">—</option>
                {tasks.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}
              </select>
            </Field>
          )}
          {canManage && (
            <Field label="Pour">
              <select className="input w-44" value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })}>
                <option value="">Moi</option>
                {users.filter((u) => u.active).map((u) => <option key={u.id} value={u.id}>{u.displayName}</option>)}
              </select>
            </Field>
          )}
          <Field label="Commentaire" className="min-w-48 flex-1"><input className="input" maxLength={300} value={form.comment} onChange={(e) => setForm({ ...form, comment: e.target.value })} /></Field>
          <Button type="submit" icon={<Plus className="size-4" />} loading={m.log.isPending}>Saisir</Button>
        </form>
      )}
      {!data || data.content.length === 0 ? (
        <p className="px-5 py-6 text-sm text-ink-3">Aucun temps saisi.</p>
      ) : (
        <table className="w-full text-sm">
          <tbody>
            {data.content.map((e) => (
              <tr key={e.id} className="group border-b border-line last:border-0">
                <td className="px-5 py-2.5 whitespace-nowrap text-ink-3 tabular-nums">{formatDate(e.date, true)}</td>
                <td className="px-3 py-2.5 whitespace-nowrap">{e.userName}</td>
                <td className="px-3 py-2.5 font-semibold whitespace-nowrap tabular-nums">{num.format(e.days)} j</td>
                <td className="w-full px-3 py-2.5 text-ink-2">{[e.taskTitle, e.comment].filter(Boolean).join(' — ')}</td>
                <td className="px-5 py-2.5">
                  {e.editable && (
                    <button aria-label="Supprimer ce temps" className="rounded p-1 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                      onClick={() => m.remove.mutate(e.id, { onError })}><Trash2 className="size-4" /></button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 border-t border-line px-5 py-2 text-xs text-ink-3">
          Page {data.page + 1} / {data.totalPages}
          <Button size="sm" variant="secondary" aria-label="Page précédente" disabled={page === 0} onClick={() => setPage(page - 1)}><ChevronLeft className="size-4" /></Button>
          <Button size="sm" variant="secondary" aria-label="Page suivante" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}><ChevronRight className="size-4" /></Button>
        </div>
      )}
    </section>
  );
}

function Staffing({ project }: { project: Project }) {
  const { data: allocations = [] } = useAllocations(project.id);
  const { data: users = [] } = useUsers();
  const m = useAllocationMutations(project.id);
  const toast = useToast();
  const canManage = can(project, 'TIME_MANAGE');
  const today = toIsoDate(new Date());
  const [form, setForm] = useState({ userId: '', startDate: today, endDate: project.endDate ?? today, daysPerWeek: '2' });
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  return (
    <section className="card overflow-hidden">
      <div className="border-b border-line px-5 py-3 text-sm font-semibold">Staffing <span className="font-normal text-ink-3">· affectations prévues (jours par semaine)</span></div>
      <ul className="divide-y divide-line">
        {allocations.length === 0 && <li className="px-5 py-4 text-sm text-ink-3">Aucune affectation.</li>}
        {allocations.map((a) => (
          <li key={a.id} className="group flex items-center gap-3 px-5 py-2.5 text-sm">
            <Avatar name={a.userName} size="sm" />
            <span className="flex-1">{a.userName}</span>
            <span className="tabular-nums text-ink-3">{formatDate(a.startDate)} → {formatDate(a.endDate, true)}</span>
            <span className="w-20 text-right font-semibold tabular-nums">{num.format(a.daysPerWeek)} j/sem.</span>
            {canManage && (
              <button aria-label={`Retirer ${a.userName}`} className="rounded p-1 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                onClick={() => m.remove.mutate(a.id, { onError })}><Trash2 className="size-4" /></button>
            )}
          </li>
        ))}
      </ul>
      {canManage && (
        <form className="flex flex-wrap items-end gap-3 border-t border-line bg-surface-2/40 px-5 py-4"
          onSubmit={(e) => {
            e.preventDefault();
            m.create.mutate({ userId: Number(form.userId), startDate: form.startDate, endDate: form.endDate, daysPerWeek: Number(form.daysPerWeek) },
              { onSuccess: () => setForm({ ...form, userId: '' }), onError });
          }}>
          <Field label="Personne">
            <select className="input w-48" required value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })}>
              <option value="">Choisir…</option>
              {users.filter((u) => u.active && u.role !== 'GUEST').map((u) => <option key={u.id} value={u.id}>{u.displayName}</option>)}
            </select>
          </Field>
          <Field label="Du"><input className="input w-40" type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></Field>
          <Field label="Au"><input className="input w-40" type="date" required value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></Field>
          <Field label="Jours / semaine"><input className="input w-24" type="number" min={0.5} max={7} step={0.5} required value={form.daysPerWeek} onChange={(e) => setForm({ ...form, daysPerWeek: e.target.value })} /></Field>
          <Button type="submit" icon={<UserPlus className="size-4" />} disabled={!form.userId} loading={m.create.isPending}>Affecter</Button>
        </form>
      )}
    </section>
  );
}
