package com.barbonperformance.analysis.coach.application.dto;

import com.barbonperformance.analysis.user.domain.MembershipTier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    // Blank clears the link. Only https: the player's phone opens it straight in the browser.
    public record SetBookingLinkRequest(
            @Size(max = 500) @Pattern(regexp = "^$|^https://[^\\s/?#]+\\.[^\\s]*$",
                    message = "must be an https:// link") String bookingUrl) {
    }

    public record BookingLinkView(String bookingUrl) {
    }
}
