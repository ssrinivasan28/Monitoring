# Adding a New Monitoring Service (extensibility guide)

IP Sentinel is designed so a new monitor is **mostly config, not code**. This is what to do — and
what happens automatically — when you add a new monitoring service in the future.

> Foundations that make this easy: multiple per-tenant data sources (decision #27) and the
> config-driven dashboard engine (decision #29, chunk `0.12`).

## Steps you perform

1. **Run the new monitor.** Build/deploy it like the existing 21 — it exposes Prometheus metrics on a
   `/metrics` port (`metrics.port` in its `.properties`). Nothing about the other monitors changes.
2. **Scrape it.** Add its job to the install's `prometheus.yml` (`localhost:<metrics.port>`); the
   installer generates this from the port. Its data now lands in central Prometheus.
3. **Register a data source — only if separate.** If the monitor reports to the *same* central
   Prometheus, skip this. If it has its **own** Prometheus/Loki endpoint, add it once in
   **Admin › Data Sources** (`tenant_datasource`) — the gateway will fan out to it and merge results.
4. **Add a dashboard.** Drop in a JSON **dashboard definition** (or import an existing Grafana JSON)
   via the dashboard engine (`0.12`). The dashboard appears in the picker — **no code**.

## What happens automatically (no extra work)
Because the platform reads Prometheus/incidents generically, the new monitor immediately flows into:
- **Dashboards** — once its definition exists
- **ChatOps / Assistant** — its metrics are queryable in plain English
- **Reports** — included in the daily/weekly/monthly digests
- **Correlation → Incidents → Triage** — *if* it emits breach signals in the standard form, its
  breaches are grouped into incidents with AI root cause like any other monitor

## What needs a small config touch (still not code)
- **Capacity headroom (`0.9`):** the fleet health score uses a fixed input set (ASP / disk / CPU /
  mem-jobq). If the new monitor adds a **new capacity dimension** that should affect fleet health, add
  it to the headroom inputs + weights config.
- **Cross-platform correlation (`1.1`):** if the new monitor should **group with** related systems,
  add it to the per-tenant topology map so its breaches correlate cross-platform.
- **SLA (`REPORTING.md`):** if the new component is **SLA-critical**, add it to that tenant's
  SLA-critical component list so it counts toward composite availability.

## What you never do
- Modify the existing 21 monitors
- Change platform code or rebuild the platform
- Register many URLs into IP Sentinel — Prometheus aggregates; the platform reads that one endpoint
  (plus any per-tenant data sources)

## One line
**New monitor → point it at Prometheus + add a dashboard definition → it shows up everywhere.**
The only manual mapping is when a new **capacity signal**, **correlation topology**, or **SLA-critical
component** should participate — everything else is automatic.
