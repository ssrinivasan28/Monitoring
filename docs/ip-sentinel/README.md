# IP Sentinel — Enhancement Documentation

This folder holds the plan and per-phase execution prompts for **IP Sentinel** (working name):
turning the existing 21-monitor suite into a central, multi-tenant, **read-only** AIOps platform
with an AI operations analyst built in.

> **IP Sentinel is additive.** The existing monitors are untouched — they become the sensor layer.
> Everything here builds a new platform *on top* that only *reads* their metrics/logs. No
> remediation, no writes to IBM i/Windows. See the guardrails section of the plan.

## Contents

| File | What it is |
|---|---|
| [`ENHANCEMENT_PLAN.md`](ENHANCEMENT_PLAN.md) | The full plan — vision, capabilities, UI modules, agent roster, architecture, roadmap, governance, verification. |
| [`PRODUCT_DECISIONS.md`](PRODUCT_DECISIONS.md) | Living log of product-direction decisions (personas, fleet view, tiers, auth, retention, etc.) and their build implications. |
| [`enhancement-plan.html`](enhancement-plan.html) | The same plan as a branded, self-contained web page for sharing (open in a browser). |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | How it's packaged (Windows/WinSW-led, containers for scale) and the three hosting topologies with a recommendation. |
| [`ENTERPRISE_READINESS.md`](ENTERPRISE_READINESS.md) | Non-functional requirements — SLAs, DR/backup, AI safety, security, compliance, self-monitoring — with status and first-release priorities. |
| [`REPORTING.md`](REPORTING.md) | Contents of the daily/weekly/monthly customer reports, delivery/export behavior, and open SLA-definition questions. |
| [`prompts/`](prompts/README.md) | **Ticket-sized build prompts** — ~28 focused chunks across 4 phases, plus a shared preamble. See [`prompts/README.md`](prompts/README.md) for the index. |

## How to use the build prompts

1. Read [`ENHANCEMENT_PLAN.md`](ENHANCEMENT_PLAN.md) + [`PRODUCT_DECISIONS.md`](PRODUCT_DECISIONS.md).
2. Open [`prompts/README.md`](prompts/README.md) and pick the next chunk (build in order per phase).
3. In a **fresh Claude Code session on the `IPSentinel` branch**, paste
   [`prompts/00-shared-preamble.md`](prompts/00-shared-preamble.md) first, then the chunk. Build to its
   **Acceptance** criteria, open a PR, move on.
4. **MVP = the ⭐ chunks:** fleet view + pilot dashboards (Phase 0) and the cross-platform correlation
   showcase for one pilot tenant (Phase 1).

## Stack at a glance
- **Backend:** Java 17 + Spring Boot (`aiops-platform/`)
- **Frontend:** React + TypeScript + uPlot (`aiops-web/`) — replaces Grafana
- **Store:** PostgreSQL 18 + pgvector
- **LLM:** pluggable `LlmProvider` — Claude API *and* a local model, with routing + fallback
- **Brand:** primary blue `#0057B8`, accent amber `#F5A300`, white background
