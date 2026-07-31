# IP Sentinel — Project Enhancement Plan
**Working product name:** IP Sentinel (placeholder — rename freely)
**Prepared for:** Island Pacific — Operations · **Status:** Proposed, for review

---

## Context — why build this

Today the suite is 21 independent Java 17 monitor JARs (Maven shade → ProGuard → WinSW services).
Each polls IBM i (jt400) or Windows (OSHI)/IFS/FTPS, checks **static thresholds**, exposes Prometheus
metrics, and fires **one email per monitor**. Grafana shows charts; Loki holds logs; config is
per-box `.properties`; the installer supports multiple clients via `ClientInstanceId`.

The pain this product removes:
- **Alert storms, no correlation.** A single IBM i outage trips subsystem, job-queue, QSYSOPR, and
  dependent Windows-service monitors at once → many disconnected emails, no "one incident" view.
- **No cross-client / cross-platform picture.** Each box is an island; nobody sees the fleet.
- **Every diagnosis is manual.** An engineer reads Grafana + logs + green-screen to guess the cause.
- **Static thresholds** cause false positives and miss slow-building problems.
- **No memory.** The same incident is re-investigated from scratch each time; runbooks live in heads.

**IP Sentinel** turns the existing monitors into the **sensor layer** of a single, AI-driven,
multi-tenant operations product with its own application UI.

## Decisions locked
- **Central multi-tenant platform** ingesting from all client installs.
- **PostgreSQL 18 + pgvector** = incidents, agent memory, audit, RAG over runbooks/past incidents.
- **Pluggable LLM** — Claude API *and* local/on-prem model, selectable + fallback routing.
- **Read-only insight only** — observe, correlate, explain, recommend. No remediation/writes.
- **Integrations:** ServiceNow/Jira, PagerDuty/Opsgenie, Teams/Slack, SIEM/webhook.
- **Governance (v1):** audit logging, PII/secret redaction, RBAC + Azure AD SSO, cost & model governance.
- **UI: fully custom React/TypeScript SPA — Grafana dropped.**

---

## What IP Sentinel IS

A **single pane of glass** across every customer's IBM i + Windows estate, with an **AI operations
analyst** built in. When something breaks it doesn't just alert — it **assembles a complete
incident**: what happened, everything related across platforms, the likely root cause, the relevant
runbook, and what to check next. You can also **ask it questions in plain English**.

Three pillars:
1. **See everything** — unified fleet dashboards (metrics + logs), no more Grafana-per-box.
2. **Understand fast** — AI correlation + root-cause reasoning turn alert noise into ranked incidents.
3. **Never re-learn** — every incident, resolution, and runbook becomes searchable memory.

---

## What it can DO — capability catalog

### A. Unified observability (replaces Grafana)
- Fleet health grid: every tenant/monitor at a glance (red/amber/green).
- Metric dashboards (uPlot): IBM i subsystems, job queues, QSYSOPR, IFS, file members, system
  matrix (CPU/ASP/disk); Windows CPU/mem/disk/processes/services; SSL/URL/uptime.
- Log explorer over central Loki (LogQL), tenant-scoped, pivot from any incident.
- Drill-down: click a chart anomaly → jump to logs + related incidents for that window.

### B. AI incident intelligence (the core differentiator)
- **Correlation** across monitors/platforms/tenants into one incident.
- **Root-cause hypothesis** with confidence and the evidence used.
- **RAG grounding:** matching runbook + most similar past incidents from pgvector.
- **Suggested checks / next steps** (read-only — never auto-executed).
- **Severity & impact scoring**, noise suppression (dedupe, N-consecutive-breach, flap detection),
  and an **incident timeline**.

### C. Natural-language ChatOps
- Ask questions in plain English (SPA + Teams/Slack); the agent answers via read-only tools and
  cites the exact queries it ran. RBAC-scoped to the user's tenants.

### D. Proactive / predictive (adaptive thresholds)
- Learned baselines + seasonality → alerts on **anomalies** static thresholds miss.
- **Forecasts:** disk/ASP fill date, cert-expiry runway, job-duration drift, queue-growth trend.
- **Threshold recommendations** for human approval; the product never rewrites `.properties`.

### E. Integrations & workflow
- **ServiceNow/Jira** (ticket create/enrich), **PagerDuty/Opsgenie** (paging), **Teams/Slack**
  (incidents + ChatOps), **SIEM/webhook** (Splunk/Sentinel/generic). Existing email kept as fallback.

### F. Governance & administration
- **Audit** of every prompt, tool call, and answer.
- **PII/secret redaction** before any LLM call (enforced on the Claude path).
- **RBAC + Azure AD SSO;** per-tenant isolation on every query.
- **Cost & model governance:** token/cost dashboards, per-tenant quotas, Claude↔local routing + fallback.
- **Knowledge base management:** upload/curate runbooks; chunked + embedded for RAG.

