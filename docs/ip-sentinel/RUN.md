# IP Sentinel — How to Run (simple guide)

Think of each **chunk** (like `0.1`, `0.9`) as a **to-do ticket**. "Running" one = telling Claude to
build that ticket. There are **36 tickets total** (~16 for the MVP; all 36 for production-ready).

## How many chunks  (scope FROZEN)
| Phase | Chunks | Count |
|---|---|---|
| Phase 0 — Foundation + fleet view | `0.1`–`0.11` | 11 |
| Phase 1 — Correlation + integrations | `1.1`–`1.8` | 8 |
| Phase 2 — ChatOps (Pro) | `2.1`–`2.4` | 4 |
| Phase 3 — Adaptive + reporting | `3.1`–`3.5` | 5 |
| Phase 4 — Production hardening | `4.1`–`4.8` | 8 |
| **Total** | | **36** |

- **MVP** (demoable): all of Phase 0 + the 5 ⭐ Phase 1 chunks (~16).
- **Production-ready:** all **36** + the Definition of Done in
  [`ENTERPRISE_READINESS.md`](ENTERPRISE_READINESS.md).

## What's in the MVP (build these first)
The MVP is **not** a clean `0.1`–`0.9` range. The ⭐ MVP-defining features depend on the whole
foundation, so the buildable MVP = **all of Phase 0 + the 5 starred Phase 1 chunks** (~16 chunks):

```
Phase 0 (all 11):  0.1  0.2  0.3  0.4  0.5  0.6  0.7  0.8  0.9  0.10  0.11
Phase 1 (starred): 1.1  1.2       1.4       1.6       1.8
```
- ⭐ features: `0.9` Fleet Overview, `0.10` Dashboards, `1.1` Correlation, `1.2` Triage,
  `1.4` Incident Console, `1.6` Teams, `1.8` KPI + demo.
- The rest of Phase 1 (`1.3`, `1.5`, `1.7`) and all of Phase 2 & 3 come **after** the MVP.

**What the MVP demo shows (one pilot tenant):** the fleet view + a single IBM i + Windows incident
with AI root cause + the alert-noise-reduction KPI, delivered to Teams.

**Rule of thumb:** build all of Phase 0 in order, then the ⭐ chunks in Phase 1 → you have the MVP.

## Run ONE ticket — pick one way

**Way 1 — Slash command (simplest).** In Claude Code, on the `IPSentinel` branch, type:
```
/build-chunk 0.1
```
Claude reads the instructions and builds ticket 0.1.

**Way 2 — Plain English.**
```
Read the preamble and chunk 0.1, then build it.
```

**Way 3 — Copy to clipboard, paste elsewhere.**
```
powershell -ExecutionPolicy Bypass -File docs/ip-sentinel/prompts/show-prompt.ps1 0.1
```
Then paste (Ctrl+V) into a chat and send.

> Use Way 1. Ways 2–3 are backups.

## Do the whole project
Run the tickets **in order**, one at a time — `0.1`, `0.2`, … `0.11`, `1.1`, … `3.5` (28 total).
Order is in [`../planning/sprint-plan.md`](../planning/sprint-plan.md). After each, save the work as a
PR into `IPSentinel`, then do the next.

## See the actual app (later, after some Phase 0 chunks exist)
Start the program:
```
cd aiops-platform && mvn spring-boot:run
```
Open in a browser:
```
https://localhost:8443
```
(Needs Java 17, Maven, Node, PostgreSQL 18 + pgvector, and your existing Prometheus/Loki.)

## Simplest summary
- **Build a ticket** → type `/build-chunk <number>`.
- **Do the project** → run all 28 in order, `0.1` first.
- **See the app** (later) → `mvn spring-boot:run`, then open `localhost:8443`.
