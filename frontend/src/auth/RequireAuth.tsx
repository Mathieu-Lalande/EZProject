import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useMe } from '../api/hooks';
import { Spinner } from '../components/ui/Spinner';

export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: me, isLoading } = useMe();
  const location = useLocation();
  if (isLoading) return <Spinner />;
  if (!me) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}

export function useIsManager(): boolean {
  const { data: me } = useMe();
  return me?.role === 'ADMIN' || me?.role === 'MANAGER';
}
