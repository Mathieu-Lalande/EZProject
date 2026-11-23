import { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { CornerDownLeft, FolderKanban, LayoutDashboard, Search, UserRound, Users } from 'lucide-react';
import clsx from 'clsx';
import { useProjects } from '../../api/hooks';
import { useIsManager } from '../../auth/RequireAuth';

interface Item {
  id: string;
  label: string;
  hint?: string;
  icon: React.ReactNode;
  to: string;
}

export function CommandPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);
  const navigate = useNavigate();
  const { data: projects = [] } = useProjects();
  const isManager = useIsManager();

  const items = useMemo<Item[]>(() => {
    const base: Item[] = [
      ...(isManager ? [{ id: 'portfolio', label: 'Portefeuille', hint: 'Navigation', icon: <LayoutDashboard className="size-4" />, to: '/portfolio' }] : []),
      { id: 'me', label: 'Mon espace', hint: 'Navigation', icon: <UserRound className="size-4" />, to: '/me' },
      { id: 'projects', label: 'Tous les projets', hint: 'Navigation', icon: <FolderKanban className="size-4" />, to: '/projects' },
      { id: 'team', label: 'Équipe', hint: 'Navigation', icon: <Users className="size-4" />, to: '/team' },
    ];
    const projectItems: Item[] = projects.flatMap((p) => [
      { id: `p${p.id}`, label: p.name, hint: p.code, icon: <span className="size-2.5 rounded-[4px]" style={{ backgroundColor: p.color }} />, to: `/projects/${p.id}` },
      ...(p.modules.includes('BOARD') ? [{ id: `p${p.id}b`, label: `${p.name} › Kanban`, hint: p.code, icon: <span className="size-2.5 rounded-[4px]" style={{ backgroundColor: p.color }} />, to: `/projects/${p.id}/board` }] : []),
    ]);
    const q = query.trim().toLowerCase();
    return [...base, ...projectItems].filter((i) => !q || `${i.label} ${i.hint ?? ''}`.toLowerCase().includes(q)).slice(0, 12);
  }, [projects, query, isManager]);

  useEffect(() => {
    if (open) {
      setQuery('');
      setActive(0);
    }
  }, [open]);

  if (!open) return null;

  const go = (item: Item | undefined) => {
    if (!item) return;
    navigate(item.to);
    onClose();
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-[15vh]">
      <div className="fixed inset-0 animate-fade-in bg-black/40 backdrop-blur-[2px]" onClick={onClose} aria-hidden />
      <div className="relative w-full max-w-xl animate-pop-in overflow-hidden rounded-2xl border border-line bg-surface shadow-pop" role="dialog" aria-label="Recherche rapide">
        <div className="flex items-center gap-3 border-b border-line px-4">
          <Search className="size-4 text-ink-3" />
          <input
            autoFocus
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setActive(0);
            }}
            onKeyDown={(e) => {
              if (e.key === 'ArrowDown') { e.preventDefault(); setActive((a) => Math.min(a + 1, items.length - 1)); }
              if (e.key === 'ArrowUp') { e.preventDefault(); setActive((a) => Math.max(a - 1, 0)); }
              if (e.key === 'Enter') go(items[active]);
              if (e.key === 'Escape') onClose();
            }}
            placeholder="Aller à un projet, une vue…"
            className="h-12 flex-1 bg-transparent text-sm outline-none placeholder:text-ink-3"
          />
        </div>
        <ul className="max-h-80 overflow-y-auto p-2">
          {items.length === 0 && <li className="px-3 py-6 text-center text-sm text-ink-3">Aucun résultat</li>}
          {items.map((item, i) => (
            <li key={item.id}>
              <button
                onMouseEnter={() => setActive(i)}
                onClick={() => go(item)}
                className={clsx('flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left text-sm', i === active ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200' : 'text-ink-2')}
              >
                <span className="flex size-5 items-center justify-center">{item.icon}</span>
                <span className="flex-1 truncate">{item.label}</span>
                {item.hint && <span className="text-xs text-ink-3">{item.hint}</span>}
                {i === active && <CornerDownLeft className="size-3.5 opacity-60" />}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>,
    document.body,
  );
}
