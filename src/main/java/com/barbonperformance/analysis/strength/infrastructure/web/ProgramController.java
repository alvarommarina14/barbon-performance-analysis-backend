package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.ProgramDocumentService;
import com.barbonperformance.analysis.strength.application.ProgramTemplateService;
import com.barbonperformance.analysis.strength.application.TrainingProgramService;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ApplyProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.CreateProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramPdfLinkView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramTemplateSummary;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramTemplateView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.TrainingProgramView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.UpdateProgramTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.WriteTrainingProgramRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/strength")
public class ProgramController {

    private final TrainingProgramService programs;
    private final ProgramTemplateService templates;
    private final ProgramDocumentService documents;

    public ProgramController(TrainingProgramService programs, ProgramTemplateService templates,
            ProgramDocumentService documents) {
        this.programs = programs;
        this.templates = templates;
        this.documents = documents;
    }

    @GetMapping("/program")
    @Operation(summary = "A program's cover texts: a player's own, or a coach's client's")
    TrainingProgramView program(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return programs.viewForCaller(principal.id(), playerProfileId);
    }

    @GetMapping("/program-pdf-link")
    @Operation(summary = "A short-lived link that opens the program PDF in any browser, generated on open")
    ProgramPdfLinkView programPdfLink(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return documents.linkForCaller(principal.id(), playerProfileId);
    }

    @PutMapping("/players/{playerProfileId}/program")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Write a client's athlete level, \"about your program\" and schedule texts")
    TrainingProgramView writeProgram(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody WriteTrainingProgramRequest request) {
        return programs.write(principal.id(), playerProfileId, request);
    }

    @PostMapping("/players/{playerProfileId}/program/apply-template")
    @PreAuthorize("hasRole('COACH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Replace a client's days and cycle weeks with a copy of a program template")
    void applyTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody ApplyProgramTemplateRequest request) {
        templates.applyToPlayer(principal.id(), playerProfileId, request);
    }

    @GetMapping("/program-templates")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "The organisation's program templates")
    List<ProgramTemplateSummary> programTemplates(@AuthenticationPrincipal AuthenticatedUser principal) {
        return templates.list(principal.id());
    }

    @PostMapping("/program-templates")
    @PreAuthorize("hasRole('COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a program template, empty or as a copy of a client's program")
    ProgramTemplateView createProgramTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateProgramTemplateRequest request) {
        return templates.create(principal.id(), request);
    }

    @GetMapping("/program-templates/{templateId}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "One program template with its cycle weeks and days")
    ProgramTemplateView programTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID templateId) {
        return templates.view(principal.id(), templateId);
    }

    @PutMapping("/program-templates/{templateId}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Rename a program template and write its cover texts")
    ProgramTemplateView updateProgramTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID templateId, @Valid @RequestBody UpdateProgramTemplateRequest request) {
        return templates.update(principal.id(), templateId, request);
    }

    @DeleteMapping("/program-templates/{templateId}")
    @PreAuthorize("hasRole('COACH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a program template; clients it was applied to keep their copy")
    void deleteProgramTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID templateId) {
        templates.delete(principal.id(), templateId);
    }

    @PutMapping("/program-templates/{templateId}/cycle-phases")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace a program template's four cycle weeks")
    ProgramTemplateView replaceTemplateWeeks(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID templateId, @Valid @RequestBody ReplaceCyclePhasesRequest request) {
        return templates.replaceWeeks(principal.id(), templateId, request);
    }

    @PutMapping("/program-templates/{templateId}/training-days")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace a program template's training days")
    ProgramTemplateView replaceTemplateDays(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID templateId, @Valid @RequestBody ReplaceTrainingDaysRequest request) {
        return templates.replaceDays(principal.id(), templateId, request);
    }
}
