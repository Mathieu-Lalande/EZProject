import { useEffect, useState } from 'react';
import { Lock, RotateCcw } from 'lucide-react';
import clsx from 'clsx';
import { useMe, useOrganization, usePermissionMatrix, usePermissionMatrixMutations } from '../../api/hooks';
import type { Permission, ProjectRole } from '../../api/types';
import { ApiError } from '../../api/client';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Spinner } from '../../components/ui/Spinner';
import { useToast } from '../../components/ui/Toast';
import { permissionGroups, projectRoleLabel } from '../../lib/labels';
import { isAdmin } from '../../lib/permissions';

const ROLES: ProjectRole[] = ['LEAD', 'CONTRIBUTOR', 'VIEWER'];
type Matrix = Record<ProjectRole, Permission[]>;

/** Matrice rôles × permissions : qui peut voir, ajouter, modifier chaque élément d'un projet. */
export function PermissionsTab() {
  const { data, isLoading } = usePermissionMatrix();
  const { data: org } = useOrganization();
  const { data: me } = useMe();
  const m = usePermissionMatrixMutations();
  const toast = useToast();
  const [draft, setDraft] = useState<Matrix | null>(null);
  useEffect(() => setDraft(data ? structuredClone(data.roles) : null), [data]);

  if (isLoading || !data || !draft) return <Spinner />;
  const planAllows = org?.usage.customPermissions ?? false;
  const editable = isAdmin(me) && planAllows;
  const dirty = JSON.stringify(sorted(draft)) !== JSON.stringify(sorted(data.roles));

  const toggle = (role: ProjectRole, p: Permission) =>
    setDraft({ ...draft, [role]: draft[role].includes(p) ? draft[role].filter((x) => x !== p) : [...draft[role], p] });
  const onError = (e: unknown) => toast('error', e instanceof ApiError ? e.message : 'Action impossible');

  return (
    <div className="max-w-4xl space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <p className="flex-1 text-sm text-ink-3">
          Les <strong>propriétaires et administrateurs</strong> ont tous les droits ; les <strong>managers</strong> sont Responsables
          sur tous les projets ; un <strong>invité</strong> est au plus Contributeur.
        </p>
        <Badge tone={data.customized ? 'brand' : 'neutral'}>{data.customized ? 'Matrice personnalisée' : 'Matrice par défaut'}</Badge>
      </div>
      {!planAllows && (
        <div className="flex items-center gap-2 rounded-xl border border-brand-200 bg-brand-50 px-4 py-3 text-sm text-brand-700 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-200">
          <Lock className="size-4" /> La personnalisation des permissions est disponible avec les offres Team et Enterprise.
        </div>
      )}
      <div className="card overflow-x-auto">
        <table className="w-full min-w-[560px] text-sm">
          <thead>
            <tr className="border-b border-line bg-surface-2/60 text-xs text-ink-3">
              <th className="px-5 py-2.5 text-left font-medium">Élément / action</th>
              {ROLES.map((r) => <th key={r} className="w-32 px-3 py-2.5 text-center font-medium">{projectRoleLabel[r]}</th>)}
            </tr>
          </thead>
          {permissionGroups.map((g) => (
            <tbody key={g.label}>
              <tr><td colSpan={4} className="bg-surface-2/30 px-5 pt-3 pb-1.5 text-[11px] font-semibold tracking-wider text-ink-3 uppercase">{g.label}</td></tr>
              {g.items.map((item) => (
                <tr key={item.id} className="border-b border-line last:border-0">
                  <td className="px-5 py-2">{item.label}</td>
                  {ROLES.map((r) => {
                    const checked = draft[r].includes(item.id);
                    // Garde-fou aligné sur le backend : le Responsable garde la consultation et la gestion des accès
                    const locked = r === 'LEAD' && (item.id === 'PROJECT_MEMBERS' || item.id === 'TASK_VIEW');
                    return (
                      <td key={r} className="px-3 py-2 text-center">
                        <input
                          type="checkbox"
                          className={clsx('size-4 accent-brand-600', (editable && !locked) && 'cursor-pointer')}
                          checked={checked}
                          disabled={!editable || locked}
                          onChange={() => toggle(r, item.id)}
                          aria-label={`${projectRoleLabel[r]} : ${g.label} — ${item.label}`}
                        />
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          ))}
        </table>
      </div>
      {editable && (
        <div className="flex justify-end gap-2">
          {data.customized && (
            <Button variant="ghost" icon={<RotateCcw className="size-4" />} loading={m.reset.isPending}
              onClick={() => m.reset.mutate(undefined, { onSuccess: () => toast('success', 'Matrice par défaut restaurée'), onError })}>
              Rétablir les valeurs par défaut
            </Button>
          )}
          <Button variant="secondary" disabled={!dirty} onClick={() => setDraft(structuredClone(data.roles))}>Annuler</Button>
          <Button disabled={!dirty} loading={m.save.isPending}
            onClick={() => m.save.mutate(draft, { onSuccess: () => toast('success', 'Permissions enregistrées'), onError })}>
            Enregistrer
          </Button>
        </div>
      )}
    </div>
  );
}

function sorted(m: Matrix) {
  return Object.fromEntries(ROLES.map((r) => [r, [...m[r]].sort()]));
}
