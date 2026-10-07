package com.barbonperformance.analysis.strength;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.ExerciseLogView;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.LogExerciseRequest;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.LoggedSetInput;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.SetPlanStartRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDaySectionsRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
import com.barbonperformance.analysis.strength.domain.TrainingDayKind;
import com.barbonperformance.analysis.strength.domain.WeightUnit;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

class ExerciseLogIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";

    private PlayerProfile player;
    private String coachAuth;
    private String playerAuth;
    private UUID libraryItemId;
    private TrainingDayView day;
    private DayExerciseView exercise;

    @BeforeEach
    void clientInWeekTwoWithOneExercise() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        player = fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);
        coachAuth = bearerFor("coach@example.com", PASSWORD);
        playerAuth = bearerFor("player@example.com", PASSWORD);
        libraryItemId = fixtures.createLibraryItem("Romanian deadlift").getId();

        client.put().uri("/api/v1/strength/players/{id}/plan", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new SetPlanStartRequest(LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).minusWeeks(1)))
                .exchange()
                .expectStatus().isOk();

        day = saveDays(new TrainingDayInput(null, "Lower body", null, 45, TrainingDayKind.STRENGTH, null)).get(0);
        exercise = saveExercises(day.id(), new DayExerciseInput(null, libraryItemId, "3x8", "3x8", "4x6", "2x8", null, null))
                .get(0);
    }

    @Test
    void thePlayerLogsSetsAndTheCoachReadsThem() {
        log(1, new LogExerciseRequest(WeightUnit.KG, List.of(
                new LoggedSetInput(8, new BigDecimal("60.00")),
                new LoggedSetInput(8, new BigDecimal("62.50")),
                new LoggedSetInput(7, null))))
                .expectStatus().isOk();

        List<ExerciseLogView> seenByCoach = client.get()
                .uri("/api/v1/strength/training-days/{id}/logs?planWeek=1", day.id())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<ExerciseLogView>>() {
                })
                .getResponseBody();

        assertThat(seenByCoach).hasSize(1);
        assertThat(seenByCoach.get(0).unit()).isEqualTo(WeightUnit.KG);
        assertThat(seenByCoach.get(0).sets()).extracting(set -> set.reps()).containsExactly(8, 8, 7);
        assertThat(seenByCoach.get(0).sets().get(2).weight()).isNull();
    }

    @Test
    void aWeekThatHasNotStartedCannotBeLogged() {
        log(3, new LogExerciseRequest(WeightUnit.LB, List.of(new LoggedSetInput(8, BigDecimal.TEN))))
                .expectStatus().isEqualTo(409);
    }

    @Test
    void editingTheProgramInPlaceKeepsTheLogs() {
        log(1, new LogExerciseRequest(WeightUnit.LB, List.of(new LoggedSetInput(10, new BigDecimal("135")))))
                .expectStatus().isOk();

        saveDays(new TrainingDayInput(day.id(), "Lower body — heavier", null, 50, TrainingDayKind.STRENGTH, null),
                new TrainingDayInput(null, "Upper body", null, 40, TrainingDayKind.STRENGTH, null));
        saveExercises(day.id(), new DayExerciseInput(exercise.id(), libraryItemId, "4x6", "4x6", "5x5", "2x8", "Brace", null));

        List<ExerciseLogView> seenByPlayer = client.get()
                .uri("/api/v1/strength/training-days/{id}/logs?planWeek=1", day.id())
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<ExerciseLogView>>() {
                })
                .getResponseBody();

        assertThat(seenByPlayer).extracting(ExerciseLogView::dayExerciseId).containsExactly(exercise.id());
    }

    @Test
    void daysAreNumberedByTheirPositionAndRenumberedWhenReordered() {
        List<TrainingDayView> saved = saveDays(
                new TrainingDayInput(null, "Upper body", null, 40, TrainingDayKind.STRENGTH, null),
                new TrainingDayInput(day.id(), "Lower body", null, 45, TrainingDayKind.STRENGTH, null));

        assertThat(saved).extracting(TrainingDayView::dayLabel, TrainingDayView::title)
                .containsExactly(
                        tuple("Day 1", "Upper body"),
                        tuple("Day 2", "Lower body"));
    }

    @Test
    void theCoachCannotLogForThePlayer() {
        client.put().uri("/api/v1/strength/day-exercises/{id}/logs/{week}", exercise.id(), 1)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new LogExerciseRequest(WeightUnit.KG, List.of(new LoggedSetInput(5, BigDecimal.ONE))))
                .exchange()
                .expectStatus().isForbidden();
    }

    private org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec log(int planWeek,
            LogExerciseRequest request) {
        return client.put().uri("/api/v1/strength/day-exercises/{id}/logs/{week}", exercise.id(), planWeek)
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .body(request)
                .exchange();
    }

    private List<TrainingDayView> saveDays(TrainingDayInput... days) {
        return client.put().uri("/api/v1/strength/players/{id}/training-days", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceTrainingDaysRequest(List.of(days)))
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<TrainingDayView>>() {
                })
                .getResponseBody();
    }

    private List<DayExerciseView> saveExercises(UUID trainingDayId, DayExerciseInput... exercises) {
        List<DaySectionView> sections = client.put().uri("/api/v1/strength/training-days/{id}/sections", trainingDayId)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceDaySectionsRequest(
                        List.of(new DaySectionInput(null, "Main work", null, List.of(exercises)))))
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<DaySectionView>>() {
                })
                .getResponseBody();
        return sections.get(0).exercises();
    }
}
