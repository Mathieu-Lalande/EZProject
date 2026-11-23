import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth, useIsManager } from './auth/RequireAuth';
import { LoginPage } from './auth/LoginPage';
import { AppLayout } from './components/layout/AppLayout';
import { PortfolioPage } from './pages/PortfolioPage';
import { MySpacePage } from './pages/MySpacePage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProjectPage } from './pages/ProjectPage';
import { OrganizationPage } from './pages/OrganizationPage';
import { SignupPage } from './auth/SignupPage';

function Home() {
  return <Navigate to={useIsManager() ? '/portfolio' : '/me'} replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
        <Route index element={<Home />} />
        <Route path="portfolio" element={<PortfolioPage />} />
        <Route path="me" element={<MySpacePage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="projects/:id/:tab?" element={<ProjectPage />} />
        <Route path="organization/:tab?" element={<OrganizationPage />} />
        <Route path="team" element={<Navigate to="/organization/members" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
