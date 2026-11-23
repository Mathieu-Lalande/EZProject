import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ParticipantCard, PokerCard } from './PokerCard';

describe('PokerCard', () => {
  it('signale la sélection et notifie au clic', async () => {
    const onSelect = vi.fn();
    render(<PokerCard value="8" selected={false} onSelect={onSelect} />);

    const card = screen.getByRole('button', { name: '8' });
    expect(card).toHaveAttribute('aria-pressed', 'false');
    await userEvent.click(card);

    expect(onSelect).toHaveBeenCalledWith('8');
  });

  it('masque la carte d’un participant tant que le tour n’est pas révélé', () => {
    const { rerender } = render(<ParticipantCard name="Hugo" card={null} revealed={false} />);
    expect(screen.queryByText('5')).not.toBeInTheDocument();

    rerender(<ParticipantCard name="Hugo" card="5" revealed />);
    expect(screen.getByText('5')).toBeInTheDocument();
  });
});
