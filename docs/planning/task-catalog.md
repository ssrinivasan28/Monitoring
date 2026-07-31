# IP Sentinel — Task Catalog

The full backlog: 28 ticket-sized build chunks derived from the [build prompts](../ip-sentinel/prompts/README.md).
Each row is one build session / PR. ⭐ = MVP. Paste
[`00-shared-preamble.md`](../ip-sentinel/prompts/00-shared-preamble.md) + the chunk to build.

## Phase 0 — Foundation, governance, fleet view
| ID | Task | MVP | Depends on | Acceptance (summary) |
|---|---|---|---|---|
| 0.1 | [Data model & migrations](../ip-sentinel/prompts/phase-0/0.1-data-model-migrations.md) | | — | Migrations apply on Postgres 18 + pgvector; incident + vector round-trip |
| 0.2 | [Authentication (staff + customer)](../ip-sentinel/prompts/phase-0/0.2-authentication.md) | | 0.1 | Azure AD + invite/MFA + federated login resolve correct tenant/role |
| 0.3 | [RBAC, tenancy & entitlement](../ip-sentinel/prompts/phase-0/0.3-rbac-tenancy-entitlement.md) | | 0.2 | Cross-tenant denied; Basic blocked from Assistant endpoints |
| 0.4 | [Query gateway](../ip-sentinel/prompts/phase-0/0.4-query-gateway.md) | | 0.1 | Tenant label injected; cross-tenant rejected; every query audited |
| 0.5 | [LLM provider layer](../ip-sentinel/prompts/phase-0/0.5-llm-provider.md) | | — | Claude & local same flow; disabled mode returns clean "AI unavailable" |
| 0.6 | [Read-only tool registry](../ip-sentinel/prompts/phase-0/0.6-tool-registry.md) | | 0.4, 0.5 | `ibmi_sql` SELECT-only; tools tenant-scoped + audited |
| 0.7 | [Governance spine](../ip-sentinel/prompts/phase-0/0.7-governance-spine.md) | | 0.1, 0.5 | Immutable audit; redaction strips secrets; cost metered + quota |
| 0.8 | [SPA shell](../ip-sentinel/prompts/phase-0/0.8-spa-shell.md) | | 0.2, 0.3 | Scoped shell; tier-gated nav; responsive; served by backend |
| 0.9 | [Fleet Overview — capacity headroom](../ip-sentinel/prompts/phase-0/0.9-fleet-overview.md) | ⭐ | 0.4, 0.8 | Weighted-blend headroom + hard critical override; no LLM |
| 0.10 | [Pilot dashboards (uPlot parity)](../ip-sentinel/prompts/phase-0/0.10-pilot-dashboards.md) | ⭐ | 0.4, 0.8 | SPA panels match Grafana JSON for pilot monitors |
| 0.11 | [Data-plane & packaging](../ip-sentinel/prompts/phase-0/0.11-data-plane-packaging.md) | | 0.9, 0.10 | Remote-write w/ tenant label; WinSW install; monitors unchanged |

## Phase 1 — Correlation, triage, integrations
| ID | Task | MVP | Depends on | Acceptance (summary) |
|---|---|---|---|---|
| 1.1 | [Correlation engine (rule-based)](../ip-sentinel/prompts/phase-1/1.1-correlation-engine.md) | ⭐ | 0.4 | Many signals → one incident; runs LLM-off; no cross-tenant merge |
| 1.2 | [Triage / root-cause agent](../ip-sentinel/prompts/phase-1/1.2-triage-agent.md) | ⭐ | 1.1, 0.6 | Root cause + cited evidence + runbook; degrades LLM-off |
| 1.3 | [Incident persistence & API](../ip-sentinel/prompts/phase-1/1.3-incident-api.md) | | 1.1 | Ranked, tenant-scoped list/detail; customer sees only theirs |
| 1.4 | [Incident Console (SPA)](../ip-sentinel/prompts/phase-1/1.4-incident-console.md) | ⭐ | 1.3, 0.8 | Renders ranked incidents + detail; functions with no AI narrative |
| 1.5 | [Lightweight incident lifecycle](../ip-sentinel/prompts/phase-1/1.5-incident-lifecycle.md) | | 1.3 | Ack/assign/resolve + notes; timelined + audited; maps to ITSM |
| 1.6 | [Microsoft Teams integration](../ip-sentinel/prompts/phase-1/1.6-teams-integration.md) | ⭐ | 1.3 | Posts once per incident to correct tenant channel; updates on status |
| 1.7 | [ITSM two-way sync](../ip-sentinel/prompts/phase-1/1.7-itsm-sync.md) | | 1.5 | One ticket per incident; status syncs both ways; idempotent |
| 1.8 | [Alert-noise KPI + demo seed](../ip-sentinel/prompts/phase-1/1.8-kpi-and-demo.md) | ⭐ | 1.1 | KPI computes; seed yields one cross-platform incident w/ root cause |

## Phase 2 — ChatOps (Pro)
| ID | Task | MVP | Depends on | Acceptance (summary) |
|---|---|---|---|---|
| 2.1 | [Assistant agent](../ip-sentinel/prompts/phase-2/2.1-assistant-agent.md) | | 0.6 | Correct ranked answer from real queries, cited; no cross-tenant |
| 2.2 | [Assistant SPA](../ip-sentinel/prompts/phase-2/2.2-assistant-spa.md) | | 2.1, 0.8 | Streams response + tool trace; hidden for Basic |
| 2.3 | [Teams ChatOps](../ip-sentinel/prompts/phase-2/2.3-teams-chatops.md) | | 2.1, 1.6 | Pro user answered in Teams; Basic told it's Pro |
| 2.4 | [Pro-tier gating + data-only scope](../ip-sentinel/prompts/phase-2/2.4-tier-gating-scope.md) | | 2.1, 0.3 | Basic blocked server-side; customer Assistant answers only from their data |

## Phase 3 — Adaptive, forecasting, reporting
| ID | Task | MVP | Depends on | Acceptance (summary) |
|---|---|---|---|---|
| 3.1 | [Baseline & anomaly (statistical)](../ip-sentinel/prompts/phase-3/3.1-baseline-anomaly.md) | | 0.4 | Benign spike → recommendation not alert; runs LLM-off |
| 3.2 | [Forecasting / capacity](../ip-sentinel/prompts/phase-3/3.2-forecasting-capacity.md) | | 0.4, 0.9 | Trend → forecast date; escalates fleet view; runs LLM-off |
| 3.3 | [Knowledge Curator (RAG)](../ip-sentinel/prompts/phase-3/3.3-knowledge-curator.md) | | 0.1, 0.6 | Uploaded runbook retrievable per-tenant; resolved incident matched |
| 3.4 | [Reporting / Digest agent](../ip-sentinel/prompts/phase-3/3.4-reporting-digest.md) | | 1.8, 3.2 | Daily/weekly/monthly reports per [REPORTING.md](../ip-sentinel/REPORTING.md); templated fallback LLM-off |
| 3.5 | [Insights (SPA)](../ip-sentinel/prompts/phase-3/3.5-insights-spa.md) | | 3.1, 3.2, 0.8 | Lists anomalies/forecasts/recs; accept/dismiss writes feedback only |

**28 tasks · MVP = the ⭐ set (Phase 0 fleet view + Phase 1 correlation showcase).**
