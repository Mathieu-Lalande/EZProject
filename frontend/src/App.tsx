import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Spinner } from './components/ui/Spinner';
import { RequireAuth, useIsManager } from './auth/RequireAuth';
import { LoginPage } from './auth/LoginPage';
import { AppLayout } from './components/layout/AppLayout';

// Découpage du code : chaque page est chargée à la demande (bundle initial plus léger)
const PortfolioPage = lazy(() => import('./pages/PortfolioPage').then((m) => ({ default: m.PortfolioPage })));
const MySpacePage = lazy(() => import('./pages/MySpacePage').then((m) => ({ default: m.MySpacePage })));
const ProjectsPage = lazy(() => import('./pages/ProjectsPage').then((m) => ({ default: m.ProjectsPage })));
const ProjectPage = lazy(() => import('./pages/ProjectPage').then((m) => ({ default: m.ProjectPage })));
const OrganizationPage = lazy(() => import('./pages/OrganizationPage').then((m) => ({ default: m.OrganizationPage })));
const SignupPage = lazy(() => import('./auth/SignupPage').then((m) => ({ default: m.SignupPage })));
const AccountPage = lazy(() => import('./pages/AccountPage').then((m) => ({ default: m.AccountPage })));
const PrivacyPage = lazy(() => import('./pages/PrivacyPage').then((m) => ({ default: m.PrivacyPage })));
const CapacityPage = lazy(() => import('./pages/CapacityPage').then((m) => ({ default: m.CapacityPage })));
const CopilPage = lazy(() => import('./pages/CopilPage').then((m) => ({ default: m.CopilPage })));
const RoadmapPage = lazy(() => import('./pages/RoadmapPage').then((m) => ({ default: m.RoadmapPage })));

function Home() {
  return <Navigate to={useIsManager() ? '/portfolio' : '/me'} replace />;
}

export function App() {
  return (
    <Suspense fallback={<Spinner />}>
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/projects/:id/copil" element={<RequireAuth><CopilPage /></RequireAuth>} />
      <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
        <Route index element={<Home />} />
        <Route path="portfolio" element={<PortfolioPage />} />
        <Route path="me" element={<MySpacePage />} />
        <Route path="account" element={<AccountPage />} />
        <Route path="capacity" element={<CapacityPage />} />
        <Route path="roadmap" element={<RoadmapPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="projects/:id/:tab?" element={<ProjectPage />} />
        <Route path="organization/:tab?" element={<OrganizationPage />} />
        <Route path="team" element={<Navigate to="/organization/members" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
    </Suspense>
  );
}
