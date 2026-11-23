import type { Health, ModuleType, PhaseState, Priority, ProjectStatus, ProjectType, Role, TaskStatus } from '../api/types';

export const projectTypeLabel: Record<ProjectType, string> = {
  DEVELOPMENT: 'Développement',
  CONSULTING: 'Conseil',
  INTERNAL: 'Interne',
};

export const projectStatusLabel: Record<ProjectStatus, string> = {
  PLANNED: 'Planifié',
  ACTIVE: 'En cours',
  ON_HOLD: 'En pause',
  COMPLETED: 'Terminé',
  CANCELLED: 'Annulé',
};

export const healthLabel: Record<Health, string> = {
  GREEN: 'Sous contrôle',
  AMBER: 'Vigilance',
  RED: 'Critique',
};

export const taskStatusLabel: Record<TaskStatus, string> = {
  BACKLOG: 'Backlog',
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  REVIEW: 'En revue',
  DONE: 'Terminé',
};

export const priorityLabel: Record<Priority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  CRITICAL: 'Critique',
};

export const roleLabel: Record<Role, string> = {
  ADMIN: 'Administrateur',
  MANAGER: 'Manager',
  MEMBER: 'Membre',
};

export const phaseStateLabel: Record<PhaseState, string> = {
  DONE: 'Terminé',
  LATE: 'En retard',
  IN_PROGRESS: 'En cours',
  UPCOMING: 'À venir',
  UNSCHEDULED: 'Non daté',
};

export const moduleInfo: Record<ModuleType, { label: string; description: string }> = {
  BOARD: { label: 'Tableau Kanban', description: 'Tâches en colonnes, glisser-déposer, priorités et échéances.' },
  TODOS: { label: 'Checklist', description: "Todos partagés de l'équipe projet." },
  RETRO_PLANNING: { label: 'Rétro-planning', description: "Phases et jalons calculés à rebours depuis l'échéance." },
  POKER: { label: 'Planning poker', description: 'Estimation collaborative des stories en points.' },
};

export const TASK_COLUMNS: TaskStatus[] = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
