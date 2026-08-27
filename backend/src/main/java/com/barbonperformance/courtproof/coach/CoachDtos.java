package com.barbonperformance.courtproof.coach;

import com.barbonperformance.courtproof.user.MembershipTier;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class CoachDtos {

    private CoachDtos() {
    }

    /** One row of the coach's client list. */
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
