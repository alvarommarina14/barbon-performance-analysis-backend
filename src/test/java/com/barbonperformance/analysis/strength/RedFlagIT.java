package com.barbonperformance.analysis.strength;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagInput;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.ReplaceRedFlagsRequest;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class RedFlagIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String LIBRARY = "/api/v1/strength/red-flag-library";

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
    void aPlayerSeesOnlyTheFlagsTheirCoachGaveThemAndNeverTheLibrary() {
        replace(LIBRARY, List.of(
                new RedFlagInput("Sharp or electric pain", "Stop the exercise and message your coach."),
                new RedFlagInput("Joint pain that lingers over 48 hours", null)));
        replace(playerFlags(player), List.of(
                new RedFlagInput("Sharp or electric pain", "Stop the exercise and message your coach."),
                new RedFlagInput("Elbow pain above 3/10", "Skip the pressing work that day.")));
        replace(playerFlags(otherPlayer), List.of(new RedFlagInput("Someone else's knee", null)));

        assertThat(read("/api/v1/strength/red-flags", bearerFor("player@example.com", PASSWORD)))
                .extracting(RedFlagView::title)
                .containsExactly("Sharp or electric pain", "Elbow pain above 3/10");
    }

    @Test
    void aClientWithNoFlagsPickedSeesNone() {
        replace(LIBRARY, List.of(new RedFlagInput("Sharp or electric pain", null)));

        assertThat(read("/api/v1/strength/red-flags", bearerFor("player@example.com", PASSWORD))).isEmpty();
    }

    @Test
    void editingTheLibraryLeavesTheCopiesClientsAlreadyHave() {
        replace(LIBRARY, List.of(new RedFlagInput("Sharp pain", null)));
        replace(playerFlags(player), List.of(new RedFlagInput("Sharp pain", null)));

        replace(LIBRARY, List.of(new RedFlagInput("Sharp or electric pain", null)));

        assertThat(read(LIBRARY, coachAuth)).extracting(RedFlagView::title)
                .containsExactly("Sharp or electric pain");
        assertThat(read("/api/v1/strength/red-flags?playerProfileId=" + player.getId(), coachAuth))
                .extracting(RedFlagView::title)
                .containsExactly("Sharp pain");
    }

    @Test
    void aPlayerCannotReadTheLibraryOrWriteRedFlags() {
        String playerAuth = bearerFor("player@example.com", PASSWORD);

        client.get().uri(LIBRARY)
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isForbidden();
        client.put().uri(playerFlags(player))
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .body(new ReplaceRedFlagsRequest(List.of(new RedFlagInput("Nope", null))))
                .exchange()
                .expectStatus().isForbidden();
    }

    private static String playerFlags(PlayerProfile target) {
        return "/api/v1/strength/players/" + target.getId() + "/red-flags";
    }

    private void replace(String uri, List<RedFlagInput> flags) {
        client.put().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceRedFlagsRequest(flags))
                .exchange()
                .expectStatus().isOk();
    }

    private List<RedFlagView> read(String uri, String auth) {
        return List.of(client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(RedFlagView[].class)
                .getResponseBody());
    }
}
