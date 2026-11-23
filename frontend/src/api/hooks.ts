import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query';
import { api } from './client';
import type {
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
};

/** Invalide plusieurs clés après une mutation (les vues dérivées comme le portefeuille se rafraîchissent). */
function useInvalidate() {
  const qc = useQueryClient();
  return (...queryKeys: QueryKey[]) => Promise.all(queryKeys.map((k) => qc.invalidateQueries({ queryKey: k })));
}

// ---------- Auth & utilisateurs ----------
export const useMe = () =>
  useQuery({ queryKey: keys.me, queryFn: () => api.get<User>('/api/auth/me'), retry: false, staleTime: Infinity });

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

export function useCreateUser() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: (input: { email: string; displayName: string; password: string; role: string; jobTitle?: string }) =>
      api.post<User>('/api/users', input),
    onSuccess: () => invalidate(keys.users),
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
export const useTasks = (projectId: number) =>
  useQuery({ queryKey: keys.tasks(projectId), queryFn: () => api.get<Task[]>(`/api/projects/${projectId}/tasks`) });

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
export const useRetroPlan = (projectId: number) =>
  useQuery({ queryKey: keys.retro(projectId), queryFn: () => api.get<RetroPlan>(`/api/projects/${projectId}/retro-plan`) });

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
export const usePortfolio = (enabled = true) =>
  useQuery({ queryKey: keys.portfolio, queryFn: () => api.get<Portfolio>('/api/portfolio'), enabled });
