# IP Sentinel — Sprint Plan

Maps the [task catalog](task-catalog.md) (28 build chunks) into 2-week sprints. ⭐ = MVP.
Assumes a small team building backend + frontend in parallel. Sequencing follows chunk dependencies;
adjust velocity to your team.

## Milestones
- **M1 — Foundation (end Sprint 2):** platform boots, auth, isolation, gateway, governance spine.
- **M2 — MVP (end Sprint 4):** ⭐ Fleet Overview + pilot dashboards + cross-platform correlation demo
  for one pilot tenant (fleet-view-first + the flagship demo).
- **M3 — Product (end Sprint 6):** integrations, incident lifecycle, ChatOps (Pro).
- **M4 — Intelligence (end Sprint 8):** adaptive/forecasting/reporting + enterprise-readiness gates.

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

## Sprint 6 — ChatOps (Pro)  → **M3**
- 2.1 Assistant agent
- 2.2 Assistant SPA
- 2.3 Teams ChatOps
- 2.4 Pro-tier gating + customer data-only scope

## Sprint 7 — Adaptive & forecasting
- 3.1 Baseline & anomaly detection (statistical)
- 3.2 Forecasting / capacity (feeds fleet view)
- 3.3 Knowledge Curator (RAG ingestion)

## Sprint 8 — Reporting & readiness  → **M4**
- 3.4 Reporting / Digest agent
- 3.5 Insights (SPA)
- Enterprise-readiness gates: DR/backup + restore test, platform self-monitoring, secrets rotation,
  audit immutability, security/pen-test pass (see [ENTERPRISE_READINESS.md](../ip-sentinel/ENTERPRISE_READINESS.md)).

## Notes
- **MVP demo** is deliverable at end of Sprint 4: one pilot tenant, fleet view + a single
  cross-platform incident with AI root cause, noise-reduction KPI visible.
- Backend/frontend chunks within a sprint run in parallel; UI chunks (0.8→0.9/0.10, 1.4, 2.2, 3.5)
  depend on their backend counterparts landing first in the sprint.
