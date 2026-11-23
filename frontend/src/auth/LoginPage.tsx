import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { LayoutGrid, KanbanSquare, CalendarClock, Spade } from 'lucide-react';
import { useLogin, useMe } from '../api/hooks';
import { Button } from '../components/ui/Button';
import { Field } from '../components/ui/Field';

const features = [
  { icon: LayoutGrid, title: 'Vue portefeuille', text: 'Météo, risques et charge de tous les projets en un coup d’œil.' },
  { icon: KanbanSquare, title: 'Kanban & todos', text: 'Pilotez les tâches au quotidien, en glisser-déposer.' },
  { icon: CalendarClock, title: 'Rétro-planning', text: 'Partez de l’échéance, les dates se calculent toutes seules.' },
  { icon: Spade, title: 'Planning poker', text: 'Estimez en équipe, sans biais, en quelques secondes.' },
];

export function LoginPage() {
  const { data: me } = useMe();
  const login = useLogin();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  if (me) return <Navigate to="/" replace />;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    login.mutate(
      { email, password },
      { onSuccess: () => navigate((location.state as { from?: string } | null)?.from ?? '/', { replace: true }) },
    );
  };

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-brand-700 via-brand-600 to-violet-600 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="absolute -top-24 -right-24 size-96 rounded-full bg-white/10 blur-3xl" aria-hidden />
        <div className="absolute -bottom-32 -left-16 size-80 rounded-full bg-violet-300/20 blur-3xl" aria-hidden />
        <div className="relative flex items-center gap-2.5 text-lg font-semibold">
          <span className="flex size-9 items-center justify-center rounded-xl bg-white/15 backdrop-blur">EZ</span>
          EZProject
        </div>
        <div className="relative">
          <h1 className="max-w-md text-4xl leading-tight font-semibold tracking-tight">
            Tous vos projets, du sprint au comité de pilotage.
          </h1>
          <div className="mt-10 grid max-w-lg grid-cols-2 gap-5">
            {features.map(({ icon: Icon, title, text }) => (
              <div key={title} className="rounded-xl bg-white/10 p-4 backdrop-blur-sm">
                <Icon className="mb-2 size-5 opacity-90" />
                <div className="text-sm font-semibold">{title}</div>
                <div className="mt-1 text-xs leading-relaxed text-white/75">{text}</div>
              </div>
            ))}
          </div>
        </div>
        <p className="relative text-xs text-white/60">Dev, conseil, PMO : un seul outil, modulable.</p>
      </div>

      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="w-full max-w-sm animate-pop-in">
          <h2 className="text-2xl font-semibold tracking-tight">Connexion</h2>
          <p className="mt-1 mb-8 text-sm text-ink-3">Heureux de vous revoir 👋</p>
          <div className="space-y-4">
            <Field label="Email">
              <input className="input" type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
            </Field>
            <Field label="Mot de passe">
              <input className="input" type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} />
            </Field>
            {login.isError && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 dark:bg-red-500/10 dark:text-red-300">Email ou mot de passe incorrect.</p>}
            <Button type="submit" className="w-full" loading={login.isPending}>Se connecter</Button>
          </div>
          <p className="mt-6 text-center text-sm text-ink-3">
            Nouveau sur EZProject ? <Link to="/signup" className="font-medium text-brand-600 hover:underline">Créer une organisation</Link>
          </p>
          {import.meta.env.DEV && (
            <div className="mt-8 rounded-lg border border-dashed border-line p-3 text-xs text-ink-3">
              <div className="mb-1 font-medium text-ink-2">Comptes de démo (profil dev)</div>
              <b>Horizon Conseil</b> (<code>@ezproject.local</code>) : camille (COO, propriétaire) · lea (PMO, manager) ·
              hugo (resp. Delivery Web) · sarah · thomas (resp. Conseil SI) · nina · julie (invitée client)
              <br /><b>Acme Industries</b> : bob@acme.local — mot de passe <code>demo1234</code>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}
