package com.barbonperformance.courtproof.auth.dto;

import com.barbonperformance.courtproof.user.MembershipTier;
import com.barbonperformance.courtproof.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request and response shapes for {@code /api/v1/auth}. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 10, max = 128) String password,
            @NotBlank @Size(max = 120) String name,
            String timezone) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    /**
     * @param expiresInSeconds lifetime of the access token, so clients can refresh ahead of a 401
     */
    public record TokenPairResponse(
            @NotNull String accessToken,
            @NotNull String refreshToken,
            long expiresInSeconds) {
    }

    public record LoginResponse(
            @NotNull String accessToken,
            @NotNull String refreshToken,
            long expiresInSeconds,
            @NotNull CurrentUserResponse user) {
    }

    public record CurrentUserResponse(
            @NotNull UUID id,
            @NotNull String email,
            @NotNull String name,
            @NotNull UserRole role,
            String avatarUrl,
            @NotNull String timezone,
            PlayerSummary player,
            CoachSummary coach) {
    }

    /** Present only when the user is a PLAYER. */
    public record PlayerSummary(
            @NotNull UUID profileId,
            @NotNull MembershipTier membershipTier,
            int currentWeek,
            boolean hasCoach) {
    }

    /** Present only when the user is a COACH. */
    public record CoachSummary(
            @NotNull UUID profileId,
            String bookingUrl) {
    }
}
