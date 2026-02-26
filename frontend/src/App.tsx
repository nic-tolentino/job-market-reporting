import { lazy, Suspense } from 'react';
import { Analytics } from '@vercel/analytics/react';
import { SpeedInsights } from "@vercel/speed-insights/react";
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import Layout from './components/layout/Layout';
import LandingPage from './pages/LandingPage';

const TechDetailsPage = lazy(() => import('./pages/TechDetailsPage'));
const CompanyProfilePage = lazy(() => import('./pages/CompanyProfilePage'));

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-[50vh]">
      <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
    </div>
  );
}

function App() {
  return (
    <Router>
      <Layout>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/tech/:techId" element={<TechDetailsPage />} />
            <Route path="/company/:companyId" element={<CompanyProfilePage />} />
          </Routes>
        </Suspense>
      </Layout>
      <Analytics />
      <SpeedInsights />
    </Router>
  );
}

export default App;
