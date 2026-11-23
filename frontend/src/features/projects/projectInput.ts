import type { Project, ProjectInput } from '../../api/types';

/** Reconstruit la requête complète de mise à jour à partir d'un projet (pour les modifications partielles). */
export function toProjectInput(p: Project, overrides: Partial<ProjectInput> = {}): ProjectInput {
  return {
    code: p.code,
    name: p.name,
    description: p.description,
    client: p.client,
    type: p.type,
    priority: p.priority,
    health: p.health,
    color: p.color,
    ownerId: p.owner?.id ?? null,
    teamId: p.team?.id ?? null,
    visibility: p.visibility,
    startDate: p.startDate,
    endDate: p.endDate,
    budgetDays: p.budgetDays,
    ...overrides,
  };
}
