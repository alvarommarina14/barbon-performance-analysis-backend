package com.barbonperformance.courtproof;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.courtproof.organization.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationSmokeIT extends AbstractIntegrationTest {

    @Autowired
    private OrganizationRepository organizations;

    @Test
    void flywayCreatesTheBaselineSchemaAndSeedsTheOrganisation() {
        assertThat(organizations.findBySlug("barbon-performance"))
                .hasValueSatisfying(org -> {
                    assertThat(org.getName()).isEqualTo("Barbon Performance");
                    assertThat(org.getId()).isNotNull();
                    assertThat(org.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void healthEndpointIsPublic() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("\"status\":\"UP\""));
    }

    @Test
    void openApiSpecIsPublicAndDescribesTheApi() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("Court Proof API"));
    }

    @Test
    void unknownApiPathsAreClosedByDefault() {
        client.get().uri("/api/v1/anything").exchange()
                .expectStatus().isUnauthorized();
    }
}
