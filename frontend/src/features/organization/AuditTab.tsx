import { useState } from 'react';
import { ChevronLeft, ChevronRight, ScrollText, Search } from 'lucide-react';
import { useAudit } from '../../api/hooks';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { EmptyState } from '../../components/ui/EmptyState';
import { Spinner } from '../../components/ui/Spinner';

const ENTITY_TYPES: Record<string, string> = {
  '': 'Tous les éléments',
  PROJECT: 'Projets',
  TASK: 'Tâches',
  TEAM: 'Équipes',
  USER: 'Utilisateurs',
  ORGANIZATION: 'Organisation',
  TIME: 'Temps',
  RISK: 'Risques',
};
const dateTime = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'medium' });

/** Journal d'audit paginé (administrateurs) : qui a fait quoi, quand. */
export function AuditTab() {
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [entityType, setEntityType] = useState('');
  const { data, isLoading } = useAudit({ page, q, entityType });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-ink-3" />
          <input className="input w-72 pl-8" placeholder="Rechercher (personne, projet, action…)" value={q}
            onChange={(e) => { setQ(e.target.value); setPage(0); }} />
        </div>
        <select className="input w-52" value={entityType} onChange={(e) => { setEntityType(e.target.value); setPage(0); }} aria-label="Type d'élément">
          {Object.entries(ENTITY_TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      {isLoading || !data ? (
        <Spinner />
      ) : data.content.length === 0 ? (
        <EmptyState icon={<ScrollText className="size-5" />} title="Aucun événement" />
      ) : (
        <div className="card overflow-x-auto">
          <table className="w-full min-w-[720px] text-sm">
            <thead>
              <tr className="border-b border-line bg-surface-2/60 text-left text-xs text-ink-3">
                <th className="px-4 py-2.5 font-medium">Date</th>
                <th className="px-3 py-2.5 font-medium">Auteur</th>
                <th className="px-3 py-2.5 font-medium">Action</th>
                <th className="px-4 py-2.5 font-medium">Détail</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((e) => (
                <tr key={e.id} className="border-b border-line last:border-0">
                  <td className="px-4 py-2.5 whitespace-nowrap text-ink-3 tabular-nums">{dateTime.format(new Date(e.createdAt))}</td>
                  <td className="px-3 py-2.5 whitespace-nowrap">{e.actorName ?? 'Système'}</td>
                  <td className="px-3 py-2.5"><Badge>{e.action}</Badge></td>
                  <td className="px-4 py-2.5 text-ink-2">{e.summary}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-end gap-2 text-sm text-ink-3">
          <span>{data.totalElements} événements · page {data.page + 1} / {data.totalPages}</span>
          <Button size="sm" variant="secondary" aria-label="Page précédente" disabled={page === 0} onClick={() => setPage(page - 1)}><ChevronLeft className="size-4" /></Button>
          <Button size="sm" variant="secondary" aria-label="Page suivante" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}><ChevronRight className="size-4" /></Button>
        </div>
      )}
    </div>
  );
}
