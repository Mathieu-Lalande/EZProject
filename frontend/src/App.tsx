import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth, useIsManager } from './auth/RequireAuth';
import { LoginPage } from './auth/LoginPage';
import { AppLayout } from './components/layout/AppLayout';
import { PortfolioPage } from './pages/PortfolioPage';
import { MySpacePage } from './pages/MySpacePage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProjectPage } from './pages/ProjectPage';
import { TeamPage } from './pages/TeamPage';

function Home() {
  return <Navigate to={useIsManager() ? '/portfolio' : '/me'} replace />;
}

function ManagerOnly({ children }: { children: React.ReactNode }) {
  return useIsManager() ? <>{children}</> : <Navigate to="/me" replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
        <Route index element={<Home />} />
        <Route path="portfolio" element={<ManagerOnly><PortfolioPage /></ManagerOnly>} />
        <Route path="me" element={<MySpacePage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="projects/:id/:tab?" element={<ProjectPage />} />
        <Route path="team" element={<TeamPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
