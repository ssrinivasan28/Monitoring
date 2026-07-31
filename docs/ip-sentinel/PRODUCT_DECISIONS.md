# IP Sentinel — Product Decisions Log

Product-direction decisions captured with the stakeholder (SSrinivasan). These refine the
[enhancement plan](ENHANCEMENT_PLAN.md) and shape Phase 0+. Living document — append as decisions are made.

## Decisions

| # | Question | Decision |
|---|---|---|
| 1 | Primary user for v1 | **All personas** — build shared foundations with **role-based views** |
| 2 | Top pain to solve first | **Single fleet view** (unified pane across clients) — ahead of AI correlation |
| 3 | Fleet scale target | **Under 25 tenants** — simple federation, no sharding needed |
| 4 | At-a-glance health signal | **Capacity headroom** (proactive posture) |
| 5 | Capacity inputs | **All four:** IBM i ASP/disk, Windows disk, CPU, memory/job-queue depth |
| 6 | Headroom scoring | **Weighted blend** (ASP weighted highest) — *plus a hard override so any single critical resource still surfaces* |
| 7 | Who logs in | **Customer-facing too** — a sellable product, not internal-only |
| 8 | Customer capabilities | Dashboards & health · their incidents · AI Assistant · alerts & reports |
| 9 | Branding | **Island Pacific branded** (single brand; no per-tenant theming) |
| 10 | Delivery channels | **Email · in-portal · Teams/Slack** (no SMS for now) |
| 11 | Report cadence | **Daily · Weekly · Monthly SLA** digests (AI-generated) |
| 12 | Cross-client intelligence | **No — strict tenant isolation**, no cross-client learning |
| 13 | Customer AI Assistant scope | **Their data only** (staff may get broader) |
| 14 | History retention | **13 months** (rolling year+1 for YoY & monthly SLA) |
| 15 | Incident lifecycle | **Lightweight in-app** (ack/assign/resolve/notes) **+ two-way sync** to ServiceNow/Jira (ITSM = system-of-record) |
| 16 | Customer packaging | **Tiered Basic / Pro** — entitlement/feature-gating layer |
| 17 | Device experience | **Responsive web** (one adaptive React app; no native app) |
| 18 | Customer authentication | **Both/configurable** — invite-based accounts + MFA by default; optional federated per-tenant SSO (SAML/OIDC). Staff stay on Azure AD SSO |
| 19 | New-client onboarding | **IP-managed provisioning** — staff provision each tenant (fits <25 tenants, on-prem installs) |
| 20 | Basic vs Pro split | **Pro = AI Assistant only.** Basic gets dashboards, incidents, reports, history, alerts; Pro unlocks the Assistant |
| 21 | Core differentiator | **AI correlation + root cause** — compete on the AI intelligence layer (across IBM i + Windows) |
| 22 | Primary success metric | **Alert-noise reduction** (raw alerts → correlated incidents ratio) — headline KPI, provable day one |
| 23 | First integration | **Microsoft Teams** — lowest friction (Graph/Azure AD), covers delivery + ChatOps |
| 24 | Compliance target | **SOC 2 Type II** — start controls/evidence early |
| 25 | Flagship demo | **Cross-platform correlation** — IBM i outage + Windows fallout → one incident with root cause |
| 26 | SLA definition | **Composite health** (reachable + SLA-critical components running), **24×7**, **no maintenance exclusions**, **99.9% default with per-tenant override**. Per-system, rolled up per tenant. See [`REPORTING.md`](REPORTING.md) |
| 27 | Data-source config | **Multiple** metrics/logs data sources per tenant (add more as monitoring grows), **managed in the Admin UI**, stored in `tenant_datasource`; the query gateway **fans out** across a tenant's enabled sources and **falls back to the central** `application.yml` endpoint (central, per-tenant, hybrid) |
| 28 | Scope freeze | v1 scope **FROZEN at 36 build chunks (Phases 0–4)**. MVP = ⭐ set; **production-ready = all 36 + the Definition of Done** in [`ENTERPRISE_READINESS.md`](ENTERPRISE_READINESS.md). Phase 4 = production hardening (DR, self-monitoring, secrets rotation, resilience, perf, security, AI-safety, SOC 2 + a11y) |

## Key implications for the build

- **Two access dimensions:** RBAC (role) **and** entitlement (Basic/Pro tier) — feature-gating layered
  on top of per-tenant isolation.
- **Dual authentication:** Azure AD SSO for staff; invite+MFA / optional federated IdP for customers.
- **Capacity-first fleet view:** the Forecasting/Capacity agent feeds the fleet grid, not just Insights.
  Weighted-blend score with a **hard critical override** per resource.
- **New Reporting/Digest agent:** generates the daily/weekly/monthly summaries — add to the agent roster.
- **Strict isolation everywhere:** no cross-tenant data mixing, including for model learning.
- **13-month retention:** plan Prometheus retention or Thanos + object storage even at <25 tenants;
  Postgres retention for incidents/audit sized to match.
- **ITSM two-way sync:** incident status flows both directions with ServiceNow/Jira.
- **Customer-facing = productized:** hardened tenant isolation, customer SLAs, Basic/Pro tiers, and a
  data-only customer Assistant with disclaimers.
- **AI Assistant is the paid unlock (Pro):** entitlement gate is a single clean line — Assistant on/off.
- **Positioning vs build:** *build* fleet view first (foundation), *market* the AI correlation + root
  cause as the differentiator.
- **Instrument the noise-reduction KPI from day one:** track raw-alert count vs correlated-incident
  count; surface the ratio on dashboards and in reports as the primary success metric.
- **MVP must nail cross-platform correlation:** even with fleet-view-first, the demo showcase is one
  incident assembled from an IBM i outage + its Windows fallout — build a seed scenario for it.
- **SOC 2 Type II front-loads controls:** audit-log immutability/retention, access reviews, change
  management, and evidence collection start in Phase 0 (see ENTERPRISE_READINESS §6–7).
- **Teams-first integration:** build the Graph/Teams adapter before ServiceNow/PagerDuty.

## Open questions (not yet decided)
- Pricing model for the Basic/Pro tiers (split itself is decided — see #20).
- DR RTO/RPO targets (see [`ENTERPRISE_READINESS.md`](ENTERPRISE_READINESS.md)).
- Report export format (PDF?) and the default SLA-critical component set (see [`REPORTING.md`](REPORTING.md)).
- Time-to-value target for IP-managed onboarding.
