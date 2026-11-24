import { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import clsx from 'clsx';
import { useCapacity, useMe, useTeams } from '../api/hooks';
import type { WeekLoad } from '../api/types';
import { PageHeader } from '../components/ui/PageHeader';
import { Spinner } from '../components/ui/Spinner';
import { Button } from '../components/ui/Button';
import { Avatar } from '../components/ui/Avatar';
import { formatDate, parseDate, toIsoDate } from '../lib/format';
import { isManager } from '../lib/permissions';

const num = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });

function mondayOf(date: Date) {
  const d = new Date(date);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return toIsoDate(d);
}

/**
 * Couleur d'une cellule selon le taux d'occupation : rampe séquentielle (une teinte) jusqu'à 100 %,
 * puis statut « critique » au-delà. Le pourcentage est toujours écrit : la couleur n'est jamais seule porteuse.
 */
function cellClass(load: WeekLoad) {
  const pct = load.loadPct;
  if (pct === null) return 'bg-surface-2 text-ink-3';
  if (pct > 100) return 'bg-red-500 text-white';
  if (pct >= 90) return 'bg-brand-600 text-white';
  if (pct >= 60) return 'bg-brand-400 text-white';
  if (pct >= 30) return 'bg-brand-200 text-brand-700 dark:bg-brand-500/40 dark:text-white';
  if (pct > 0) return 'bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200';
  return 'bg-surface-2 text-ink-3';
}

/** Capacité : disponibilité (temps de travail, fériés, absences) face au staffing, semaine par semaine. */
export function CapacityPage() {
  const { data: me } = useMe();
  const { data: teams = [] } = useTeams();
  const [from, setFrom] = useState(() => mondayOf(new Date()));
  const [teamId, setTeamId] = useState<number | undefined>(undefined);
  const { data, isLoading } = useCapacity(from, 8, teamId);

  const shift = (weeks: number) => {
    const d = parseDate(from);
    d.setDate(d.getDate() + 7 * weeks);
    setFrom(toIsoDate(d));
  };

  return (
    <div className="animate-fade-in">
      <PageHeader
        title="Capacité des équipes"
        subtitle={isManager(me) ? 'Charge affectée par rapport à la disponibilité réelle (temps partiel, jours fériés, absences).'
          : 'Votre charge et celle des équipes dont vous êtes responsable.'}
        actions={
          <div className="flex items-center gap-1">
            {teams.length > 1 && (
              <select className="input mr-2 w-48" value={teamId ?? ''} onChange={(e) => setTeamId(e.target.value ? Number(e.target.value) : undefined)} aria-label="Équipe">
                <option value="">Toutes les équipes</option>
                {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            )}
            <Button variant="secondary" size="sm" aria-label="Semaines précédentes" onClick={() => shift(-4)}><ChevronLeft className="size-4" /></Button>
            <Button variant="secondary" size="sm" onClick={() => setFrom(mondayOf(new Date()))}>Aujourd'hui</Button>
            <Button variant="secondary" size="sm" aria-label="Semaines suivantes" onClick={() => shift(4)}><ChevronRight className="size-4" /></Button>
          </div>
        }
      />
      {isLoading || !data ? <Spinner /> : (
        <>
          <div className="card overflow-x-auto">
            <table className="w-full min-w-[860px] border-separate border-spacing-0.5 p-2 text-sm">
              <thead>
                <tr className="text-xs text-ink-3">
                  <th className="px-3 py-2 text-left font-medium">Personne</th>
                  {data.weeks.map((w) => <th key={w} className="px-1 py-2 text-center font-medium whitespace-nowrap">Sem. {formatDate(w)}</th>)}
                </tr>
              </thead>
              <tbody>
                {data.people.map((p) => (
                  <tr key={p.userId}>
                    <td className="px-3 py-1.5">
                      <div className="flex items-center gap-2.5">
                        <Avatar name={p.displayName} size="sm" />
                        <div className="min-w-0">
                          <div className="truncate font-medium">{p.displayName}</div>
                          <div className="truncate text-xs text-ink-3">{num.format(p.daysPerWeek)} j/sem. · {p.allocations.map((a) => a.projectCode).filter((c, i, arr) => arr.indexOf(c) === i).join(', ') || 'non staffé'}</div>
                        </div>
                      </div>
                    </td>
                    {p.weeks.map((w) => (
                      <td key={w.weekStart}
                        className={clsx('h-11 rounded-md text-center text-xs font-semibold tabular-nums', cellClass(w))}
                        title={`Disponible ${num.format(w.available)} j (capacité ${num.format(w.capacity)} j, absent ${num.format(w.absent)} j) · affecté ${num.format(w.allocated)} j`}>
                        {w.loadPct === null ? '—' : w.loadPct >= 999 ? 'Absent' : `${w.loadPct}%`}
                        {w.absent > 0 && w.loadPct !== null && w.loadPct < 999 && <span className="block text-[10px] font-normal opacity-80">abs. {num.format(w.absent)} j</span>}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-ink-3">
            <span>Taux d'occupation :</span>
            {[['bg-brand-50', '< 30 %'], ['bg-brand-200', '30–60 %'], ['bg-brand-400', '60–90 %'], ['bg-brand-600', '90–100 %'], ['bg-red-500', '> 100 % surcharge']].map(([c, l]) => (
              <span key={l} className="flex items-center gap-1.5"><span className={clsx('size-3 rounded-sm', c)} />{l}</span>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
