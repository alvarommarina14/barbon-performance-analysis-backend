package com.barbonperformance.analysis.auth.application.dto;

import com.barbonperformance.analysis.user.domain.MembershipTier;
import com.barbonperformance.analysis.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 10, max = 128) String password,
            @NotBlank @Size(max = 120) String name,
            String timezone) {
    }

    public record RegisterCoachRequest(
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

    public record PlayerSummary(
            @NotNull UUID profileId,
            @NotNull MembershipTier membershipTier,
            Integer planWeek,
            boolean hasCoach) {
    }

    public record CoachSummary(
            @NotNull UUID profileId,
            String bookingUrl) {
    }
}
