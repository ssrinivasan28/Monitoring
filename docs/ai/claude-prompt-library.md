# IP Sentinel — Claude Prompt Library

Reusable prompts for building and operating IP Sentinel with Claude Code. Two kinds:
**build prompts** (the 28 chunks) and **workflow prompts** (review, tests, ADRs, docs).

> Always work on the `IPSentinel` branch. For build chunks, paste the shared preamble first.

## 1. Shared preamble
The canonical context (stack, brand, product decisions, hard rules) lives in
[`../ip-sentinel/prompts/00-shared-preamble.md`](../ip-sentinel/prompts/00-shared-preamble.md).
**Prepend it to every build-chunk prompt.**

## 2. Build prompts (the 28 chunks)
Indexed in the [task catalog](../planning/task-catalog.md) and
[prompts/README.md](../ip-sentinel/prompts/README.md). Phases 0–3, ⭐ = MVP. Build in order per phase.

## 3. Workflow prompts (reusable)

### 3.1 Acceptance-criteria PR review
```
On the IPSentinel branch. Review the current diff against the Acceptance criteria in
<chunk file>. For each criterion: state PASS/FAIL with the file:line evidence. Then check the
golden rules: (1) no change to the 21 monitors, (2) read-only, (3) AI-optional graceful
degradation, (4) tenant isolation deny-by-default, (5) every LLM/tool call redacted+audited+metered.
List any gaps as a checklist. Do not change code.
```

### 3.2 Test generation
```
Write TestNG + Mockito unit tests and Testcontainers integration tests (Postgres+pgvector, local
Prometheus/Loki) for <component>. Must include: a cross-tenant isolation test, an AI-provider-disabled
test (if applicable), and an audit/redaction assertion for any LLM/tool path. Match the suite's
existing test style.
```

### 3.3 ADR authoring
```
Draft an ADR in docs/adr/ using the template in docs/adr/README.md for the decision: <decision>.
Capture context, options considered, decision, consequences, and status. Keep it to one page.
```

### 3.4 Doc sync
```
The following decision changed: <change>. Update docs/ip-sentinel/PRODUCT_DECISIONS.md, and any of
ENHANCEMENT_PLAN.md / DEPLOYMENT.md / ENTERPRISE_READINESS.md / REPORTING.md / the affected build
chunk that reference it. Keep them consistent; list what you changed.
```

### 3.5 Cross-platform correlation demo (fixtures)
```
Build the seed scenario for chunk 1.8: an IBM i subsystem outage plus its Windows service fallout
that the correlation engine groups into ONE incident, usable as a demo and an integration test.
Provide the fixtures and a script to trigger it.
```

### 3.6 Security review
```
Review <area> for: prompt-injection via tool-returned data, non-SELECT reaching ibmi_sql, plaintext
secrets, missing tenant scoping, non-immutable audit writes, and unredacted LLM context. Report
findings with severity and file:line. Reference docs/ip-sentinel/ENTERPRISE_READINESS.md.
```

## 4. Guardrails for every prompt
- Read-only; never modify the 21 monitors; AI-optional; strict tenant isolation.
- Secrets via `CredentialProtector`; audit is append-only; redaction before any provider call.
- When a decision changes, update the docs (3.4) and add/adjust an ADR (3.3).
