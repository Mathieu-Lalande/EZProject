import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { FolderPlus, Plus, Search } from 'lucide-react';
import clsx from 'clsx';
import { useProjects } from '../api/hooks';
import type { ProjectStatus } from '../api/types';
import { useIsManager } from '../auth/RequireAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Button } from '../components/ui/Button';
import { Spinner } from '../components/ui/Spinner';
import { HealthBadge } from '../components/ui/HealthBadge';
import { EmptyState } from '../components/ui/EmptyState';
import { Avatar } from '../components/ui/Avatar';
import { formatDate } from '../lib/format';
import { moduleInfo, projectStatusLabel, projectTypeLabel } from '../lib/labels';
import { ProjectFormModal } from '../features/projects/ProjectFormModal';

export function ProjectsPage() {
  const { data: projects = [], isLoading } = useProjects();
  const isManager = useIsManager();
  const [creating, setCreating] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ProjectStatus | 'ALL'>('ALL');

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return projects.filter(
      (p) => (status === 'ALL' || p.status === status) && (!q || `${p.name} ${p.code} ${p.client ?? ''}`.toLowerCase().includes(q)),
    );
  }, [projects, search, status]);

  if (isLoading) return <Spinner />;

  return (
    <div className="animate-fade-in">
      <PageHeader
        title="Projets"
        subtitle={`${projects.length} projet${projects.length > 1 ? 's' : ''} — développement, conseil et interne`}
        actions={isManager && <Button icon={<Plus className="size-4" />} onClick={() => setCreating(true)}>Nouveau projet</Button>}
      />
      <div className="mb-5 flex flex-wrap gap-2">
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-ink-3" />
          <input className="input w-64 pl-8" placeholder="Nom, code, client…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="flex flex-wrap rounded-lg border border-line bg-surface p-0.5">
          {(['ALL', 'ACTIVE', 'PLANNED', 'ON_HOLD', 'COMPLETED'] as const).map((s) => (
            <button key={s} onClick={() => setStatus(s)} className={clsx('rounded-md px-2.5 py-1 text-xs font-medium', status === s ? 'bg-surface-2 text-ink shadow-sm' : 'text-ink-3 hover:text-ink')}>
              {s === 'ALL' ? 'Tous' : projectStatusLabel[s]}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon={<FolderPlus className="size-5" />} title="Aucun projet" description="Créez un premier projet pour commencer." action={isManager && <Button onClick={() => setCreating(true)}>Créer un projet</Button>} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {filtered.map((p) => (
            <Link key={p.id} to={`/projects/${p.id}`} className="card group flex flex-col p-5 transition hover:-translate-y-0.5 hover:shadow-pop">
              <div className="flex items-start gap-3">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-xl text-xs font-bold text-white" style={{ backgroundColor: p.color }}>{p.code.slice(0, 2)}</span>
                <div className="min-w-0 flex-1">
                  <div className="truncate font-semibold group-hover:text-brand-600">{p.name}</div>
                  <div className="truncate text-xs text-ink-3">{projectTypeLabel[p.type]} · {p.client ?? 'Interne'}</div>
                </div>
                <HealthBadge health={p.health} />
              </div>
              {p.description && <p className="mt-3 line-clamp-2 text-sm text-ink-2">{p.description}</p>}
              <div className="mt-4 flex flex-wrap gap-1">
                {p.modules.map((m) => <span key={m} className="rounded-md bg-surface-2 px-1.5 py-0.5 text-[11px] text-ink-2">{moduleInfo[m].label}</span>)}
              </div>
              <div className="mt-auto flex items-center justify-between pt-4 text-xs text-ink-3">
                <span className="flex items-center gap-1.5">{p.owner && <Avatar name={p.owner.displayName} size="xs" />}{projectStatusLabel[p.status]}</span>
                <span>Échéance {formatDate(p.endDate, true)}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
      <ProjectFormModal open={creating} onClose={() => setCreating(false)} project={null} />
    </div>
  );
}
