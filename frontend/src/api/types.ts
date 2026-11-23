export type Role = 'ADMIN' | 'MANAGER' | 'MEMBER';
export type ProjectType = 'DEVELOPMENT' | 'CONSULTING' | 'INTERNAL';
export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type Health = 'GREEN' | 'AMBER' | 'RED';
export type ModuleType = 'BOARD' | 'TODOS' | 'RETRO_PLANNING' | 'POKER';
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
}

export interface Project {
  id: number;
  code: string;
  name: string;
  description: string | null;
  client: string | null;
  type: ProjectType;
  status: ProjectStatus;
  health: Health;
  color: string;
  owner: User | null;
  startDate: string | null;
  endDate: string | null;
  budgetDays: number | null;
  modules: ModuleType[];
}

export interface ProjectInput {
  code: string;
  name: string;
  description?: string | null;
  client?: string | null;
  type: ProjectType;
  status?: ProjectStatus;
  health?: Health;
  color?: string;
  ownerId?: number | null;
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
  health: Health;
  color: string;
  ownerName: string | null;
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
}

export interface Portfolio {
  kpis: {
    openProjects: number;
    atRiskProjects: number;
    openTasks: number;
    overdueTasks: number;
    completedLast7Days: number;
  };
  projects: ProjectSummary[];
  workload: { userId: number; displayName: string; jobTitle: string | null; openTasks: number; openPoints: number }[];
}
