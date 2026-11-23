import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query';
import { api, request } from './client';
import type {
  AuditEntry,
  PageDto,
  ImportReport,
  Me,
  Organization,
  PermissionMatrix,
  Permission,
  Plan,
  ProjectMembers,
  ProjectPriority,
  ProjectRole,
  ProjectStatus,
  Role,
  StatusChange,
  Team,
  ModuleType,
  PhaseInput,
  Portfolio,
  PokerRound,
  Project,
  ProjectInput,
  ProjectType,
  RetroPlan,
  Task,
  TaskInput,
  TaskStatus,
  Todo,
  TodoInput,
  User,
} from './types';

export const keys = {
  me: ['me'] as const,
  users: ['users'] as const,
  projects: ['projects'] as const,
  project: (id: number) => ['projects', id] as const,
  tasks: (projectId: number) => ['projects', projectId, 'tasks'] as const,
  myTasks: ['tasks', 'mine'] as const,
  todos: (projectId?: number) => (projectId ? (['projects', projectId, 'todos'] as const) : (['todos'] as const)),
  retro: (projectId: number) => ['projects', projectId, 'retro'] as const,
  poker: (projectId: number) => ['projects', projectId, 'poker'] as const,
  portfolio: ['portfolio'] as const,
  organization: ['organization'] as const,
  teams: ['teams'] as const,
  permissions: ['permissions'] as const,
  history: (projectId: number) => ['projects', projectId, 'history'] as const,
  members: (projectId: number) => ['projects', projectId, 'members'] as const,
};

/** Invalide plusieurs clés après une mutation (les vues dérivées comme le portefeuille se rafraîchissent). */
function useInvalidate() {
  const qc = useQueryClient();
  return (...queryKeys: QueryKey[]) => Promise.all(queryKeys.map((k) => qc.invalidateQueries({ queryKey: k })));
}

// ---------- Auth & utilisateurs ----------
export const useMe = () =>
  useQuery({ queryKey: keys.me, queryFn: () => api.get<Me>('/api/auth/me'), retry: false, staleTime: Infinity });

export function useLogin() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async ({ email, password }: { email: string; password: string }) => {
      // Une première requête GET garantit la présence du cookie XSRF-TOKEN
      await api.get('/api/auth/me').catch(() => undefined);
      await api.postForm('/api/auth/login', new URLSearchParams({ email, password }));
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.me }),
  });
}

export function useLogout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => api.post('/api/auth/logout'),
    onSettled: () => {
      qc.clear();
      qc.setQueryData(keys.me, undefined);
      void qc.invalidateQueries({ queryKey: keys.me });
    },
  });
}

export const useUsers = () => useQuery({ queryKey: keys.users, queryFn: () => api.get<User[]>('/api/users') });

export function useSignup() {
  return useMutation({
    mutationFn: (input: { organizationName: string; displayName: string; email: string; password: string }) =>
      api.post('/api/auth/signup', input),
  });
}

export function useCreateUser() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (input: { email: string; displayName: string; password: string; role: Role; jobTitle?: string }) =>
      api.post<User>('/api/users', input),
    onSuccess: () => invalidate(keys.users, keys.organization),
  });
}

export function useChangeUserRole() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: Role }) => api.put<User>(`/api/users/${userId}/role`, { role }),
    onSuccess: () => invalidate(keys.users, keys.projects, keys.portfolio),
  });
}

// ---------- Mon compte ----------
export function useAccountMutations() {
  const qc = useQueryClient();
  return {
    profile: useMutation({
      mutationFn: (input: { displayName: string; jobTitle: string | null }) => api.put<User>('/api/account/profile', input),
      onSuccess: () => qc.invalidateQueries({ queryKey: keys.me }),
    }),
    password: useMutation({
      mutationFn: (input: { currentPassword: string; newPassword: string }) => api.put('/api/account/password', input),
      onSuccess: () => qc.invalidateQueries({ queryKey: keys.me }),
    }),
  };
}

export function useDeleteAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (password: string) => request('DELETE', '/api/account', { password }),
    onSuccess: () => {
      qc.clear();
      void qc.invalidateQueries({ queryKey: keys.me });
    },
  });
}

