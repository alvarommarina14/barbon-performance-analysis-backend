package com.barbonperformance.analysis.coach.application.dto;

import com.barbonperformance.analysis.user.domain.MembershipTier;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CoachDtos {

    private CoachDtos() {
    }

    public record ClientSummary(
            @NotNull UUID profileId,
            @NotNull UUID userId,
            @NotNull String name,
            @NotNull String email,
            String avatarUrl,
            @NotNull MembershipTier membershipTier,
            Integer planWeek,
            int planWeeks,
            LocalDate planStartedAt,
            Instant nextCallAt) {
    }
}
