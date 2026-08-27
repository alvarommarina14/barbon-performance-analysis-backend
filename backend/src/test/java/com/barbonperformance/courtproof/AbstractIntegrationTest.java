package com.barbonperformance.courtproof;

import com.barbonperformance.courtproof.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests run against a real Postgres, so Flyway migrations and the Hibernate
 * schema validation are exercised exactly as they are in production. The container is
 * static: one database for the whole suite, wiped between tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    protected TestFixtures fixtures;

    /** Talks to the running server over real HTTP, so the security filter chain is in play. */
    protected RestTestClient client;

    @BeforeEach
    void resetStateAndBindClient() {
        // Everything identity-related hangs off app_user, so one cascading truncate clears it.
        // organization is left alone: it is seeded by the baseline migration.
        jdbcTemplate.execute("TRUNCATE TABLE app_user CASCADE");

        this.client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
