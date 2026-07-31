# Shared Preamble — prepend to every chunk

Paste this block first in a fresh Claude Code session on the `IPSentinel` branch, then paste the
chunk you're building.

---

You are working on the **IPSentinel** branch of the Island Pacific Monitoring suite, building
**IP Sentinel** — a central, multi-tenant, **read-only** AIOps **product** layered on top of the
existing 21 monitor JARs. **Do not modify those monitors**; they are the sensor layer and already
emit Prometheus metrics + Loki logs.

**Stack**
- Backend: **Java 17 + Spring Boot** (`aiops-platform/`)
- Frontend: **React + TypeScript + uPlot**, **responsive** (`aiops-web/`) — replaces Grafana
- Store: **PostgreSQL 18 + pgvector**
- LLM: pluggable **`LlmProvider`** — Claude API *and* a local OpenAI-compatible model, with routing + fallback
- Brand: **Island Pacific** — primary blue `#0057B8`, accent amber `#F5A300`, white background (single brand)

**Product context (decisions — see ../PRODUCT_DECISIONS.md)**
- **Customer-facing product:** Island Pacific staff *and* end customers log in. **Strict per-tenant
  isolation** everywhere; **no cross-tenant data mixing**, including for model learning.
- **Tiered Basic / Pro:** Basic = fleet health, dashboards, incidents, reports, alerts, history.
  **Pro unlocks the AI Assistant only.** Entitlement gating sits alongside RBAC.
- **Auth:** staff via Azure AD SSO; customers via invite-based accounts + MFA, optional federated per-tenant SSO.
- **Scale:** under 25 tenants. **Retention: 13 months.**
- **Fleet-view-first:** the unified fleet pane is the top priority; **capacity headroom** is the
  at-a-glance health signal.
- **Differentiator:** AI **correlation + root cause** across IBM i + Windows (flagship demo).
- **Primary KPI:** **alert-noise reduction** (raw alerts → correlated incidents ratio).
- **Compliance target:** **SOC 2 Type II** — design controls + collect evidence from Phase 0.

**Hard rules**
- **Read-only only** — no remediation, no writes to IBM i/Windows, no auto threshold edits.
- **AI-optional / graceful degradation:** the platform MUST work with the LLM unavailable —
  dashboards, fleet health, threshold-detected incidents, alerts, integrations, forecasting and
  anomaly detection (both statistical, no LLM) all function; only root-cause narrative, the AI
  Assistant, and AI-written report prose degrade to templated/disabled.
- Every LLM call passes through **redaction + audit + cost metering**.
- Secrets via the existing **DPAPI** (`CredentialProtector`) or Key Vault — never plaintext.
