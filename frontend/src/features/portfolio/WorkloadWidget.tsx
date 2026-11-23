import type { Portfolio } from '../../api/types';
import { Avatar } from '../../components/ui/Avatar';

/** Charge ouverte par personne : barre proportionnelle au nombre de tâches non terminées. */
export function WorkloadWidget({ workload }: { workload: Portfolio['workload'] }) {
  const max = Math.max(1, ...workload.map((w) => w.openTasks));
  return (
    <div className="card p-5">
      <h3 className="text-sm font-semibold">Charge de l'équipe</h3>
      <p className="mb-4 text-xs text-ink-3">Tâches ouvertes assignées</p>
      <ul className="space-y-3">
        {workload.map((w) => {
          const ratio = w.openTasks / max;
          const color = ratio > 0.8 ? 'bg-red-500' : ratio > 0.5 ? 'bg-amber-500' : 'bg-brand-500';
          return (
            <li key={w.userId} className="flex items-center gap-3">
              <Avatar name={w.displayName} size="sm" />
              <div className="min-w-0 flex-1">
                <div className="flex justify-between text-xs">
                  <span className="truncate font-medium">{w.displayName}</span>
                  <span className="tabular-nums text-ink-3">
                    {w.openTasks} t.{w.openPoints > 0 ? ` · ${w.openPoints} pts` : ''}
                  </span>
                </div>
                <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-surface-2">
                  <div className={`h-full rounded-full ${color} transition-[width] duration-500`} style={{ width: `${Math.max(ratio * 100, w.openTasks ? 4 : 0)}%` }} />
                </div>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
