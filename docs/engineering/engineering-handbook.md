# IP Sentinel — Engineering Handbook

How we build IP Sentinel. Pairs with the [enterprise architecture](../architecture/enterprise-architecture.md),
[task catalog](../planning/task-catalog.md), and [build prompts](../ip-sentinel/prompts/README.md).

## 1. Golden rules (non-negotiable)
1. **Never modify the 21 monitors.** They are the sensor layer. Changes are limited to new modules +
   `prometheus.yml` (remote-write) + installer role.
2. **Read-only.** No writes to IBM i/Windows, no remediation, no auto threshold edits.
3. **AI-optional.** Every feature degrades gracefully when the LLM is down; non-AI features never depend on it.
4. **Tenant isolation is deny-by-default.** Enforced server-side, never only in the UI.
5. **Every LLM/tool call** goes through redaction + audit + cost metering.

## 2. Repositories & branches
- Repo: `github.com/ssrinivasan28/Monitoring`. Authoritative working copy: `C:\RD\Monitoring\Monitoring`.
- Platform work lives on branch **`IPSentinel`** (off `main`). New code in **`aiops-platform/`**
  (backend) and **`aiops-web/`** (frontend); the monitors' single-module build is untouched.
- Branch per chunk: `feat/0.4-query-gateway`, etc. PR into `IPSentinel`; `IPSentinel` → `main` when a phase lands.
- Commits: imperative, scoped, end with `Co-Authored-By` where applicable. Never commit the Eclipse
  `.metadata/` noise or secrets.

## 3. Tech stack & conventions
| Area | Standard |
|---|---|
| Backend | **Java 17 + Spring Boot** (platform only). REST + WebSocket, JPA, Flyway migrations |
| Frontend | **React + TypeScript**, **uPlot** for charts, **responsive**, Island Pacific brand (`#0057B8`/`#F5A300`, white) |
| Store | **PostgreSQL 18 + pgvector** |
| LLM | pluggable `LlmProvider` (Claude + local), routing + fallback |
| Secrets | DPAPI (`common/CredentialProtector`) or Key Vault — never plaintext |
| Existing monitors | Keep their plain-Java / shade-JAR / WinSW conventions unchanged |

Match existing suite idioms where the monitors are touched; use standard Spring Boot idioms for the platform.

## 4. Testing & Definition of Done
A chunk is **Done** when:
- Its **Acceptance criteria** (in the chunk prompt) pass.
- **Unit tests** (TestNG + Mockito) and **integration tests** (Testcontainers: Postgres+pgvector, local
  Prometheus/Loki) cover the change.
- **Isolation test** proves no cross-tenant access for any new endpoint.
- **AI-optional test** proves the feature works with the provider disabled (where applicable).
- **Governance:** audit rows written; redaction verified; cost metered (for LLM paths).
- No change to the 21 monitors (`git diff` shows only new modules / prometheus.yml / installer).
- PR reviewed; docs/ADRs updated if a decision changed.

## 5. Security & governance in code
- All secret reads via `CredentialProtector.resolve(...)`.
- `ibmi_sql` is **SELECT-only**, parameterized; reject DDL/DML.
- Audit is **append-only** (no update/delete); enforce at the DB + service layer.
- Redaction runs **before** any provider call; add tests with seeded secrets/PII.
- Cost ledger + per-tenant quotas enforced in the orchestrator.

## 6. Build & release
- Backend: Maven → Spring Boot fat JAR (bundles the built SPA static assets → single service/port).
- Packaging: WinSW service `IPMonitoring_AIOpsPlatform`; installer "AIOps Platform" role; `application.yml`
  deployed with `onlyifdoesntexist`. Container image for the scale topology.
- Config: `application.yml` beside the JAR (port, Postgres, Prometheus/Loki, LLM, Azure AD, tenant).
- CI/CD: build + test gates, dependency + SAST scanning, Flyway migrations on startup, blue/green or rolling.

## 7. Local development
- Run Postgres 18 + pgvector locally (or Testcontainers); point `application.yml` at local Prometheus/Loki.
- Frontend dev: Vite dev server proxying `/api` to the backend (dev only; prod is served by Spring Boot).
- Provide a seed/fixture for the **cross-platform correlation demo** (chunk 1.8).

## 8. Using the build prompts
On `IPSentinel`, feed a chunk to Claude Code one of three ways (see
[prompts/README.md](../ip-sentinel/prompts/README.md)):
- **`/build-chunk 0.9`** — the slash command reads the shared preamble + chunk and builds to Acceptance.
- **Point at files** — *"Read prompts/00-shared-preamble.md and the chunk for 0.9, then implement it."*
- **`./show-prompt.ps1 0.9`** (or `.sh`) — copies preamble + chunk to the clipboard to paste elsewhere.

Pick chunks from the [task catalog](../planning/task-catalog.md) / [sprint plan](../planning/sprint-plan.md);
build in order per phase; open a PR into `IPSentinel` per chunk.
