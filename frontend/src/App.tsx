import { lazy, Suspense } from 'react';
import { Analytics } from '@vercel/analytics/react';
import { SpeedInsights } from "@vercel/speed-insights/react";
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import PageLoader from './components/common/PageLoader';
import Layout from './components/layout/Layout';
import LandingPage from './pages/LandingPage';

const TechDetailsPage = lazy(() => import('./pages/TechDetailsPage'));
const CompanyProfilePage = lazy(() => import('./pages/CompanyProfilePage'));
const JobPage = lazy(() => import('./pages/JobPage'));

function App() {
  return (
    <Router>
      <Layout>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/tech/:techId" element={<TechDetailsPage />} />
            <Route path="/company/:companyId" element={<CompanyProfilePage />} />
            <Route path="/job/:jobId" element={<JobPage />} />
          </Routes>
        </Suspense>
      </Layout>
      <Analytics />
      <SpeedInsights />
    </Router>
  );
}

export default App;
