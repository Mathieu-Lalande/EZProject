import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Printer } from 'lucide-react';
import { useCopil } from '../api/hooks';
import { Spinner } from '../components/ui/Spinner';
import { Button } from '../components/ui/Button';
import { HealthBadge } from '../components/ui/HealthBadge';
import { PriorityBadge } from '../components/ui/PriorityBadge';
import { StatusBadge } from '../components/ui/StatusBadge';
import { TrendIcon } from '../features/report/WeatherPanel';
import { formatDate } from '../lib/format';
import { decisionStatusLabel, phaseStateLabel, riskStatusLabel, trendLabel } from '../lib/labels';

const num = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });

/**
 * Rapport de comité de pilotage, mis en page pour l'impression (« Enregistrer au format PDF »).
 * Les sections non autorisées pour le lecteur ne sont pas renvoyées par l'API et n'apparaissent pas.
 */
export function CopilPage() {
  const { id } = useParams();
  const { data, isLoading } = useCopil(Number(id));
  if (isLoading || !data) return <Spinner />;
  const { project, latestReport: r } = data;

  return (
    <div className="mx-auto max-w-4xl bg-surface px-8 py-8 print:max-w-none print:px-0 print:py-0">
      <div className="mb-6 flex items-center gap-2 print:hidden">
        <Link to={`/projects/${project.id}`} className="inline-flex items-center gap-1 text-sm text-ink-3 hover:text-ink"><ArrowLeft className="size-4" />Retour au projet</Link>
        <Button className="ml-auto" icon={<Printer className="size-4" />} onClick={() => window.print()}>Imprimer / PDF</Button>
      </div>

      <header className="mb-6 border-b-4 pb-4" style={{ borderColor: project.color }}>
        <p className="text-xs tracking-wider text-ink-3 uppercase">Comité de pilotage · {formatDate(data.generatedOn, true)}</p>
        <h1 className="mt-1 text-3xl font-semibold tracking-tight">{project.name}</h1>
        <p className="mt-1 text-sm text-ink-2">{project.code}{project.client ? ` · ${project.client}` : ''}{project.team ? ` · ${project.team.name}` : ''} · Responsable : {project.owner?.displayName ?? '—'}</p>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <HealthBadge health={project.health} /><StatusBadge status={project.status} reason={project.statusReason} /><PriorityBadge priority={project.priority} />
          <span className="text-xs text-ink-3">Échéance : {formatDate(project.endDate, true)}</span>
        </div>
      </header>

      <section className="mb-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Kpi label="Avancement" value={data.tasks ? `${data.tasks.progress} %` : '—'} sub={data.tasks ? `${data.tasks.done}/${data.tasks.total} tâches` : undefined} />
        <Kpi label="Tâches en retard" value={data.tasks ? String(data.tasks.overdue) : '—'} />
        <Kpi label="Budget consommé" value={data.budget?.consumptionPct != null ? `${data.budget.consumptionPct} %` : '—'}
          sub={data.budget?.budgetDays ? `${num.format(data.budget.consumedDays)} / ${data.budget.budgetDays} j` : undefined} />
        <Kpi label="Risques critiques" value={data.risks ? String(data.risks.filter((x) => x.critical).length) : '—'} />
      </section>

      <Section title="Météo et synthèse">
        {!r ? <p className="text-sm text-ink-3">Aucun point publié.</p> : (
          <>
            <p className="mb-2 flex items-center gap-2 text-sm"><HealthBadge health={r.health} /><TrendIcon trend={r.trend} />{trendLabel[r.trend]} · {formatDate(r.reportDate, true)} · {r.authorName}</p>
            <p className="text-sm">{r.summary}</p>
            <div className="mt-3 grid gap-4 sm:grid-cols-3">
              <Block title="Réalisé" text={r.achievements} />
              <Block title="Prochaines étapes" text={r.nextSteps} />
              <Block title="Points d'attention / arbitrages" text={r.attentionPoints} />
            </div>
          </>
        )}
      </Section>

      {data.plan && data.plan.phases.length > 0 && (
        <Section title="Planning et jalons">
          <table className="w-full text-sm">
            <tbody>
              {data.plan.phases.map((p) => (
                <tr key={p.id} className="border-b border-line last:border-0">
                  <td className="py-1.5">{p.kind === 'MILESTONE' ? '◆ ' : ''}{p.name}</td>
                  <td className="py-1.5 text-right text-ink-3 tabular-nums">{p.start ? (p.kind === 'MILESTONE' ? formatDate(p.start, true) : `${formatDate(p.start)} → ${formatDate(p.end, true)}`) : '—'}</td>
                  <td className="w-28 py-1.5 text-right">{phaseStateLabel[p.state]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Section>
      )}

      {data.risks && (
        <Section title="Principaux risques">
          {data.risks.length === 0 ? <p className="text-sm text-ink-3">Aucun risque actif.</p> : (
            <table className="w-full text-sm">
              <thead><tr className="text-left text-xs text-ink-3"><th className="py-1 font-medium">Risque</th><th className="py-1 font-medium">Criticité</th><th className="py-1 font-medium">Plan d'action</th><th className="py-1 font-medium">Statut</th></tr></thead>
              <tbody>
                {data.risks.map((x) => (
                  <tr key={x.id} className="border-t border-line align-top">
                    <td className="py-1.5 pr-3">{x.title}<span className="block text-xs text-ink-3">{x.ownerName}</span></td>
                    <td className="py-1.5 pr-3 font-semibold tabular-nums">{x.score}{x.critical ? ' (critique)' : ''}</td>
                    <td className="py-1.5 pr-3 text-ink-2">{x.mitigation ?? '—'}</td>
                    <td className="py-1.5">{riskStatusLabel[x.status]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Section>
      )}

      {data.decisions && data.decisions.length > 0 && (
        <Section title="Décisions">
          <ul className="space-y-1 text-sm">
            {data.decisions.map((d) => (
              <li key={d.id} className="flex gap-2"><span className="flex-1">{d.title}</span><span className="text-ink-3">{[d.decidedBy, d.decidedOn && formatDate(d.decidedOn, true)].filter(Boolean).join(' · ')}</span><strong>{decisionStatusLabel[d.status]}</strong></li>
            ))}
          </ul>
        </Section>
      )}

      {data.history.length > 1 && (
        <Section title="Historique de la météo">
          <ul className="space-y-1 text-sm">
            {data.history.map((h) => (
              <li key={h.id} className="flex items-center gap-2"><span className="w-24 text-ink-3 tabular-nums">{formatDate(h.reportDate, true)}</span><HealthBadge health={h.health} /><TrendIcon trend={h.trend} className="size-3.5" /><span className="flex-1 truncate">{h.summary}</span></li>
            ))}
          </ul>
        </Section>
      )}
    </div>
  );
}

function Kpi({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="rounded-lg border border-line p-3">
      <div className="text-xs text-ink-3">{label}</div>
      <div className="text-xl font-semibold tabular-nums">{value}</div>
      {sub && <div className="text-xs text-ink-3">{sub}</div>}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mb-6 break-inside-avoid">
      <h2 className="mb-2 border-b border-line pb-1 text-base font-semibold">{title}</h2>
      {children}
    </section>
  );
}

function Block({ title, text }: { title: string; text: string | null }) {
  return (
    <div>
      <div className="text-xs font-semibold text-ink-3">{title}</div>
      <p className="mt-0.5 text-sm whitespace-pre-line">{text ?? '—'}</p>
    </div>
  );
}
