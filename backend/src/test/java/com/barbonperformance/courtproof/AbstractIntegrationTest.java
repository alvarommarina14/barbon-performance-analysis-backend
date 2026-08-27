package com.barbonperformance.courtproof;

import com.barbonperformance.courtproof.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration tests run against a real Postgres, so Flyway migrations and the Hibernate
 * schema validation are exercised exactly as they are in production. One database serves
 * the whole suite and is wiped between tests.
 *
 * <p>By default Testcontainers starts that database. Setting {@code COURT_PROOF_TEST_DB_URL}
 * (plus {@code _USER} and {@code _PASSWORD}) points the suite at an already-running Postgres
 * instead — the escape hatch for machines where the Docker daemon is unavailable. The tests
 * themselves cannot tell the difference; they truncate rather than assume an empty database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String EXTERNAL_URL = System.getenv("COURT_PROOF_TEST_DB_URL");

    private static final PostgreSQLContainer POSTGRES;

    static {
        if (EXTERNAL_URL == null) {
            POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username",
                    () -> envOrDefault("COURT_PROOF_TEST_DB_USER", "courtproof"));
            registry.add("spring.datasource.password",
                    () -> envOrDefault("COURT_PROOF_TEST_DB_PASSWORD", "courtproof"));
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value;
    }

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
