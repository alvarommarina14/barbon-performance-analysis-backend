package com.barbonperformance.analysis.strength.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class WeeklyNoteDtos {

    private WeeklyNoteDtos() {
    }

    public record WriteWeeklyNoteRequest(@Size(max = 4000) String body) {
    }

    public record WeeklyNoteView(
            int planWeek,
            @NotNull String body,
            LocalDate weekStartsOn,
            @NotNull Instant updatedAt) {
    }
}
