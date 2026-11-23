import { History } from 'lucide-react';
import { useProjectHistory } from '../../api/hooks';
import { StatusBadge } from '../../components/ui/StatusBadge';

const dateTime = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

/** Journal des changements de statut : traçabilité des arbitrages (qui, quand, pourquoi). */
export function ProjectHistory({ projectId }: { projectId: number }) {
  const { data: history = [] } = useProjectHistory(projectId);
  return (
    <div className="card p-5">
      <h3 className="mb-4 flex items-center gap-2 text-sm font-semibold"><History className="size-4 text-ink-3" />Journal des statuts</h3>
      {history.length === 0 ? (
        <p className="text-sm text-ink-3">Aucun changement enregistré.</p>
      ) : (
        <ol className="relative space-y-4 border-l border-line pl-5">
          {history.map((h) => (
            <li key={h.id} className="relative">
              <span className="absolute top-1.5 -left-[24.5px] size-2 rounded-full bg-brand-500 ring-4 ring-surface" />
              <div className="flex flex-wrap items-center gap-1.5 text-sm">
                {h.fromStatus && <><StatusBadge status={h.fromStatus} /><span className="text-ink-3">→</span></>}
                <StatusBadge status={h.toStatus} />
              </div>
              {h.reason && <p className="mt-1 text-sm text-ink-2">« {h.reason} »</p>}
              <p className="mt-0.5 text-xs text-ink-3">{h.changedBy ?? 'Système'} · {dateTime.format(new Date(h.changedAt))}</p>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
