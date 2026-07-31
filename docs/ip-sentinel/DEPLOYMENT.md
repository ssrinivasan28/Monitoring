# IP Sentinel — Deployment Plan

How IP Sentinel is packaged, wired to the existing suite, and rolled out. Deployment leads with the
**Windows Server + WinSW** model (matching the current installer pattern) and documents **containers**
as the scale/HA alternative. All three hosting topologies are covered, with a recommendation.

---

## Principle: the sensors don't change — only their wiring does

The 21 monitors keep deploying exactly as today (Inno Setup + WinSW services, per client, per box).
The **only change to existing installs** is configuration:

- Point each client's Prometheus at the central platform via **remote-write** (or Thanos/federation).
- Stream logs to **central Loki** (Promtail already ships logs — repoint or federate).

No new agent software is added to every box. Grafana is retired from the MonitoringServer role once
the SPA reaches dashboard parity.

---

## The central platform stack

One central stack (per hosting boundary — see topologies):

| Component | What | Notes |
|---|---|---|
| `aiops-platform` | Spring Boot backend (query gateway, agents, governance, integrations, REST/WS) | Java 17 fat JAR |
| `aiops-web` | React/TS SPA (static build) | Served by the backend or a static host |
| **PostgreSQL 18 + pgvector** | Incidents, memory, audit, cost, RAG embeddings | Already installed on target servers |
| Central **Prometheus/Thanos** | Fleet metrics (multi-tenant, remote-write target) | Storage engine behind the API |
| Central **Loki** | Fleet logs | Storage engine behind the API |
| **LLM host** (optional) | Ollama/vLLM for the local-model path | Only if not using Claude API |

---

## Packaging

### Primary — Windows Server + WinSW (matches the existing suite)
- Package `aiops-platform` as a **WinSW service** (`IPMonitoring_AIOpsPlatform`), exactly like the
  current monitors.
- Serve the SPA from the backend (or IIS).
- Use the **PostgreSQL 18 already present** on the server; enable `pgvector`.
- Run central **Prometheus/Thanos** and **Loki** as WinSW services.
- Add a single Inno Setup role **"AIOps Platform"** to the installer; `build-installer.ps1` bundles
  the JAR + SPA build + config. `.properties`/config use `onlyifdoesntexist` (never overwritten on upgrade).
- Credentials via **DPAPI** (`CredentialProtector`) as elsewhere, or Azure Key Vault.

### Alternative — Containers (scale + HA)
- **Docker Compose** for a single-node pilot; **Kubernetes / AKS** for scale and HA (Azure AD is
  already in use, so AKS is a natural fit).
- Images: `aiops-platform`, `aiops-web` (nginx), Postgres+pgvector (or managed Azure DB for
  PostgreSQL), Thanos, Loki, optional LLM host.

---

## Hosting topologies (all three — with a recommendation)

### 1. On-prem per-customer  ⭐ recommended default
Platform runs in **each customer's data center** (Windows Server + WinSW, or their own k8s). Metrics
stay local; the **local-model path** keeps all data on-prem. Best data residency — the right default
for conservative IBM i shops.

### 2. SaaS / central-hosted
Island Pacific runs **one multi-tenant platform** (AKS). Clients **remote-write** metrics over a
secured tunnel (mTLS/token). Lowest per-customer footprint; easiest to operate and upgrade. Offer to
customers who permit outbound telemetry.

### 3. Hybrid
Central brain + **lightweight per-customer collectors** (local Prometheus/Loki + secure forwarder).
Balances residency and central operation; more moving parts.

> **Recommendation:** default to **on-prem per-customer** (topology 1) with the **local model** for
> residency-sensitive clients, and offer **SaaS** (topology 2) to clients who allow the Claude API /
> central hosting. The pluggable `LlmProvider` means the same build serves both — only config differs.

---

## Data-plane wiring (existing installs)
- Enable `remote_write` in each client's `resources/prometheus/prometheus.yml` → central
  Prometheus/Thanos endpoint; label series with the tenant = `ClientInstanceId`.
- Secure the channel: mTLS or bearer token; outbound-only from the client where possible.
- Repoint Promtail to central Loki (or run a per-customer Loki that federates).

## LLM hosting
- **Claude API:** outbound HTTPS to Anthropic — approve egress; redaction runs before every call.
- **Local model:** Ollama/vLLM as a WinSW service or container on a GPU box; no data leaves the estate.
- Routing + fallback between the two is handled by the `LlmProvider` layer.

## Environments & CI/CD
- **Dev → Staging → Prod.** Build extends `build-installer.ps1` (adds the AIOps Platform role) and a
  container image build.
- **DB migrations** via **Flyway on startup** — no manual schema steps.
- **Releases:** blue/green or rolling for the stateless app tier; Postgres migrations are
  backward-compatible within a release.

## HA & scale
- App tier is **stateless** → scale horizontally behind a load balancer.
- **PostgreSQL** primary + read replica (or managed Azure DB for PostgreSQL with pgvector).
- **Thanos** for long-term/HA metrics; Loki with object storage for log retention.

## Security & networking
- **Azure AD OIDC** SSO + RBAC; per-tenant isolation enforced at the query gateway.
- **TLS everywhere**; secured remote-write; secrets via DPAPI / Key Vault.
- Egress policy explicitly scoped for the Claude path when SaaS/Claude is used.

## Rollout / MVP
1. Stand up **one** platform instance (single Windows Server or a Compose stack) + Postgres/pgvector.
2. Enable remote-write from **one pilot tenant's** Prometheus; verify dashboards + a forced incident.
3. Validate governance (audit, redaction, RBAC, cost) and the Claude-vs-local paths.
4. Expand tenant-by-tenant; retire Grafana once SPA dashboards reach parity.

## Installer / ops changes summary
- New Inno role **"AIOps Platform"**; new WinSW service `IPMonitoring_AIOpsPlatform`.
- `prometheus.yml` gains `remote_write`; Grafana dropped from the MonitoringServer bundle.
- Everything else in the existing per-box installers is unchanged.
