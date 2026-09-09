-- 1.7 ITSM two-way sync: tracks the ServiceNow/Jira ticket created for each incident so a status
-- change updates that same ticket instead of creating a duplicate (dedupe/idempotency), and records
-- both the last-known local status and last-known ITSM state to detect which side changed.
CREATE TABLE itsm_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    kind VARCHAR(50) NOT NULL CHECK (kind IN ('servicenow', 'jira')),
    external_id VARCHAR(255) NOT NULL,
    external_url VARCHAR(1000),
    last_local_status VARCHAR(50) NOT NULL,
    last_itsm_state VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_itsm_tickets_incident UNIQUE (tenant_id, incident_id)
);
CREATE INDEX idx_itsm_tickets_tenant_id ON itsm_tickets(tenant_id);

-- Dead-letter/retry bookkeeping: one outstanding-problem row per incident. Cleared on the next
-- successful sync; flagged dead-lettered after too many consecutive failures so the background
-- sweep stops hammering a broken config (manual "Push to ITSM" can still retry it).
CREATE TABLE itsm_sync_failures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    kind VARCHAR(50) NOT NULL CHECK (kind IN ('servicenow', 'jira')),
    operation VARCHAR(50) NOT NULL,
    error_message VARCHAR(2000),
    attempt_count INT NOT NULL DEFAULT 1,
    dead_lettered BOOLEAN NOT NULL DEFAULT FALSE,
    first_failed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_failed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_itsm_sync_failures_incident UNIQUE (tenant_id, incident_id)
);
CREATE INDEX idx_itsm_sync_failures_tenant_id ON itsm_sync_failures(tenant_id);