---

## Personas
- **NOC / support engineer** — incident console + ChatOps; one enriched incident instead of 12 emails.
- **IBM i / systems admin** — dashboards + IBM i SQL answers; capacity forecasts.
- **Service desk / account manager** — per-client health, ticket status, SLA-relevant trends.
- **Platform admin** — governance: users/roles, tenants, cost, model config, audit, KB curation.

---

## The application UI (React/TypeScript SPA — the only UI)

The browser never queries Prometheus/Loki directly — every read passes through the platform's
tenant-scoped, audited query gateway.

1. **Fleet Overview** — multi-tenant health grid; filter by client/platform/severity.
2. **Dashboards** — uPlot metric panels per category; parity with today's `grafana-dashboards/*.json`.
3. **Log Explorer** — LogQL search over central Loki, cross-linked from incidents.
4. **Incident Console** — ranked incidents; detail: signals, AI root cause + evidence, runbook,
   similar incidents, timeline, suggested checks, and push-to-channel actions.
5. **Assistant (ChatOps)** — streaming chat; shows the tools/queries the agent ran.
6. **Insights** — anomaly feed, forecasts, threshold recommendations.
7. **Knowledge Base** — upload/edit runbooks; view embeddings/coverage.
8. **Admin & Governance** — tenants, roles (SSO), cost & quotas, model config, audit log, integrations.

---

## The agent roster

A small set of specialized, **read-only** agents coordinated by an orchestrator, all sharing one
substrate: the pluggable LLM (`LlmProvider`), the read-only tool registry, and pgvector memory.
Every agent call is redacted, audited, and cost-metered.

| Agent | Role | Runs when | Key tools |
|---|---|---|---|
| **Correlation Agent** | Group simultaneous breaches across monitors/platforms/tenants into one incident | On new breaches | `promql_query`, `service_status` |
| **Triage / Root-Cause Agent** ⭐ | Gather context, retrieve runbook + similar incidents (RAG), produce root-cause + evidence + severity + suggested checks | Per assembled incident | all read-only tools + `kb_search` |
| **Assistant (ChatOps) Agent** | Answer plain-English questions on demand (SPA + Teams/Slack), citing queries | Interactive | `promql_query`, `loki_query`, `ibmi_sql`, `kb_search` |
| **Anomaly / Baseline Agent** | Learn baselines/seasonality; flag anomalies; recommend thresholds | Scheduled | `promql_query` (history) |
| **Forecasting / Capacity Agent** | Project disk/ASP fill, cert runway, job-duration drift | Scheduled | `promql_query` (history) |
| **Knowledge Curator Agent** | Turn resolved incidents + runbooks into searchable memory (chunk → embed → store) | On resolve / KB upload | `kb_search`, embeddings |
| **Reporting / Digest Agent** | Generate daily/weekly/monthly health & SLA reports (AI prose when available, templated fallback when the LLM is off) | Scheduled | `promql_query`, `kb_search` |

**Supporting (not LLM agents):** the **Orchestrator** (routes work, retries, Claude↔local fallback),
the **Notification Router** (channel selection, dedupe, per-channel formatting), and the
**Governance guard** (PII redaction, audit, cost metering) wrapping every call.

