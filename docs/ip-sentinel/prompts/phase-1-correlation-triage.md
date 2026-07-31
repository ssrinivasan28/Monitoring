# Phase 1 — Correlation & Triage Engine + Integrations ⭐

> **How to use:** open a fresh Claude Code session on the `IPSentinel` branch and paste everything
> below. Assumes Phase 0 is merged.

---

You are working on the **IPSentinel** branch of the Island Pacific Monitoring suite. You are building
**IP Sentinel**, a central, multi-tenant, **read-only** AIOps platform on top of the existing 21
monitor JARs — **do not modify those monitors**. Stack: backend **Java 17 + Spring Boot**
(`aiops-platform/`), frontend **React + TypeScript + uPlot** (`aiops-web/`), store **PostgreSQL 18 +
pgvector**, LLM via a pluggable **`LlmProvider`** (Claude API *and* a local OpenAI-compatible model,
with routing + fallback). UI brand: primary blue `#0057B8`, accent amber `#F5A300`, white background.
Every LLM call passes through redaction + audit + cost metering. **Read-only only** — no remediation,
no writes to IBM i/Windows, no auto threshold edits.

**GOAL:** Turn breaches into single, AI-enriched, RAG-grounded incidents and route them out.

**DELIVERABLES**
- **Correlation Agent:** poll federated Prometheus alert-state metrics; group simultaneous breaches
  across monitors/platforms/tenants into one incident (persist `incidents` + `incident_signals` +
  `correlations` + `incident_timeline`).
- **Triage/Root-Cause Agent:** for each incident, gather context via tools, retrieve the matching
  runbook + top-K similar past incidents from pgvector (`kb_search`), and produce
  `{root_cause_hypothesis, evidence, severity, suggested_checks}`. Persist and expose via REST.
- **Notification Router + adapters:** ServiceNow/Jira (create+enrich ticket), PagerDuty/Opsgenie
  (page), Teams/Slack (post), SIEM/webhook (structured event). Dedupe + per-channel formatting.
  Keep the existing per-monitor email as fallback. Config in `integration_config`.
- **SPA Incident Console:** ranked incidents; detail view with signals, AI root cause + evidence,
  runbook, similar incidents, timeline, and "push to <channel>" actions.

**ACCEPTANCE**
- A forced multi-signal breach across two mock tenants yields ONE incident (not N), correctly
  scoped, with a populated root-cause + evidence and a matched runbook.
- Each configured channel fires once with full context; tenant isolation intact.
- All actions remain read-only advice; nothing is executed on IBM i/Windows.
