import { NavLink } from 'react-router-dom';
import clsx from 'clsx';
import { LayoutDashboard, UserRound, FolderKanban, Building2, Search, Moon, Sun, LogOut, X, PauseCircle } from 'lucide-react';
import { useLogout, useMe, useProjects } from '../../api/hooks';
import { useTheme } from '../../lib/theme';
import { planLabel, roleLabel } from '../../lib/labels';
import { Avatar } from '../ui/Avatar';

const healthDot = { GREEN: 'bg-emerald-500', AMBER: 'bg-amber-500', RED: 'bg-red-500' } as const;

interface SidebarProps {
  onSearch: () => void;
  mobileOpen: boolean;
  onNavigate: () => void;
}

export function Sidebar({ onSearch, mobileOpen, onNavigate }: SidebarProps) {
  const { data: me } = useMe();
  const { data: projects = [] } = useProjects();
  const logout = useLogout();
  const [theme, toggleTheme] = useTheme();

  const nav = [
    { to: '/portfolio', label: 'Portefeuille', icon: LayoutDashboard },
    { to: '/me', label: 'Mon espace', icon: UserRound },
    { to: '/projects', label: 'Projets', icon: FolderKanban },
    ...(me?.role !== 'GUEST' ? [{ to: '/organization', label: 'Organisation', icon: Building2 }] : []),
  ];
  const priorityRank = { P1: 0, P2: 1, P3: 2, P4: 3 } as const;
  const openProjects = projects
    .filter((p) => ['PLANNED', 'ACTIVE', 'ON_HOLD'].includes(p.status))
    .sort((a, b) => priorityRank[a.priority] - priorityRank[b.priority] || a.name.localeCompare(b.name));

  return (
    <>
      {mobileOpen && <div className="fixed inset-0 z-30 bg-black/40 lg:hidden" onClick={onNavigate} aria-hidden />}
      <aside
        className={clsx(
          'fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-line bg-surface transition-transform lg:sticky lg:top-0 lg:h-screen lg:translate-x-0',
          mobileOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex h-16 items-center justify-between px-5">
          <div className="flex items-center gap-2.5 font-semibold">
            <span className="flex size-8 items-center justify-center rounded-lg bg-gradient-to-br from-brand-500 to-violet-600 text-xs font-bold text-white shadow-sm shadow-brand-600/30">
              EZ
            </span>
            <span className="min-w-0">
              <span className="block leading-tight">EZProject</span>
              {me && (
                <span className="block truncate text-[11px] font-normal text-ink-3">
                  {me.organization.name} · {planLabel[me.organization.plan]}
                </span>
              )}
            </span>
          </div>
          <button className="rounded-md p-1 hover:bg-surface-2 lg:hidden" onClick={onNavigate} aria-label="Fermer le menu">
            <X className="size-4" />
          </button>
        </div>

        <div className="px-3">
          <button
            onClick={onSearch}
            className="flex w-full items-center gap-2 rounded-lg border border-line bg-canvas px-3 py-2 text-sm text-ink-3 transition hover:border-brand-400"
          >
            <Search className="size-4" />
            Rechercher…
            <kbd className="ml-auto rounded border border-line bg-surface px-1.5 font-sans text-[10px]">Ctrl K</kbd>
          </button>
        </div>

        <nav className="mt-4 space-y-0.5 px-3">
          {nav.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={onNavigate}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition',
                  isActive ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200' : 'text-ink-2 hover:bg-surface-2 hover:text-ink',
                )
              }
            >
              <Icon className="size-4" />
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-6 px-6 text-[11px] font-semibold tracking-wider text-ink-3 uppercase">Projets actifs</div>
        <div className="scrollbar-thin mt-2 flex-1 space-y-0.5 overflow-y-auto px-3 pb-4">
          {openProjects.map((p) => (
            <NavLink
              key={p.id}
              to={`/projects/${p.id}`}
              onClick={onNavigate}
              className={({ isActive }) =>
                clsx(
                  'group flex items-center gap-2.5 rounded-lg px-3 py-1.5 text-sm transition',
                  isActive ? 'bg-surface-2 text-ink' : 'text-ink-2 hover:bg-surface-2 hover:text-ink',
                )
              }
            >
              <span className="size-2.5 shrink-0 rounded-[4px]" style={{ backgroundColor: p.color }} />
              <span className="truncate">{p.name}</span>
              {p.status === 'ON_HOLD' ? (
                <PauseCircle className="ml-auto size-3.5 shrink-0 text-amber-500" aria-label="En pause" />
              ) : (
                <span className={clsx('ml-auto size-1.5 shrink-0 rounded-full', healthDot[p.health])} title="Météo" />
              )}
            </NavLink>
          ))}
        </div>

        {me && (
          <div className="flex items-center gap-2.5 border-t border-line p-3">
            <Avatar name={me.displayName} />
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-medium">{me.displayName}</div>
              <div className="truncate text-xs text-ink-3">{me.jobTitle ?? roleLabel[me.role]}</div>
            </div>
            <button onClick={toggleTheme} className="rounded-md p-1.5 text-ink-3 hover:bg-surface-2 hover:text-ink" aria-label="Changer de thème">
              {theme === 'dark' ? <Sun className="size-4" /> : <Moon className="size-4" />}
            </button>
            <button onClick={() => logout.mutate()} className="rounded-md p-1.5 text-ink-3 hover:bg-surface-2 hover:text-ink" aria-label="Se déconnecter">
              <LogOut className="size-4" />
            </button>
          </div>
        )}
      </aside>
    </>
  );
}
