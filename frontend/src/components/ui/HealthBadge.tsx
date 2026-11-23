import type { Health } from '../../api/types';
import { healthLabel } from '../../lib/labels';
import { Badge, type Tone } from './Badge';

const tone: Record<Health, Tone> = { GREEN: 'green', AMBER: 'amber', RED: 'red' };
const dot: Record<Health, string> = { GREEN: 'bg-emerald-500', AMBER: 'bg-amber-500', RED: 'bg-red-500' };

export function HealthBadge({ health }: { health: Health }) {
  return (
    <Badge tone={tone[health]}>
      <span className={`size-1.5 rounded-full ${dot[health]}`} aria-hidden />
      {healthLabel[health]}
    </Badge>
  );
}
