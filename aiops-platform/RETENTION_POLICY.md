# Data Retention Policy — 13-Month Horizon (SOC 2 / IP Sentinel)

## Overview
To satisfy operational historical analysis, capacity forecasting, and SOC 2 Type II audit compliance while managing database growth, **IP Sentinel** enforces a standard **13-month (396 days)** retention horizon for all time-series and event log tables.

## Time-Bound Tables & Retention Key Columns

| Table | Retention Horizon | Key Timestamp Column | Action Strategy |
|---|---|---|---|
| `incidents` | 13 months | `opened_at` | Scheduled purge of resolved incidents older than 13 months |
| `alerts` | 13 months | `fired_at` | Scheduled purge of cleared alerts older than 13 months |
| `agent_runs` | 13 months | `created_at` | Scheduled purge of LLM audit logs older than 13 months |
| `tool_calls` | 13 months | `created_at` | Purged in tandem with associated `agent_runs` (foreign key cascade) |
| `cost_ledger` | 13 months | `created_at` | Scheduled purge of financial ledger records older than 13 months |
| `incident_timeline` | 13 months | `at` | Purged in tandem with parent `incidents` (foreign key cascade) |
| `incident_signals` | 13 months | N/A (FK `incident_id`) | Purged in tandem with parent `incidents` (foreign key cascade) |

## Implementation Notes
- Purges will be executed by a background scheduled service (`RetentionPurgeScheduler`) calling tenant-isolated partition or batch delete routines.
- Append-only audit table triggers (`agent_runs`, `tool_calls`, `incident_timeline`) allow scheduled system maintenance purges via an administrative cleanup procedure or partition drops.
