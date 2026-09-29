package com.barbonperformance.analysis.strength;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagInput;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagsView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.ReplaceRedFlagsRequest;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class RedFlagIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";

    private PlayerProfile player;
    private PlayerProfile otherPlayer;
    private String coachAuth;

    @BeforeEach
    void coachWithTwoClients() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        player = fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);
        otherPlayer = fixtures.createPlayer("other@example.com", PASSWORD, "Other Client", coach);
        coachAuth = bearerFor("coach@example.com", PASSWORD);
    }

    @Test
    void aPlayerSeesTheSharedFlagsAndOnlyTheirOwn() {
        replace("/api/v1/strength/red-flags", List.of(
                new RedFlagInput("Sharp or electric pain", "Stop the exercise and message your coach."),
                new RedFlagInput("Joint pain that lingers over 48 hours", null)));
        replace("/api/v1/strength/players/" + player.getId() + "/red-flags", List.of(
                new RedFlagInput("Elbow pain above 3/10", "Skip the pressing work that day.")));
        replace("/api/v1/strength/players/" + otherPlayer.getId() + "/red-flags", List.of(
                new RedFlagInput("Someone else's knee", null)));

        RedFlagsView seen = client.get().uri("/api/v1/strength/red-flags")
                .header(HttpHeaders.AUTHORIZATION, bearerFor("player@example.com", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .returnResult(RedFlagsView.class)
                .getResponseBody();

        assertThat(seen.forEveryone()).extracting(RedFlagView::title)
                .containsExactly("Sharp or electric pain", "Joint pain that lingers over 48 hours");
        assertThat(seen.forPlayer()).extracting(RedFlagView::title).containsExactly("Elbow pain above 3/10");
    }

    @Test
    void replacingTheListDropsTheFlagsThatWereLeftOut() {
        replace("/api/v1/strength/red-flags", List.of(new RedFlagInput("Old", null), new RedFlagInput("Kept", null)));
        replace("/api/v1/strength/red-flags", List.of(new RedFlagInput("Kept", null)));

        RedFlagsView seenByCoach = client.get().uri("/api/v1/strength/red-flags")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(RedFlagsView.class)
                .getResponseBody();

        assertThat(seenByCoach.forEveryone()).extracting(RedFlagView::title).containsExactly("Kept");
        assertThat(seenByCoach.forPlayer()).isEmpty();
    }

    @Test
    void aPlayerCannotWriteRedFlags() {
        client.put().uri("/api/v1/strength/red-flags")
                .header(HttpHeaders.AUTHORIZATION, bearerFor("player@example.com", PASSWORD))
                .body(new ReplaceRedFlagsRequest(List.of(new RedFlagInput("Nope", null))))
                .exchange()
                .expectStatus().isForbidden();
    }

    private void replace(String uri, List<RedFlagInput> flags) {
        client.put().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceRedFlagsRequest(flags))
                .exchange()
                .expectStatus().isOk();
    }
}
