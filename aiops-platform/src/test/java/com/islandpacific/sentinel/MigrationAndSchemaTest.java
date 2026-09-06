package com.islandpacific.sentinel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationAndSchemaTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify Flyway applies migrations cleanly, enables pgvector, and creates all 23 expected tables")
    void flywayMigrationsAppliedAndVectorExtensionEnabled() {
        // 1. Verify pgvector extension is enabled
        Integer extensionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
        assertThat(extensionCount).isNotNull().isEqualTo(1);

        // 2. List of all 23 expected platform tables
        List<String> expectedTables = List.of(
                "tenants", "users", "roles", "user_tenant_roles", "entitlements",
                "monitors", "alerts", "incidents", "incident_signals", "correlations",
                "incident_timeline", "agent_runs", "tool_calls", "feedback", "kb_docs",
                "kb_chunks", "forecasts", "threshold_recommendations", "cost_ledger",
                "model_config", "integration_config", "tenant_datasource", "reports"
        );

        List<String> actualTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(actualTables).containsAll(expectedTables);

        // 3. Verify vector index on kb_chunks
        Integer vectorIndexCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE tablename = 'kb_chunks' AND indexname = 'idx_kb_chunks_embedding'", Integer.class);
        assertThat(vectorIndexCount).isNotNull().isEqualTo(1);
    }
}
