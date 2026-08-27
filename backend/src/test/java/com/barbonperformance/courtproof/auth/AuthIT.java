package com.barbonperformance.courtproof.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.courtproof.AbstractIntegrationTest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.LoginResponse;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.RefreshRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.courtproof.auth.dto.AuthDtos.TokenPairResponse;
import com.barbonperformance.courtproof.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class AuthIT extends AbstractIntegrationTest {

    private static final String EMAIL = "player@example.com";
    private static final String PASSWORD = "a-long-enough-password";

    @Test
    void registerCreatesAPlayerAndReturnsASession() {
        LoginResponse response = register(EMAIL, PASSWORD, "Rosario Fiol");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.expiresInSeconds()).isPositive();
        assertThat(response.user().role()).isEqualTo(UserRole.PLAYER);
        assertThat(response.user().name()).isEqualTo("Rosario Fiol");
        assertThat(response.user().player()).isNotNull();
        assertThat(response.user().player().currentWeek()).isEqualTo(1);
        assertThat(response.user().player().hasCoach()).isFalse();
        assertThat(response.user().coach()).isNull();
    }

    @Test
    void emailIsStoredLowerCasedAndMatchedCaseInsensitively() {
        register("Mixed.Case@Example.com", PASSWORD, "Case Test");

        client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("mixed.case@example.com", PASSWORD))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void registeringTheSameEmailTwiceIsAConflict() {
        register(EMAIL, PASSWORD, "First");

        client.post().uri("/api/v1/auth/register")
                .body(new RegisterRequest(EMAIL, PASSWORD, "Second", null))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void shortPasswordsAreRejected() {
        client.post().uri("/api/v1/auth/register")
                .body(new RegisterRequest(EMAIL, "short", "Too Short", null))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void loginWithTheWrongPasswordIsUnauthorized() {
        register(EMAIL, PASSWORD, "Rosario Fiol");

        client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest(EMAIL, "not-the-right-password"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void loginWithAnUnknownEmailIsUnauthorizedAndDoesNotRevealTheAccountExists() {
        client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("nobody@example.com", PASSWORD))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .value(body -> assertThat(body).doesNotContain("Unknown email"));
    }

    @Test
    void meReturnsTheSignedInUser() {
        LoginResponse session = register(EMAIL, PASSWORD, "Rosario Fiol");

        client.get().uri("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains(EMAIL));
    }

    @Test
    void meWithoutATokenIsUnauthorized() {
        client.get().uri("/api/v1/auth/me").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void meWithAGarbageTokenIsUnauthorized() {
        client.get().uri("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer("not.a.jwt"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refreshRotatesTheTokenPair() {
        LoginResponse session = register(EMAIL, PASSWORD, "Rosario Fiol");

        TokenPairResponse rotated = client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(session.refreshToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();

        assertThat(rotated.refreshToken()).isNotEqualTo(session.refreshToken());
        assertThat(rotated.accessToken()).isNotBlank();

        // The new access token works.
        client.get().uri("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(rotated.accessToken()))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void reusingAConsumedRefreshTokenIsRejectedAndKillsTheWholeFamily() {
        LoginResponse session = register(EMAIL, PASSWORD, "Rosario Fiol");

        TokenPairResponse rotated = client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(session.refreshToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();

        // Replaying the original token is treated as a possible theft.
        client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(session.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();

        // ...which also invalidates the token that legitimately replaced it.
        client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(rotated.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        LoginResponse session = register(EMAIL, PASSWORD, "Rosario Fiol");

        client.post().uri("/api/v1/auth/logout")
                .body(new RefreshRequest(session.refreshToken()))
                .exchange()
                .expectStatus().isNoContent();

        client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(session.refreshToken()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void anUnknownRefreshTokenIsUnauthorized() {
        client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest("this-token-was-never-issued"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private LoginResponse register(String email, String password, String name) {
        return client.post().uri("/api/v1/auth/register")
                .body(new RegisterRequest(email, password, name, null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }
}
