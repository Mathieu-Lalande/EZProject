import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Timer } from 'lucide-react';
import clsx from 'clsx';
import { useSaveTimesheet, useTimesheet } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { parseDate, toIsoDate } from '../../lib/format';

const dayFmt = new Intl.DateTimeFormat('fr-FR', { weekday: 'short', day: 'numeric' });
const num = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 3 });
const OPTIONS = [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1];

function mondayOf(date: Date) {
  const d = new Date(date);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return toIsoDate(d);
}

/** Feuille de temps hebdomadaire : une ligne par projet, une colonne par jour (en jours, au pas d'une heure). */
export function Timesheet() {
  const [week, setWeek] = useState(() => mondayOf(new Date()));
  const { data, isLoading } = useTimesheet(week);
  const save = useSaveTimesheet();
  const toast = useToast();
  const [draft, setDraft] = useState<Record<string, number>>({});
  const today = toIsoDate(new Date());

  const initial = useMemo(() => Object.fromEntries((data?.cells ?? []).map((c) => [`${c.projectId}|${c.date}`, c.days])), [data]);
  useEffect(() => setDraft(initial), [initial]);

  if (isLoading || !data) return <div className="card p-5"><Spinner /></div>;

  const shift = (weeks: number) => {
    const d = parseDate(week);
    d.setDate(d.getDate() + 7 * weeks);
    setWeek(toIsoDate(d));
  };
  const taskDays = (date: string) => data.taskEntries.filter((e) => e.date === date).reduce((s, e) => s + e.days, 0);
  const dayTotal = (date: string) => data.projects.reduce((s, p) => s + (draft[`${p.projectId}|${date}`] ?? 0), 0) + taskDays(date);
  const weekTotal = data.days.reduce((s, d) => s + dayTotal(d), 0);
  const dirty = JSON.stringify(draft) !== JSON.stringify(initial);

  const submit = () => {
    const cells = data.projects.flatMap((p) =>
      data.days
        .filter((d) => d <= today)
        .map((d) => ({ projectId: p.projectId, date: d, days: draft[`${p.projectId}|${d}`] ?? 0 }))
        .filter((c) => c.days !== (initial[`${c.projectId}|${c.date}`] ?? 0)),
    );
    save.mutate({ weekStart: week, cells }, {
      onSuccess: () => toast('success', 'Feuille de temps enregistrée'),
      onError: (e) => toast('error', e instanceof ApiError ? e.message : 'Enregistrement impossible'),
    });
  };

  return (
    <section className="card overflow-hidden">
      <div className="flex flex-wrap items-center gap-2 border-b border-line px-5 py-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold"><Timer className="size-4 text-ink-3" />Ma feuille de temps</h3>
        <div className="ml-auto flex items-center gap-1">
          <Button size="sm" variant="ghost" aria-label="Semaine précédente" onClick={() => shift(-1)}><ChevronLeft className="size-4" /></Button>
          <span className="text-xs text-ink-2 tabular-nums">Semaine du {dayFmt.format(parseDate(week))}</span>
          <Button size="sm" variant="ghost" aria-label="Semaine suivante" disabled={week >= mondayOf(new Date())} onClick={() => shift(1)}><ChevronRight className="size-4" /></Button>
        </div>
      </div>
      {data.projects.length === 0 ? (
        <p className="px-5 py-6 text-sm text-ink-3">Aucun projet avec le module « Temps & budget » sur lequel vous pouvez saisir.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="text-xs text-ink-3">
                <th className="px-5 py-2 text-left font-medium">Projet</th>
                {data.days.map((d, i) => (
                  <th key={d} className={clsx('px-1 py-2 text-center font-medium capitalize', !data.workingDays[i] && 'text-ink-3/60')}>{dayFmt.format(parseDate(d))}</th>
                ))}
                <th className="px-5 py-2 text-right font-medium">Total</th>
              </tr>
            </thead>
            <tbody>
              {data.projects.map((p) => {
                const total = data.days.reduce((s, d) => s + (draft[`${p.projectId}|${d}`] ?? 0), 0);
                return (
                  <tr key={p.projectId} className="border-t border-line">
                    <td className="px-5 py-1.5"><span className="flex items-center gap-2"><span className="size-2.5 rounded-[3px]" style={{ backgroundColor: p.color }} />{p.name}</span></td>
                    {data.days.map((d, i) => (
                      <td key={d} className={clsx('px-1 py-1.5 text-center', !data.workingDays[i] && 'bg-surface-2/50')}>
                        <select
                          aria-label={`${p.name}, ${d}`}
                          disabled={d > today}
                          className="w-16 rounded-md border border-line bg-surface px-1 py-1 text-xs tabular-nums disabled:opacity-40"
                          value={draft[`${p.projectId}|${d}`] ?? 0}
                          onChange={(e) => setDraft({ ...draft, [`${p.projectId}|${d}`]: Number(e.target.value) })}
                        >
                          {OPTIONS.map((o) => <option key={o} value={o}>{o === 0 ? '—' : num.format(o)}</option>)}
                        </select>
                      </td>
                    ))}
                    <td className="px-5 py-1.5 text-right font-medium tabular-nums">{total ? `${num.format(total)} j` : '—'}</td>
                  </tr>
                );
              })}
              <tr className="border-t border-line text-xs">
                <td className="px-5 py-2 font-medium text-ink-3">Total du jour{data.taskEntries.length > 0 && ' (incl. temps sur tâches)'}</td>
                {data.days.map((d) => {
                  const t = dayTotal(d);
                  return <td key={d} className={clsx('px-1 py-2 text-center tabular-nums', t > 1 ? 'font-semibold text-red-600' : 'text-ink-2')}>{t ? num.format(t) : ''}</td>;
                })}
                <td className="px-5 py-2 text-right font-semibold tabular-nums">{num.format(weekTotal)} j</td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
      {data.projects.length > 0 && (
        <div className="flex justify-end gap-2 border-t border-line px-5 py-3">
          <Button variant="secondary" disabled={!dirty} onClick={() => setDraft(initial)}>Annuler</Button>
          <Button disabled={!dirty} loading={save.isPending} onClick={submit}>Enregistrer la semaine</Button>
        </div>
      )}
    </section>
  );
}
