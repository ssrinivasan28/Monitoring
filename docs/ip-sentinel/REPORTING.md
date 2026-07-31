# IP Sentinel — Reporting Spec

Content of the scheduled customer reports produced by the **Reporting/Digest agent**
(build chunk [`prompts/phase-3/3.4-reporting-digest.md`](prompts/phase-3/3.4-reporting-digest.md)).

All reports are **tenant-scoped**, **Island Pacific branded**, and available to **all tiers**
(reports are Basic; only the AI Assistant is Pro-gated). Data comes entirely from what the platform
already holds — Prometheus (capacity/uptime), the incident store, the alert-noise KPI, and forecasts —
so no new collection is required.

## Cadences & contents

### Daily digest — operational ("what happened")
- Incidents opened / resolved in the last 24h, by severity
- Still-open incidents needing attention
- Current **capacity headroom** per system (red/amber highlights)
- **Alert-noise ratio** for the day (raw alerts → correlated incidents)
- New anomalies or forecasts flagged

### Weekly digest — review ("how's the week trending")
- Week-over-week: incident counts, resolution times, noise-reduction KPI
- Capacity headroom **trend** + top at-risk systems
- Top recurring / repeat issues
- **Upcoming risks** from forecasts (disk/ASP filling, certs expiring soon)
- Notable events summary

### Monthly SLA report — formal (QBRs / contracts)
- **Uptime / availability %** per system vs. SLA target, with breach windows + durations
- Incident summary: counts by severity + resolution stats
- **Alert-noise reduction KPI** for the month
- Capacity trend over the month + forward outlook (fill dates, cert runway)
- **Month-over-month / year-over-year** comparison (enabled by 13-month retention)
- Executive summary in plain English

## Cross-cutting behavior
- **Delivery:** email · in-portal · Microsoft Teams (decision #10).
- **Narrative:** AI-written prose when the LLM is available; **templated fallback** when it isn't —
  the numbers always render (graceful degradation).
- **Export:** on-demand PDF/HTML for sharing.
- **Scope & isolation:** strictly one tenant's data; no cross-tenant aggregation.
- **Branding:** Island Pacific (blue `#0057B8` / amber `#F5A300`).

## Open questions (decide before the monthly SLA report is built)
- **SLA definition:** 99.x% *of what* — monitor/endpoint reachability, system availability, or a
  composite? Per system or per tenant? Business hours vs 24×7? Excused maintenance windows?
- **Export format:** PDF required, or HTML/in-portal sufficient for v1?
- **SLA target source:** fixed global default, or per-tenant/contract value stored in config?
