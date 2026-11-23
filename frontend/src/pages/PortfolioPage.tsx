import { useMemo, useState } from 'react';
import { Activity, AlertTriangle, CheckCheck, Clock, FolderKanban, LayoutGrid, Rows3, SlidersHorizontal } from 'lucide-react';
import clsx from 'clsx';
import { usePortfolio } from '../api/hooks';
import type { ProjectType } from '../api/types';
import { PageHeader } from '../components/ui/PageHeader';
import { Spinner } from '../components/ui/Spinner';
import { Button } from '../components/ui/Button';
import { KpiCard } from '../features/portfolio/KpiCard';
import { ProjectsHealthTable } from '../features/portfolio/ProjectsHealthTable';
import { ProjectCardsGrid } from '../features/portfolio/ProjectCardsGrid';
import { WorkloadWidget } from '../features/portfolio/WorkloadWidget';
import { MilestonesWidget } from '../features/portfolio/MilestonesWidget';
import { RisksWidget } from '../features/portfolio/RisksWidget';
import { useLocalStorage } from '../lib/useLocalStorage';
import { projectTypeLabel } from '../lib/labels';

type WidgetId = 'kpis' | 'projects' | 'risks' | 'milestones' | 'workload';
const WIDGETS: { id: WidgetId; label: string }[] = [
  { id: 'kpis', label: 'Indicateurs clés' },
  { id: 'projects', label: 'Projets' },
  { id: 'risks', label: "Points d'attention" },
  { id: 'milestones', label: 'Prochains jalons' },
  { id: 'workload', label: "Charge de l'équipe" },
];

export function PortfolioPage() {
  const { data, isLoading } = usePortfolio();
  const [visible, setVisible] = useLocalStorage<WidgetId[]>('ez-portfolio-widgets', WIDGETS.map((w) => w.id));
  const [view, setView] = useLocalStorage<'table' | 'cards'>('ez-portfolio-view', 'table');
  const [typeFilter, setTypeFilter] = useState<ProjectType | 'ALL'>('ALL');
  const [showClosed, setShowClosed] = useState(false);
  const [customizing, setCustomizing] = useState(false);

  const projects = useMemo(
    () =>
      (data?.projects ?? [])
        .filter((p) => showClosed || ['PLANNED', 'ACTIVE', 'ON_HOLD'].includes(p.status))
        .filter((p) => typeFilter === 'ALL' || p.type === typeFilter),
    [data, typeFilter, showClosed],
  );

  if (isLoading || !data) return <Spinner />;
  const has = (id: WidgetId) => visible.includes(id);
  const toggle = (id: WidgetId) => setVisible(has(id) ? visible.filter((v) => v !== id) : [...visible, id]);
  const today = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());

  return (
    <div className="animate-fade-in">
      <PageHeader
        eyebrow={today.charAt(0).toUpperCase() + today.slice(1)}
        title="Portefeuille projets"
        subtitle="Vue consolidée pour piloter l'ensemble des projets de l'organisation."
        actions={
          <div className="relative">
            <Button variant="secondary" icon={<SlidersHorizontal className="size-4" />} onClick={() => setCustomizing((c) => !c)}>
              Personnaliser
            </Button>
            {customizing && (
              <div className="absolute right-0 z-20 mt-2 w-60 animate-pop-in rounded-xl border border-line bg-surface p-2 shadow-pop">
                <div className="px-2 py-1.5 text-xs font-medium text-ink-3">Widgets affichés</div>
                {WIDGETS.map((w) => (
                  <label key={w.id} className="flex cursor-pointer items-center gap-2.5 rounded-lg px-2 py-1.5 text-sm hover:bg-surface-2">
                    <input type="checkbox" className="accent-brand-600" checked={has(w.id)} onChange={() => toggle(w.id)} />
                    {w.label}
                  </label>
                ))}
              </div>
            )}
          </div>
        }
      />

      {has('kpis') && (
        <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-5">
          <KpiCard label="Projets ouverts" value={data.kpis.openProjects} icon={<FolderKanban className="size-4" />} />
          <KpiCard label="Projets à risque" value={data.kpis.atRiskProjects} icon={<AlertTriangle className="size-4" />} tone={data.kpis.atRiskProjects ? 'amber' : 'green'} />
          <KpiCard label="Tâches ouvertes" value={data.kpis.openTasks} icon={<Activity className="size-4" />} />
          <KpiCard label="Tâches en retard" value={data.kpis.overdueTasks} icon={<Clock className="size-4" />} tone={data.kpis.overdueTasks ? 'red' : 'green'} />
          <KpiCard label="Terminées (7 j)" value={data.kpis.completedLast7Days} icon={<CheckCheck className="size-4" />} tone="green" />
        </div>
      )}

      <div className="grid gap-6 2xl:grid-cols-[1fr_340px]">
        <div className="min-w-0 space-y-6">
          {has('projects') && (
            <section>
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <div className="flex rounded-lg border border-line bg-surface p-0.5">
                  {(['ALL', 'DEVELOPMENT', 'CONSULTING', 'INTERNAL'] as const).map((t) => (
                    <button
                      key={t}
                      onClick={() => setTypeFilter(t)}
                      className={clsx('rounded-md px-2.5 py-1 text-xs font-medium transition', typeFilter === t ? 'bg-surface-2 text-ink shadow-sm' : 'text-ink-3 hover:text-ink')}
                    >
                      {t === 'ALL' ? 'Tous' : projectTypeLabel[t]}
                    </button>
                  ))}
                </div>
                <label className="flex items-center gap-1.5 text-xs text-ink-3">
                  <input type="checkbox" className="accent-brand-600" checked={showClosed} onChange={(e) => setShowClosed(e.target.checked)} />
                  Inclure terminés
                </label>
                <div className="ml-auto flex rounded-lg border border-line bg-surface p-0.5">
                  <button aria-label="Vue tableau" onClick={() => setView('table')} className={clsx('rounded-md p-1.5', view === 'table' ? 'bg-surface-2 text-ink' : 'text-ink-3')}><Rows3 className="size-4" /></button>
                  <button aria-label="Vue cartes" onClick={() => setView('cards')} className={clsx('rounded-md p-1.5', view === 'cards' ? 'bg-surface-2 text-ink' : 'text-ink-3')}><LayoutGrid className="size-4" /></button>
                </div>
              </div>
              {view === 'table' ? <ProjectsHealthTable projects={projects} /> : <ProjectCardsGrid projects={projects} />}
            </section>
          )}
        </div>
        <div className="grid content-start gap-6 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-1">
          {has('risks') && <RisksWidget projects={projects} />}
          {has('milestones') && <MilestonesWidget projects={projects} />}
          {has('workload') && <WorkloadWidget workload={data.workload} />}
        </div>
      </div>
    </div>
  );
}
