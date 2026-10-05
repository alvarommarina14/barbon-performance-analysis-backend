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
            String videoFileName,
            Long videoSizeBytes,
            Integer videoDurationSeconds,
            Instant videoUploadedAt,
            String pdfUrl,
            String pdfFileName,
            Long pdfSizeBytes,
            Instant pdfUploadedAt,
            Instant updatedAt) {
    }

    public record VideoUploadIntentRequest(@Positive long sizeBytes, @NotBlank String fileName) {
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

    // Shared by every "confirm a plain PDF upload" endpoint (recording tips, RPE guide): each
    // keeps its own object key template, but the confirmation payload is always the same shape.
    public record ConfirmPdfUploadRequest(
            @NotBlank String objectKey,
            @NotBlank String contentType,
            @NotBlank String fileName,
            @Positive long sizeBytes) {
    }

    public record RpeGuideView(
            String pdfUrl,
            String pdfFileName,
            Long pdfSizeBytes,
            Instant pdfUploadedAt,
            Instant updatedAt) {
    }
}
