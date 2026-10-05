package com.barbonperformance.analysis.strength.application.dto;

import com.barbonperformance.analysis.strength.domain.WeightUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ExerciseLogDtos {

    private ExerciseLogDtos() {
    }

    // A null weight is a bodyweight set.
    public record LoggedSetInput(
            @Min(0) @Max(999) int reps,
            @DecimalMin("0") @DecimalMax("9999.99") BigDecimal weight) {
    }

    public record LogExerciseRequest(
            @NotNull WeightUnit unit,
            @NotNull @Valid @Size(max = 20) List<LoggedSetInput> sets) {
    }

    public record LoggedSetView(int setNumber, int reps, BigDecimal weight) {
    }

    public record ExerciseLogView(
            @NotNull UUID dayExerciseId,
            int planWeek,
            @NotNull WeightUnit unit,
            @NotNull Instant loggedAt,
            @NotNull List<LoggedSetView> sets) {
    }
}
