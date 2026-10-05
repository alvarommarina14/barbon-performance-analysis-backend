package com.barbonperformance.analysis.strength;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.SetPlanStartRequest;
import com.barbonperformance.analysis.strength.application.dto.WeeklyNoteDtos.WeeklyNoteView;
import com.barbonperformance.analysis.strength.application.dto.WeeklyNoteDtos.WriteWeeklyNoteRequest;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

class WeeklyNoteIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final ParameterizedTypeReference<List<WeeklyNoteView>> NOTES =
            new ParameterizedTypeReference<>() {
            };

    private PlayerProfile player;
    private String coachAuth;

    @BeforeEach
    void coachWithAClientInWeekTwo() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        player = fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);
        coachAuth = bearerFor("coach@example.com", PASSWORD);

        LocalDate oneWeekAgo = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).minusWeeks(1);
        client.put().uri("/api/v1/strength/players/{id}/plan", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new SetPlanStartRequest(oneWeekAgo))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void thePlayerOnlyReceivesTheWeeksTheyHaveReached() {
        write(1, "Good start — keep the rest periods honest.");
        write(2, "Volume goes up this week.");
        write(3, "Not yet.");

        List<WeeklyNoteView> seenByPlayer = client.get().uri("/api/v1/strength/weekly-notes")
                .header(HttpHeaders.AUTHORIZATION, bearerFor("player@example.com", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .returnResult(NOTES)
                .getResponseBody();

        assertThat(seenByPlayer).extracting(WeeklyNoteView::planWeek).containsExactly(1, 2);
        assertThat(seenByPlayer.get(1).weekStartsOn()).isEqualTo(
                LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")));
    }

    @Test
    void theCoachSeesEveryWeekAndClearsANoteWithAnEmptyBody() {
        write(1, "Week one.");
        write(12, "Last week — deload and test.");

        List<WeeklyNoteView> afterClearing = write(1, "   ");

        assertThat(afterClearing).extracting(WeeklyNoteView::planWeek).containsExactly(12);
    }

    @Test
    void aWeekOutsideThePlanDoesNotExist() {
        client.put().uri("/api/v1/strength/players/{id}/weekly-notes/{week}", player.getId(), 13)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new WriteWeeklyNoteRequest("Too far."))
                .exchange()
                .expectStatus().isNotFound();
    }

    private List<WeeklyNoteView> write(int planWeek, String body) {
        return client.put().uri("/api/v1/strength/players/{id}/weekly-notes/{week}", player.getId(), planWeek)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new WriteWeeklyNoteRequest(body))
                .exchange()
                .expectStatus().isOk()
                .returnResult(NOTES)
                .getResponseBody();
    }
}
