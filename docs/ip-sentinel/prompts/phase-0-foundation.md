# Phase 0 — Foundation + Governance + UI Shell

> **How to use:** open a fresh Claude Code session on the `IPSentinel` branch and paste everything
> below (from "You are working on…" through the ACCEPTANCE block).

---

You are working on the **IPSentinel** branch of the Island Pacific Monitoring suite. You are building
**IP Sentinel**, a central, multi-tenant, **read-only** AIOps platform on top of the existing 21
monitor JARs — **do not modify those monitors**. Stack: backend **Java 17 + Spring Boot**
(`aiops-platform/`), frontend **React + TypeScript + uPlot** (`aiops-web/`), store **PostgreSQL 18 +
pgvector**, LLM via a pluggable **`LlmProvider`** (Claude API *and* a local OpenAI-compatible model,
with routing + fallback). UI brand: primary blue `#0057B8`, accent amber `#F5A300`, white background.
Every LLM call passes through redaction + audit + cost metering. **Read-only only** — no remediation,
no writes to IBM i/Windows, no auto threshold edits.

**GOAL:** Stand up the platform foundation, governance spine, and UI shell.

**DELIVERABLES**

Backend (`aiops-platform/`, new Spring Boot module):
- Postgres 18 schema via Flyway; enable `pgvector`; JPA entities: `tenants`, `users`, `roles`,
  `monitors`, `alerts`, `incidents`, `incident_signals`, `correlations`, `incident_timeline`,
  `agent_runs`, `tool_calls`, `feedback`, `kb_docs`, `kb_chunks(embedding vector)`, `forecasts`,
  `threshold_recommendations`, `cost_ledger`, `model_config`, `integration_config`.
- Azure AD OIDC SSO + RBAC; per-tenant data isolation enforced at the query layer.
- Query gateway: tenant-scoped PromQL + LogQL proxy; every query audited; deny cross-tenant.
- `LlmProvider` interface + `ClaudeApiProvider` (Messages API, native tools) + `LocalModelProvider`
  (OpenAI-compatible), with routing + fallback. Keys via `CredentialProtector`/DPAPI or Key Vault.
- Tool registry (read-only): `promql_query`, `loki_query`, `ibmi_sql` (SELECT-only — reuse the JDBC
  pattern from `ibmsqlthresholdmonitoring/SqlThresholdService.java`), `service_status`, `read_config`
  (secrets redacted), `kb_search` (stub returning pgvector matches).
- Governance filters wrapping every LLM call: audit → `agent_runs`/`tool_calls`; PII/secret
  redaction before send; cost/token metering → `cost_ledger` with per-tenant quotas.
- REST + WebSocket scaffolding; health endpoints.

Frontend (`aiops-web/`, new React+TS SPA):
- Auth/SSO, tenant switcher, app nav shell; brand palette (blue `#0057B8` / amber `#F5A300`, white bg).
- Dashboards module (uPlot) reaching parity with `grafana-dashboards/windows-monitor.json` and
  `win-service-monitor.json`, fed ONLY through the platform query gateway.

Ops:
- Prometheus remote-write/federation config (`resources/prometheus/prometheus.yml`); installer role
  "AIOps Platform"; drop Grafana from the bundle.

**ACCEPTANCE**
- App boots against Postgres+pgvector (Testcontainers ok); migrations apply cleanly.
- `ibmi_sql` rejects any non-SELECT; redaction strips seeded secrets before any provider call;
  an audit row is written per agent/tool call; gateway denies a cross-tenant PromQL query.
- SPA shell logs in via SSO and renders the two pilot dashboards at parity.
- The 21 existing monitors are unchanged (git diff shows only new modules + prometheus.yml/installer).
