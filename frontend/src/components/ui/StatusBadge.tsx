import { PauseCircle } from 'lucide-react';
import type { ProjectStatus } from '../../api/types';
import { projectStatusLabel } from '../../lib/labels';
import { Badge, type Tone } from './Badge';

const tone: Record<ProjectStatus, Tone> = {
  PLANNED: 'neutral',
  ACTIVE: 'brand',
  ON_HOLD: 'amber',
  COMPLETED: 'green',
  CANCELLED: 'neutral',
};

export function StatusBadge({ status, reason }: { status: ProjectStatus; reason?: string | null }) {
  return (
    <span title={reason ?? undefined}>
      <Badge tone={tone[status]}>
        {status === 'ON_HOLD' && <PauseCircle className="size-3" />}
        {projectStatusLabel[status]}
      </Badge>
    </span>
  );
}
