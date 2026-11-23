import type { Me, Permission, Project } from '../api/types';

/** Vérifie une permission projet côté interface (le backend reste l'autorité et revérifie tout). */
export function can(project: Pick<Project, 'myPermissions'> | undefined, permission: Permission): boolean {
  return !!project?.myPermissions.includes(permission);
}

export function isAdmin(me: Me | undefined): boolean {
  return me?.role === 'OWNER' || me?.role === 'ADMIN';
}

/** Vision portefeuille complète (COO, PMO, administrateurs). */
export function isManager(me: Me | undefined): boolean {
  return isAdmin(me) || me?.role === 'MANAGER';
}

/** Peut créer un projet : manager, ou responsable d'au moins une équipe. */
export function canCreateProject(me: Me | undefined): boolean {
  return isManager(me) || !!me?.teams.some((t) => t.role === 'LEAD');
}
