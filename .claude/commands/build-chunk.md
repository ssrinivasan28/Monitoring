---
description: Build an IP Sentinel chunk by id (e.g. /build-chunk 0.9)
argument-hint: <chunk-id, e.g. 0.9>
---
You are implementing **IP Sentinel build chunk `$ARGUMENTS`** on the `IPSentinel` branch.

Before writing any code:
1. Read `docs/ip-sentinel/prompts/00-shared-preamble.md` — this is binding context (stack, brand,
   product decisions, hard rules).
2. Find and read the chunk file for `$ARGUMENTS` using Glob pattern
   `docs/ip-sentinel/prompts/phase-*/$ARGUMENTS-*.md`, then read it — this is the task.
3. If a `Dependencies` section lists earlier chunks, assume they are already built; do not rebuild them.

Then implement chunk `$ARGUMENTS` fully, honoring the golden rules:
- **Never modify the 21 existing monitors** (changes limited to `aiops-platform/`, `aiops-web/`,
  `prometheus.yml`, installer).
- **Read-only** (no writes to IBM i/Windows); **AI-optional** (works with the LLM disabled);
  **strict tenant isolation** (deny-by-default, server-side).
- Every LLM/tool call goes through redaction + audit + cost metering; secrets via `CredentialProtector`.

Finish by:
- Adding the tests the chunk specifies (incl. any isolation / AI-disabled / audit tests).
- Verifying each item in the chunk's **Acceptance** checklist and reporting PASS/FAIL with file:line.
- Summarizing the change; confirm `git diff` touches only new modules / prometheus.yml / installer.

Do not start coding until you have read both files.
