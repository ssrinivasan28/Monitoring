import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Bot, Sparkles, Lock, Send, ShieldAlert, AlertCircle, Loader2, Copy, Check } from 'lucide-react';
import { streamAssistantChat } from '../services/apiClient';
import { AssistantAnswerPayload, AssistantCitation } from '../types';

interface ToolTraceEntry {
  tool: string;
  query: string;
  summary?: string;
}

type ChatMessage =
  | { id: string; sender: 'user'; text: string }
  | {
      id: string;
      sender: 'assistant';
      status: 'streaming' | 'done' | 'error';
      trace: ToolTraceEntry[];
      answer?: string;
      citations?: AssistantCitation[];
      aiAvailable?: boolean;
      insufficient?: boolean;
      errorText?: string;
    };

type AssistantMessage = Extract<ChatMessage, { sender: 'assistant' }>;

export const AiAssistantPage: React.FC = () => {
  const { entitlementTier } = useAuth();
  const isProTier = entitlementTier === 'PRO';

  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'greeting',
      sender: 'assistant',
      status: 'done',
      trace: [],
      answer:
        'Hello! I am your Sentinel AI Assistant. I can help analyze cross-system anomalies, correlate IBM i and Windows logs, and assist with root-cause investigations.',
      citations: [],
      aiAvailable: true,
      insufficient: false,
    },
  ]);
  const [loading, setLoading] = useState(false);
  const [entitlementDenied, setEntitlementDenied] = useState(false);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const showUpsell = !isProTier || entitlementDenied;

  const copyToClipboard = async (key: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedKey(key);
      setTimeout(() => setCopiedKey((k) => (k === key ? null : k)), 1500);
    } catch (_) {
      // Clipboard API unavailable/denied - nothing to fall back to, just skip the feedback.
    }
  };

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim() || !isProTier || loading) return;

    const userText = prompt;
    const userMsgId = crypto.randomUUID();
    const assistantMsgId = crypto.randomUUID();

    setMessages((prev) => [
      ...prev,
      { id: userMsgId, sender: 'user', text: userText },
      { id: assistantMsgId, sender: 'assistant', status: 'streaming', trace: [] },
    ]);
    setPrompt('');
    setLoading(true);

    const updateAssistant = (updater: (msg: AssistantMessage) => AssistantMessage) => {
      setMessages((prev) =>
        prev.map((m) => (m.id === assistantMsgId && m.sender === 'assistant' ? updater(m) : m))
      );
    };

    try {
      await streamAssistantChat(userText, (evt) => {
        if (evt.event === 'tool_call') {
          updateAssistant((m) => ({
            ...m,
            trace: [...m.trace, { tool: evt.data.tool, query: evt.data.query }],
          }));
        } else if (evt.event === 'tool_result') {
          updateAssistant((m) => {
            const trace = [...m.trace];
            for (let i = trace.length - 1; i >= 0; i--) {
              if (trace[i].tool === evt.data.tool && trace[i].summary === undefined) {
                trace[i] = { ...trace[i], summary: evt.data.summary };
                break;
              }
            }
            return { ...m, trace };
          });
        } else if (evt.event === 'answer') {
          const payload: AssistantAnswerPayload = evt.data;
          updateAssistant((m) => ({
            ...m,
            status: 'done',
            answer: payload.answer,
            citations: payload.citations || [],
            aiAvailable: payload.aiAvailable,
            insufficient: payload.insufficient,
          }));
        }
      });
    } catch (err: any) {
      if (err?.status === 403) {
        // Stale client-side entitlement state (e.g. a downgrade that hasn't refreshed yet) - the
        // controller's @RequiresEntitlement check rejected the stream before it ever opened. Drop the
        // in-flight placeholder and fall back to the same upsell state a Basic tenant sees.
        setMessages((prev) => prev.filter((m) => m.id !== assistantMsgId));
        setEntitlementDenied(true);
      } else {
        updateAssistant((m) => ({
          ...m,
          status: 'error',
          errorText:
            err?.message ||
            'The LLM provider is currently unavailable. Statistical forecasting and threshold rules remain active.',
        }));
      }
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

      {/* Gating Banner for Basic Tier (or a stale-entitlement 403 on the stream itself) */}
      {showUpsell ? (
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
        <div className="card assistant-chat-card" style={{ display: 'flex', flexDirection: 'column', padding: 0, overflow: 'hidden' }}>
          {/* Chat Messages */}
          <div style={{ flex: 1, padding: '1.5rem', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {messages.map((m) => (
              <div key={m.id} style={{ display: 'flex', justifyContent: m.sender === 'user' ? 'flex-end' : 'flex-start' }}>
                {m.sender === 'user' ? (
                  <div
                    style={{
                      maxWidth: '80%',
                      padding: '0.85rem 1.1rem',
                      borderRadius: 'var(--radius-md)',
                      backgroundColor: '#0057B8',
                      color: 'white',
                      fontSize: '0.9rem',
                      lineHeight: 1.5,
                      wordBreak: 'break-word',
                    }}
                  >
                    {m.text}
                  </div>
                ) : (
                  <div
                    style={{
                      maxWidth: '80%',
                      padding: '0.85rem 1.1rem',
                      borderRadius: 'var(--radius-md)',
                      backgroundColor: '#F1F5F9',
                      color: 'var(--text-main)',
                      fontSize: '0.9rem',
                      lineHeight: 1.5,
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem', marginBottom: '0.4rem' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', fontWeight: 700, color: '#0057B8' }}>
                        <Sparkles size={12} color="#F5A300" /> SENTINEL AI
                      </div>
                      {m.status === 'done' && m.answer && (
                        <button
                          type="button"
                          onClick={() => copyToClipboard(`${m.id}:answer`, m.answer || '')}
                          className="btn btn-secondary"
                          style={{ padding: '0.15rem 0.4rem', fontSize: '11px', gap: '0.25rem' }}
                          aria-label="Copy answer"
                        >
                          {copiedKey === `${m.id}:answer` ? <Check size={11} /> : <Copy size={11} />}
                        </button>
                      )}
                    </div>

                    {m.status === 'streaming' && (
                      <div>
                        {m.trace.length === 0 && (
                          <div style={{ color: 'var(--text-muted)' }}>Analysing telemetry & querying live data...</div>
                        )}
                        {m.trace.length > 0 && (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                            {m.trace.map((t, idx) => (
                              <div key={idx} className="assistant-trace-row">
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 600, marginBottom: '0.2rem' }}>
                                  {t.summary === undefined ? (
                                    <Loader2 size={12} className="spin" color="#0057B8" />
                                  ) : (
                                    <Check size={12} color="#16a34a" />
                                  )}
                                  {t.tool}
                                </div>
                                <div className="assistant-trace-query">{t.query}</div>
                                {t.summary && (
                                  <div style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>{t.summary}</div>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    )}

                    {m.status === 'error' && <div>[Analysis Error]: {m.errorText}</div>}

                    {m.status === 'done' && (
                      <div>
                        {m.aiAvailable === false && (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', fontWeight: 700, color: '#dc2626', marginBottom: '0.5rem' }}>
                            <ShieldAlert size={13} /> AI UNAVAILABLE
                          </div>
                        )}
                        {m.insufficient && (
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', fontSize: '0.75rem', fontWeight: 700, color: '#d97706', marginBottom: '0.5rem' }}>
                            <AlertCircle size={13} /> INSUFFICIENT EVIDENCE
                          </div>
                        )}
                        <div style={{ wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>{m.answer}</div>

                        {m.citations && m.citations.length > 0 && (
                          <>
                            <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em', margin: '0.75rem 0 0.5rem' }}>
                              Queries Run
                            </div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                              {m.citations.map((c, idx) => (
                                <div key={idx} className="assistant-citation-row">
                                  <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '0.5rem', marginBottom: '0.2rem' }}>
                                    <div style={{ fontWeight: 600 }}>{c.tool}</div>
                                    <button
                                      type="button"
                                      onClick={() => copyToClipboard(`${m.id}:citation:${idx}`, c.query)}
                                      className="btn btn-secondary"
                                      style={{ padding: '0.1rem 0.35rem', fontSize: '11px' }}
                                      aria-label="Copy query"
                                    >
                                      {copiedKey === `${m.id}:citation:${idx}` ? <Check size={10} /> : <Copy size={10} />}
                                    </button>
                                  </div>
                                  <div className="assistant-citation-query">{c.query}</div>
                                  {c.summary && <div style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>{c.summary}</div>}
                                </div>
                              ))}
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
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
