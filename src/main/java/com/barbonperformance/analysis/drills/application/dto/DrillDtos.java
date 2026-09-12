package com.barbonperformance.analysis.drills.application.dto;

import com.barbonperformance.analysis.drills.domain.DrillCategory;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DrillDtos {

    private DrillDtos() {
    }

    public record LibraryUploadIntentRequest(@Positive long sizeBytes) {
    }

    public record LibraryUploadIntentResponse(
            @NotNull UUID videoAssetId,
            @NotNull String uploadUrl,
            int maxDurationSeconds) {
    }

    public record CreateLibraryItemRequest(
            @NotNull UUID videoAssetId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String cue,
            @NotNull DrillCategory category) {
    }

    public record UpdateLibraryItemRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String cue,
            @NotNull DrillCategory category) {
    }

    public record LibraryItemView(
            @NotNull UUID id,
            @NotNull String title,
            String cue,
            @NotNull DrillCategory category,
            @NotNull UUID videoAssetId,
            @NotNull VideoAssetStatus videoStatus,
            Integer durationSeconds,
            String thumbnailUrl,
            @NotNull Instant createdAt) {
    }

    public record AssignDrillInput(
            @NotNull UUID libraryItemId,
            @Size(max = 2000) String coachNote) {
    }

    public record ReplaceAssignmentsRequest(
            @NotNull @Valid List<AssignDrillInput> drills) {
    }

    public record DrillAssignmentView(
            @NotNull UUID id,
            @NotNull UUID libraryItemId,
            @NotNull String title,
            String cue,
            @NotNull DrillCategory category,
            @NotNull UUID videoAssetId,
            @NotNull VideoAssetStatus videoStatus,
            Integer durationSeconds,
            String thumbnailUrl,
            String coachNote,
            UUID analysisId,
            String analysisTitle,
            Integer sortOrder) {
    }

    public record DrillPlaybackView(
            @NotNull UUID videoAssetId,
            @NotNull String hlsUrl,
            @NotNull String dashUrl,
            @NotNull String token,
            @NotNull Instant expiresAt) {
    }
}
