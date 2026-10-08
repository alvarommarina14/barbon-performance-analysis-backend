package com.barbonperformance.analysis.player.application.dto;

import com.barbonperformance.analysis.user.domain.MembershipTier;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PlayerHomeDtos {

    private PlayerHomeDtos() {
    }

    public record PlayerHomeResponse(
            @NotNull UUID profileId,
            @NotNull String name,
            String avatarUrl,
            @NotNull MembershipTier membershipTier,
            Integer planWeek,
            int planWeeks,
            Integer cycleWeek,
            LocalDate planStartedAt,
            Instant nextCallAt,
            CoachCard coach,
            PhaseCard currentPhase,
            String programPdfUrl) {
    }

    public record CoachCard(
            @NotNull UUID profileId,
            @NotNull String name,
            String avatarUrl,
            String bookingUrl) {
    }

    public record PhaseCard(
            int weekNumber,
            @NotNull String phaseName,
            @NotNull String goal,
            String rpeRange) {
    }
}
