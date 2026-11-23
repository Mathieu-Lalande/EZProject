import type {
  Health,
  ModuleType,
  Permission,
  PhaseState,
  Plan,
  Priority,
  ProjectPriority,
  ProjectRole,
  ProjectStatus,
  ProjectType,
  Role,
  TaskStatus,
  Visibility,
} from '../api/types';

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
  OWNER: 'Propriétaire',
  ADMIN: 'Administrateur',
  MANAGER: 'Manager',
  MEMBER: 'Membre',
  GUEST: 'Invité',
};

export const roleDescription: Record<Role, string> = {
  OWNER: 'Gère l’offre et tous les paramètres. Accès complet.',
  ADMIN: 'Gère membres, équipes et permissions. Accès complet aux projets.',
  MANAGER: 'Pilotage COO / PMO : voit et dirige tous les projets.',
  MEMBER: 'Accède aux projets de ses équipes selon son rôle d’équipe.',
  GUEST: 'Externe (client, partenaire) : uniquement les projets où il est invité.',
};

export const projectRoleLabel: Record<ProjectRole, string> = {
  LEAD: 'Responsable',
  CONTRIBUTOR: 'Contributeur',
  VIEWER: 'Lecteur',
};

export const visibilityLabel: Record<Visibility, { label: string; description: string }> = {
  ORGANIZATION: { label: 'Organisation', description: 'Tous les membres peuvent consulter (lecture).' },
  TEAM: { label: 'Équipe', description: 'Réservé à l’équipe du projet et aux membres ajoutés.' },
  RESTRICTED: { label: 'Confidentiel', description: 'Uniquement les membres ajoutés explicitement.' },
};

export const projectPriorityLabel: Record<ProjectPriority, string> = {
  P1: 'P1 · Stratégique',
  P2: 'P2 · Haute',
  P3: 'P3 · Normale',
  P4: 'P4 · Basse',
};

export const planLabel: Record<Plan, string> = { FREE: 'Free', TEAM: 'Team', ENTERPRISE: 'Enterprise' };

/** Permissions regroupées par élément de projet, pour la matrice rôles / permissions. */
export const permissionGroups: { label: string; items: { id: Permission; label: string }[] }[] = [
  {
    label: 'Projet',
    items: [
      { id: 'PROJECT_EDIT', label: 'Modifier infos, statut, priorité' },
      { id: 'PROJECT_MODULES', label: 'Activer / désactiver les modules' },
      { id: 'PROJECT_MEMBERS', label: 'Gérer les accès et membres' },
      { id: 'PROJECT_DELETE', label: 'Supprimer le projet' },
    ],
  },
  {
    label: 'Tâches (Kanban)',
    items: [
      { id: 'TASK_VIEW', label: 'Voir' },
      { id: 'TASK_CREATE', label: 'Ajouter' },
      { id: 'TASK_EDIT', label: 'Modifier / déplacer' },
      { id: 'TASK_DELETE', label: 'Supprimer' },
    ],
  },
  {
    label: 'Checklist',
    items: [
      { id: 'TODO_VIEW', label: 'Voir' },
      { id: 'TODO_EDIT', label: 'Ajouter / modifier' },
    ],
  },
  {
    label: 'Rétro-planning',
    items: [
      { id: 'RETRO_VIEW', label: 'Voir' },
      { id: 'RETRO_EDIT', label: 'Modifier' },
    ],
  },
  {
    label: 'Planning poker',
    items: [
      { id: 'POKER_VIEW', label: 'Voir' },
      { id: 'POKER_VOTE', label: 'Voter' },
      { id: 'POKER_FACILITATE', label: 'Animer (lancer, révéler, valider)' },
    ],
  },
  {
    label: 'Temps & budget',
    items: [
      { id: 'TIME_VIEW', label: 'Voir les temps et le budget' },
      { id: 'TIME_LOG', label: 'Saisir ses temps' },
      { id: 'TIME_MANAGE', label: 'Gérer les temps des autres et le staffing' },
    ],
  },
  {
    label: 'Risques & pilotage',
    items: [
      { id: 'RISK_VIEW', label: 'Voir risques et décisions' },
      { id: 'RISK_EDIT', label: 'Gérer risques et décisions' },
      { id: 'REPORT_EDIT', label: 'Publier la météo / rapport COPIL' },
    ],
  },
  {
    label: 'Collaboration',
    items: [{ id: 'TASK_COMMENT', label: 'Commenter les tâches' }],
  },
];

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
  TIME_TRACKING: { label: 'Temps & budget', description: 'Saisie des temps passés, consommation du budget, staffing.' },
  RISKS: { label: 'Risques & décisions', description: 'Registre des risques (probabilité × impact) et des décisions.' },
};

export const absenceKindLabel: Record<'VACATION' | 'SICK' | 'TRAINING' | 'OTHER', string> = {
  VACATION: 'Congés',
  SICK: 'Arrêt maladie',
  TRAINING: 'Formation',
  OTHER: 'Autre',
};

export const TASK_COLUMNS: TaskStatus[] = ['BACKLOG', 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];

export const trendLabel: Record<'UP' | 'STABLE' | 'DOWN', string> = { UP: 'En amélioration', STABLE: 'Stable', DOWN: 'En dégradation' };

export const riskStatusLabel: Record<'OPEN' | 'MITIGATING' | 'OCCURRED' | 'CLOSED', string> = {
  OPEN: 'Ouvert',
  MITIGATING: 'En traitement',
  OCCURRED: 'Survenu',
  CLOSED: 'Clos',
};

export const decisionStatusLabel: Record<'PROPOSED' | 'APPROVED' | 'REJECTED', string> = {
  PROPOSED: 'À décider',
  APPROVED: 'Validée',
  REJECTED: 'Refusée',
};

export const probabilityLabel = ['', 'Faible', 'Possible', 'Probable', 'Quasi certaine'];
export const impactLabel = ['', 'Mineur', 'Modéré', 'Majeur', 'Critique'];
