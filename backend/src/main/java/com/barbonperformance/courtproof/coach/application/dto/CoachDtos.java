package com.barbonperformance.courtproof.coach.application.dto;

import com.barbonperformance.courtproof.user.domain.MembershipTier;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
            int currentWeek,
            Instant nextCallAt) {
    }
}
