package com.barbonperformance.analysis.analysis.application.dto;

import com.barbonperformance.analysis.analysis.domain.AnalysisStatus;
import com.barbonperformance.analysis.video.domain.StrokeType;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AnalysisDtos {

    private AnalysisDtos() {
    }

    public record CreateAnalysisRequest(
            UUID submissionId,
            UUID playerProfileId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String summary) {

        @AssertTrue(message = "provide exactly one of submissionId or playerProfileId")
        public boolean isExactlyOneTarget() {
            return (submissionId == null) ^ (playerProfileId == null);
        }
    }

    public record UpdateAnalysisRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String summary) {
    }

    public record FindingInput(
            @PositiveOrZero int timestampSeconds,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String detail) {
    }

    public record ReplaceFindingsRequest(
            @NotNull @Valid List<FindingInput> findings) {
    }

    public record FeedbackUploadIntentRequest(
            @Positive long sizeBytes) {
    }

    public record FeedbackUploadIntentResponse(
            @NotNull UUID analysisId,
            @NotNull UUID videoAssetId,
            @NotNull String uploadUrl,
            int maxDurationSeconds) {
    }

    public record FindingView(
            @NotNull UUID id,
            int timestampSeconds,
            @NotNull String title,
            String detail,
            int sortOrder) {
    }

    public record AnalysisSummary(
            @NotNull UUID id,
            @NotNull String title,
            @NotNull AnalysisStatus status,
            StrokeType strokeType,
            @NotNull String coachName,
            @NotNull String playerName,
            boolean feedbackReady,
            Instant publishedAt,
            int findingCount,
            @NotNull Instant createdAt) {
    }

    public record AnalysisDetail(
            @NotNull UUID id,
            @NotNull AnalysisStatus status,
            @NotNull String title,
            String summary,
            @NotNull UUID playerProfileId,
            @NotNull String playerName,
            @NotNull UUID coachProfileId,
            @NotNull String coachName,
            UUID submissionId,
            StrokeType strokeType,
            String playerNote,
            UUID feedbackVideoAssetId,
            VideoAssetStatus feedbackVideoStatus,
            boolean feedbackReady,
            Instant publishedAt,
            @NotNull List<FindingView> findings,
            @NotNull Instant createdAt,
            @NotNull Instant updatedAt) {
    }

    public record PlaybackView(
            @NotNull UUID videoAssetId,
            @NotNull String hlsUrl,
            @NotNull String dashUrl,
            @NotNull String token,
            @NotNull Instant expiresAt) {
    }
}
