import React, { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { authService } from '../services/authService';
import { ShieldCheck, ArrowRight, AlertCircle, CheckCircle2 } from 'lucide-react';

export const ActivatePage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [inviteToken, setInviteToken] = useState(searchParams.get('token') || '');
  const [password, setPassword] = useState('');
  const [totpSecret, setTotpSecret] = useState('');
  const [totpCode, setTotpCode] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleActivate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await authService.activateUser(inviteToken, password, totpCode, totpSecret);
      setSuccess(res.message || 'Account activated successfully! You can now log in.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err: any) {
      setError(err.message || 'Account activation failed.');
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
          padding: '2rem',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <ShieldCheck size={40} color="#0057B8" style={{ marginBottom: '0.5rem' }} />
          <h2 style={{ fontSize: '1.3rem', fontWeight: 800 }}>Activate Your Customer Account</h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Complete your invitation setup by setting a password and TOTP secret.
          </p>
        </div>

        {error && (
          <div
            style={{
              backgroundColor: 'var(--color-danger-bg)',
              color: 'var(--color-danger)',
              border: '1px solid #FCA5A5',
              borderRadius: 'var(--radius-sm)',
              padding: '0.75rem',
              fontSize: '0.85rem',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {success && (
          <div
            style={{
              backgroundColor: 'var(--color-success-bg)',
              color: 'var(--color-success)',
              border: '1px solid #6EE7B7',
              borderRadius: 'var(--radius-sm)',
              padding: '0.75rem',
              fontSize: '0.85rem',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
            }}
          >
            <CheckCircle2 size={18} />
            <span>{success}</span>
          </div>
        )}

        <form onSubmit={handleActivate}>
          <div className="form-group">
            <label className="form-label">Invitation Token</label>
            <input
              type="text"
              required
              className="form-control"
              placeholder="Paste invitation token"
              value={inviteToken}
              onChange={(e) => setInviteToken(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">New Password</label>
            <input
              type="password"
              required
              className="form-control"
              placeholder="Set account password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">TOTP Base32 Secret</label>
            <input
              type="text"
              required
              className="form-control"
              placeholder="e.g. JBSWY3DPEHPK3PXP"
              value={totpSecret}
              onChange={(e) => setTotpSecret(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Verification Code from App</label>
            <input
              type="text"
              required
              maxLength={6}
              className="form-control"
              placeholder="123456"
              style={{ letterSpacing: '4px', textAlign: 'center', fontSize: '1.1rem', fontWeight: 700 }}
              value={totpCode}
              onChange={(e) => setTotpCode(e.target.value)}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading}
            style={{ width: '100%', padding: '0.75rem', fontSize: '0.95rem', marginTop: '0.5rem' }}
          >
            {loading ? 'Activating Account...' : 'Activate Account'}
            <ArrowRight size={18} />
          </button>
        </form>

        <div style={{ marginTop: '1.25rem', textAlign: 'center', fontSize: '0.85rem' }}>
          Already activated? <Link to="/login" style={{ color: '#0057B8', fontWeight: 600 }}>Back to Login</Link>
        </div>
      </div>
    </div>
  );
};
