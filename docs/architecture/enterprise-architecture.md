# IP Sentinel — Enterprise Architecture

**Status:** Proposed · **Scope:** the central AIOps platform layered on the existing monitoring suite.
Companion docs: [product plan](../ip-sentinel/ENHANCEMENT_PLAN.md) ·
[decisions](../ip-sentinel/PRODUCT_DECISIONS.md) · [deployment](../ip-sentinel/DEPLOYMENT.md) ·
[enterprise readiness](../ip-sentinel/ENTERPRISE_READINESS.md) · [ADRs](../adr/README.md).

## 1. Purpose & principles
IP Sentinel adds a central, multi-tenant, **read-only** AI operations layer on top of the existing
21 monitor JARs. Principles:
- **Additive, not invasive** — the monitors are the sensor layer and are never modified.
- **Read-only** — observe, correlate, explain, recommend. No writes to IBM i/Windows.
- **AI-optional** — the platform is fully functional with the LLM unavailable; AI enriches, it isn't load-bearing.
- **Strict tenant isolation** — no cross-tenant data mixing, including model learning.
- **Governance first** — audit, redaction, cost, RBAC are foundational, not add-ons.

## 2. System context (C4 L1)
```
Staff (Azure AD SSO) ─┐                       ┌─ IBM i systems (jt400)  ─┐
Customers (invite/MFA)─┼─▶ IP Sentinel ◀─────┤  Windows systems (OSHI)  ├─ 21 monitors (unchanged)
                       │   (central platform) │  IFS/FTPS/URL/SSL        ─┘
External systems ◀─────┘   Teams · ServiceNow/Jira · PagerDuty · SIEM · Email
```
Actors: Island Pacific staff and end customers (tenant-scoped). Upstream: the monitors emit
Prometheus metrics + Loki logs. Downstream: Teams, ITSM, paging, SIEM, email.

## 3. Container view (C4 L2)
```
Existing monitors ──metrics/logs──▶ Central data plane (Prometheus/Thanos + Loki, storage only)
                                          │ queried ONLY via the platform API
                                          ▼
   aiops-platform (Java 17 + Spring Boot)
     • Query gateway (tenant-scoped, audited PromQL/LogQL)
     • Correlation engine (rule-based)  • Agent orchestrator (LLM + tools + RAG)
     • Governance spine (audit · redaction · cost · RBAC/entitlement)
     • Integration adapters (Teams · ITSM · paging · SIEM · email)
     • REST + WebSocket
        │                                   │
   aiops-web (React/TS SPA, responsive) ◀───┘   PostgreSQL 18 + pgvector
   (the only UI; Grafana retired)
```

## 4. Components
| Component | Responsibility |
|---|---|
| **Query gateway** | Sole read path to Prometheus/Loki; injects tenant label; audits; denies cross-tenant |
| **Correlation engine** | Rule-based grouping of breaches → incidents (no LLM) |
| **Agent orchestrator** | Runs the read-only agents over the tool registry + RAG |
| **Governance spine** | Audit (immutable), PII/secret redaction, cost metering/quotas, RBAC + entitlement |
| **Integration adapters** | Teams (first), ServiceNow/Jira (2-way sync), PagerDuty/Opsgenie, SIEM/webhook, email |
| **SPA** | Fleet Overview, Dashboards, Log Explorer, Incident Console, Assistant, Insights, KB, Admin |
| **Data store** | PostgreSQL 18 + pgvector (incidents, memory, audit, cost, reports) |

## 5. Multi-tenancy & isolation
- Tenant = `ClientInstanceId`; every metric series, log stream, and DB row is tenant-labeled.
- Isolation is enforced **centrally** (gateway + repository filters), deny-by-default — not per-query opt-in.
- Staff may span tenants (RBAC); customers are pinned to their own. **No cross-tenant learning.**
- Scale target: **< 25 tenants** → single central Prometheus with federation (no sharding).
- **Data-source resolution:** default is one **central** Prometheus/Loki (tenants separated by label,
  URL in `application.yml`). Each tenant may also register **one or more** `tenant_datasource` endpoints
  (managed in the Admin UI) — added over time as monitoring grows. The query gateway **fans out** across
  a tenant's enabled sources of the needed kind, merges results, and **falls back to central** when none
  exist; one unreachable source degrades gracefully.

## 6. Security architecture
- **AuthN:** staff via Azure AD **OIDC SSO**; customers via **invite + MFA**, optional federated per-tenant SSO.
- **AuthZ (two dimensions):** **RBAC** (role) × **entitlement** (Basic/Pro tier). Pro unlocks the AI Assistant.
- **Secrets:** DPAPI (`CredentialProtector`) for on-prem; Azure Key Vault / Vault with rotation for enterprise.
- **Transport:** TLS everywhere; secured remote-write (mTLS/token); scoped egress on the Claude path.
- **Compliance target:** **SOC 2 Type II** — controls/evidence from Phase 0.

## 7. AI & agent architecture
- **`LlmProvider`** abstraction → Claude API + local (OpenAI-compatible), routing + fallback, **disabled mode**.
- **Read-only tool registry:** `promql_query`, `loki_query`, `ibmi_sql` (SELECT-only), `service_status`,
  `read_config` (redacted), `kb_search`.
- **Agents:** Correlation, Triage/Root-Cause, Assistant (Pro), Anomaly/Baseline, Forecasting/Capacity,
  Knowledge Curator, Reporting/Digest. Supporting: Orchestrator, Notification Router, Governance guard.
- **AI safety:** all tool data treated as untrusted (prompt-injection defense); outputs validated;
  answers cite evidence/queries; every call redacted + audited + cost-metered.
- **Statistical, not LLM:** anomaly/baseline and forecasting run without the model.

## 8. Data architecture
- **Store:** PostgreSQL 18 + pgvector. Tables: tenants, users, roles, entitlements, monitors, alerts,
  incidents (+signals, correlations, timeline), agent_runs, tool_calls, feedback, kb_docs/kb_chunks
  (embeddings), forecasts, threshold_recommendations, cost_ledger, model_config, integration_config, reports.
- **Time-series:** Prometheus/Thanos (metrics), Loki (logs) — storage engines behind the API.
- **Retention: 13 months** (YoY + monthly SLA). Postgres retention sized to match.
- **SLA measure:** composite health (reachable + SLA-critical components running), 24×7, no maintenance
  exclusions, **99.9% default / per-tenant override** (see [REPORTING.md](../ip-sentinel/REPORTING.md)).

## 9. Integration architecture
Teams-first (Graph). ITSM (ServiceNow/Jira) is the incident **system-of-record** with two-way status
sync; IP Sentinel owns a lightweight lifecycle (ack/assign/resolve/notes). Paging + SIEM/webhook as
adapters. Idempotent delivery; per-tenant config in `integration_config`.

## 10. Deployment & quality attributes
- **Packaging:** Windows Server + WinSW (primary, single service serves API + SPA), containers for scale/HA.
- **Topologies:** on-prem per-customer (default), SaaS central, hybrid.
- **HA/scale:** stateless app tier scales horizontally; Postgres primary+replica; Thanos for long-term metrics.
- **NFRs:** see [ENTERPRISE_READINESS.md](../ip-sentinel/ENTERPRISE_READINESS.md) (SLAs, DR/backup, AI
  safety, self-monitoring, compliance) and the Definition-of-Done gates.
