export type Role = 'OWNER' | 'ADMIN' | 'MANAGER' | 'MEMBER' | 'GUEST';
export type ProjectRole = 'LEAD' | 'CONTRIBUTOR' | 'VIEWER';
export type Visibility = 'ORGANIZATION' | 'TEAM' | 'RESTRICTED';
export type ProjectPriority = 'P1' | 'P2' | 'P3' | 'P4';
export type Plan = 'FREE' | 'TEAM' | 'ENTERPRISE';
export type Permission =
  | 'PROJECT_EDIT'
  | 'PROJECT_MODULES'
  | 'PROJECT_MEMBERS'
  | 'PROJECT_DELETE'
  | 'TASK_VIEW'
  | 'TASK_CREATE'
  | 'TASK_EDIT'
  | 'TASK_DELETE'
  | 'TODO_VIEW'
  | 'TODO_EDIT'
  | 'RETRO_VIEW'
  | 'RETRO_EDIT'
  | 'POKER_VIEW'
  | 'POKER_VOTE'
  | 'POKER_FACILITATE'
  | 'TIME_VIEW'
  | 'TIME_LOG'
  | 'TIME_MANAGE'
  | 'RISK_VIEW'
  | 'RISK_EDIT'
  | 'REPORT_EDIT'
  | 'TASK_COMMENT';
export type HolidayCalendar = 'NONE' | 'FR';
export type AbsenceKind = 'VACATION' | 'SICK' | 'TRAINING' | 'OTHER';
export type ProjectType = 'DEVELOPMENT' | 'CONSULTING' | 'INTERNAL';
export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type Health = 'GREEN' | 'AMBER' | 'RED';
export type ModuleType = 'BOARD' | 'TODOS' | 'RETRO_PLANNING' | 'POKER' | 'TIME_TRACKING' | 'RISKS';
export type TaskStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type PhaseKind = 'PHASE' | 'MILESTONE';
export type PhaseState = 'DONE' | 'LATE' | 'IN_PROGRESS' | 'UPCOMING' | 'UNSCHEDULED';
export type RoundStatus = 'VOTING' | 'REVEALED' | 'CLOSED';

export interface User {
  id: number;
  email: string;
  displayName: string;
  role: Role;
  jobTitle: string | null;
  active: boolean;
  capacityDaysPerWeek: number;
}

export interface PageDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditEntry {
  id: number;
  actorName: string | null;
  action: string;
  entityType: string | null;
  entityId: number | null;
  summary: string | null;
  createdAt: string;
}

export interface Me extends Omit<User, 'active' | 'capacityDaysPerWeek'> {
  organization: { id: number; name: string; slug: string; plan: Plan };
  teams: { teamId: number; teamName: string; role: ProjectRole }[];
  mustChangePassword: boolean;
}

export interface Project {
  id: number;
  code: string;
  name: string;
  description: string | null;
  client: string | null;
  type: ProjectType;
  status: ProjectStatus;
  statusReason: string | null;
  priority: ProjectPriority;
  health: Health;
  color: string;
  owner: User | null;
  team: { id: number; name: string; color: string } | null;
  visibility: Visibility;
  startDate: string | null;
  endDate: string | null;
  budgetDays: number | null;
  modules: ModuleType[];
  /** null lorsque l'utilisateur est administrateur (accès complet). */
  myRole: ProjectRole | null;
  myPermissions: Permission[];
}

export interface StatusChange {
  id: number;
  fromStatus: ProjectStatus | null;
  toStatus: ProjectStatus;
  reason: string | null;
  changedBy: string | null;
  changedAt: string;
}

export interface Team {
  id: number;
  name: string;
  description: string | null;
  color: string;
  canManage: boolean;
  members: { userId: number; displayName: string; email: string; jobTitle: string | null; role: ProjectRole }[];
}

export interface ProjectMember {
  userId: number;
  displayName: string;
  email: string;
  orgRole: Role;
  role: ProjectRole;
}

export interface ProjectMembers {
  teamName: string | null;
  visibility: Visibility;
  teamMembers: ProjectMember[];
  explicitMembers: ProjectMember[];
}

export interface PermissionMatrix {
  customized: boolean;
  roles: Record<ProjectRole, Permission[]>;
}

export interface Organization {
  id: number;
  name: string;
  slug: string;
  plan: Plan;
  usage: { projects: number; maxProjects: number; users: number; maxUsers: number; customPermissions: boolean };
  holidayCalendar: HolidayCalendar;
}

export interface ImportReport {
  dryRun: boolean;
  applied: boolean;
  rows: number;
  created: number;
  updated: number;
  teamsCreated: number;
  errors: { line: number; message: string }[];
  preview: string[];
}