// ---------- Administration des comptes ----------
export function useUserAdmin() {
  const invalidate = useInvalidate();
  const onSuccess = () => invalidate(keys.users, keys.organization);
  return {
    resetPassword: useMutation({
      mutationFn: (userId: number) => api.post<{ temporaryPassword: string }>(`/api/users/${userId}/reset-password`),
    }),
    setActive: useMutation({
      mutationFn: ({ userId, active }: { userId: number; active: boolean }) =>
        api.put<User>(`/api/users/${userId}/active`, { active }),
      onSuccess,
    }),
    remove: useMutation({ mutationFn: (userId: number) => api.delete(`/api/users/${userId}`), onSuccess }),
  };
}

export const useAudit = (params: { page: number; q: string; entityType: string }) =>
  useQuery({
    queryKey: ['audit', params],
    queryFn: () =>
      api.get<PageDto<AuditEntry>>(
        `/api/organization/audit?page=${params.page}&size=25&q=${encodeURIComponent(params.q)}&entityType=${params.entityType}`,
      ),
    placeholderData: (previous) => previous,
  });

export function useDeleteOrganization() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (confirmationName: string) => request('DELETE', '/api/organization', { confirmationName }),
    onSuccess: () => {
      qc.clear();
      void qc.invalidateQueries({ queryKey: keys.me });
    },
  });
}

// ---------- Organisation (SaaS) ----------
export const useOrganization = () =>
  useQuery({ queryKey: keys.organization, queryFn: () => api.get<Organization>('/api/organization') });

export function useOrganizationMutations() {
  const invalidate = useInvalidate();
  const onSuccess = () => invalidate(keys.organization, keys.me);
  return {
    rename: useMutation({ mutationFn: (name: string) => api.put<Organization>('/api/organization', { name }), onSuccess }),
    changePlan: useMutation({ mutationFn: (plan: Plan) => api.put<Organization>('/api/organization/plan', { plan }), onSuccess }),
  };
}

export const usePermissionMatrix = () =>
  useQuery({ queryKey: keys.permissions, queryFn: () => api.get<PermissionMatrix>('/api/organization/permissions') });

export function usePermissionMatrixMutations() {
  const qc = useQueryClient();
  const onSuccess = (m: PermissionMatrix) => {
    qc.setQueryData(keys.permissions, m);
    void qc.invalidateQueries({ queryKey: keys.projects });
  };
  return {
    save: useMutation({
      mutationFn: (roles: Record<ProjectRole, Permission[]>) => api.put<PermissionMatrix>('/api/organization/permissions', { roles }),
      onSuccess,
    }),
    reset: useMutation({ mutationFn: () => api.delete<PermissionMatrix>('/api/organization/permissions'), onSuccess }),
  };
}

// ---------- Équipes ----------
export const useTeams = () => useQuery({ queryKey: keys.teams, queryFn: () => api.get<Team[]>('/api/teams') });

export function useTeamMutations() {
  const invalidate = useInvalidate();
  const onSuccess = () => invalidate(keys.teams, keys.me, keys.projects, keys.portfolio);
  return {
    save: useMutation({
      mutationFn: ({ id, input }: { id?: number; input: { name: string; description?: string | null; color?: string } }) =>
        id ? api.put<Team>(`/api/teams/${id}`, input) : api.post<Team>('/api/teams', input),
      onSuccess,
    }),
    remove: useMutation({ mutationFn: (id: number) => api.delete(`/api/teams/${id}`), onSuccess }),
    putMember: useMutation({
      mutationFn: ({ teamId, userId, role }: { teamId: number; userId: number; role: ProjectRole }) =>
        api.put<Team>(`/api/teams/${teamId}/members/${userId}`, { role }),
      onSuccess,
    }),
    removeMember: useMutation({
      mutationFn: ({ teamId, userId }: { teamId: number; userId: number }) => api.delete<Team>(`/api/teams/${teamId}/members/${userId}`),
      onSuccess,
    }),
  };
}

// ---------- Import / export ----------
export function useImport(kind: 'projects' | 'tasks') {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ file, dryRun }: { file: File; dryRun: boolean }) => {
      const data = new FormData();
      data.append('file', file);
      return api.upload<ImportReport>(`/api/import/${kind}?dryRun=${dryRun}`, data);
    },
    onSuccess: (report) => (report.applied ? invalidate(keys.projects, keys.portfolio, keys.teams, keys.organization) : undefined),
  });
}

