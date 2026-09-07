import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authService } from '../services/authService';
import { useAuth } from '../context/AuthContext';
import { Shield, KeyRound, Globe, ArrowRight, Lock, CheckCircle2, AlertCircle } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { refreshUser } = useAuth();

  const [activeTab, setActiveTab] = useState<'staff' | 'customer' | 'federated'>('staff');

  // Customer state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [mfaRequired, setMfaRequired] = useState(false);
  const [mfaPendingToken, setMfaPendingToken] = useState('');
  const [mfaCode, setMfaCode] = useState('');

  // Staff Azure AD state
  const [azureToken, setAzureToken] = useState('');

  // Federated SSO state
  const [tenantId, setTenantId] = useState('');
  const [ssoCode, setSsoCode] = useState('');

  // Status/Error state
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(
    searchParams.get('expired') ? 'Your session expired. Please log in again.' : null
  );

  const handleCustomerLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      if (!mfaRequired) {
        const res = await authService.loginPassword(email, password);
        if (res.mfaRequired && res.mfaPendingToken) {
          setMfaPendingToken(res.mfaPendingToken);
          setMfaRequired(true);
        }
      } else {
        await authService.verifyMfa(mfaPendingToken, mfaCode);
        await refreshUser();
        navigate('/fleet');
      }
    } catch (err: any) {
      setError(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleStaffSso = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      // If no token provided in demo field, pass mock staff bearer token
      const tokenToUse = azureToken.trim() || 'MOCK_AZURE_AD_STAFF_TOKEN';
      await authService.staffAzureAdLogin(tokenToUse);
      await refreshUser();
      navigate('/fleet');
    } catch (err: any) {
      setError(err.message || 'Staff Azure AD authentication failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleFederatedSso = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await authService.federatedLogin(tenantId, ssoCode, window.location.origin + '/login');
      await refreshUser();
      navigate('/fleet');
    } catch (err: any) {
      setError(err.message || 'Federated SSO authentication failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#0F172A',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1.5rem',
        backgroundImage: 'radial-gradient(circle at 50% 0%, #0057B8 0%, #0F172A 70%)',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '460px',
          backgroundColor: '#FFFFFF',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <div
          style={{
            backgroundColor: '#0057B8',
            color: 'white',
            padding: '2rem 1.75rem 1.5rem 1.75rem',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              backgroundColor: '#F5A300',
              borderRadius: 'var(--radius-md)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '1.5rem',
              color: '#0057B8',
              marginBottom: '0.75rem',
            }}
          >
            IP
          </div>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 800, letterSpacing: '0.5px', marginBottom: '0.25rem' }}>
            ISLAND PACIFIC SENTINEL
          </h1>
          <p style={{ fontSize: '0.85rem', color: '#E6F0FA', opacity: 0.9 }}>
            Enterprise Multi-Tenant AIOps Suite
          </p>
        </div>

        {/* Tab Navigation */}
        <div
          style={{
            display: 'flex',
            borderBottom: '1px solid var(--border-color)',
            backgroundColor: '#F8FAFC',
          }}
        >
          <button
            onClick={() => { setActiveTab('staff'); setError(null); }}
            style={{
              flex: 1,
              padding: '0.85rem 0.5rem',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: activeTab === 'staff' ? '#0057B8' : '#64748B',
              borderBottom: activeTab === 'staff' ? '3px solid #0057B8' : '3px solid transparent',
              backgroundColor: activeTab === 'staff' ? 'white' : 'transparent',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.4rem',
            }}
          >
            <Shield size={16} /> Staff SSO
          </button>

          <button
            onClick={() => { setActiveTab('customer'); setError(null); }}
            style={{
              flex: 1,
              padding: '0.85rem 0.5rem',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: activeTab === 'customer' ? '#0057B8' : '#64748B',
              borderBottom: activeTab === 'customer' ? '3px solid #0057B8' : '3px solid transparent',
              backgroundColor: activeTab === 'customer' ? 'white' : 'transparent',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.4rem',
            }}
          >
            <KeyRound size={16} /> Customer + MFA
          </button>

          <button
            onClick={() => { setActiveTab('federated'); setError(null); }}
            style={{
              flex: 1,
              padding: '0.85rem 0.5rem',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: activeTab === 'federated' ? '#0057B8' : '#64748B',
              borderBottom: activeTab === 'federated' ? '3px solid #0057B8' : '3px solid transparent',
              backgroundColor: activeTab === 'federated' ? 'white' : 'transparent',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.4rem',
            }}
          >
            <Globe size={16} /> Federated SSO
          </button>
        </div>

        {/* Form Body */}
        <div style={{ padding: '1.75rem' }}>
          {error && (
            <div
              style={{
                backgroundColor: 'var(--color-danger-bg)',
                color: 'var(--color-danger)',
                border: '1px solid #FCA5A5',
                borderRadius: 'var(--radius-sm)',
                padding: '0.75rem 1rem',
                fontSize: '0.85rem',
                marginBottom: '1.25rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
              }}
            >
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          {/* TAB 1: STAFF AZURE AD SSO */}
          {activeTab === 'staff' && (
            <form onSubmit={handleStaffSso}>
              <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                  Sign in with your Island Pacific staff Microsoft Azure Active Directory credentials.
                </p>
                <div className="form-group" style={{ textAlign: 'left' }}>
                  <label className="form-label">Azure AD Token (Optional for Demo)</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="Enter OIDC ID Token or leave blank for default SSO"
                    value={azureToken}
                    onChange={(e) => setAzureToken(e.target.value)}
                  />
                </div>
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
                style={{ width: '100%', padding: '0.75rem', fontSize: '0.95rem' }}
              >
                {loading ? 'Authenticating with Azure AD...' : 'Sign in with Azure AD SSO'}
                <ArrowRight size={18} />
              </button>
            </form>
          )}

          {/* TAB 2: CUSTOMER EMAIL + MFA */}
          {activeTab === 'customer' && (
            <form onSubmit={handleCustomerLogin}>
              {!mfaRequired ? (
                <>
                  <div className="form-group">
                    <label className="form-label">Email Address</label>
                    <input
                      type="email"
                      required
                      className="form-control"
                      placeholder="user@organization.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Password</label>
                    <input
                      type="password"
                      required
                      className="form-control"
                      placeholder="••••••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </div>

                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={loading}
                    style={{ width: '100%', padding: '0.75rem', fontSize: '0.95rem', marginTop: '0.5rem' }}
                  >
                    {loading ? 'Verifying...' : 'Continue to MFA'}
                    <ArrowRight size={18} />
                  </button>
                </>
              ) : (
                <>
                  <div style={{ textAlign: 'center', marginBottom: '1.25rem' }}>
                    <Lock size={32} color="#0057B8" style={{ marginBottom: '0.5rem' }} />
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Two-Factor Authentication Required</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Enter the 6-digit verification code from your Authenticator app.
                    </p>
                  </div>

                  <div className="form-group">
                    <label className="form-label">TOTP Verification Code</label>
                    <input
                      type="text"
                      required
                      maxLength={6}
                      className="form-control"
                      placeholder="123456"
                      style={{ letterSpacing: '4px', textAlign: 'center', fontSize: '1.2rem', fontWeight: 700 }}
                      value={mfaCode}
                      onChange={(e) => setMfaCode(e.target.value)}
                    />
                  </div>

                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={loading}
                    style={{ width: '100%', padding: '0.75rem', fontSize: '0.95rem', marginTop: '0.5rem' }}
                  >
                    {loading ? 'Authenticating TOTP...' : 'Verify Code & Sign In'}
                    <CheckCircle2 size={18} />
                  </button>
                </>
              )}
            </form>
          )}

          {/* TAB 3: FEDERATED SSO */}
          {activeTab === 'federated' && (
            <form onSubmit={handleFederatedSso}>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                Sign in using your organization's configured SAML or OIDC IdP.
              </p>

              <div className="form-group">
                <label className="form-label">Tenant ID</label>
                <input
                  type="text"
                  required
                  className="form-control"
                  placeholder="e.g. 75891059-50b9-4a63-b225-89317e46307e"
                  value={tenantId}
                  onChange={(e) => setTenantId(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">SSO Authorization Code / Assertion</label>
                <input
                  type="text"
                  required
                  className="form-control"
                  placeholder="IdP Authorization Code"
                  value={ssoCode}
                  onChange={(e) => setSsoCode(e.target.value)}
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
                style={{ width: '100%', padding: '0.75rem', fontSize: '0.95rem', marginTop: '0.5rem' }}
              >
                {loading ? 'Authenticating...' : 'Sign In via Federated SSO'}
                <Globe size={18} />
              </button>
            </form>
          )}

          {/* Account Setup / Reset Links */}
          <div
            style={{
              marginTop: '1.5rem',
              paddingTop: '1rem',
              borderTop: '1px solid var(--border-color)',
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '0.8rem',
            }}
          >
            <Link to="/activate" style={{ color: '#0057B8', fontWeight: 600 }}>
              Activate Account / Invite
            </Link>
            <Link to="/reset-password" style={{ color: '#64748B' }}>
              Forgot Password?
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
