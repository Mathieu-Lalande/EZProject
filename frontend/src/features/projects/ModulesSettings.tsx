import { CalendarClock, KanbanSquare, ListChecks, ShieldAlert, Spade, Timer } from 'lucide-react';
import clsx from 'clsx';
import type { ModuleType, Project } from '../../api/types';
import { useUpdateModules } from '../../api/hooks';
import { moduleInfo } from '../../lib/labels';
import { useToast } from '../../components/ui/Toast';

const icons: Record<ModuleType, React.ReactNode> = {
  BOARD: <KanbanSquare className="size-5" />,
  TODOS: <ListChecks className="size-5" />,
  RETRO_PLANNING: <CalendarClock className="size-5" />,
  POKER: <Spade className="size-5" />,
  TIME_TRACKING: <Timer className="size-5" />,
  RISKS: <ShieldAlert className="size-5" />,
};

/** Activation/désactivation des modules : rend chaque projet aussi léger ou complet que nécessaire. */
export function ModulesSettings({ project }: { project: Project }) {
  const editable = project.myPermissions.includes('PROJECT_MODULES');
  const update = useUpdateModules(project.id);
  const toast = useToast();

  const toggle = (m: ModuleType) => {
    const next = project.modules.includes(m) ? project.modules.filter((x) => x !== m) : [...project.modules, m];
    update.mutate(next, { onSuccess: () => toast('success', 'Modules mis à jour') });
  };

  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {(Object.keys(moduleInfo) as ModuleType[]).map((m) => {
        const enabled = project.modules.includes(m);
        return (
          <button
            key={m}
            disabled={!editable || update.isPending}
            onClick={() => toggle(m)}
            role="switch"
            aria-checked={enabled}
            className={clsx('card flex items-start gap-4 p-4 text-left transition', enabled ? 'border-brand-400 ring-1 ring-brand-400/40' : 'opacity-70 hover:opacity-100', !editable && 'cursor-default')}
          >
            <span className={clsx('flex size-10 shrink-0 items-center justify-center rounded-xl', enabled ? 'bg-brand-600 text-white' : 'bg-surface-2 text-ink-3')}>{icons[m]}</span>
            <span className="flex-1">
              <span className="block text-sm font-semibold">{moduleInfo[m].label}</span>
              <span className="mt-0.5 block text-xs text-ink-3">{moduleInfo[m].description}</span>
            </span>
            <span className={clsx('relative mt-1 h-5 w-9 shrink-0 rounded-full transition', enabled ? 'bg-brand-600' : 'bg-line')}>
              <span className={clsx('absolute top-0.5 size-4 rounded-full bg-white shadow transition-all', enabled ? 'left-4.5' : 'left-0.5')} />
            </span>
          </button>
        );
      })}
    </div>
  );
}
