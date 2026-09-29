package com.barbonperformance.analysis.player.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class ProgramPdfDtos {

    private ProgramPdfDtos() {
    }

    public record ProgramPdfUploadIntentRequest(@Positive long sizeBytes) {
    }

    public record ProgramPdfUploadIntentResponse(
            @NotNull String uploadUrl,
            @NotNull String objectKey) {
    }

    public record ConfirmProgramPdfRequest(@NotBlank String objectKey) {
    }
}
