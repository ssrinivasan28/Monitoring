-- 1.1 Correlation engine: monitor platform tagging + cross-platform topology map

ALTER TABLE monitors ADD COLUMN platform VARCHAR(20) NOT NULL DEFAULT 'windows'
    CHECK (platform IN ('ibmi', 'windows'));

-- Per-tenant topology: which Windows host(s) relate to which IBM i system(s),
-- used by the correlation engine to group cross-platform breaches into one incident.
CREATE TABLE topology_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    windows_host VARCHAR(255) NOT NULL,
    ibmi_system VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_topology_links_pair UNIQUE (tenant_id, windows_host, ibmi_system)
);
CREATE INDEX idx_topology_links_tenant_id ON topology_links(tenant_id);
