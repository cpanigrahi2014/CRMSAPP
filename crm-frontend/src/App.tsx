/* ============================================================
   App.tsx – Root component with React Router configuration
   ============================================================ */
import React, { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from './hooks/useRedux';
import { fetchProfile } from './store/slices/authSlice';

/* Layouts */
import MainLayout from './layouts/MainLayout';
import AuthLayout from './layouts/AuthLayout';

/* Auth Pages */
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';

/* App Pages */
import DashboardPage from './pages/DashboardPage';
import LeadsPage from './pages/LeadsPage';
import LeadDetailPage from './pages/LeadDetailPage';
import AccountsPage from './pages/AccountsPage';
import AccountDetailPage from './pages/AccountDetailPage';
import ContactsPage from './pages/ContactsPage';
import ContactDetailPage from './pages/ContactDetailPage';
import OpportunitiesPage from './pages/OpportunitiesPage';
import OpportunityDetailPage from './pages/OpportunityDetailPage';
import ActivitiesPage from './pages/ActivitiesPage';
import CasesPage from './pages/CasesPage';
import CampaignsPage from './pages/CampaignsPage';
import ReportsPage from './pages/ReportsPage';
import SettingsPage from './pages/SettingsPage';
import AiConfigPage from './pages/AiConfigPage';
import EmailPage from './pages/EmailPage';
import WorkflowsPage from './pages/WorkflowsPage';
import SecurityPage from './pages/SecurityPage';
import IntegrationsPage from './pages/IntegrationsPage';
import AiInsightsPage from './pages/AiInsightsPage';
import ZeroConfigPage from './pages/ZeroConfigPage';
import CommunicationsPage from './pages/CommunicationsPage';
import CollaborationPage from './pages/CollaborationPage';
import SmartAutomationPage from './pages/SmartAutomationPage';
import DeveloperPortalPage from './pages/DeveloperPortalPage';
import ObjectManagerPage from './pages/ObjectManagerPage';
import PricingPage from './pages/PricingPage';
import LandingPage from './pages/LandingPage';

/* ---- Protected Route wrapper ---- */
const RequireAuth: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAppSelector((s) => s.auth);
  if (!isAuthenticated) return <Navigate to="/landing" replace />;
  return <>{children}</>;
};

const App: React.FC = () => {
  const dispatch = useAppDispatch();
  const { token, user } = useAppSelector((s) => s.auth);

  // Attempt to load user profile if token exists but user hasn't been fetched
  useEffect(() => {
    if (token && !user) {
      dispatch(fetchProfile());
    }
  }, [dispatch, token, user]);

  return (
    <Routes>
      {/* ---- Public: Landing page ---- */}
      <Route path="/landing" element={<LandingPage />} />

      {/* ---- Public: Auth routes ---- */}
      <Route path="/auth" element={<AuthLayout />}>
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route index element={<Navigate to="login" replace />} />
      </Route>

      {/* ---- Protected: App routes ---- */}
      <Route
        path="/"
        element={
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="leads" element={<LeadsPage />} />
        <Route path="leads/:id" element={<LeadDetailPage />} />
        <Route path="accounts" element={<AccountsPage />} />
        <Route path="accounts/:id" element={<AccountDetailPage />} />
        <Route path="contacts" element={<ContactsPage />} />
        <Route path="contacts/:id" element={<ContactDetailPage />} />
        <Route path="opportunities" element={<OpportunitiesPage />} />
        <Route path="opportunities/:id" element={<OpportunityDetailPage />} />
        <Route path="activities" element={<ActivitiesPage />} />
        <Route path="email" element={<EmailPage />} />
        <Route path="cases" element={<CasesPage />} />
        <Route path="campaigns" element={<CampaignsPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="workflows" element={<WorkflowsPage />} />          <Route path="security" element={<SecurityPage />} />
          <Route path="integrations" element={<IntegrationsPage />} />
          <Route path="settings" element={<SettingsPage />} />
        <Route path="ai-config" element={<AiConfigPage />} />
        <Route path="ai-insights" element={<AiInsightsPage />} />
        <Route path="zero-config" element={<ZeroConfigPage />} />
        <Route path="communications" element={<CommunicationsPage />} />
        <Route path="collaboration" element={<CollaborationPage />} />
        <Route path="automation" element={<SmartAutomationPage />} />
        <Route path="developer" element={<DeveloperPortalPage />} />
        <Route path="object-manager" element={<ObjectManagerPage />} />
        <Route path="pricing" element={<PricingPage />} />
      </Route>

      {/* ---- Catch-all ---- */}
      <Route path="*" element={<Navigate to="/landing" replace />} />
    </Routes>
  );
};

export default App;
