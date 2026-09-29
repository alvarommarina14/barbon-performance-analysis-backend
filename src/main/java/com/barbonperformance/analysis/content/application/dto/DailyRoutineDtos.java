package com.barbonperformance.analysis.content.application.dto;

import com.barbonperformance.analysis.content.domain.DailyRoutinePart;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public final class DailyRoutineDtos {

    private DailyRoutineDtos() {
    }

    public record RoutineVideoView(
            @NotNull UUID videoAssetId,
            @NotNull VideoAssetStatus status,
            String fileName,
            Long sizeBytes,
            Integer durationSeconds,
            Instant uploadedAt) {
    }

    public record DailyRoutineView(
            RoutineVideoView warmup,
            RoutineVideoView recovery,
            String pdfUrl,
            String pdfFileName,
            Long pdfSizeBytes,
            Instant pdfUploadedAt,
            Instant updatedAt) {
    }

    public record RoutineVideoUploadIntentRequest(
            @NotNull DailyRoutinePart part,
            @Positive long sizeBytes,
            @NotBlank String fileName) {
    }

    public record ConfirmRoutineVideoRequest(@NotNull DailyRoutinePart part, @NotNull UUID videoAssetId) {
    }

    public record DailyRoutinePlaybackView(
            @NotNull UUID videoAssetId,
            @NotNull String hlsUrl,
            @NotNull String dashUrl,
            @NotNull String token,
            @NotNull Instant expiresAt) {
    }
}
