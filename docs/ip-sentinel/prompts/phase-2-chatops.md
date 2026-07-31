# Phase 2 — Natural-Language ChatOps

> **How to use:** open a fresh Claude Code session on the `IPSentinel` branch and paste everything
> below. Assumes Phases 0–1 are merged.

---

You are working on the **IPSentinel** branch of the Island Pacific Monitoring suite. You are building
**IP Sentinel**, a central, multi-tenant, **read-only** AIOps platform on top of the existing 21
monitor JARs — **do not modify those monitors**. Stack: backend **Java 17 + Spring Boot**
(`aiops-platform/`), frontend **React + TypeScript + uPlot** (`aiops-web/`), store **PostgreSQL 18 +
pgvector**, LLM via a pluggable **`LlmProvider`** (Claude API *and* a local OpenAI-compatible model,
with routing + fallback). UI brand: primary blue `#0057B8`, accent amber `#F5A300`, white background.
Every LLM call passes through redaction + audit + cost metering. **Read-only only** — no remediation,
no writes to IBM i/Windows, no auto threshold edits.

**GOAL:** Conversational, read-only ops Q&A over live data + memory.

**DELIVERABLES**
- **Assistant Agent:** agent loop over the read-only tool registry + `kb_search`; streams tokens and
  the tool calls/queries it ran; answers cite the exact queries. RBAC-scoped to the user's tenants.
- **SPA Assistant module:** streaming chat over WebSocket; shows the tool/query trace inline.
- **Teams/Slack ChatOps:** same agent reachable from chat (Graph/Slack API), tenant/role scoped.

**ACCEPTANCE**
- "Which clients had job-queue backups in the last 24h, and what was running?" returns a correct,
  ranked answer built from a real PromQL + `ibmi_sql` call, with the queries shown.
- A user cannot retrieve data for a tenant they lack a role on.
- Every turn is audited and cost-metered; redaction applied to context.
