import { useEffect, useState } from 'react';
import type { ProjectStatus } from '../../api/types';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { projectStatusLabel } from '../../lib/labels';

interface ReasonDialogProps {
  open: boolean;
  status: ProjectStatus | null;
  loading?: boolean;
  onConfirm: (reason: string) => void;
  onClose: () => void;
}

/** Motif obligatoire pour une mise en pause ou une annulation : il est tracé dans le journal du projet. */
export function ReasonDialog({ open, status, loading, onConfirm, onClose }: ReasonDialogProps) {
  const [reason, setReason] = useState('');
  useEffect(() => {
    if (open) setReason('');
  }, [open]);
  if (!status) return null;
  const title = status === 'ON_HOLD' ? 'Mettre le projet en pause' : `Passer en « ${projectStatusLabel[status]} »`;
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Annuler</Button>
          <Button disabled={!reason.trim()} loading={loading} onClick={() => onConfirm(reason.trim())}>Confirmer</Button>
        </>
      }
    >
      <label className="block">
        <span className="label">Motif (obligatoire, visible dans le journal et le portefeuille)</span>
        <textarea
          className="input min-h-24"
          maxLength={500}
          placeholder="ex. Budget client gelé, arbitrage de staffing, attente de décision COPIL…"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </label>
    </Modal>
  );
}
