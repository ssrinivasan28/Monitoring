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
| [`enhancement-plan.html`](enhancement-plan.html) | The same plan as a branded, self-contained web page for sharing (open in a browser). |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | How it's packaged (Windows/WinSW-led, containers for scale) and the three hosting topologies with a recommendation. |
| [`ENTERPRISE_READINESS.md`](ENTERPRISE_READINESS.md) | Non-functional requirements — SLAs, DR/backup, AI safety, security, compliance, self-monitoring — with status and first-release priorities. |
| [`prompts/phase-0-foundation.md`](prompts/phase-0-foundation.md) | Foundation + governance spine + UI shell. |
| [`prompts/phase-1-correlation-triage.md`](prompts/phase-1-correlation-triage.md) | Correlation & triage engine + integrations. |
| [`prompts/phase-2-chatops.md`](prompts/phase-2-chatops.md) | Natural-language ChatOps. |
| [`prompts/phase-3-adaptive-thresholds.md`](prompts/phase-3-adaptive-thresholds.md) | Adaptive thresholds, forecasting & learning. |

## How to use the phase prompts

1. Read [`ENHANCEMENT_PLAN.md`](ENHANCEMENT_PLAN.md) for the full picture.
2. When you're ready to build a phase, open a **fresh Claude Code session on the `IPSentinel`
   branch** and paste the entire contents of that phase's prompt file.
3. Each prompt is self-contained: it carries a shared preamble (goals, stack, brand, the read-only
   rule, "don't touch the 21 monitors") plus that phase's deliverables and acceptance criteria.
4. Build phases in order — 1 assumes 0 is merged, and so on. Recommended MVP = Phase 0 + a thin
   Phase 1 slice (correlation → incident console + Teams + ServiceNow for one pilot tenant).

## Stack at a glance
- **Backend:** Java 17 + Spring Boot (`aiops-platform/`)
- **Frontend:** React + TypeScript + uPlot (`aiops-web/`) — replaces Grafana
- **Store:** PostgreSQL 18 + pgvector
- **LLM:** pluggable `LlmProvider` — Claude API *and* a local model, with routing + fallback
- **Brand:** primary blue `#0057B8`, accent amber `#F5A300`, white background
