-- 1.6 Microsoft Teams integration: tracks the Graph message posted for each incident so a status
-- change updates that same card instead of posting a new one (dedupe/idempotency).
CREATE TABLE teams_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    channel_id VARCHAR(255) NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    last_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_teams_notifications_incident UNIQUE (tenant_id, incident_id)
);
CREATE INDEX idx_teams_notifications_tenant_id ON teams_notifications(tenant_id);