**Collaboration on one incident:** Correlation assembles → Triage explains (grounded by Knowledge
Curator's memory) → Notification Router dispatches; Anomaly + Forecasting run in the background.

### Read-only tool registry
- `promql_query` — Prometheus metric queries
- `loki_query` — LogQL log search
- `ibmi_sql` — SELECT-only DB2/QSYS2 (reuse `SqlThresholdService` JDBC path)
- `service_status` / `list_monitors` — monitor & Windows-service state
- `read_config` — non-secret config (secrets redacted)
- `kb_search` — pgvector RAG over runbooks + past incidents

---

## Concrete scenarios

**1. IBM i subsystem outage (correlation win).** QINTER stops; the subsystem, job-queue-count, and
QSYSOPR monitors breach and a dependent Windows sync service goes down. IP Sentinel correlates all
four into **one incident**, surfaces the QSYSOPR message naming the cause, retrieves the "QINTER
restart" runbook + two similar past incidents, scores it Critical, pages on-call and opens a
ServiceNow ticket — **one actionable incident instead of 4+ emails.**

**2. Slow-building capacity risk (predictive).** ASP usage trends up over two weeks. No static
threshold has fired, but the forecast projects 95% in six days. IP Sentinel raises a proactive
Insight with the projected date and driver, before any outage.

**3. Plain-English investigation (ChatOps).** An engineer asks in Teams: "Which clients had
job-queue backups over the last 24h, and what was running?" The agent runs PromQL across tenants +
an IBM i SQL lookup and replies with a ranked list and the top jobs — citing the queries it ran.

---

## Architecture (unchanged sensors, new brain + UI)

```
 [21 per-box monitors × N clients]  →  Prometheus remote-write/Thanos + central Loki (storage only)
                                          │  (queried ONLY via the platform API)
                                          ▼
   AIOps Platform Service (Java 17 + Spring Boot):
     query gateway (tenant-scoped, audited) · correlation engine · agent orchestrator
     (LlmProvider Claude|Local + tools + RAG) · governance spine · integration adapters · REST/WS
                                          │                          │
                    React/TS SPA (only UI) ◄┘        PostgreSQL 18 + pgvector ◄┘
```

Grafana is removed; Prometheus/Thanos + Loki remain as storage engines behind the API. The browser
never queries them directly. Stateless app tier scales horizontally; Postgres primary + replica for HA.

## Impact on the existing suite
- **The 21 monitors are unchanged** — they already emit the metrics/logs the platform consumes.
- **Grafana is dropped** (replaced by the SPA). Prometheus + Loki stay as storage engines.
- **`prometheus.yml`** gains remote-write/federation so the central platform can pull fleet-wide data.
- **Installer** gains an optional "AIOps Platform" role and stops bundling Grafana.

## Data model (Postgres)
`tenants`, `users`, `roles` · `monitors`, `alerts` · `incidents`, `incident_signals`, `correlations`,
`incident_timeline` · `agent_runs` + `tool_calls` (audit) · `feedback` · `kb_docs` + `kb_chunks`
(pgvector embeddings) · `forecasts`, `threshold_recommendations` · `cost_ledger`, `model_config`,
`integration_config`.

## Tech stack
- **Backend:** Java 17 + Spring Boot (SSO/OIDC, RBAC, REST+WS, JPA→Postgres, workers). Departure from
  the shade-JAR pattern **for the platform only**; the 21 monitors keep their existing build.
- **Frontend:** React + TypeScript SPA; **uPlot** for time-series; log explorer, incident console,
  streaming ChatOps, admin.
- **Data:** PostgreSQL 18 + `pgvector`. **LLM/embeddings:** pluggable providers; keys via DPAPI/Key Vault.
- **Deploy:** Docker/K8s + on-prem Windows-server install via existing WinSW/Inno path.

---

## Roadmap (phased)

**Governance and the UI shell are foundational — built first, not bolted on.**

- **Phase 0 — Foundation + governance + UI shell.** Central data plane (remove Grafana); Postgres
  schema + pgvector; Spring Boot query gateway + LlmProvider (+routing/fallback) + tool registry +
  agent loop; governance day one (audit, PII redaction, Azure AD SSO+RBAC, cost metering); React
  shell with dashboards at parity for the two pilot monitors. **[MVP]**
- **Phase 1 — Correlation & triage + integrations.** Correlation engine → RAG-grounded incidents in
  the console; ServiceNow/Jira, PagerDuty/Opsgenie, Teams/Slack, SIEM/webhook. Email fallback kept.
  **[MVP slice]**
- **Phase 2 — Natural-language ChatOps.** SPA (WebSocket) + Teams/Slack; agent over tools + RAG,
  RBAC-scoped.
- **Phase 3 — Adaptive thresholds & learning.** Baselines/forecasts/anomalies + threshold
  recommendations in Insights; learn from analyst feedback in Postgres.

**Recommended MVP:** Phase 0 + a thin Phase 1 slice (correlation → incident console + Teams +
ServiceNow for one pilot tenant), then expand.

---

## Governance & scope

First-release controls: **audit logging · PII/secret redaction · RBAC + Azure AD SSO · cost & model
governance · per-tenant isolation.**

**Read-only by design.** No remediation, no auto-restart, no auto threshold edits, no writes to
IBM i or Windows. The AI observes, correlates, explains, and recommends — a human takes every action.
Closed-loop remediation (a natural future phase via the existing `servicescheduler`) is deferred.

## Verification
- **Backend & agent:** triage yields one correlated incident from many mocked breaches; `ibmi_sql`
  rejects non-SELECT; redaction strips seeded secrets pre-provider; an audit row per agent run; query
  gateway denies cross-tenant access.
- **Platform & UI:** forced breach across two tenants → one incident + ServiceNow/PagerDuty/Teams/SIEM
  fire with isolation intact; SPA dashboards reach parity with the pilot Grafana panels; incident
  console + ChatOps stream over WebSocket.
- **Both LLM backends:** identical triage on Claude vs local — same tool flow, comparable output,
  correct cost ledger + fallback.
