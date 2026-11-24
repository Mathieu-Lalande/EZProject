import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { useRoadmap, useTeams } from '../api/hooks';
import type { RoadmapProject } from '../api/types';
import { PageHeader } from '../components/ui/PageHeader';
import { Spinner } from '../components/ui/Spinner';
import { PriorityBadge } from '../components/ui/PriorityBadge';
import { daysBetween, formatDate, parseDate } from '../lib/format';
import { phaseStateLabel } from '../lib/labels';

const monthFmt = new Intl.DateTimeFormat('fr-FR', { month: 'short', year: '2-digit' });
const healthDot = { GREEN: 'bg-emerald-500', AMBER: 'bg-amber-500', RED: 'bg-red-500' } as const;

/** Roadmap de portefeuille : un Gantt des projets ouverts, avec leurs phases et jalons de rétro-planning. */
export function RoadmapPage() {
  const { data: teams = [] } = useTeams();
  const [teamId, setTeamId] = useState<number | undefined>(undefined);
  const [months, setMonths] = useState(9);
  const { data: projects = [], isLoading } = useRoadmap(teamId);

  const { from, to, ticks } = useMemo(() => {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const end = new Date(now.getFullYear(), now.getMonth() - 1 + months, 0);
    const t: Date[] = [];
    for (let d = new Date(start); d <= end; d = new Date(d.getFullYear(), d.getMonth() + 1, 1)) t.push(d);
    return { from: start, to: end, ticks: t };
  }, [months]);
  const span = daysBetween(from, to) + 1;
  const pct = (iso: string) => Math.min(100, Math.max(0, (daysBetween(from, parseDate(iso)) / span) * 100));
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayPct = (daysBetween(from, today) / span) * 100;

  const groups = useMemo(() => {
    const byTeam = new Map<string, RoadmapProject[]>();
    projects.forEach((p) => {
      const key = p.teamName ?? 'Sans équipe';
      byTeam.set(key, [...(byTeam.get(key) ?? []), p]);
    });
    return [...byTeam.entries()].sort(([a], [b]) => a.localeCompare(b));
  }, [projects]);

  return (
    <div className="animate-fade-in">
      <PageHeader
        title="Roadmap"
        subtitle="Projets ouverts, phases et jalons de rétro-planning, regroupés par équipe."
        actions={
          <div className="flex gap-2">
            {teams.length > 1 && (
              <select className="input w-48" value={teamId ?? ''} onChange={(e) => setTeamId(e.target.value ? Number(e.target.value) : undefined)} aria-label="Équipe">
                <option value="">Toutes les équipes</option>
                {teams.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            )}
            <select className="input w-36" value={months} onChange={(e) => setMonths(Number(e.target.value))} aria-label="Horizon">
              <option value={6}>6 mois</option><option value={9}>9 mois</option><option value={12}>12 mois</option><option value={18}>18 mois</option>
            </select>
          </div>
        }
      />
      {isLoading ? <Spinner /> : (
        <div className="card overflow-x-auto p-4">
          <div className="min-w-[900px]">
            <div className="relative ml-60 h-6 border-b border-line text-[11px] text-ink-3">
              {ticks.map((t) => (
                <span key={t.toISOString()} className="absolute top-1 pl-1 capitalize" style={{ left: `${(daysBetween(from, t) / span) * 100}%` }}>{monthFmt.format(t)}</span>
              ))}
            </div>
            <div className="relative">
              <div className="pointer-events-none absolute inset-y-0 right-0 left-60">
                {ticks.map((t) => <div key={t.toISOString()} className="absolute inset-y-0 border-l border-dashed border-line" style={{ left: `${(daysBetween(from, t) / span) * 100}%` }} />)}
                {todayPct >= 0 && todayPct <= 100 && <div className="absolute inset-y-0 z-10 border-l-2 border-red-500" style={{ left: `${todayPct}%` }} title="Aujourd'hui" />}
              </div>
              {groups.map(([team, list]) => (
                <div key={team}>
                  <div className="py-2 text-[11px] font-semibold tracking-wider text-ink-3 uppercase">{team}</div>
                  {list.map((p) => (
                    <div key={p.id} className="flex items-center border-t border-line/60 py-1.5">
                      <Link to={`/projects/${p.id}`} className="flex w-60 shrink-0 items-center gap-2 pr-3 hover:text-brand-600">
                        <PriorityBadge priority={p.priority} short />
                        <span className={clsx('size-1.5 shrink-0 rounded-full', healthDot[p.health])} aria-label={`Météo ${p.health}`} />
                        <span className="truncate text-sm font-medium">{p.name}</span>
                      </Link>
                      <div className="relative h-9 flex-1">
                        {p.startDate && p.endDate && (
                          <div className={clsx('absolute top-1 h-3 rounded-[4px]', p.status === 'ON_HOLD' && 'opacity-40')}
                            style={{ left: `${pct(p.startDate)}%`, width: `${Math.max(0.5, pct(p.endDate) - pct(p.startDate))}%`, backgroundColor: p.color }}
                            title={`${p.name} : ${formatDate(p.startDate, true)} → ${formatDate(p.endDate, true)}${p.status === 'ON_HOLD' ? ' (en pause)' : ''}`} />
                        )}
                        {p.phases.filter((ph) => ph.start && ph.end).map((ph) =>
                          ph.kind === 'MILESTONE' ? (
                            <span key={ph.id} className="absolute top-[18px] size-2.5 -translate-x-1/2 rotate-45 rounded-[2px] bg-ink ring-2 ring-surface"
                              style={{ left: `${pct(ph.start!)}%` }} title={`◆ ${ph.name} · ${formatDate(ph.start, true)} · ${phaseStateLabel[ph.state]}`} />
                          ) : (
                            <span key={ph.id} className={clsx('absolute top-5 h-1.5 rounded-full', ph.state === 'LATE' ? 'bg-red-500' : ph.state === 'DONE' ? 'bg-emerald-500' : 'bg-ink-3/50')}
                              style={{ left: `${pct(ph.start!)}%`, width: `${Math.max(0.3, pct(ph.end!) - pct(ph.start!))}%` }}
                              title={`${ph.name} · ${formatDate(ph.start)} → ${formatDate(ph.end)} · ${phaseStateLabel[ph.state]}`} />
                          ),
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-4 text-xs text-ink-3">
            <span className="flex items-center gap-1.5"><span className="h-3 w-6 rounded-[4px] bg-brand-500" />Période du projet</span>
            <span className="flex items-center gap-1.5"><span className="h-1.5 w-6 rounded-full bg-ink-3/50" />Phase</span>
            <span className="flex items-center gap-1.5"><span className="h-1.5 w-6 rounded-full bg-emerald-500" />Phase terminée</span>
            <span className="flex items-center gap-1.5"><span className="h-1.5 w-6 rounded-full bg-red-500" />Phase en retard</span>
            <span className="flex items-center gap-1.5"><span className="size-2.5 rotate-45 rounded-[2px] bg-ink" />Jalon</span>
            <span className="flex items-center gap-1.5"><span className="h-3 w-0.5 bg-red-500" />Aujourd'hui</span>
          </div>
        </div>
      )}
    </div>
  );
}
