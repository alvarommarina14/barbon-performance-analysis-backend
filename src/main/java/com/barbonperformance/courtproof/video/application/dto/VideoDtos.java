package com.barbonperformance.courtproof.video.application.dto;

import com.barbonperformance.courtproof.video.domain.StrokeType;
import com.barbonperformance.courtproof.video.domain.SubmissionStatus;
import com.barbonperformance.courtproof.video.domain.VideoAssetStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class VideoDtos {

    private VideoDtos() {
    }

    public record UploadIntentRequest(
            @NotNull StrokeType strokeType,
            @Size(max = 2000) String playerNote,
            Instant recordedAt) {
    }

    public record UploadIntentResponse(
            @NotNull UUID submissionId,
            @NotNull UUID videoAssetId,
            @NotNull String uploadUrl,
            int maxDurationSeconds) {
    }

    public record SubmissionSummary(
            @NotNull UUID id,
            @NotNull UUID playerProfileId,
            @NotNull String playerName,
            @NotNull StrokeType strokeType,
            String playerNote,
            Instant recordedAt,
            @NotNull SubmissionStatus status,
            @NotNull VideoAssetStatus assetStatus,
            @NotNull UUID videoAssetId,
            Integer durationSeconds,
            String thumbnailUrl,
            @NotNull Instant createdAt) {
    }

    public record PlaybackResponse(
            @NotNull UUID videoAssetId,
            @NotNull String hlsUrl,
            @NotNull String dashUrl,
            @NotNull String token,
            @NotNull Instant expiresAt) {
    }
}
