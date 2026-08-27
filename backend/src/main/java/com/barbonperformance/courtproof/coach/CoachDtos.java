package com.barbonperformance.courtproof.coach;

import com.barbonperformance.courtproof.user.MembershipTier;
import java.time.Instant;
import java.util.UUID;

public final class CoachDtos {

    private CoachDtos() {
    }

    /** One row of the coach's client list. */
    public record ClientSummary(
            UUID profileId,
            UUID userId,
            String name,
            String email,
            String avatarUrl,
            MembershipTier membershipTier,
            int currentWeek,
            Instant nextCallAt) {
    }
}
