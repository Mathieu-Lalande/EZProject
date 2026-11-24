import type { Me } from '../api/types';
import { can, canCreateProject, isAdmin, isManager } from './permissions';

const me = (role: Me['role'], teams: Me['teams'] = []): Me => ({
  id: 1, email: 'a@b.c', displayName: 'A', role, jobTitle: null,
  organization: { id: 1, name: 'Org', slug: 'org', plan: 'TEAM' }, teams, mustChangePassword: false, mfaEnabled: false,
});

describe('permissions', () => {
  it('vérifie les permissions projet', () => {
    expect(can({ myPermissions: ['TASK_VIEW'] }, 'TASK_VIEW')).toBe(true);
    expect(can({ myPermissions: ['TASK_VIEW'] }, 'TASK_DELETE')).toBe(false);
    expect(can(undefined, 'TASK_VIEW')).toBe(false);
  });

  it('distingue administrateurs, managers et membres', () => {
    expect(isAdmin(me('OWNER'))).toBe(true);
    expect(isAdmin(me('MANAGER'))).toBe(false);
    expect(isManager(me('MANAGER'))).toBe(true);
    expect(isManager(me('GUEST'))).toBe(false);
  });

  it('autorise la création de projet aux responsables d’équipe', () => {
    expect(canCreateProject(me('MEMBER'))).toBe(false);
    expect(canCreateProject(me('MEMBER', [{ teamId: 1, teamName: 'T', role: 'LEAD' }]))).toBe(true);
  });
});
