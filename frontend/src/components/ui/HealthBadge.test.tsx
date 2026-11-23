import { render, screen } from '@testing-library/react';
import { HealthBadge } from './HealthBadge';

describe('HealthBadge', () => {
  it.each([
    ['GREEN', 'Sous contrôle'],
    ['AMBER', 'Vigilance'],
    ['RED', 'Critique'],
  ] as const)('affiche le libellé de la météo %s', (health, label) => {
    render(<HealthBadge health={health} />);
    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
