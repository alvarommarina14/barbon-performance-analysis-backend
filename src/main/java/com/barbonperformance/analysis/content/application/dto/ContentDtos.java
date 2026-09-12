package com.barbonperformance.analysis.content.application.dto;

import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.UUID;

public final class ContentDtos {

    private ContentDtos() {
    }

    public record RecordingTipsView(
            UUID videoAssetId,
            VideoAssetStatus videoStatus,
            String pdfUrl,
            Instant updatedAt) {
    }

    public record VideoUploadIntentRequest(@Positive long sizeBytes) {
    }

    public record VideoUploadIntentResponse(
            @NotNull UUID videoAssetId,
            @NotNull String uploadUrl,
            int maxDurationSeconds) {
    }

    public record ConfirmVideoRequest(@NotNull UUID videoAssetId) {
    }

    public record RecordingTipsPlaybackView(
            @NotNull UUID videoAssetId,
            @NotNull String hlsUrl,
            @NotNull String dashUrl,
            @NotNull String token,
            @NotNull Instant expiresAt) {
    }

    public record PdfUploadIntentRequest(
            @Positive long sizeBytes,
            @NotBlank String contentType) {
    }

    public record PdfUploadIntentResponse(
            @NotNull String uploadUrl,
            @NotNull String objectKey) {
    }

    public record ConfirmPdfRequest(
            @NotBlank String objectKey,
            @NotBlank String contentType) {
    }
}
