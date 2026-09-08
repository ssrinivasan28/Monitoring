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

  const authInputStyle: React.CSSProperties = {
    width: '100%',
    height: 44,
    background: 'white',
    border: '1px solid #CBD5E1',
    borderRadius: 12,
    padding: '0 16px',
    fontSize: 14,
  };

  const tabs: { key: typeof activeTab; label: string; icon: React.ReactNode }[] = [
    { key: 'staff', label: 'Staff SSO', icon: <Shield size={15} /> },
    { key: 'customer', label: 'Customer + MFA', icon: <KeyRound size={15} /> },
    { key: 'federated', label: 'Federated SSO', icon: <Globe size={15} /> },
  ];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', width: '100%', background: '#F3F6FB' }}>
      {/* Left brand panel */}
      <div
        style={{
          width: '44%',
          position: 'relative',
          background: 'var(--brand-primary)',
          padding: 56,
          display: 'none',
          flexDirection: 'column',
          justifyContent: 'space-between',
          overflow: 'hidden',
        }}
        className="login-brand-panel"
      >
        <div style={{ position: 'absolute', top: -180, right: -180, width: 500, height: 500, borderRadius: '50%', background: 'var(--brand-accent)', opacity: 0.1 }} />
        <div style={{ position: 'absolute', bottom: -140, left: -140, width: 320, height: 320, borderRadius: '50%', background: 'white', opacity: 0.07 }} />

        <div style={{ position: 'relative', zIndex: 1 }}>
          <div style={{ display: 'inline-flex', background: 'white', borderRadius: 12, padding: '10px 16px', boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
            <img src="/IPLogo.jpg" alt="Island Pacific" style={{ height: 32, width: 'auto', objectFit: 'contain' }} />
          </div>
          <div style={{ marginTop: 12, fontSize: 13, fontWeight: 600, color: '#BFDBFE', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
            Enterprise AIOps Suite
          </div>
        </div>

        <div style={{ position: 'relative', zIndex: 1 }}>
          <h1 style={{ fontSize: 32, fontWeight: 900, color: 'white', letterSpacing: '-0.02em', lineHeight: 1.15 }}>
            IP Sentinel<br />
            <span style={{ color: 'var(--brand-accent)' }}>AIOps Platform</span>
          </h1>
          <p style={{ marginTop: 12, fontSize: 14, fontWeight: 500, color: '#BFDBFE', maxWidth: 380 }}>
            Read-only, multi-tenant monitoring and AI-assisted operations across your entire IBM i and Windows fleet.
          </p>
        </div>

        <div style={{ position: 'relative', zIndex: 1, display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#BFDBFE' }}>
          <span>© {new Date().getFullYear()} Island Pacific</span>
          <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#22c55e' }} className="pulse-dot" />
            All systems operational
          </span>
        </div>
      </div>

      {/* Right form panel */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '32px 24px' }}>
        <div style={{ width: '100%', maxWidth: 420 }}>
          <div className="login-mobile-logo" style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
            <img src="/IPLogo.jpg" alt="Island Pacific" style={{ height: 26, width: 'auto', objectFit: 'contain' }} />
          </div>

          <h2 style={{ fontSize: 26, fontWeight: 900, color: 'var(--brand-primary)', letterSpacing: '-0.02em' }}>
            Island Pacific Sentinel
          </h2>
          <p style={{ marginTop: 4, fontSize: 14, color: 'var(--text-muted)' }}>Sign in to your operations console</p>
          <div style={{ height: 3, width: 48, borderRadius: 3, background: 'var(--brand-primary)', marginTop: 12, marginBottom: 24 }} />

          {/* Tab Navigation */}
          <div style={{ display: 'flex', gap: 4, background: '#EAF0F8', padding: 4, borderRadius: 12, marginBottom: 24 }}>
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => { setActiveTab(tab.key); setError(null); }}
                style={{
                  flex: 1,
                  padding: '0.6rem 0.4rem',
                  fontSize: 12,
                  fontWeight: 600,
                  borderRadius: 9,
                  color: activeTab === tab.key ? 'var(--brand-primary)' : 'var(--text-muted)',
                  backgroundColor: activeTab === tab.key ? 'white' : 'transparent',
                  boxShadow: activeTab === tab.key ? 'var(--shadow-card)' : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.35rem',
                  transition: 'all 150ms ease',
                }}
              >
                {tab.icon} {tab.label}
              </button>
            ))}
          </div>

          {error && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '12px 16px',
                background: '#FEF2F2',
                border: '1px solid #fecaca',
                borderRadius: 12,
                color: '#b91c1c',
                fontSize: 13,
                fontWeight: 500,
                marginBottom: 20,
              }}
            >
              <AlertCircle size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* TAB 1: STAFF AZURE AD SSO */}
          {activeTab === 'staff' && (
            <form onSubmit={handleStaffSso}>
              <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 16 }}>
                Sign in with your Island Pacific staff Microsoft Azure Active Directory credentials.
              </p>
              <div style={{ marginBottom: 20 }}>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                  Azure AD Token (Optional for Demo)
                </label>
                <input
                  type="text"
                  style={authInputStyle}
                  placeholder="Enter OIDC ID Token or leave blank for default SSO"
                  value={azureToken}
                  onChange={(e) => setAzureToken(e.target.value)}
                />
              </div>

              <button type="submit" disabled={loading} className="login-submit-btn">
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
                  <div style={{ marginBottom: 16 }}>
                    <label style={{ display: 'block', fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                      Email Address
                    </label>
                    <input
                      type="email"
                      required
                      style={authInputStyle}
                      placeholder="user@organization.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>

                  <div style={{ marginBottom: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 6 }}>
                      <label style={{ fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)' }}>
                        Password
                      </label>
                      <Link to="/reset-password" style={{ fontSize: 12, fontWeight: 600, color: 'var(--brand-primary)' }}>
                        Forgot password?
                      </Link>
                    </div>
                    <input
                      type="password"
                      required
                      style={authInputStyle}
                      placeholder="••••••••••••"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </div>

                  <button type="submit" disabled={loading} className="login-submit-btn" style={{ marginTop: 16 }}>
                    {loading ? 'Verifying...' : 'Continue to MFA'}
                    <ArrowRight size={18} />
                  </button>
                </>
              ) : (
                <>
                  <div style={{ textAlign: 'center', marginBottom: 20 }}>
                    <Lock size={30} color="var(--brand-primary)" style={{ marginBottom: 8 }} />
                    <h3 style={{ fontSize: 16, fontWeight: 700 }}>Two-Factor Authentication Required</h3>
                    <p style={{ fontSize: 13, color: 'var(--text-muted)', marginTop: 4 }}>
                      Enter the 6-digit verification code from your Authenticator app.
                    </p>
                  </div>

                  <div style={{ marginBottom: 16 }}>
                    <label style={{ display: 'block', fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                      TOTP Verification Code
                    </label>
                    <input
                      type="text"
                      required
                      maxLength={6}
                      style={{ ...authInputStyle, height: 56, fontFamily: 'var(--font-mono)', fontSize: 22, textAlign: 'center', letterSpacing: '0.6em', paddingLeft: 0, paddingRight: 0 }}
                      placeholder="123456"
                      value={mfaCode}
                      onChange={(e) => setMfaCode(e.target.value)}
                    />
                  </div>

                  <button type="submit" disabled={loading} className="login-submit-btn">
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
              <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 16 }}>
                Sign in using your organization's configured SAML or OIDC IdP.
              </p>

              <div style={{ marginBottom: 16 }}>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                  Tenant ID
                </label>
                <input
                  type="text"
                  required
                  style={authInputStyle}
                  placeholder="e.g. 75891059-50b9-4a63-b225-89317e46307e"
                  value={tenantId}
                  onChange={(e) => setTenantId(e.target.value)}
                />
              </div>

              <div style={{ marginBottom: 16 }}>
                <label style={{ display: 'block', fontSize: 12, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-muted)', marginBottom: 6 }}>
                  SSO Authorization Code / Assertion
                </label>
                <input
                  type="text"
                  required
                  style={authInputStyle}
                  placeholder="IdP Authorization Code"
                  value={ssoCode}
                  onChange={(e) => setSsoCode(e.target.value)}
                />
              </div>

              <button type="submit" disabled={loading} className="login-submit-btn">
                {loading ? 'Authenticating...' : 'Sign In via Federated SSO'}
                <Globe size={18} />
              </button>
            </form>
          )}

          {/* Account Setup Link */}
          <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid var(--border-subtle)', textAlign: 'center', fontSize: 12 }}>
            <Link to="/activate" style={{ color: 'var(--brand-primary)', fontWeight: 600 }}>
              Activate Account / Invite
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
