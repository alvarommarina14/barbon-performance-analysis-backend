package com.barbonperformance.analysis.strength;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ApplyProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.CreateProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramTemplateSummary;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramTemplateView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.TrainingProgramView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.UpdateProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.WriteTrainingProgramRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDaySectionsRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
import com.barbonperformance.analysis.strength.domain.TrainingDayKind;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;

class ProgramIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";

    private PlayerProfile player;
    private PlayerProfile otherClient;
    private String coachAuth;
    private String playerAuth;
    private String otherCoachAuth;
    private UUID squat;
    private UUID clamshell;

    @BeforeEach
    void coachWithTwoClients() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Federico Barbon");
        player = fixtures.createPlayer("player@example.com", PASSWORD, "Bill Van Mater", coach);
        otherClient = fixtures.createPlayer("other@example.com", PASSWORD, "Bob Abraham", coach);
        fixtures.createCoach("other-coach@example.com", PASSWORD, "Another Coach");
        coachAuth = bearerFor("coach@example.com", PASSWORD);
        playerAuth = bearerFor("player@example.com", PASSWORD);
        otherCoachAuth = bearerFor("other-coach@example.com", PASSWORD);
        squat = fixtures.createLibraryItem("Goblet Squat").getId();
        clamshell = fixtures.createLibraryItem("Banded Clamshell").getId();
    }

    @Test
    void aDayIsSplitIntoBlocksAndAnExerciseCanMoveBetweenThem() {
        TrainingDayView day = saveDays(player.getId(), strength(null, "Lower body")).get(0);

        List<DaySectionView> first = saveSections(day.id(),
                new DaySectionInput(null, "Injury prevention", "10-12 min", false,
                        List.of(exercise(null, clamshell, "2 × 12 ea", "Both sides."))),
                new DaySectionInput(null, "Main work", null, true,
                        List.of(exercise(null, squat, "3×8 @ RPE 7", null))));

        assertThat(first).extracting(DaySectionView::title, DaySectionView::note)
                .containsExactly(tuple("Injury prevention", "10-12 min"), tuple("Main work", null));
        assertThat(first.get(0).exercises()).extracting(DayExerciseView::title, DayExerciseView::notes)
                .containsExactly(tuple("Banded Clamshell", "Both sides."));
        assertThat(first).extracting(DaySectionView::variesByWeek).containsExactly(false, true);
        assertThat(first.get(0).exercises().get(0))
                .extracting(DayExerciseView::setsRepsWk1, DayExerciseView::setsRepsWk2, DayExerciseView::setsRepsWk4)
                .as("a block that doesn't vary by week keeps only week 1")
                .containsExactly("2 × 12 ea", null, null);

        DayExerciseView squatRow = first.get(1).exercises().get(0);
        List<DaySectionView> second = saveSections(day.id(),
                new DaySectionInput(first.get(0).id(), "Warm-up", null, true,
                        List.of(exercise(squatRow.id(), squat, "2×8", null))));

        assertThat(second).extracting(DaySectionView::id, DaySectionView::title)
                .containsExactly(tuple(first.get(0).id(), "Warm-up"));
        assertThat(second.get(0).exercises()).extracting(DayExerciseView::id).containsExactly(squatRow.id());

        List<DaySectionView> seenByPlayer = client.get()
                .uri("/api/v1/strength/training-days/{id}/sections", day.id())
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<DaySectionView>>() {
                })
                .getResponseBody();
        assertThat(seenByPlayer).extracting(DaySectionView::title).containsExactly("Warm-up");
    }

    @Test
    void onlyACardioDayKeepsItsProtocol() {
        List<TrainingDayView> days = saveDays(player.getId(),
                new TrainingDayInput(null, "Steady-State Cardio", "Aerobic base", 45, TrainingDayKind.CARDIO,
                        "  Stationary bike, 40 min.  "),
                new TrainingDayInput(null, "Upper body", null, 45, TrainingDayKind.STRENGTH, "ignored"));

        assertThat(days).extracting(TrainingDayView::kind, TrainingDayView::protocol)
                .containsExactly(
                        tuple(TrainingDayKind.CARDIO, "Stationary bike, 40 min."),
                        tuple(TrainingDayKind.STRENGTH, null));
    }

    @Test
    void theCoachWritesTheCoverAndThePlayerReadsIt() {
        client.put().uri("/api/v1/strength/players/{id}/program", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new WriteTrainingProgramRequest("USTA 4.0", "Tennis longevity.", " "))
                .exchange()
                .expectStatus().isOk();

        TrainingProgramView seen = client.get().uri("/api/v1/strength/program")
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(TrainingProgramView.class)
                .getResponseBody();

        assertThat(seen).isEqualTo(new TrainingProgramView("USTA 4.0", "Tennis longevity.", null));

        client.put().uri("/api/v1/strength/players/{id}/program", player.getId())
                .header(HttpHeaders.AUTHORIZATION, otherCoachAuth)
                .body(new WriteTrainingProgramRequest("USTA 5.0", null, null))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void aClientsProgramSavedAsATemplateCanBeAppliedToAnotherClient() {
        TrainingDayView day = saveDays(player.getId(), strength(null, "Lower body"),
                new TrainingDayInput(null, "Norwegian 4×4", null, 35, TrainingDayKind.CARDIO, "4 rounds.")).get(0);
        saveSections(day.id(), new DaySectionInput(null, "Main work", null, true,
                List.of(exercise(null, squat, "3×8 @ RPE 7", "KB at chest."))));
        saveWeeks(player.getId());
        client.put().uri("/api/v1/strength/players/{id}/program", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new WriteTrainingProgramRequest("USTA 4.0", "Longevity.", "Order the days freely."))
                .exchange()
                .expectStatus().isOk();

        ProgramTemplateView template = createTemplate(new CreateProgramTemplateRequest("Longevity", player.getId()));

        assertThat(template.about()).isEqualTo("Longevity.");
        assertThat(template.weeks()).hasSize(4);
        assertThat(template.days()).extracting(TrainingDayView::title, TrainingDayView::exerciseCount)
                .containsExactly(tuple("Lower body", 1), tuple("Norwegian 4×4", 0));

        client.post().uri("/api/v1/strength/players/{id}/program/apply-template", otherClient.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ApplyProgramTemplateRequest(template.id()))
                .exchange()
                .expectStatus().isNoContent();

        List<TrainingDayView> copied = client.get()
                .uri("/api/v1/strength/training-days?playerProfileId={id}", otherClient.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<TrainingDayView>>() {
                })
                .getResponseBody();
        assertThat(copied).extracting(TrainingDayView::dayLabel, TrainingDayView::title, TrainingDayView::protocol)
                .containsExactly(tuple("Day 1", "Lower body", null), tuple("Day 2", "Norwegian 4×4", "4 rounds."));
        assertThat(copied.get(0).id()).isNotEqualTo(day.id());

        List<DaySectionView> copiedBlocks = client.get()
                .uri("/api/v1/strength/training-days/{id}/sections", copied.get(0).id())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<DaySectionView>>() {
                })
                .getResponseBody();
        assertThat(copiedBlocks.get(0).exercises())
                .extracting(DayExerciseView::title, DayExerciseView::setsRepsWk1, DayExerciseView::notes)
                .containsExactly(tuple("Goblet Squat", "3×8 @ RPE 7", "KB at chest."));

        List<CyclePhaseView> copiedWeeks = client.get()
                .uri("/api/v1/strength/cycle-phases?playerProfileId={id}", otherClient.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<CyclePhaseView>>() {
                })
                .getResponseBody();
        assertThat(copiedWeeks).extracting(CyclePhaseView::phaseName)
                .containsExactly("Base", "Volume", "Intensity", "Deload");

        TrainingProgramView cover = client.get()
                .uri("/api/v1/strength/program?playerProfileId={id}", otherClient.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(TrainingProgramView.class)
                .getResponseBody();
        assertThat(cover).isEqualTo(new TrainingProgramView(null, "Longevity.", "Order the days freely."));
    }

    @Test
    void aTemplateIsBuiltFromScratchAndEditingItLeavesClientsAlone() {
        ProgramTemplateView template = createTemplate(new CreateProgramTemplateRequest("Tennis elbow", null));

        ProgramTemplateView withDays = client.put()
                .uri("/api/v1/strength/program-templates/{id}/training-days", template.id())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceTrainingDaysRequest(List.of(
                        new TrainingDayInput(null, "Rehab", null, 25, TrainingDayKind.REHAB, null))))
                .exchange()
                .expectStatus().isOk()
                .returnResult(ProgramTemplateView.class)
                .getResponseBody();
        UUID templateDay = withDays.days().get(0).id();
        saveSections(templateDay, new DaySectionInput(null, "A. Pain modulation", "start here if flared", true,
                List.of(exercise(null, clamshell, "4 × 30s", null))));

        client.post().uri("/api/v1/strength/players/{id}/program/apply-template", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ApplyProgramTemplateRequest(template.id()))
                .exchange()
                .expectStatus().isNoContent();

        client.put().uri("/api/v1/strength/program-templates/{id}", template.id())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new UpdateProgramTemplateRequest("Tennis elbow v2", "Chain problem.", null))
                .exchange()
                .expectStatus().isOk();
        saveSections(templateDay);

        List<TrainingDayView> clientDays = client.get().uri("/api/v1/strength/training-days")
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<TrainingDayView>>() {
                })
                .getResponseBody();
        assertThat(clientDays).extracting(TrainingDayView::title, TrainingDayView::exerciseCount)
                .containsExactly(tuple("Rehab", 1));

        List<ProgramTemplateSummary> listed = client.get().uri("/api/v1/strength/program-templates")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<ProgramTemplateSummary>>() {
                })
                .getResponseBody();
        assertThat(listed).extracting(ProgramTemplateSummary::name, ProgramTemplateSummary::dayCount)
                .containsExactly(tuple("Tennis elbow v2", 1));
    }

    @Test
    void templateNamesAreUniqueAndAPlayerCannotSeeATemplateDay() {
        ProgramTemplateView template = createTemplate(new CreateProgramTemplateRequest("Longevity", null));

        client.post().uri("/api/v1/strength/program-templates")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new CreateProgramTemplateRequest("Longevity", null))
                .exchange()
                .expectStatus().isEqualTo(409);

        UUID templateDay = client.put()
                .uri("/api/v1/strength/program-templates/{id}/training-days", template.id())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceTrainingDaysRequest(List.of(strength(null, "Lower body"))))
                .exchange()
                .expectStatus().isOk()
                .returnResult(ProgramTemplateView.class)
                .getResponseBody()
                .days().get(0).id();

        client.get().uri("/api/v1/strength/training-days/{id}/sections", templateDay)
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isNotFound();
    }

    private static TrainingDayInput strength(UUID id, String title) {
        return new TrainingDayInput(id, title, null, 45, TrainingDayKind.STRENGTH, null);
    }

    private static DayExerciseInput exercise(UUID id, UUID libraryItemId, String sets, String notes) {
        return new DayExerciseInput(id, libraryItemId, sets, sets, sets, sets, null, notes);
    }

    private List<TrainingDayView> saveDays(UUID playerProfileId, TrainingDayInput... days) {
        return client.put().uri("/api/v1/strength/players/{id}/training-days", playerProfileId)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceTrainingDaysRequest(List.of(days)))
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<TrainingDayView>>() {
                })
                .getResponseBody();
    }

    private List<DaySectionView> saveSections(UUID trainingDayId, DaySectionInput... sections) {
        return client.put().uri("/api/v1/strength/training-days/{id}/sections", trainingDayId)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceDaySectionsRequest(List.of(sections)))
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<DaySectionView>>() {
                })
                .getResponseBody();
    }

    private void saveWeeks(UUID playerProfileId) {
        client.put().uri("/api/v1/strength/players/{id}/cycle-phases", playerProfileId)
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceCyclePhasesRequest(List.of(
                        new CyclePhaseInput(1, "Base", "Learn the movements", "RPE 5-6", null),
                        new CyclePhaseInput(2, "Volume", "Build work capacity", "RPE 6-7", null),
                        new CyclePhaseInput(3, "Intensity", "Build strength", "RPE 7-8", null),
                        new CyclePhaseInput(4, "Deload", "Recover", "RPE 4-5", null))))
                .exchange()
                .expectStatus().isOk();
    }

    private ProgramTemplateView createTemplate(CreateProgramTemplateRequest request) {
        return client.post().uri("/api/v1/strength/program-templates")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(ProgramTemplateView.class)
                .getResponseBody();
    }
}
