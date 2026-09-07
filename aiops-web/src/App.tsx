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
import { DatasourcesPage } from './pages/DatasourcesPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { TenantsPage } from './pages/TenantsPage';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: '#0F172A', color: 'white' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '0.5rem' }}>Loading IP Sentinel Shell...</div>
          <div style={{ fontSize: '0.85rem', color: '#94A3B8' }}>Verifying tenant session & entitlements</div>
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
