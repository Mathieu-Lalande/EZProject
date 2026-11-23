import { Navigate, NavLink, useParams } from 'react-router-dom';
import { Building2, FileSpreadsheet, KeyRound, Users, UsersRound } from 'lucide-react';
import clsx from 'clsx';
import { useMe } from '../api/hooks';
import { PageHeader } from '../components/ui/PageHeader';
import { Badge } from '../components/ui/Badge';
import { planLabel } from '../lib/labels';
import { GeneralTab } from '../features/organization/GeneralTab';
import { MembersTab } from '../features/organization/MembersTab';
import { TeamsTab } from '../features/organization/TeamsTab';
import { PermissionsTab } from '../features/organization/PermissionsTab';
import { ImportExportTab } from '../features/organization/ImportExportTab';

const TABS = [
  { id: 'general', label: 'Général & offre', icon: <Building2 className="size-4" /> },
  { id: 'members', label: 'Membres', icon: <Users className="size-4" /> },
  { id: 'teams', label: 'Équipes', icon: <UsersRound className="size-4" /> },
  { id: 'permissions', label: 'Rôles & permissions', icon: <KeyRound className="size-4" /> },
  { id: 'data', label: 'Import / export', icon: <FileSpreadsheet className="size-4" /> },
];

export function OrganizationPage() {
  const { tab = 'general' } = useParams();
  const { data: me } = useMe();
  // Un invité n'a pas accès à l'administration de l'organisation
  if (me?.role === 'GUEST') return <Navigate to="/me" replace />;
  if (!TABS.some((t) => t.id === tab)) return <Navigate to="/organization" replace />;

  return (
    <div className="animate-fade-in">
      <PageHeader
        title={<span className="flex items-center gap-3">{me?.organization.name}{me && <Badge tone="brand">{planLabel[me.organization.plan]}</Badge>}</span>}
        subtitle="Membres, équipes, droits d'accès et reprise de vos données."
      />
      <nav className="scrollbar-thin mb-6 flex gap-1 overflow-x-auto border-b border-line">
        {TABS.map((t) => (
          <NavLink key={t.id} end to={t.id === 'general' ? '/organization' : `/organization/${t.id}`}
            className={({ isActive }) => clsx('-mb-px flex items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium whitespace-nowrap transition',
              isActive ? 'border-brand-600 text-ink' : 'border-transparent text-ink-3 hover:text-ink')}>
            {t.icon}{t.label}
          </NavLink>
        ))}
      </nav>
      {tab === 'general' && <GeneralTab />}
      {tab === 'members' && <MembersTab />}
      {tab === 'teams' && <TeamsTab />}
      {tab === 'permissions' && <PermissionsTab />}
      {tab === 'data' && <ImportExportTab />}
    </div>
  );
}
