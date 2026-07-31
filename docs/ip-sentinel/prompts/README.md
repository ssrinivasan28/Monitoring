# IP Sentinel — Build Prompts (ticket-sized)

Each chunk below is a **single build session / PR**. Three ways to feed a chunk to Claude Code:

- **Slash command (easiest):** on the `IPSentinel` branch, run `/build-chunk 0.9` — it reads the
  preamble + chunk and builds to Acceptance. (Defined in `.claude/commands/build-chunk.md`.)
- **Point at the files:** *"Read `prompts/00-shared-preamble.md` and the chunk for 0.9, then implement it."*
- **Clipboard helper:** `./show-prompt.ps1 0.9` (or `show-prompt.sh 0.9`) copies preamble + chunk ready to paste.

Then build to the chunk's **Acceptance** criteria; open a PR; move to the next.

Build in order within a phase. Phases are sequential (1 assumes 0, etc.). The **MVP** is the ⭐ chunks.

> **Detail level:** `0.1` and every **⭐ MVP chunk** are **fully detailed** specs (context, scope,
> concrete deliverables, gotchas, tests, acceptance checklist, references). The remaining chunks are
> concise specs — ask to expand any to full detail when you reach it.

## Phase 0 — Foundation, governance, fleet view
| # | Chunk | |
|---|---|---|
| 0.1 | [Data model & migrations](phase-0/0.1-data-model-migrations.md) | |
| 0.2 | [Authentication (staff + customer)](phase-0/0.2-authentication.md) | |
| 0.3 | [RBAC, tenant isolation & Basic/Pro entitlement](phase-0/0.3-rbac-tenancy-entitlement.md) | |
| 0.4 | [Query gateway (audited PromQL/LogQL proxy)](phase-0/0.4-query-gateway.md) | |
| 0.5 | [LLM provider layer (Claude + local, AI-optional)](phase-0/0.5-llm-provider.md) | |
| 0.6 | [Read-only tool registry](phase-0/0.6-tool-registry.md) | |
| 0.7 | [Governance spine (audit, redaction, cost, SOC 2)](phase-0/0.7-governance-spine.md) | |
| 0.8 | [SPA shell (auth, tenants, brand, responsive)](phase-0/0.8-spa-shell.md) | |
| 0.9 | [Fleet Overview — capacity headroom](phase-0/0.9-fleet-overview.md) | ⭐ |
| 0.10 | [Pilot dashboards (uPlot parity)](phase-0/0.10-pilot-dashboards.md) | ⭐ |
| 0.11 | [Data-plane & packaging](phase-0/0.11-data-plane-packaging.md) | |

## Phase 1 — Correlation, triage, integrations
| # | Chunk | |
|---|---|---|
| 1.1 | [Correlation engine (rule-based, no-LLM)](phase-1/1.1-correlation-engine.md) | ⭐ |
| 1.2 | [Triage / root-cause agent (LLM + RAG)](phase-1/1.2-triage-agent.md) | ⭐ |
| 1.3 | [Incident persistence & REST API](phase-1/1.3-incident-api.md) | |
| 1.4 | [Incident Console (SPA)](phase-1/1.4-incident-console.md) | ⭐ |
| 1.5 | [Lightweight incident lifecycle](phase-1/1.5-incident-lifecycle.md) | |
| 1.6 | [Microsoft Teams integration (first)](phase-1/1.6-teams-integration.md) | ⭐ |
| 1.7 | [ITSM two-way sync (ServiceNow/Jira)](phase-1/1.7-itsm-sync.md) | |
| 1.8 | [Alert-noise KPI + correlation demo seed](phase-1/1.8-kpi-and-demo.md) | ⭐ |

## Phase 2 — Natural-language ChatOps (Pro)
| # | Chunk | |
|---|---|---|
| 2.1 | [Assistant agent (agent loop, cites queries)](phase-2/2.1-assistant-agent.md) | |
| 2.2 | [Assistant SPA (streaming, tool trace)](phase-2/2.2-assistant-spa.md) | |
| 2.3 | [Teams ChatOps](phase-2/2.3-teams-chatops.md) | |
| 2.4 | [Pro-tier gating + customer data-only scope](phase-2/2.4-tier-gating-scope.md) | |

## Phase 3 — Adaptive, forecasting, reporting
| # | Chunk | |
|---|---|---|
| 3.1 | [Baseline & anomaly detection (statistical)](phase-3/3.1-baseline-anomaly.md) | |
| 3.2 | [Forecasting / capacity (feeds fleet view)](phase-3/3.2-forecasting-capacity.md) | |
| 3.3 | [Knowledge Curator (RAG ingestion)](phase-3/3.3-knowledge-curator.md) | |
| 3.4 | [Reporting / Digest agent (daily/weekly/monthly)](phase-3/3.4-reporting-digest.md) | |
| 3.5 | [Insights (SPA)](phase-3/3.5-insights-spa.md) | |

> ⭐ = MVP: fleet view + pilot dashboards (Phase 0) and the cross-platform correlation showcase
> (Phase 1) for one pilot tenant.
