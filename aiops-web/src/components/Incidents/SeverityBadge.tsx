import React from 'react';
import { AlertOctagon, AlertTriangle, AlertCircle, Info } from 'lucide-react';
import { IncidentSeverity } from '../../services/incidentService';

/**
 * Severity affordance used across the incident console. Deliberately icon + label + color, never
 * color alone, so it stays legible for colorblind users and in monochrome print/screenshot contexts.
 */
const SEVERITY_META: Record<IncidentSeverity, { label: string; color: string; bg: string; icon: React.ReactNode }> = {
  critical: { label: 'CRITICAL', color: '#DC2626', bg: '#FEF2F2', icon: <AlertOctagon size={13} /> },
  high: { label: 'HIGH', color: '#EA580C', bg: '#FFF7ED', icon: <AlertTriangle size={13} /> },
  medium: { label: 'MEDIUM', color: '#D97706', bg: '#FFFBEB', icon: <AlertCircle size={13} /> },
  low: { label: 'LOW', color: '#2563EB', bg: '#EFF6FF', icon: <Info size={13} /> },
};

export const SeverityBadge: React.FC<{ severity: IncidentSeverity }> = ({ severity }) => {
  const meta = SEVERITY_META[severity] || SEVERITY_META.low;
  return (
    <span
      className="badge"
      style={{ backgroundColor: meta.bg, color: meta.color, border: `1px solid ${meta.color}33`, fontWeight: 700 }}
    >
      {meta.icon} {meta.label}
    </span>
  );
};

/** Left-edge stripe for row/card containers — the "form, not color alone" cue pairs with the pill above. */
export const severityStripeStyle = (severity: IncidentSeverity): React.CSSProperties => ({
  borderLeft: `4px solid ${SEVERITY_META[severity]?.color || SEVERITY_META.low.color}`,
});