// ---------- Projets ----------
export const useProjects = () =>
  useQuery({ queryKey: keys.projects, queryFn: () => api.get<Project[]>('/api/projects') });

export const useProject = (id: number) =>
  useQuery({ queryKey: keys.project(id), queryFn: () => api.get<Project>(`/api/projects/${id}`) });

export function useSaveProject() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: ProjectInput }) =>
      id ? api.put<Project>(`/api/projects/${id}`, input) : api.post<Project>('/api/projects', input),
    onSuccess: () => invalidate(keys.projects, keys.portfolio),
  });
}

export const useProjectHistory = (projectId: number) =>
  useQuery({ queryKey: keys.history(projectId), queryFn: () => api.get<StatusChange[]>(`/api/projects/${projectId}/history`) });

export function useProjectStatusMutations(projectId: number) {
  const invalidate = useInvalidate();
  const onSuccess = () => invalidate(keys.projects, keys.portfolio, keys.history(projectId));
  return {
    status: useMutation({
      mutationFn: (input: { status: ProjectStatus; reason?: string }) => api.patch<Project>(`/api/projects/${projectId}/status`, input),
      onSuccess,
    }),
    priority: useMutation({
      mutationFn: (priority: ProjectPriority) => api.patch<Project>(`/api/projects/${projectId}/priority`, { priority }),
      onSuccess,
    }),
  };
}

export const useProjectMembers = (projectId: number) =>
  useQuery({ queryKey: keys.members(projectId), queryFn: () => api.get<ProjectMembers>(`/api/projects/${projectId}/members`) });

export function useProjectMemberMutations(projectId: number) {
  const qc = useQueryClient();
  const onSuccess = (m: ProjectMembers) => qc.setQueryData(keys.members(projectId), m);
  return {
    put: useMutation({
      mutationFn: ({ userId, role }: { userId: number; role: ProjectRole }) =>
        api.put<ProjectMembers>(`/api/projects/${projectId}/members/${userId}`, { role }),
      onSuccess,
    }),
    remove: useMutation({
      mutationFn: (userId: number) => api.delete<ProjectMembers>(`/api/projects/${projectId}/members/${userId}`),
      onSuccess,
    }),
  };
}

export function useUpdateModules(projectId: number) {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (modules: ModuleType[]) => api.put<Project>(`/api/projects/${projectId}/modules`, { modules }),
    onSuccess: () => invalidate(keys.projects),
  });
}

export function useDeleteProject() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (id: number) => api.delete(`/api/projects/${id}`),
    onSuccess: () => invalidate(keys.projects, keys.portfolio),
  });
}

// ---------- Tâches ----------
export const useTasks = (projectId: number, enabled = true) =>
  useQuery({ queryKey: keys.tasks(projectId), queryFn: () => api.get<Task[]>(`/api/projects/${projectId}/tasks`), enabled });

export const useMyTasks = () =>
  useQuery({ queryKey: keys.myTasks, queryFn: () => api.get<Task[]>('/api/tasks/mine') });

export function useSaveTask(projectId: number) {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: ({ id, input }: { id?: number; input: TaskInput }) =>
      id ? api.put<Task>(`/api/tasks/${id}`, input) : api.post<Task>(`/api/projects/${projectId}/tasks`, input),
    onSuccess: () => invalidate(keys.tasks(projectId), keys.myTasks, keys.portfolio),
  });
}

export function useMoveTask(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status, position }: { id: number; status: TaskStatus; position: number }) =>
      api.patch<Task>(`/api/tasks/${id}/move`, { status, position }),
    onSettled: () => {
      void qc.invalidateQueries({ queryKey: keys.tasks(projectId) });
      void qc.invalidateQueries({ queryKey: keys.portfolio });
      void qc.invalidateQueries({ queryKey: keys.myTasks });
    },
  });
}

export function useDeleteTask(projectId: number) {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (id: number) => api.delete(`/api/tasks/${id}`),
    onSuccess: () => invalidate(keys.tasks(projectId), keys.myTasks, keys.portfolio),
  });
}

// ---------- Todos ----------
export const useTodos = (projectId?: number) =>
  useQuery({
    queryKey: keys.todos(projectId),
    queryFn: () => api.get<Todo[]>(projectId ? `/api/projects/${projectId}/todos` : '/api/todos'),
  });