export interface ProjectInput {
  code: string;
  name: string;
  description?: string | null;
  client?: string | null;
  type: ProjectType;
  status?: ProjectStatus;
  statusReason?: string | null;
  priority?: ProjectPriority;
  health?: Health;
  color?: string;
  ownerId?: number | null;
  teamId?: number | null;
  visibility?: Visibility;
  startDate?: string | null;
  endDate?: string | null;
  budgetDays?: number | null;
}

export interface Task {
  id: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  projectColor: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: Priority;
  assignee: User | null;
  storyPoints: number | null;
  dueDate: string | null;
  position: number;
  completedAt: string | null;
  overdue: boolean;
}

export interface TaskInput {
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: Priority;
  assigneeId?: number | null;
  storyPoints?: number | null;
  dueDate?: string | null;
}

export interface Todo {
  id: number;
  title: string;
  done: boolean;
  dueDate: string | null;
  projectId: number | null;
  ownerName: string;
}

export interface TodoInput {
  title: string;
  done: boolean;
  dueDate?: string | null;
}

export interface RetroPhase {
  id: number;
  name: string;
  kind: PhaseKind;
  durationDays: number;
  done: boolean;
  start: string | null;
  end: string | null;
  state: PhaseState;
}

export interface RetroPlan {
  deadline: string | null;
  projectStart: string | null;
  computedStart: string | null;
  startConflict: boolean;
  totalWorkingDays: number;
  phases: RetroPhase[];
}

export interface PhaseInput {
  name: string;
  kind: PhaseKind;
  durationDays: number;
  done: boolean;
}

export interface PokerRound {
  id: number;
  taskId: number | null;
  storyTitle: string;
  status: RoundStatus;
  finalEstimate: string | null;
  myCard: string | null;
  votes: { userId: number; displayName: string; card: string | null }[];
  stats: { average: number | null; suggested: string | null; consensus: boolean } | null;
}

export interface ProjectSummary {
  id: number;
  code: string;
  name: string;
  client: string | null;
  type: ProjectType;
  status: ProjectStatus;
  statusReason: string | null;
  priority: ProjectPriority;
  health: Health;
  color: string;
  ownerName: string | null;
  teamId: number | null;
  teamName: string | null;
  startDate: string | null;
  endDate: string | null;
  daysRemaining: number | null;
  progress: number;
  tasksTotal: number;
  tasksByStatus: Record<TaskStatus, number>;
  overdueTasks: number;
  nextMilestone: { name: string; date: string } | null;
  latePhases: number;
  atRisk: boolean;
  budgetDays: number | null;
  consumedDays: number;
  budgetPct: number | null;
}

export interface Portfolio {
  scope: 'ORGANIZATION' | 'MY_PROJECTS';
  teams: { id: number; name: string; color: string; openProjects: number; atRiskProjects: number }[];
  kpis: {
    openProjects: number;
    atRiskProjects: number;
    onHoldProjects: number;
    p1Projects: number;
    overBudgetProjects: number;
    openTasks: number;
    overdueTasks: number;
    completedLast7Days: number;
  };
  projects: ProjectSummary[];
  workload: { userId: number; displayName: string; jobTitle: string | null; openTasks: number; openPoints: number }[];
}

// ---------- Temps & budget ----------
export interface TimeSummary {
  budgetDays: number | null;
  consumedDays: number;
  remainingDays: number | null;
  consumptionPct: number | null;
  weeklyBurn: number;
  projectedExhaustion: string | null;
  overBudget: boolean;
  byUser: { userId: number; name: string; days: number }[];
  byMonth: { month: string; days: number }[];
}

export interface TimeEntry {
  id: number;
  date: string;
  days: number;
  userId: number;
  userName: string;
  taskId: number | null;
  taskTitle: string | null;
  comment: string | null;
  editable: boolean;
}

export interface WeekCell {
  projectId: number;
  date: string;
  days: number;
}

export interface Timesheet {
  weekStart: string;
  days: string[];
  workingDays: boolean[];
  projects: { projectId: number; code: string; name: string; color: string }[];
  cells: WeekCell[];
  taskEntries: TimeEntry[];
}

// ---------- Capacité ----------
export interface Allocation {
  id: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  projectColor: string;
  userId: number;
  userName: string;
  startDate: string;
  endDate: string;
  daysPerWeek: number;
}

export interface Absence {
  id: number;
  userId: number;
  userName: string;
  startDate: string;
  endDate: string;
  kind: AbsenceKind | null;
  comment: string | null;
  editable: boolean;
}

export interface WeekLoad {
  weekStart: string;
  capacity: number;
  absent: number;
  available: number;
  allocated: number;
  loadPct: number | null;
}

export interface Capacity {
  weeks: string[];
  people: { userId: number; displayName: string; jobTitle: string | null; daysPerWeek: number; weeks: WeekLoad[]; allocations: Allocation[] }[];
}
