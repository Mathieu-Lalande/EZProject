import { useMemo, useState, type FormEvent } from 'react';
import { Eye, RotateCcw, Spade, CheckCircle2, Sparkles } from 'lucide-react';
import clsx from 'clsx';
import { usePokerDeck, usePokerMutations, usePokerRounds, useTasks } from '../../api/hooks';
import type { PokerRound, Project } from '../../api/types';
import { can } from '../../lib/permissions';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Spinner } from '../../components/ui/Spinner';
import { EmptyState } from '../../components/ui/EmptyState';
import { PokerCard, ParticipantCard } from './PokerCard';

export function PokerRoom({ project }: { project: Project }) {
  const projectId = project.id;
  const canVote = can(project, 'POKER_VOTE');
  const canFacilitate = can(project, 'POKER_FACILITATE');
  const { data: rounds = [], isLoading, live } = usePokerRounds(projectId);
  const { data: deck = [] } = usePokerDeck();
  const { data: tasks = [] } = useTasks(projectId);
  const m = usePokerMutations(projectId);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [storyTitle, setStoryTitle] = useState('');
  const [taskId, setTaskId] = useState<string>('');

  const current: PokerRound | undefined = useMemo(
    () => rounds.find((r) => r.id === selectedId) ?? rounds.find((r) => r.status !== 'CLOSED') ?? rounds[0],
    [rounds, selectedId],
  );
  const estimable = tasks.filter((t) => t.status !== 'DONE' && t.storyPoints === null);

  if (isLoading) return <Spinner />;

  const start = (e: FormEvent) => {
    e.preventDefault();
    const input = taskId ? { taskId: Number(taskId) } : { storyTitle: storyTitle.trim() };
    if (!taskId && !storyTitle.trim()) return;
    m.start.mutate(input, { onSuccess: (r) => { setSelectedId(r.id); setStoryTitle(''); setTaskId(''); } });
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
      <div className="min-w-0">
        {!current ? (
          <EmptyState icon={<Spade className="size-5" />} title="Aucune session d'estimation" description="Lancez un tour sur une tâche du backlog ou une story libre." />
        ) : (
          <div className="card p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <div className="flex items-center gap-1.5 text-xs text-ink-3">
                  Story en cours d'estimation
                  <span className={`size-1.5 rounded-full ${live ? 'bg-emerald-500' : 'bg-zinc-400'}`} title={live ? 'Temps réel actif' : 'Actualisation périodique'} />
                </div>
                <h3 className="text-lg font-semibold">{current.storyTitle}</h3>
              </div>
              <Badge tone={current.status === 'VOTING' ? 'brand' : current.status === 'REVEALED' ? 'amber' : 'green'}>
                {current.status === 'VOTING' ? 'Vote en cours' : current.status === 'REVEALED' ? 'Cartes révélées' : `Estimé : ${current.finalEstimate}`}
              </Badge>
            </div>

            <div className="my-8 flex min-h-32 flex-wrap items-center justify-center gap-5 rounded-2xl bg-gradient-to-b from-emerald-50 to-emerald-100/50 p-6 dark:from-emerald-500/5 dark:to-emerald-500/10">
              {current.votes.length === 0 ? (
                <p className="text-sm text-ink-3">En attente des premiers votes… (partagez cette page à l'équipe)</p>
              ) : (
                current.votes.map((v) => <ParticipantCard key={v.userId} name={v.displayName} card={v.card} revealed={current.status !== 'VOTING'} />)
              )}
            </div>

            {current.stats && (
              <div className="mb-6 flex flex-wrap items-center justify-center gap-6 text-sm">
                <div className="text-center"><div className="text-xs text-ink-3">Moyenne</div><div className="text-xl font-semibold tabular-nums">{current.stats.average ?? '—'}</div></div>
                <div className="text-center"><div className="text-xs text-ink-3">Suggestion</div><div className="text-xl font-semibold text-brand-600">{current.stats.suggested ?? '—'}</div></div>
                {current.stats.consensus && <Badge tone="green"><Sparkles className="size-3" /> Consensus !</Badge>}
              </div>
            )}

            {current.status === 'VOTING' && canVote && (
              <div className="flex flex-wrap justify-center gap-2.5">
                {deck.map((c) => (
                  <PokerCard key={c} value={c} selected={current.myCard === c} onSelect={(card) => m.vote.mutate({ roundId: current.id, card })} />
                ))}
              </div>
            )}

            {canFacilitate && <div className="mt-6 flex flex-wrap justify-center gap-2">
              {current.status === 'VOTING' && (
                <Button icon={<Eye className="size-4" />} onClick={() => m.reveal.mutate(current.id)} disabled={current.votes.length === 0} loading={m.reveal.isPending}>
                  Révéler les cartes
                </Button>
              )}
              {current.status === 'REVEALED' && (
                <>
                  <Button variant="secondary" icon={<RotateCcw className="size-4" />} onClick={() => m.restart.mutate(current.id)}>Revoter</Button>
                  {deck.filter((c) => /^\d+$/.test(c)).map((c) => (
                    <Button key={c} size="sm" variant={current.stats?.suggested === c ? 'primary' : 'secondary'} onClick={() => m.close.mutate({ roundId: current.id, card: c })}>
                      Valider {c}
                    </Button>
                  ))}
                </>
              )}
            </div>}
            {current.status === 'REVEALED' && current.taskId && <p className="mt-3 text-center text-xs text-ink-3">L'estimation validée sera reportée sur la tâche.</p>}
          </div>
        )}
      </div>

      <div className="space-y-4">
        {canFacilitate && <form onSubmit={start} className="card space-y-3 p-4">
          <h3 className="text-sm font-semibold">Nouveau tour</h3>
          <select className="input" value={taskId} onChange={(e) => setTaskId(e.target.value)}>
            <option value="">Story libre…</option>
            {estimable.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}
          </select>
          {!taskId && <input className="input" placeholder="Titre de la story" maxLength={200} value={storyTitle} onChange={(e) => setStoryTitle(e.target.value)} />}
          <Button type="submit" className="w-full" icon={<Spade className="size-4" />} loading={m.start.isPending}>Lancer l'estimation</Button>
        </form>}

        {rounds.length > 0 && (
          <div className="card p-2">
            <div className="px-2 py-1.5 text-xs font-medium text-ink-3">Historique</div>
            <ul>
              {rounds.map((r) => (
                <li key={r.id}>
                  <button
                    onClick={() => setSelectedId(r.id)}
                    className={clsx('flex w-full items-center gap-2 rounded-lg px-2 py-2 text-left text-sm', current?.id === r.id ? 'bg-surface-2' : 'hover:bg-surface-2/60')}
                  >
                    {r.status === 'CLOSED' ? <CheckCircle2 className="size-4 shrink-0 text-emerald-500" /> : <span className="size-2 shrink-0 animate-pulse rounded-full bg-brand-500 mx-1" />}
                    <span className="flex-1 truncate">{r.storyTitle}</span>
                    {r.finalEstimate && <span className="rounded-md bg-surface-2 px-1.5 text-xs font-semibold">{r.finalEstimate}</span>}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
