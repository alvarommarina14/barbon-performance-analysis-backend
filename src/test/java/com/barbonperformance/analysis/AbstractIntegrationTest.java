package com.barbonperformance.analysis;

import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.support.TestFixtures;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String EXTERNAL_URL = System.getenv("ANALYSIS_TEST_DB_URL");

    private static final PostgreSQLContainer POSTGRES;

    // Cloudflare Stream has no emulator, so the API is stubbed. Every test that touches the
    // video flow states the Cloudflare response it expects to be working against.
    protected static final WireMockServer CLOUDFLARE = new WireMockServer(0);

    static {
        CLOUDFLARE.start();
        if (EXTERNAL_URL == null) {
            POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @AfterAll
    static void stopCloudflare() {
        CLOUDFLARE.resetAll();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("analysis.video.api-base-url", () -> CLOUDFLARE.baseUrl());
        if (EXTERNAL_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username",
                    () -> envOrDefault("ANALYSIS_TEST_DB_USER", "bpa"));
            registry.add("spring.datasource.password",
                    () -> envOrDefault("ANALYSIS_TEST_DB_PASSWORD", "bpa"));
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

    protected RestTestClient client;

    @BeforeEach
    void resetStateAndBindClient() {
        // Everything identity-related hangs off app_user, so one cascading truncate clears it.
        // organization is left alone: it is seeded by the baseline migration.
        jdbcTemplate.execute("TRUNCATE TABLE app_user CASCADE");
        // These hang off organization, not app_user, so the cascade above misses them.
        jdbcTemplate.execute("TRUNCATE TABLE video_asset, red_flag, daily_routine, program_template CASCADE");
        CLOUDFLARE.resetAll();

        this.client = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    protected String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected String bearerFor(String email, String password) {
        LoginResponse session = client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .returnResult(LoginResponse.class)
                .getResponseBody();
        return bearer(session.accessToken());
    }
}
