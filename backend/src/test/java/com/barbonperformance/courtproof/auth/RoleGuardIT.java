package com.barbonperformance.courtproof.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.courtproof.AbstractIntegrationTest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginResponse;
import com.barbonperformance.courtproof.user.CoachProfile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class RoleGuardIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";

    @Test
    void playerReachesTheirOwnHome() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);

        String token = login("player@example.com").accessToken();

        client.get().uri("/api/v1/players/me/home")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("Rosario Fiol");
                    assertThat(body).contains("Marcelo Barbon");
                    assertThat(body).contains("\"cycleWeeks\":4");
                });
    }

    @Test
    void coachCannotReachThePlayerHome() {
        fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");

        client.get().uri("/api/v1/players/me/home")
                .header(HttpHeaders.AUTHORIZATION, bearer(login("coach@example.com").accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void coachSeesOnlyTheirOwnClients() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        CoachProfile otherCoach = fixtures.createCoach("other@example.com", PASSWORD, "Other Coach");
        fixtures.createPlayer("mine@example.com", PASSWORD, "My Client", coach);
        fixtures.createPlayer("theirs@example.com", PASSWORD, "Their Client", otherCoach);

        client.get().uri("/api/v1/coach/clients")
                .header(HttpHeaders.AUTHORIZATION, bearer(login("coach@example.com").accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("My Client");
                    assertThat(body).doesNotContain("Their Client");
                });
    }

    @Test
    void playerCannotReachTheCoachClientList() {
        fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", null);

        client.get().uri("/api/v1/coach/clients")
                .header(HttpHeaders.AUTHORIZATION, bearer(login("player@example.com").accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void bothSurfacesRejectAnonymousCalls() {
        client.get().uri("/api/v1/players/me/home").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v1/coach/clients").exchange().expectStatus().isUnauthorized();
    }

    private LoginResponse login(String email) {
        return client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest(email, PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }
}
