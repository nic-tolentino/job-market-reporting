import { lazy, Suspense } from 'react';
import { Analytics } from '@vercel/analytics/react';
import { SpeedInsights } from "@vercel/speed-insights/react";
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import PageLoader from './components/common/PageLoader';
import ScrollToTop from './components/common/ScrollToTop';
import Layout from './components/layout/Layout';
import LandingPage from './pages/LandingPage';

const TechDetailsPage = lazy(() => import('./pages/TechDetailsPage'));
const CompanyProfilePage = lazy(() => import('./pages/CompanyProfilePage'));
const JobPage = lazy(() => import('./pages/JobPage'));
const TransparencyPage = lazy(() => import('./pages/TransparencyPage'));
const PrivacyPolicyPage = lazy(() => import('./pages/PrivacyPolicyPage'));
const TermsPage = lazy(() => import('./pages/TermsPage'));
const ContactPage = lazy(() => import('./pages/ContactPage'));
const CompaniesPage = lazy(() => import('./pages/CompaniesPage'));

function App() {
  return (
    <Router>
      <ScrollToTop />
      <Layout>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/tech/:techId" element={<TechDetailsPage />} />
            <Route path="/company/:companyId" element={<CompanyProfilePage />} />
            <Route path="/job/:jobId" element={<JobPage />} />
            <Route path="/transparency" element={<TransparencyPage />} />
            <Route path="/privacy" element={<PrivacyPolicyPage />} />
            <Route path="/terms" element={<TermsPage />} />
            <Route path="/companies" element={<CompaniesPage />} />
            <Route path="/contact" element={<ContactPage />} />
          </Routes>
        </Suspense>
      </Layout>
      <Analytics />
      <SpeedInsights />
    </Router>
  );
}

export default App;
