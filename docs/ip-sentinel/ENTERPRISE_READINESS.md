# IP Sentinel — Enterprise Readiness / Non-Functional Requirements

The [enhancement plan](ENHANCEMENT_PLAN.md) covers the **functional** architecture at enterprise
altitude (multi-tenancy, RBAC/SSO, governance spine, HA, integrations, pluggable LLM). This document
tracks the **non-functional requirements (NFRs)** that make it enterprise-*ready* — the parts proven
during Phase 0 implementation and operations, not on paper.

**Legend:** ✅ addressed in the plan · 🟡 partially addressed · ⬜ to define during Phase 0.

---

## 1. Availability & SLAs  🟡
- **Customer SLA defined** (see [`REPORTING.md`](REPORTING.md) / decision #26): composite health,
  24×7, no maintenance exclusions, **99.9% default with per-tenant override**. Still to define: the
  *platform's own* availability target per hosting topology (distinct from the monitored-system SLA).
- Stateless app tier behind a load balancer; ≥2 instances in prod. ✅ (design)
- Define maintenance windows and graceful-degradation behavior (metrics/logs read-only if the LLM
  or an integration is down — the platform must still show dashboards & incidents without AI).

## 2. Disaster recovery & backup  ⬜
- **RTO/RPO targets** for PostgreSQL (incidents, audit, knowledge base) and config.
- Automated Postgres backups + tested restore; PITR where supported.
- Back up the **knowledge base / embeddings** (regenerable, but costly) and `application.yml`.
- Prometheus/Loki are reconstructable from agents; document acceptable metric/log loss on DR.
- Cross-site/offsite copy for on-prem; managed backups for the SaaS/AKS topology.

## 3. Performance, scale & capacity  🟡
- Publish target scale: # tenants, # monitors, metric/log ingest rate, concurrent SPA users.
- **Load/soak testing** with quality gates before each release.
- Postgres sizing + connection pooling; Thanos for long-term metrics; Loki object storage. ✅ (design)
- Capacity-planning guidance per topology (CPU/RAM/disk, GPU sizing for the local model).

## 4. Platform self-monitoring & observability  ⬜
- The AIOps platform must be **monitored like any other system**: health/liveness/readiness probes,
  its own Prometheus metrics, and alerting on itself (ideally by the existing suite — dogfooding).
- Structured app logs to Loki; request tracing; dashboards for API latency, agent latency, LLM
  errors, integration failures, queue depth, and cost burn.

## 5. AI safety & governance  🟡
*The highest-priority enterprise gap for an AI product making operational claims.*
- **Prompt-injection defense:** treat all tool-returned data (logs, IBM i rows, configs) as untrusted;
  never let it issue instructions. Read-only tools cap blast radius. ✅ (read-only design)
- **Output validation:** structured/typed agent outputs; validate before persist/display.
- **Hallucination guardrails:** every root-cause/answer **cites the evidence & queries used** (already
  a Phase-1/2 acceptance criterion) 🟡; label confidence; "insufficient data" path.
- **Model evaluation & red-teaming:** a regression eval set of real incidents; adversarial testing of
  triage/ChatOps before release; track quality across Claude vs local.
- **Human-in-the-loop by design:** read-only — AI never acts. ✅
- **Cost governance:** per-tenant token quotas + routing/fallback. ✅ (design)

## 6. Security  🟡
- Azure AD OIDC SSO + RBAC; per-tenant isolation at the query gateway. ✅ (design)
- **Secrets:** DPAPI works for on-prem 🟡; for enterprise add **Azure Key Vault / HashiCorp Vault**
  with **rotation** and no long-lived secrets in files.
- TLS everywhere; secured remote-write (mTLS/token); scoped egress for the Claude path. ✅ (design)
- **`ibmi_sql` SELECT-only** enforcement + parameterization. ✅ (design)
- **Security testing:** SAST/DAST in CI, dependency scanning, and a **pen test** before go-live.
- Session management, CSRF/CORS posture, security headers for the SPA.

## 7. Compliance & data governance  ⬜
- **Audit-log immutability & retention** (append-only, tamper-evident, defined retention).
- **Data residency** guarantees per topology (on-prem/local model = no egress). 🟡
- **PII handling:** redaction before any LLM call ✅; document what is stored vs. transient; data
  subject / deletion handling if applicable.
- **Target: SOC 2 Type II** (decided — see [`PRODUCT_DECISIONS.md`](PRODUCT_DECISIONS.md) #24). Start
  control design + evidence collection in Phase 0: audit-log immutability/retention, access reviews,
  change management, vendor/subprocessor list (incl. Anthropic on the Claude path), and a defined
  audit period. Map GDPR/ISO 27001 later as the customer base demands.

## 8. Reliability & resilience  🟡
- **Fallback** between Claude ↔ local, and graceful degradation when the LLM is unavailable. ✅ (design)
- **Rate limiting** on API + LLM calls; backpressure on ingestion.
- Idempotent integration delivery (no duplicate tickets/pages); retry with dead-letter.
- Circuit breakers around external integrations.

## 9. Operability & lifecycle  ⬜
- **Runbooks** for the platform itself (start/stop, DB restore, key rotation, model switch).
- **Change management:** versioning, release notes, backward-compatible DB migrations (Flyway). 🟡
- **IaC / GitOps** for the container topology; scripted installer for Windows. 🟡
- **Upgrade path** and rollback tested; config preserved (`onlyifdoesntexist`). ✅ (design)

## 10. Quality gates  ⬜
- Unit + integration (Testcontainers) coverage thresholds; the acceptance criteria in the phase
  prompts are the functional gates. 🟡
- **Load/performance** and **security** testing gates per release.
- **Accessibility (WCAG)** for the SPA; keyboard nav, contrast (brand palette already high-contrast).
- Internationalization/localization if required.

---

## Priority for first release (Phase 0)
Close these before calling it enterprise-ready:
1. **AI safety** — output validation, evidence citation, an eval/red-team pass (§5).
2. **DR/backup** — Postgres backup + tested restore, RTO/RPO (§2).
3. **Platform self-monitoring** — health probes + self-alerting (§4).
4. **Secrets rotation** — Key Vault/Vault option beyond DPAPI (§6).
5. **Audit immutability & retention** (§7).

Everything else can harden iteratively, but these five are what auditors and enterprise security
reviews will ask for on day one.
