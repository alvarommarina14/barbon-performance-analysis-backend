package com.barbonperformance.analysis.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterCoachRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.analysis.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CoachRegistrationIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";

    @Test
    void registerCoachCreatesACoachAndReturnsASession() {
        LoginResponse response = registerCoach("coach@example.com", "Marcelo Barbon");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().role()).isEqualTo(UserRole.COACH);
        assertThat(response.user().name()).isEqualTo("Marcelo Barbon");
        assertThat(response.user().coach()).isNotNull();
        assertThat(response.user().player()).isNull();
    }

    @Test
    void aSecondCoachRegistrationIsAConflict() {
        registerCoach("coach@example.com", "Marcelo Barbon");

        client.post().uri("/api/v1/auth/register-coach")
                .body(new RegisterCoachRequest("assistant@example.com", PASSWORD, "Assistant", null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("already has a coach"));
    }

    @Test
    void registerCoachRejectsADuplicateEmail() {
        registerCoach("coach@example.com", "Marcelo Barbon");

        client.post().uri("/api/v1/auth/register-coach")
                .body(new RegisterCoachRequest("coach@example.com", PASSWORD, "Impersonator", null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("already registered"));
    }

    @Test
    void registerCoachRejectsAShortPassword() {
        client.post().uri("/api/v1/auth/register-coach")
                .body(new RegisterCoachRequest("coach@example.com", "short", "Marcelo Barbon", null))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void aPlayerRegisteringAfterTheCoachExistsIsAssignedThatCoach() {
        registerCoach("coach@example.com", "Marcelo Barbon");

        LoginResponse player = registerPlayer("player@example.com", "Rosario Fiol");

        assertThat(player.user().player().hasCoach()).isTrue();

        client.get().uri("/api/v1/players/me/home")
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("Marcelo Barbon"));
    }

    @Test
    void aPlayerRegisteringBeforeAnyCoachHasNoCoach() {
        LoginResponse player = registerPlayer("player@example.com", "Rosario Fiol");

        assertThat(player.user().player().hasCoach()).isFalse();
        assertThat(player.user().coach()).isNull();
    }

    private LoginResponse registerCoach(String email, String name) {
        return client.post().uri("/api/v1/auth/register-coach")
                .body(new RegisterCoachRequest(email, PASSWORD, name, null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }

    private LoginResponse registerPlayer(String email, String name) {
        return client.post().uri("/api/v1/auth/register")
                .body(new RegisterRequest(email, PASSWORD, name, null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }
}
