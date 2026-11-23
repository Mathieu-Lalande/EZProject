import { Link } from 'react-router-dom';
import { ShieldAlert, ShieldCheck } from 'lucide-react';
import type { ProjectSummary } from '../../api/types';
import { HealthBadge } from '../../components/ui/HealthBadge';

function reasons(p: ProjectSummary): string[] {
  const r: string[] = [];
  if (p.health !== 'GREEN') r.push('météo dégradée');
  if (p.overdueTasks > 0) r.push(`${p.overdueTasks} tâche(s) en retard`);
  if (p.latePhases > 0) r.push(`${p.latePhases} phase(s) en retard`);
  if (p.daysRemaining !== null && p.daysRemaining < 0) r.push('échéance dépassée');
  return r;
}

export function RisksWidget({ projects }: { projects: ProjectSummary[] }) {
  const atRisk = projects.filter((p) => p.atRisk);
  return (
    <div className="card p-5">
      <h3 className="text-sm font-semibold">Points d'attention</h3>
      <p className="mb-4 text-xs text-ink-3">Projets nécessitant un arbitrage</p>
      {atRisk.length === 0 ? (
        <div className="flex items-center gap-2 text-sm text-emerald-600"><ShieldCheck className="size-4" />Tout est sous contrôle.</div>
      ) : (
        <ul className="space-y-3">
          {atRisk.map((p) => (
            <li key={p.id} className="flex gap-3">
              <ShieldAlert className="mt-0.5 size-4 shrink-0 text-amber-500" />
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-2">
                  <Link to={`/projects/${p.id}`} className="truncate text-sm font-medium hover:text-brand-600">{p.name}</Link>
                  <HealthBadge health={p.health} />
                </div>
                <p className="mt-0.5 text-xs text-ink-3">{reasons(p).join(' · ')}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