export function useTodoMutations(projectId?: number) {
  const invalidate = useInvalidate();
  const onSuccess = () => invalidate(keys.todos(projectId));
  return {
    create: useMutation({
      mutationFn: (input: TodoInput) =>
        api.post<Todo>(projectId ? `/api/projects/${projectId}/todos` : '/api/todos', input),
      onSuccess,
    }),
    update: useMutation({
      mutationFn: ({ id, input }: { id: number; input: TodoInput }) => api.put<Todo>(`/api/todos/${id}`, input),
      onSuccess,
    }),
    remove: useMutation({ mutationFn: (id: number) => api.delete(`/api/todos/${id}`), onSuccess }),
  };
}

// ---------- Rétro-planning ----------
export const useRetroPlan = (projectId: number, enabled = true) =>
  useQuery({ queryKey: keys.retro(projectId), queryFn: () => api.get<RetroPlan>(`/api/projects/${projectId}/retro-plan`), enabled });

export function useRetroMutations(projectId: number) {
  const qc = useQueryClient();
  const onSuccess = (plan: RetroPlan) => {
    qc.setQueryData(keys.retro(projectId), plan);
    void qc.invalidateQueries({ queryKey: keys.portfolio });
  };
  return {
    add: useMutation({
      mutationFn: (input: PhaseInput) => api.post<RetroPlan>(`/api/projects/${projectId}/retro-plan/phases`, input),
      onSuccess,
    }),
    update: useMutation({
      mutationFn: ({ id, input }: { id: number; input: PhaseInput }) =>
        api.put<RetroPlan>(`/api/retro-phases/${id}`, input),
      onSuccess,
    }),
    remove: useMutation({
      mutationFn: (id: number) => api.delete<RetroPlan>(`/api/retro-phases/${id}`),
      onSuccess,
    }),
    reorder: useMutation({
      mutationFn: (phaseIds: number[]) =>
        api.put<RetroPlan>(`/api/projects/${projectId}/retro-plan/order`, { phaseIds }),
      onSuccess,
    }),
    template: useMutation({
      mutationFn: (type: ProjectType) =>
        api.post<RetroPlan>(`/api/projects/${projectId}/retro-plan/template?type=${type}`),
      onSuccess,
    }),
  };
}

// ---------- Planning poker ----------
export const usePokerDeck = () =>
  useQuery({ queryKey: ['poker', 'deck'], queryFn: () => api.get<string[]>('/api/poker/deck'), staleTime: Infinity });

/** Rafraîchi toutes les 2 s pour voir les votes des autres participants en quasi temps réel. */
export const usePokerRounds = (projectId: number) =>
  useQuery({
    queryKey: keys.poker(projectId),
    queryFn: () => api.get<PokerRound[]>(`/api/projects/${projectId}/poker/rounds`),
    refetchInterval: 2000,
  });

export function usePokerMutations(projectId: number) {
  const invalidate = useInvalidate();
  const onSuccess = () => invalidate(keys.poker(projectId), keys.tasks(projectId));
  return {
    start: useMutation({
      mutationFn: (input: { taskId?: number; storyTitle?: string }) =>
        api.post<PokerRound>(`/api/projects/${projectId}/poker/rounds`, input),
      onSuccess,
    }),
    vote: useMutation({
      mutationFn: ({ roundId, card }: { roundId: number; card: string }) =>
        api.post<PokerRound>(`/api/poker/rounds/${roundId}/vote`, { card }),
      onSuccess,
    }),
    reveal: useMutation({
      mutationFn: (roundId: number) => api.post<PokerRound>(`/api/poker/rounds/${roundId}/reveal`),
      onSuccess,
    }),
    restart: useMutation({
      mutationFn: (roundId: number) => api.post<PokerRound>(`/api/poker/rounds/${roundId}/restart`),
      onSuccess,
    }),
    close: useMutation({
      mutationFn: ({ roundId, card }: { roundId: number; card: string }) =>
        api.post<PokerRound>(`/api/poker/rounds/${roundId}/close`, { card }),
      onSuccess,
    }),
  };
}

// ---------- Portefeuille ----------
export const usePortfolio = (teamId?: number) =>
  useQuery({
    queryKey: [...keys.portfolio, teamId ?? 'all'],
    queryFn: () => api.get<Portfolio>(teamId ? `/api/portfolio?teamId=${teamId}` : '/api/portfolio'),
  });
