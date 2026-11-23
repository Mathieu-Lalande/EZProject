import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReasonDialog } from './ReasonDialog';

describe('ReasonDialog', () => {
  it('exige un motif avant de confirmer une mise en pause', async () => {
    const onConfirm = vi.fn();
    render(<ReasonDialog open status="ON_HOLD" onConfirm={onConfirm} onClose={() => {}} />);

    const confirm = screen.getByRole('button', { name: 'Confirmer' });
    expect(confirm).toBeDisabled();

    await userEvent.type(screen.getByRole('textbox'), '  Budget gelé  ');
    await userEvent.click(confirm);

    expect(onConfirm).toHaveBeenCalledWith('Budget gelé');
  });
});
