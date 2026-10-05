package com.barbonperformance.analysis.coach;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.BookingLinkView;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.SetBookingLinkRequest;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

class BookingLinkIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String LINK = "https://calendar.app.google/abc123";

    private String coachAuth;

    @BeforeEach
    void coachWithAClient() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);
        coachAuth = bearerFor("coach@example.com", PASSWORD);
    }

    @Test
    void theCoachSetsTheLinkAndThePlayerHomeCarriesIt() {
        set(LINK).expectStatus().isOk();

        client.get().uri("/api/v1/players/me/home")
                .header(HttpHeaders.AUTHORIZATION, bearerFor("player@example.com", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains(LINK));
    }

    @Test
    void aBlankLinkClearsIt() {
        set(LINK).expectStatus().isOk();
        set("  ").expectStatus().isBadRequest();

        BookingLinkView cleared = set("").expectStatus().isOk()
                .returnResult(BookingLinkView.class)
                .getResponseBody();

        assertThat(cleared.bookingUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://calendar.app.google/abc", "javascript:alert(1)", "book me on friday"})
    void onlyHttpsLinksAreAccepted(String link) {
        set(link).expectStatus().isBadRequest();
    }

    @Test
    void aPlayerCannotSetTheLink() {
        client.put().uri("/api/v1/coach/booking-link")
                .header(HttpHeaders.AUTHORIZATION, bearerFor("player@example.com", PASSWORD))
                .body(new SetBookingLinkRequest(LINK))
                .exchange()
                .expectStatus().isForbidden();
    }

    private org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec set(String link) {
        return client.put().uri("/api/v1/coach/booking-link")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new SetBookingLinkRequest(link))
                .exchange();
    }
}
