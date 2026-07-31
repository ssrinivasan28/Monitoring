# Phase 3 — Adaptive Thresholds, Forecasting & Learning

> **How to use:** open a fresh Claude Code session on the `IPSentinel` branch and paste everything
> below. Assumes Phases 0–2 are merged.

---

You are working on the **IPSentinel** branch of the Island Pacific Monitoring suite. You are building
**IP Sentinel**, a central, multi-tenant, **read-only** AIOps platform on top of the existing 21
monitor JARs — **do not modify those monitors**. Stack: backend **Java 17 + Spring Boot**
(`aiops-platform/`), frontend **React + TypeScript + uPlot** (`aiops-web/`), store **PostgreSQL 18 +
pgvector**, LLM via a pluggable **`LlmProvider`** (Claude API *and* a local OpenAI-compatible model,
with routing + fallback). UI brand: primary blue `#0057B8`, accent amber `#F5A300`, white background.
Every LLM call passes through redaction + audit + cost metering. **Read-only only** — no remediation,
no writes to IBM i/Windows, no auto threshold edits.

**GOAL:** Predictive/adaptive insight that reduces noise — recommendation-only.

**DELIVERABLES**
- **Anomaly/Baseline Agent:** learn per-series baselines + seasonality from Prometheus history; emit
  anomaly signals static thresholds miss; produce `threshold_recommendations` (never rewrite
  `.properties`).
- **Forecasting/Capacity Agent:** project disk/ASP fill dates, cert-expiry runway, job-duration
  drift; emit `forecasts` with projected dates + drivers.
- **Knowledge Curator Agent:** on incident resolve / KB upload, chunk + embed into `kb_docs`/
  `kb_chunks`; summarize resolved incidents into reusable memory.
- **SPA Insights module:** anomaly feed, forecasts, and threshold recommendations with accept/dismiss
  (writes `feedback` only — not config).

**ACCEPTANCE**
- A seeded upward trend with no static-threshold breach produces a forecast with a plausible date.
- A recurring benign spike produces a threshold recommendation, not an alert.
- Accepting/dismissing a recommendation records feedback and adjusts future scoring; no `.properties`
  file is modified by the platform.
