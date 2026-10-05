package com.barbonperformance.analysis.strength.application.dto;

import com.barbonperformance.analysis.drills.domain.DrillCategory;
import com.barbonperformance.analysis.strength.domain.TrainingDayKind;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class StrengthDtos {

    private StrengthDtos() {
    }

    public record CyclePhaseInput(
            @Min(1) @Max(4) int weekNumber,
            @NotBlank @Size(max = 100) String phaseName,
            @NotBlank @Size(max = 500) String goal,
            @Size(max = 100) String setsReps,
            @Size(max = 50) String rpeRange) {
    }

    public record ReplaceCyclePhasesRequest(@NotNull @Valid List<CyclePhaseInput> phases) {
    }

    public record CyclePhaseView(
            @NotNull UUID id,
            int weekNumber,
            @NotNull String phaseName,
            @NotNull String goal,
            String setsReps,
            String rpeRange) {
    }

    public record ReplaceCyclePhaseTemplateRequest(@NotNull @Valid List<CyclePhaseInput> weeks) {
    }

    public record CyclePhaseTemplateView(@NotNull String name, @NotNull List<CyclePhaseView> weeks) {
    }

    public record ApplyCyclePhaseTemplateRequest(@NotBlank String templateName) {
    }

    // id names an existing day to update in place; without one the day is new. Updating in place
    // keeps the day's exercises and the player's logs, which a delete-and-recreate would cascade away.
    // There is no label: a day is named "Day N" after its position in the list.
    public record TrainingDayInput(
            UUID id,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 500) String focus,
            Integer durationMinutes,
            @NotNull TrainingDayKind kind) {
    }

    public record ReplaceTrainingDaysRequest(@NotNull @Valid List<TrainingDayInput> days) {
    }

    public record TrainingDayView(
            @NotNull UUID id,
            @NotNull String dayLabel,
            @NotNull String title,
            String focus,
            Integer durationMinutes,
            @NotNull TrainingDayKind kind,
            int sortOrder,
            int exerciseCount) {
    }

    public record DayExerciseInput(
            UUID id,
            @NotNull UUID libraryItemId,
            @Size(max = 50) String setsRepsWk1,
            @Size(max = 50) String setsRepsWk2,
            @Size(max = 50) String setsRepsWk3,
            @Size(max = 50) String setsRepsWk4,
            @Size(max = 500) String cueOverride) {
    }

    public record ReplaceDayExercisesRequest(@NotNull @Valid List<DayExerciseInput> exercises) {
    }

    public record DayExerciseView(
            @NotNull UUID id,
            @NotNull UUID libraryItemId,
            @NotNull String title,
            String cue,
            @NotNull DrillCategory category,
            @NotNull UUID videoAssetId,
            @NotNull VideoAssetStatus videoStatus,
            Integer durationSeconds,
            String thumbnailUrl,
            String cueOverride,
            String setsRepsWk1,
            String setsRepsWk2,
            String setsRepsWk3,
            String setsRepsWk4,
            int sortOrder) {
    }
}
