import type { ProjectPriority } from '../../api/types';
import { projectPriorityLabel } from '../../lib/labels';
import { Badge, type Tone } from './Badge';

const tone: Record<ProjectPriority, Tone> = { P1: 'red', P2: 'amber', P3: 'blue', P4: 'neutral' };

export function PriorityBadge({ priority, short = false }: { priority: ProjectPriority; short?: boolean }) {
  return (
    <Badge tone={tone[priority]} className="font-semibold">
      {short ? priority : projectPriorityLabel[priority]}
    </Badge>
  );
}
