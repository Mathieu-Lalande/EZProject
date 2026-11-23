import { useState, type FormEvent } from 'react';
import { ArrowDownRight, ArrowRight, ArrowUpRight, CloudSun, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { Health, Project, Trend } from '../../api/types';
import { usePublishReport, useReports } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { Field } from '../../components/ui/Field';
import { HealthBadge } from '../../components/ui/HealthBadge';
import { useToast } from '../../components/ui/Toast';
import { formatDate } from '../../lib/format';
import { healthLabel, trendLabel } from '../../lib/labels';
import { can } from '../../lib/permissions';

export function TrendIcon({ trend, className = 'size-4' }: { trend: Trend; className?: string }) {
  const label = trendLabel[trend];
  if (trend === 'UP') return <ArrowUpRight className={`${className} text-emerald-600`} aria-label={label} />;
  if (trend === 'DOWN') return <ArrowDownRight className={`${className} text-red-600`} aria-label={label} />;
  return <ArrowRight className={`${className} text-ink-3`} aria-label={label} />;
}

/** Météo projet : dernier point publié, historique, publication d'un nouveau point (lien vers le rapport COPIL). */
export function WeatherPanel({ project }: { project: Project }) {
  const { data: reports = [] } = useReports(project.id);
  const publish = usePublishReport(project.id);
  const toast = useToast();
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ health: project.health as Health, trend: 'STABLE' as Trend, summary: '', achievements: '', nextSteps: '', attentionPoints: '' });
  const latest = reports[0];

  const submit = (e: FormEvent) => {
    e.preventDefault();
    publish.mutate(form, {
      onSuccess: () => { toast('success', 'Météo publiée'); setOpen(false); setForm({ ...form, summary: '', achievements: '', nextSteps: '', attentionPoints: '' }); },
      onError: (err) => toast('error', err instanceof ApiError ? err.message : 'Publication impossible'),
    });
  };

  return (
    <section className="card p-5">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <h3 className="flex items-center gap-2 text-sm font-semibold"><CloudSun className="size-4 text-ink-3" />Météo du projet</h3>
        <Link to={`/projects/${project.id}/copil`} className="ml-auto inline-flex items-center gap-1 text-xs font-medium text-brand-600 hover:underline">
          <FileText className="size-3.5" />Rapport COPIL
        </Link>
        {can(project, 'REPORT_EDIT') && !open && <Button size="sm" onClick={() => setOpen(true)}>Publier un point</Button>}
      </div>
      {open && (
        <form onSubmit={submit} className="mb-4 grid gap-3 rounded-lg border border-line bg-surface-2/40 p-4 sm:grid-cols-2">
          <Field label="Météo">
            <select className="input" value={form.health} onChange={(e) => setForm({ ...form, health: e.target.value as Health })}>
              {(Object.keys(healthLabel) as Health[]).map((h) => <option key={h} value={h}>{healthLabel[h]}</option>)}
            </select>
          </Field>
          <Field label="Tendance">
            <select className="input" value={form.trend} onChange={(e) => setForm({ ...form, trend: e.target.value as Trend })}>
              {(Object.keys(trendLabel) as Trend[]).map((t) => <option key={t} value={t}>{trendLabel[t]}</option>)}
            </select>
          </Field>
          <Field label="Synthèse (obligatoire)" className="sm:col-span-2"><textarea className="input min-h-16" required maxLength={2000} value={form.summary} onChange={(e) => setForm({ ...form, summary: e.target.value })} /></Field>
          <Field label="Réalisé"><textarea className="input min-h-16" maxLength={2000} value={form.achievements} onChange={(e) => setForm({ ...form, achievements: e.target.value })} /></Field>
          <Field label="Prochaines étapes"><textarea className="input min-h-16" maxLength={2000} value={form.nextSteps} onChange={(e) => setForm({ ...form, nextSteps: e.target.value })} /></Field>
          <Field label="Points d'attention / arbitrages" className="sm:col-span-2"><textarea className="input min-h-16" maxLength={2000} value={form.attentionPoints} onChange={(e) => setForm({ ...form, attentionPoints: e.target.value })} /></Field>
          <div className="flex justify-end gap-2 sm:col-span-2">
            <Button variant="secondary" onClick={() => setOpen(false)}>Annuler</Button>
            <Button type="submit" loading={publish.isPending}>Publier</Button>
          </div>
        </form>
      )}
      {!latest ? <p className="text-sm text-ink-3">Aucun point publié.</p> : (
        <>
          <div className="flex items-center gap-2">
            <HealthBadge health={latest.health} />
            <TrendIcon trend={latest.trend} />
            <span className="text-xs text-ink-3">{trendLabel[latest.trend]} · {formatDate(latest.reportDate, true)} · {latest.authorName ?? '—'}</span>
          </div>
          <p className="mt-2 text-sm">{latest.summary}</p>
          {latest.attentionPoints && <p className="mt-1 text-sm text-amber-700 dark:text-amber-300">⚠ {latest.attentionPoints}</p>}
          {reports.length > 1 && (
            <ol className="mt-4 flex flex-wrap gap-1.5" aria-label="Historique de la météo">
              {reports.slice(0, 12).reverse().map((r) => (
                <li key={r.id} title={`${formatDate(r.reportDate, true)} : ${healthLabel[r.health]} — ${r.summary}`}
                  className={`flex h-6 min-w-6 items-center justify-center rounded px-1 text-[10px] font-semibold text-white ${r.health === 'GREEN' ? 'bg-emerald-500' : r.health === 'AMBER' ? 'bg-amber-500' : 'bg-red-500'}`}>
                  {formatDate(r.reportDate).replace('.', '')}
                </li>
              ))}
            </ol>
          )}
        </>
      )}
    </section>
  );
}
