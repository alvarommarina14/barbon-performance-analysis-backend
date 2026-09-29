package com.barbonperformance.analysis.strength;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.PlanView;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.SetPlanStartRequest;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class PlanIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final ZoneId PLAYER_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @Test
    void coachStartsThePlanAndThePlayerSeesTheCurrentWeek() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        PlayerProfile player = fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);
        LocalDate fiveWeeksAgo = LocalDate.now(PLAYER_ZONE).minusWeeks(5);

        PlanView plan = client.put().uri("/api/v1/strength/players/{id}/plan", player.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerFor("coach@example.com", PASSWORD))
                .body(new SetPlanStartRequest(fiveWeeksAgo))
                .exchange()
                .expectStatus().isOk()
                .returnResult(PlanView.class)
                .getResponseBody();

        assertThat(plan.planWeek()).isEqualTo(6);
        assertThat(plan.cycleWeek()).isEqualTo(2);
        assertThat(plan.unlockedWeeks()).isEqualTo(6);

        client.get().uri("/api/v1/players/me/home")
                .header(HttpHeaders.AUTHORIZATION, bearerFor("player@example.com", PASSWORD))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("\"planWeek\":6");
                    assertThat(body).contains("\"cycleWeek\":2");
                });
    }

    @Test
    void anotherCoachCannotStartThePlan() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        fixtures.createCoach("other@example.com", PASSWORD, "Other Coach");
        PlayerProfile player = fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);

        client.put().uri("/api/v1/strength/players/{id}/plan", player.getId())
                .header(HttpHeaders.AUTHORIZATION, bearerFor("other@example.com", PASSWORD))
                .body(new SetPlanStartRequest(LocalDate.now(PLAYER_ZONE)))
                .exchange()
                .expectStatus().isNotFound();
    }
}
