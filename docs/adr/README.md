# Architecture Decision Records (ADRs)

An ADR captures one significant decision: its context, the options weighed, the choice, and its
consequences. ADRs are immutable once **Accepted** — to change one, add a new ADR that supersedes it.

## Process
1. Copy the template below to `docs/adr/NNNN-short-title.md` (zero-padded, incrementing).
2. Fill it in; open a PR; set status **Proposed**.
3. On merge/agreement, set status **Accepted**. To reverse later, add a new ADR marking this one **Superseded**.

## Template
```markdown
# ADR-NNNN: <title>
- Status: Proposed | Accepted | Superseded by ADR-XXXX
- Date: YYYY-MM-DD
- Deciders: <names/roles>

## Context
What problem/force requires a decision?

## Options considered
1. <option> — pros / cons
2. <option> — pros / cons

## Decision
The choice, and why.

## Consequences
Positive, negative, and follow-ups (tests, docs, ADRs to write).
```

## Index
Decisions already made (from [PRODUCT_DECISIONS.md](../ip-sentinel/PRODUCT_DECISIONS.md) and the
architecture). Write the individual ADR files as each is formalized; this table is the register.

| ADR | Decision | Status | Source |
|---|---|---|---|
| 0001 | Additive, **read-only** AIOps layer over the 21 monitors (no remediation) | Accepted | Plan |
| 0002 | **Pluggable LLM** — Claude API + local model, routing + fallback | Accepted | #— |
| 0003 | **Custom React/TS SPA replaces Grafana**; Prometheus/Loki remain storage | Accepted | UI |
| 0004 | **PostgreSQL 18 + pgvector** as central store + RAG memory | Accepted | Data |
| 0005 | **Java 17 + Spring Boot** for the central platform (monitors keep shade-JAR build) | Accepted | Stack |
| 0006 | **Central multi-tenant, customer-facing** product (not internal-only) | Accepted | #7 |
| 0007 | **Tiered Basic/Pro**; Pro unlocks the AI Assistant only | Accepted | #16, #20 |
| 0008 | **Strict tenant isolation** — no cross-tenant learning | Accepted | #12 |
| 0009 | **Fleet-view-first**; capacity headroom (weighted blend + hard override) as health signal | Accepted | #2, #4–6 |
| 0010 | **AI-optional / graceful degradation** as an architectural invariant | Accepted | Plan |
| 0011 | **Dual auth** — Azure AD SSO (staff) + invite/MFA/federated (customers) | Accepted | #18 |
| 0012 | **13-month retention** | Accepted | #14 |
| 0013 | **Lightweight incident lifecycle + two-way ITSM sync** (ITSM = system-of-record) | Accepted | #15 |
| 0014 | **Teams-first** integration | Accepted | #23 |
| 0015 | **Windows Server + WinSW-led deployment**; containers for scale; on-prem default | Accepted | Deployment |
| 0016 | **SLA = composite health, 24×7, no maintenance exclusions, 99.9% default/override** | Accepted | #26 |
| 0017 | **SOC 2 Type II** compliance target | Accepted | #24 |
| 0018 | Primary KPI = **alert-noise reduction** | Accepted | #22 |
| 0019 | **Per-tenant data-source resolution** (`tenant_datasource` + gateway fallback to central) | Accepted | #27 |

> "#N" refers to the numbered entry in [PRODUCT_DECISIONS.md](../ip-sentinel/PRODUCT_DECISIONS.md).
