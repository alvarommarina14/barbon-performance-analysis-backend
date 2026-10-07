package com.barbonperformance.analysis.strength.application.dto;

import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProgramDtos {

    private ProgramDtos() {
    }

    public record WriteTrainingProgramRequest(
            @Size(max = 100) String athleteLevel,
            @Size(max = 4000) String about,
            @Size(max = 2000) String schedule) {
    }

    public record TrainingProgramView(String athleteLevel, String about, String schedule) {
    }

    // With fromPlayerProfileId the new template starts as a copy of that client's whole program.
    public record CreateProgramTemplateRequest(
            @NotBlank @Size(max = 100) String name,
            UUID fromPlayerProfileId) {
    }

    public record UpdateProgramTemplateRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 4000) String about,
            @Size(max = 2000) String schedule) {
    }

    public record ProgramTemplateSummary(@NotNull UUID id, @NotNull String name, int dayCount) {
    }

    public record ProgramTemplateView(
            @NotNull UUID id,
            @NotNull String name,
            String about,
            String schedule,
            @NotNull List<CyclePhaseView> weeks,
            @NotNull List<TrainingDayView> days) {
    }

    public record ApplyProgramTemplateRequest(@NotNull UUID templateId) {
    }

    public record ProgramPdfLinkView(@NotNull String url, @NotNull Instant expiresAt) {
    }

    public record RenderedProgramPdf(String fileName, byte[] pdf) {
    }
}
