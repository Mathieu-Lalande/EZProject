import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useMe } from '../api/hooks';
import { Spinner } from '../components/ui/Spinner';
import { isAdmin, isManager } from '../lib/permissions';
import { ForcePasswordChange } from './ForcePasswordChange';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: me, isLoading } = useMe();
  const location = useLocation();
  if (isLoading) return <Spinner />;
  if (!me) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (me.mustChangePassword) return <ForcePasswordChange />;
  return <>{children}</>;
}

export function useIsManager(): boolean {
  return isManager(useMe().data);
}

export function useIsAdmin(): boolean {
  return isAdmin(useMe().data);
}
