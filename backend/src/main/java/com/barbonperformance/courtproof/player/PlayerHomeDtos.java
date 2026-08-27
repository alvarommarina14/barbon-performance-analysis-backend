package com.barbonperformance.courtproof.player;

import com.barbonperformance.courtproof.user.MembershipTier;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PlayerHomeDtos {

    private PlayerHomeDtos() {
    }

    /**
     * What the mockup's player home shows above the fold. The video, drills and strength
     * tiles land here in later phases.
     *
     * @param cycleWeeks total weeks in the strength cycle, so the client renders "2 / 4"
     *                   without hard-coding the four
     */
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

    /** Null while the player has not been taken on by a coach. */
    public record CoachCard(
            @NotNull UUID profileId,
            @NotNull String name,
            String avatarUrl,
            String bookingUrl) {
    }
}
