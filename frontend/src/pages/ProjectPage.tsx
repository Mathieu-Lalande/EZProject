import { useState } from 'react';
import { Navigate, NavLink, useParams } from 'react-router-dom';
import { CalendarClock, KanbanSquare, LayoutDashboard, ListChecks, PauseCircle, Pencil, Settings, ShieldCheck, Spade, Trash2, Users } from 'lucide-react';
import clsx from 'clsx';
import { useDeleteProject, useProject } from '../api/hooks';
import type { ModuleType, Permission } from '../api/types';
import { Spinner } from '../components/ui/Spinner';
import { HealthBadge } from '../components/ui/HealthBadge';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { useToast } from '../components/ui/Toast';
import { projectRoleLabel, projectTypeLabel, visibilityLabel } from '../lib/labels';
import { can } from '../lib/permissions';
import { KanbanBoard } from '../features/board/KanbanBoard';
import { TodoList } from '../features/todos/TodoList';
import { RetroPlanning } from '../features/retro/RetroPlanning';
import { PokerRoom } from '../features/poker/PokerRoom';
import { ProjectOverview } from '../features/projects/ProjectOverview';
import { ModulesSettings } from '../features/projects/ModulesSettings';
import { ProjectFormModal } from '../features/projects/ProjectFormModal';
import { ProjectAccess } from '../features/projects/ProjectAccess';
import { StatusControls } from '../features/projects/StatusControls';

interface Tab {
  id: string;
  label: string;
  icon: React.ReactNode;
  module?: ModuleType;
  /** Permission de lecture requise pour voir l'onglet. */
  view?: Permission;
}

const TABS: Tab[] = [
  { id: 'overview', label: "Vue d'ensemble", icon: <LayoutDashboard className="size-4" /> },
  { id: 'board', label: 'Kanban', icon: <KanbanSquare className="size-4" />, module: 'BOARD', view: 'TASK_VIEW' },
  { id: 'todos', label: 'Checklist', icon: <ListChecks className="size-4" />, module: 'TODOS', view: 'TODO_VIEW' },
  { id: 'retro', label: 'Rétro-planning', icon: <CalendarClock className="size-4" />, module: 'RETRO_PLANNING', view: 'RETRO_VIEW' },
  { id: 'poker', label: 'Planning poker', icon: <Spade className="size-4" />, module: 'POKER', view: 'POKER_VIEW' },
  { id: 'access', label: 'Accès', icon: <ShieldCheck className="size-4" /> },
  { id: 'settings', label: 'Modules', icon: <Settings className="size-4" /> },
];

export function ProjectPage() {
  const { id, tab = 'overview' } = useParams();
  const projectId = Number(id);
  const { data: project, isLoading, isError } = useProject(projectId);
  const remove = useDeleteProject();
  const toast = useToast();
  const [editing, setEditing] = useState(false);

  if (isLoading) return <Spinner />;
  if (isError || !project) return <Navigate to="/projects" replace />;

  const tabs = TABS.filter((t) => (!t.module || project.modules.includes(t.module)) && (!t.view || can(project, t.view)));
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
            <StatusControls project={project} />
          </div>
          <p className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-ink-3">
            <span>{project.code} · {projectTypeLabel[project.type]}{project.client ? ` · ${project.client}` : ''}</span>
            {project.team && (
              <span className="flex items-center gap-1"><Users className="size-3.5" />{project.team.name}</span>
            )}
            <Badge>{visibilityLabel[project.visibility].label}</Badge>
            <Badge tone="brand">{project.myRole ? projectRoleLabel[project.myRole] : 'Administrateur'}</Badge>
          </p>
        </div>
        <div className="flex gap-2">
          {can(project, 'PROJECT_EDIT') && (
            <Button variant="secondary" icon={<Pencil className="size-4" />} onClick={() => setEditing(true)}>Modifier</Button>
          )}
          {can(project, 'PROJECT_DELETE') && <Button variant="ghost" aria-label="Supprimer le projet" icon={<Trash2 className="size-4" />} onClick={onDelete} />}
        </div>
      </div>

      {project.status === 'ON_HOLD' && (
        <div className="mb-5 flex items-start gap-2.5 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-500/30 dark:bg-amber-500/10 dark:text-amber-200">
          <PauseCircle className="mt-0.5 size-4 shrink-0" />
          <span><strong>Projet en pause.</strong> {project.statusReason}</span>
        </div>
      )}

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
      {tab === 'board' && <KanbanBoard project={project} />}
      {tab === 'todos' && <TodoList projectId={project.id} title="Checklist de l'équipe" readOnly={!can(project, 'TODO_EDIT')} />}
      {tab === 'retro' && <RetroPlanning project={project} />}
      {tab === 'poker' && <PokerRoom project={project} />}
      {tab === 'access' && <ProjectAccess project={project} />}
      {tab === 'settings' && (
        <div className="max-w-3xl">
          <p className="mb-4 text-sm text-ink-3">
            Activez uniquement ce dont l'équipe a besoin.{' '}
            {!can(project, 'PROJECT_MODULES') && 'Votre rôle ne permet pas de modifier les modules.'}
          </p>
          <ModulesSettings project={project} />
        </div>
      )}

      <ProjectFormModal open={editing} onClose={() => setEditing(false)} project={project} />
    </div>
  );
}
