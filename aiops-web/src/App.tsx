import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { TenantProvider } from './context/TenantContext';
import { AppLayout } from './components/Layout/AppLayout';

import { LoginPage } from './pages/LoginPage';
import { ActivatePage } from './pages/ActivatePage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';

import { FleetPage } from './pages/FleetPage';
import { IncidentsPage } from './pages/IncidentsPage';
import { AiAssistantPage } from './pages/AiAssistantPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { DatasourcesPage } from './pages/DatasourcesPage';
import { TenantsPage } from './pages/TenantsPage';
import { DashboardPickerPage } from './pages/dashboards/DashboardPickerPage';
import { DynamicDashboardView } from './pages/dashboards/DynamicDashboardView';
import { WindowsMonitorDashboard } from './pages/dashboards/WindowsMonitorDashboard';
import { WinServiceMonitorDashboard } from './pages/dashboards/WinServiceMonitorDashboard';
import { LogExplorerPage } from './pages/LogExplorerPage';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--bg-main)', color: 'var(--text-main)' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '1.2rem', fontWeight: 600, marginBottom: '0.5rem', color: 'var(--brand-primary)' }}>Loading IP Sentinel Shell...</div>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Verifying tenant session & entitlements</div>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <AppLayout>{children}</AppLayout>;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <TenantProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/activate" element={<ActivatePage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />

            {/* Protected App Routes */}
            <Route
              path="/fleet"
              element={
                <ProtectedRoute>
                  <FleetPage />
                </ProtectedRoute>
              }
            />
            {/* Dashboard Catalog & Engine Routes */}
            <Route
              path="/dashboards"
              element={
                <ProtectedRoute>
                  <DashboardPickerPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboards/view/:id"
              element={
                <ProtectedRoute>
                  <DynamicDashboardView />
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboards/windows"
              element={
                <ProtectedRoute>
                  <DynamicDashboardView initialId="windows-monitor" />
                </ProtectedRoute>
              }
            />
            <Route
              path="/dashboards/win-service"
              element={
                <ProtectedRoute>
                  <DynamicDashboardView initialId="win-service-monitor" />
                </ProtectedRoute>
              }
            />
            <Route
              path="/logs"
              element={
                <ProtectedRoute>
                  <LogExplorerPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/incidents"
              element={
                <ProtectedRoute>
                  <IncidentsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/assistant"
              element={
                <ProtectedRoute>
                  <AiAssistantPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/audit-logs"
              element={
                <ProtectedRoute>
                  <AuditLogsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/datasources"
              element={
                <ProtectedRoute>
                  <DatasourcesPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/tenants"
              element={
                <ProtectedRoute>
                  <TenantsPage />
                </ProtectedRoute>
              }
            />

            {/* Default Fallback Redirect */}
            <Route path="*" element={<Navigate to="/fleet" replace />} />
          </Routes>
        </BrowserRouter>
      </TenantProvider>
    </AuthProvider>
  );
};

export default App;
