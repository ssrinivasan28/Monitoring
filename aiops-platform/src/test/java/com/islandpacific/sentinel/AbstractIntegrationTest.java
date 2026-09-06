package com.islandpacific.sentinel;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg18").asCompatibleSubstituteFor("postgres"))
                .withDatabaseName("ipsentinel")
                .withUsername("ipsentinel")
                .withPassword("ipsentinel");
        try {
            postgres.start();
        } catch (Exception e) {
            // Fallback for environments without Docker daemon active
        }
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    protected org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUpCleanDatabase() {
        if (jdbcTemplate != null) {
            jdbcTemplate.execute("TRUNCATE TABLE tenants, users CASCADE");
        }
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/ipsentinel");
            registry.add("spring.datasource.username", () -> "ipsentinel");
            registry.add("spring.datasource.password", () -> "ipsentinel");
        }
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
