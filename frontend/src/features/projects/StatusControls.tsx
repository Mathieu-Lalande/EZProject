import { useState } from 'react';
import { ChevronDown, Flag } from 'lucide-react';
import type { Project, ProjectPriority, ProjectStatus } from '../../api/types';
import { useProjectStatusMutations } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Dropdown, MenuItem } from '../../components/ui/Dropdown';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { PriorityBadge } from '../../components/ui/PriorityBadge';
import { useToast } from '../../components/ui/Toast';
import { projectPriorityLabel, projectStatusLabel } from '../../lib/labels';
import { can } from '../../lib/permissions';
import { ReasonDialog } from './ReasonDialog';

const STATUSES: ProjectStatus[] = ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];
const PRIORITIES: ProjectPriority[] = ['P1', 'P2', 'P3', 'P4'];

/** Changement rapide de statut et de priorité depuis l'en-tête du projet. */
export function StatusControls({ project }: { project: Project }) {
  const m = useProjectStatusMutations(project.id);
  const toast = useToast();
  const [pending, setPending] = useState<ProjectStatus | null>(null);
  const editable = can(project, 'PROJECT_EDIT');

  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');
  const applyStatus = (status: ProjectStatus, reason?: string) =>
    m.status.mutate(
      { status, reason },
      { onSuccess: () => { toast('success', `Statut : ${projectStatusLabel[status]}`); setPending(null); }, onError },
    );

  if (!editable) {
    return (
      <div className="flex items-center gap-1.5">
        <StatusBadge status={project.status} reason={project.statusReason} />
        <PriorityBadge priority={project.priority} />
      </div>
    );
  }

  return (
    <div className="flex items-center gap-1.5">
      <Dropdown
        trigger={({ toggle }) => (
          <button onClick={toggle} className="flex items-center gap-0.5 rounded-md hover:opacity-80" aria-label="Changer le statut">
            <StatusBadge status={project.status} reason={project.statusReason} />
            <ChevronDown className="size-3 text-ink-3" />
          </button>
        )}
      >
        {(close) =>
          STATUSES.map((s) => (
            <MenuItem
              key={s}
              active={s === project.status}
              onClick={() => {
                close();
                if (s === project.status) return;
                if (s === 'ON_HOLD' || s === 'CANCELLED') setPending(s);
                else applyStatus(s);
              }}
            >
              <StatusBadge status={s} />
            </MenuItem>
          ))
        }
      </Dropdown>
      <Dropdown
        trigger={({ toggle }) => (
          <button onClick={toggle} className="flex items-center gap-0.5 rounded-md hover:opacity-80" aria-label="Changer la priorité">
            <PriorityBadge priority={project.priority} />
            <ChevronDown className="size-3 text-ink-3" />
          </button>
        )}
      >
        {(close) =>
          PRIORITIES.map((p) => (
            <MenuItem key={p} active={p === project.priority} onClick={() => { close(); m.priority.mutate(p, { onError }); }}>
              <Flag className="size-3.5 text-ink-3" />
              {projectPriorityLabel[p]}
            </MenuItem>
          ))
        }
      </Dropdown>
      <ReasonDialog
        open={pending !== null}
        status={pending}
        loading={m.status.isPending}
        onClose={() => setPending(null)}
        onConfirm={(reason) => pending && applyStatus(pending, reason)}
      />
    </div>
  );
}
