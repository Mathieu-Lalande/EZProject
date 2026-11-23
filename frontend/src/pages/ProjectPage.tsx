import { useState } from 'react';
import { Navigate, NavLink, useParams } from 'react-router-dom';
import { CalendarClock, KanbanSquare, LayoutDashboard, ListChecks, Pencil, Settings, Spade, Trash2 } from 'lucide-react';
import clsx from 'clsx';
import { useDeleteProject, useMe, useProject } from '../api/hooks';
import type { ModuleType } from '../api/types';
import { useIsManager } from '../auth/RequireAuth';
import { Spinner } from '../components/ui/Spinner';
import { HealthBadge } from '../components/ui/HealthBadge';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { useToast } from '../components/ui/Toast';
import { projectStatusLabel, projectTypeLabel } from '../lib/labels';
import { KanbanBoard } from '../features/board/KanbanBoard';
import { TodoList } from '../features/todos/TodoList';
import { RetroPlanning } from '../features/retro/RetroPlanning';
import { PokerRoom } from '../features/poker/PokerRoom';
import { ProjectOverview } from '../features/projects/ProjectOverview';
import { ModulesSettings } from '../features/projects/ModulesSettings';
import { ProjectFormModal } from '../features/projects/ProjectFormModal';

interface Tab {
  id: string;
  label: string;
  icon: React.ReactNode;
  module?: ModuleType;
}

const TABS: Tab[] = [
  { id: 'overview', label: "Vue d'ensemble", icon: <LayoutDashboard className="size-4" /> },
  { id: 'board', label: 'Kanban', icon: <KanbanSquare className="size-4" />, module: 'BOARD' },
  { id: 'todos', label: 'Checklist', icon: <ListChecks className="size-4" />, module: 'TODOS' },
  { id: 'retro', label: 'Rétro-planning', icon: <CalendarClock className="size-4" />, module: 'RETRO_PLANNING' },
  { id: 'poker', label: 'Planning poker', icon: <Spade className="size-4" />, module: 'POKER' },
  { id: 'settings', label: 'Modules', icon: <Settings className="size-4" /> },
];

export function ProjectPage() {
  const { id, tab = 'overview' } = useParams();
  const projectId = Number(id);
  const { data: project, isLoading, isError } = useProject(projectId);
  const { data: me } = useMe();
  const isManager = useIsManager();
  const remove = useDeleteProject();
  const toast = useToast();
  const [editing, setEditing] = useState(false);

  if (isLoading) return <Spinner />;
  if (isError || !project) return <Navigate to="/projects" replace />;

  const tabs = TABS.filter((t) => !t.module || project.modules.includes(t.module));
  if (!tabs.some((t) => t.id === tab)) return <Navigate to={`/projects/${project.id}`} replace />;

  const onDelete = () => {
    if (!confirm(`Supprimer définitivement le projet « ${project.name} » et toutes ses données ?`)) return;
    remove.mutate(project.id, { onSuccess: () => toast('success', 'Projet supprimé') });
  };

  return (
    <div className="animate-fade-in">
      {remove.isSuccess && <Navigate to="/projects" replace />}
      <div className="mb-6 flex flex-wrap items-start gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-2xl text-sm font-bold text-white shadow-sm" style={{ backgroundColor: project.color }}>
          {project.code.slice(0, 2)}
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{project.name}</h1>
            <HealthBadge health={project.health} />
            <Badge>{projectStatusLabel[project.status]}</Badge>
          </div>
          <p className="mt-0.5 text-sm text-ink-3">
            {project.code} · {projectTypeLabel[project.type]}{project.client ? ` · ${project.client}` : ''}
          </p>
        </div>
        {isManager && (
          <div className="flex gap-2">
            <Button variant="secondary" icon={<Pencil className="size-4" />} onClick={() => setEditing(true)}>Modifier</Button>
            {me?.role === 'ADMIN' && <Button variant="ghost" aria-label="Supprimer le projet" icon={<Trash2 className="size-4" />} onClick={onDelete} />}
          </div>
        )}
      </div>

      <nav className="scrollbar-thin mb-6 flex gap-1 overflow-x-auto border-b border-line">
        {tabs.map((t) => (
          <NavLink
            key={t.id}
            end
            to={t.id === 'overview' ? `/projects/${project.id}` : `/projects/${project.id}/${t.id}`}
            className={({ isActive }) =>
              clsx('-mb-px flex items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium whitespace-nowrap transition', isActive ? 'border-brand-600 text-ink' : 'border-transparent text-ink-3 hover:text-ink')
            }
          >
            {t.icon}
            {t.label}
          </NavLink>
        ))}
      </nav>

      {tab === 'overview' && <ProjectOverview project={project} />}
      {tab === 'board' && <KanbanBoard projectId={project.id} />}
      {tab === 'todos' && <TodoList projectId={project.id} title="Checklist de l'équipe" />}
      {tab === 'retro' && <RetroPlanning project={project} />}
      {tab === 'poker' && <PokerRoom projectId={project.id} />}
      {tab === 'settings' && (
        <div className="max-w-3xl">
          <p className="mb-4 text-sm text-ink-3">
            Activez uniquement ce dont l'équipe a besoin. {!isManager && 'Seuls les managers peuvent modifier les modules.'}
          </p>
          <ModulesSettings project={project} editable={isManager} />
        </div>
      )}

      <ProjectFormModal open={editing} onClose={() => setEditing(false)} project={project} />
    </div>
  );
}
