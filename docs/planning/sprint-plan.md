# IP Sentinel — Sprint Plan

Maps the [task catalog](task-catalog.md) (28 build chunks) into 2-week sprints. ⭐ = MVP.
Assumes a small team building backend + frontend in parallel. Sequencing follows chunk dependencies;
adjust velocity to your team.

## Milestones
- **M1 — Foundation (end Sprint 2):** platform boots, auth, isolation, gateway, governance spine.
- **M2 — MVP (end Sprint 4):** ⭐ Fleet Overview + pilot dashboards + cross-platform correlation demo
  for one pilot tenant (fleet-view-first + the flagship demo).
- **M3 — Product (end Sprint 6):** integrations, incident lifecycle, ChatOps (Pro).
- **M4 — Intelligence (end Sprint 8):** adaptive/forecasting/reporting.
- **M5 — Production-ready (end Sprint 11):** Phase 4 hardening complete + Definition of Done green
  (see [ENTERPRISE_READINESS.md](../ip-sentinel/ENTERPRISE_READINESS.md)). SOC 2 evidence then accrues over the audit period.

> **Frozen scope: 37 chunks (Phases 0–4).**

## Sprint 0 — Inception (setup)
Repo scaffolding for `aiops-platform/` + `aiops-web/`, CI pipeline, Postgres 18 + pgvector env,
Testcontainers harness, coding standards wired. No feature chunks.

## Sprint 1 — Platform foundation
- 0.1 Data model & migrations
- 0.2 Authentication (staff + customer)
- 0.4 Query gateway
- 0.5 LLM provider layer (AI-optional)

## Sprint 2 — Governance & access  → **M1**
- 0.3 RBAC, tenant isolation & entitlement
- 0.6 Read-only tool registry
- 0.7 Governance spine (audit, redaction, cost, SOC 2)
- 0.8 SPA shell

## Sprint 3 — Fleet view (MVP core) ⭐
- 0.9 Fleet Overview — capacity headroom ⭐
- 0.10 Pilot dashboards (uPlot parity) ⭐
- 0.11 Data-plane & packaging

## Sprint 4 — Correlation MVP ⭐  → **M2**
- 1.1 Correlation engine (rule-based) ⭐
- 1.2 Triage / root-cause agent ⭐
- 1.3 Incident persistence & API
- 1.4 Incident Console ⭐
- 1.8 Alert-noise KPI + correlation demo seed ⭐

## Sprint 5 — Workflow & integrations
- 1.5 Lightweight incident lifecycle
- 1.6 Microsoft Teams integration ⭐
- 1.7 ITSM two-way sync
- 0.12 Config-driven dashboard engine (dashboards for all monitor types; post-pilot)

## Sprint 6 — ChatOps (Pro)  → **M3**
- 2.1 Assistant agent
- 2.2 Assistant SPA
- 2.3 Teams ChatOps
- 2.4 Pro-tier gating + customer data-only scope

## Sprint 7 — Adaptive & forecasting
- 3.1 Baseline & anomaly detection (statistical)
- 3.2 Forecasting / capacity (feeds fleet view)
- 3.3 Knowledge Curator (RAG ingestion)

## Sprint 8 — Reporting & intelligence  → **M4**
- 3.4 Reporting / Digest agent
- 3.5 Insights (SPA)

## Sprint 9 — Hardening I (resilience & ops)
- 4.1 DR / Backup & Restore
- 4.2 Platform self-monitoring & observability
- 4.3 Secrets management & rotation
- 4.4 Reliability & resilience hardening

## Sprint 10 — Hardening II (quality & security)
- 4.5 Performance & load testing
- 4.6 Security testing & hardening
- 4.7 AI safety evaluation & red-team

## Sprint 11 — Compliance & sign-off  → **M5 (Production-ready)**
- 4.8 Compliance (SOC 2) & accessibility
- Run the **Definition of Done: Production-Ready** checklist ([ENTERPRISE_READINESS.md](../ip-sentinel/ENTERPRISE_READINESS.md)); remediate gaps; sign off.

## Notes
- **MVP demo** is deliverable at end of Sprint 4: one pilot tenant, fleet view + a single
  cross-platform incident with AI root cause, noise-reduction KPI visible.
- Backend/frontend chunks within a sprint run in parallel; UI chunks (0.8→0.9/0.10, 1.4, 2.2, 3.5)
  depend on their backend counterparts landing first in the sprint.
