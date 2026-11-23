import { useState, type FormEvent } from 'react';
import { CalendarOff, Plus, Trash2 } from 'lucide-react';
import { useAbsenceMutations, useMyAbsences } from '../../api/hooks';
import type { AbsenceKind } from '../../api/types';
import { ApiError } from '../../api/client';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/Toast';
import { formatDate, toIsoDate } from '../../lib/format';
import { absenceKindLabel } from '../../lib/labels';

/** Déclaration de ses absences : elles réduisent la capacité disponible. Le motif reste privé. */
export function MyAbsences() {
  const { data: absences = [] } = useMyAbsences();
  const m = useAbsenceMutations();
  const toast = useToast();
  const today = toIsoDate(new Date());
  const [form, setForm] = useState({ startDate: today, endDate: today, kind: 'VACATION' as AbsenceKind });
  const upcoming = absences.filter((a) => a.endDate >= today);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    m.create.mutate(form, {
      onSuccess: () => toast('success', 'Absence enregistrée'),
      onError: (err) => toast('error', err instanceof ApiError ? err.message : 'Action impossible'),
    });
  };

  return (
    <section className="card p-5">
      <h3 className="mb-1 flex items-center gap-2 text-sm font-semibold"><CalendarOff className="size-4 text-ink-3" />Mes absences</h3>
      <p className="mb-3 text-xs text-ink-3">Le motif n'est visible que par vous et les administrateurs.</p>
      <form onSubmit={submit} className="mb-3 flex flex-wrap items-end gap-2">
        <input className="input w-36" type="date" aria-label="Début" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
        <input className="input w-36" type="date" aria-label="Fin" required min={form.startDate} value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
        <select className="input w-36" aria-label="Motif" value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value as AbsenceKind })}>
          {(Object.keys(absenceKindLabel) as AbsenceKind[]).map((k) => <option key={k} value={k}>{absenceKindLabel[k]}</option>)}
        </select>
        <Button type="submit" size="sm" icon={<Plus className="size-4" />} loading={m.create.isPending}>Ajouter</Button>
      </form>
      {upcoming.length === 0 ? <p className="text-sm text-ink-3">Aucune absence à venir.</p> : (
        <ul className="divide-y divide-line">
          {upcoming.map((a) => (
            <li key={a.id} className="group flex items-center gap-2 py-2 text-sm">
              <span className="flex-1">{formatDate(a.startDate)} → {formatDate(a.endDate, true)}</span>
              <span className="text-xs text-ink-3">{a.kind ? absenceKindLabel[a.kind] : ''}</span>
              <button aria-label="Supprimer l'absence" className="rounded p-1 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                onClick={() => m.remove.mutate(a.id)}><Trash2 className="size-4" /></button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
