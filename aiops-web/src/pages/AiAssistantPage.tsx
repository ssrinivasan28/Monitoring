import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Bot, Sparkles, Lock, Send, ShieldAlert, AlertCircle } from 'lucide-react';
import { apiRequest } from '../services/apiClient';

export const AiAssistantPage: React.FC = () => {
  const { entitlementTier } = useAuth();
  const isProTier = entitlementTier === 'PRO';

  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState<Array<{ sender: 'user' | 'assistant'; text: string }>>([
    {
      sender: 'assistant',
      text: 'Hello! I am your Sentinel AI Assistant. I can help analyze cross-system anomalies, correlate IBM i and Windows logs, and assist with root-cause investigations.',
    },
  ]);
  const [loading, setLoading] = useState(false);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || !isProTier) return;

    const userText = prompt;
    setMessages((prev) => [...prev, { sender: 'user', text: userText }]);
    setPrompt('');
    setLoading(true);

    try {
      const data = await apiRequest<{ response: string }>('/api/v1/assistant/query', {
        method: 'POST',
        body: JSON.stringify({ prompt: userText }),
      });
      setMessages((prev) => [...prev, { sender: 'assistant', text: data.response || 'Analysis complete.' }]);
    } catch (err: any) {
      setMessages((prev) => [
        ...prev,
        {
          sender: 'assistant',
          text: `[Analysis Error]: ${err.message || 'The LLM provider is currently unavailable. Statistical forecasting and threshold rules remain active.'}`,
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Bot color="#0057B8" size={26} /> AI Assistant & Root Cause Narrative
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          LLM-powered cross-system incident correlation across IBM i + Windows.
        </p>
      </div>

      {/* Gating Banner for Basic Tier */}
      {!isProTier ? (
        <div
          className="card"
          style={{
            borderLeft: '4px solid #F5A300',
            backgroundColor: '#FFFBEB',
            textAlign: 'center',
            padding: '3rem 2rem',
          }}
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              backgroundColor: '#FEF3C7',
              borderRadius: '50%',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '1rem',
            }}
          >
            <Lock size={28} color="#D97706" />
          </div>

          <h2 style={{ fontSize: '1.3rem', fontWeight: 800, color: '#92400E', marginBottom: '0.5rem' }}>
            PRO Tier Entitlement Required
          </h2>
          <p style={{ fontSize: '0.9rem', color: '#78350F', maxWidth: '520px', margin: '0 auto 1.5rem auto' }}>
            Your active tenant is currently on the <strong>BASIC</strong> tier. The AI Assistant, automated root-cause narrative generation, and LLM correlation are reserved for <strong>PRO</strong> tier subscribers.
          </p>

          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.5rem',
              backgroundColor: 'white',
              border: '1px solid #FDE68A',
              padding: '0.6rem 1.25rem',
              borderRadius: 'var(--radius-full)',
              fontSize: '0.85rem',
              fontWeight: 600,
              color: '#92400E',
            }}
          >
            <Sparkles size={16} color="#F5A300" /> Upgrade to PRO Tier in Admin › Tenants to unlock AI intelligence
          </div>
        </div>
      ) : (
        /* Unlocked PRO Tier Interface */
        <div className="card" style={{ display: 'flex', flexDirection: 'column', height: '600px', padding: 0, overflow: 'hidden' }}>
          {/* Chat Messages */}
          <div style={{ flex: 1, padding: '1.5rem', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {messages.map((m, idx) => (
              <div
                key={idx}
                style={{
                  display: 'flex',
                  justifyContent: m.sender === 'user' ? 'flex-end' : 'flex-start',
                }}
              >
                <div
                  style={{
                    maxWidth: '80%',
                    padding: '0.85rem 1.1rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: m.sender === 'user' ? '#0057B8' : '#F1F5F9',
                    color: m.sender === 'user' ? 'white' : '#0F172A',
                    fontSize: '0.9rem',
                    lineHeight: 1.5,
                  }}
                >
                  {m.sender === 'assistant' && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', fontWeight: 700, color: '#0057B8', marginBottom: '0.3rem' }}>
                      <Sparkles size={12} color="#F5A300" /> SENTINEL AI
                    </div>
                  )}
                  {m.text}
                </div>
              </div>
            ))}

            {loading && (
              <div style={{ display: 'flex', justifyContent: 'flex-start' }}>
                <div style={{ backgroundColor: '#F1F5F9', padding: '0.75rem 1rem', borderRadius: 'var(--radius-md)', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Analysing telemetry & generating root-cause narrative...
                </div>
              </div>
            )}
          </div>

          {/* Chat Input */}
          <form
            onSubmit={handleSend}
            style={{
              padding: '1rem',
              borderTop: '1px solid var(--border-color)',
              display: 'flex',
              gap: '0.75rem',
              backgroundColor: 'white',
            }}
          >
            <input
              type="text"
              className="form-control"
              placeholder="Ask Sentinel AI about root causes, queue spikes, or log correlations..."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
            />
            <button type="submit" className="btn btn-primary" disabled={loading || !prompt.trim()}>
              <Send size={16} /> Send
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
