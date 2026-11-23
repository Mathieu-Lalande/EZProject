import { useRef, useState, type FormEvent } from 'react';
import { CheckSquare, MessageSquare, Paperclip, Square, Trash2, Upload } from 'lucide-react';
import { useCollaboration, useTaskDetails } from '../../api/hooks';
import { ApiError } from '../../api/client';
import { Avatar } from '../../components/ui/Avatar';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/Toast';

const dateTime = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' });

function size(bytes: number) {
  return bytes < 1024 ? `${bytes} o` : bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} Ko` : `${(bytes / 1024 / 1024).toFixed(1)} Mo`;
}

/** Sous-tâches, pièces jointes et commentaires d'une tâche existante. */
export function TaskCollaboration({ taskId, projectId }: { taskId: number; projectId: number }) {
  const { data } = useTaskDetails(taskId);
  const m = useCollaboration(taskId, projectId);
  const toast = useToast();
  const [item, setItem] = useState('');
  const [comment, setComment] = useState('');
  const fileInput = useRef<HTMLInputElement>(null);
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  if (!data) return null;
  const done = data.checklist.filter((i) => i.done).length;

  const submitComment = (e: FormEvent) => {
    e.preventDefault();
    if (!comment.trim()) return;
    m.comment.mutate(comment.trim(), { onSuccess: () => setComment(''), onError });
  };

  return (
    <div className="mt-5 space-y-5 border-t border-line pt-5">
      <section>
        <h3 className="mb-2 flex items-center gap-2 text-xs font-semibold text-ink-2">
          <CheckSquare className="size-4 text-ink-3" />Sous-tâches {data.checklist.length > 0 && <span className="text-ink-3">{done}/{data.checklist.length}</span>}
        </h3>
        <ul className="space-y-1">
          {data.checklist.map((i) => (
            <li key={i.id} className="group flex items-center gap-2 text-sm">
              <button type="button" disabled={!data.canEdit} aria-label={i.done ? 'Marquer à faire' : 'Marquer comme fait'}
                onClick={() => m.updateItem.mutate({ id: i.id, title: i.title, done: !i.done }, { onError })} className="text-ink-3 hover:text-brand-600">
                {i.done ? <CheckSquare className="size-4 text-emerald-500" /> : <Square className="size-4" />}
              </button>
              <span className={i.done ? 'flex-1 text-ink-3 line-through' : 'flex-1'}>{i.title}</span>
              {data.canEdit && (
                <button type="button" aria-label="Supprimer la sous-tâche" className="rounded p-0.5 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                  onClick={() => m.deleteItem.mutate(i.id, { onError })}><Trash2 className="size-3.5" /></button>
              )}
            </li>
          ))}
        </ul>
        {data.canEdit && (
          <input className="input mt-2" placeholder="Ajouter une sous-tâche puis Entrée" maxLength={300} value={item}
            onChange={(e) => setItem(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                if (item.trim()) m.addItem.mutate(item.trim(), { onSuccess: () => setItem(''), onError });
              }
            }} />
        )}
      </section>

      <section>
        <h3 className="mb-2 flex items-center gap-2 text-xs font-semibold text-ink-2"><Paperclip className="size-4 text-ink-3" />Pièces jointes</h3>
        <ul className="space-y-1">
          {data.attachments.map((a) => (
            <li key={a.id} className="group flex items-center gap-2 text-sm">
              <a className="flex-1 truncate text-brand-600 hover:underline" href={`/api/attachments/${a.id}`} download>{a.filename}</a>
              <span className="text-xs text-ink-3">{size(a.sizeBytes)} · {a.uploaderName ?? '—'}</span>
              {a.deletable && (
                <button type="button" aria-label={`Supprimer ${a.filename}`} className="rounded p-0.5 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                  onClick={() => m.deleteAttachment.mutate(a.id, { onError })}><Trash2 className="size-3.5" /></button>
              )}
            </li>
          ))}
          {data.attachments.length === 0 && <li className="text-xs text-ink-3">Aucune pièce jointe.</li>}
        </ul>
        {data.canEdit && (
          <>
            <input ref={fileInput} type="file" className="hidden"
              accept=".pdf,.png,.jpg,.jpeg,.gif,.webp,.txt,.csv,.md,.docx,.xlsx,.pptx,.odt,.ods,.zip"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) m.attach.mutate(f, { onSuccess: () => toast('success', 'Fichier ajouté'), onError });
                e.target.value = '';
              }} />
            <Button type="button" size="sm" variant="secondary" className="mt-2" icon={<Upload className="size-3.5" />} loading={m.attach.isPending}
              onClick={() => fileInput.current?.click()}>Joindre un fichier (5 Mo max.)</Button>
          </>
        )}
      </section>

      <section>
        <h3 className="mb-2 flex items-center gap-2 text-xs font-semibold text-ink-2"><MessageSquare className="size-4 text-ink-3" />Commentaires</h3>
        <ul className="space-y-3">
          {data.comments.map((c) => (
            <li key={c.id} className="group flex gap-2.5">
              <Avatar name={c.authorName} size="sm" />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 text-xs">
                  <span className="font-medium">{c.authorName}</span>
                  <span className="text-ink-3">{dateTime.format(new Date(c.createdAt))}{c.editedAt ? ' · modifié' : ''}</span>
                  {c.editable && (
                    <button type="button" aria-label="Supprimer le commentaire" className="ml-auto rounded p-0.5 text-ink-3 opacity-0 group-hover:opacity-100 hover:text-red-600 focus:opacity-100"
                      onClick={() => m.deleteComment.mutate(c.id, { onError })}><Trash2 className="size-3.5" /></button>
                  )}
                </div>
                <p className="mt-0.5 text-sm whitespace-pre-line">{c.body}</p>
              </div>
            </li>
          ))}
        </ul>
        {data.canComment && (
          <div className="mt-3 flex gap-2">
            <textarea className="input min-h-10 flex-1" placeholder="Écrire un commentaire…" maxLength={4000} value={comment}
              onChange={(e) => setComment(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) submitComment(e); }} />
            <Button type="button" size="sm" onClick={submitComment} loading={m.comment.isPending}>Envoyer</Button>
          </div>
        )}
      </section>
    </div>
  );
}
