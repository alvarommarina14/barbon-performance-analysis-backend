package com.barbonperformance.courtproof.player;

import com.barbonperformance.courtproof.user.MembershipTier;
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
            int currentWeek,
            int cycleWeeks,
            LocalDate cycleStartedAt,
            Instant nextCallAt,
            CoachCard coach) {
    }

    public record CoachCard(
            @NotNull UUID profileId,
            @NotNull String name,
            String avatarUrl,
            String bookingUrl) {
    }
}
