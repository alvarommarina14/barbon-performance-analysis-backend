package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.CyclePhaseService;
import com.barbonperformance.analysis.strength.application.CyclePhaseTemplateService;
import com.barbonperformance.analysis.strength.application.DayExerciseService;
import com.barbonperformance.analysis.strength.application.TrainingDayService;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ApplyCyclePhaseTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseTemplateView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhaseTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDayExercisesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
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
public class StrengthController {

    private final CyclePhaseService cyclePhases;
    private final CyclePhaseTemplateService cyclePhaseTemplates;
    private final TrainingDayService trainingDays;
    private final DayExerciseService dayExercises;

    public StrengthController(CyclePhaseService cyclePhases, CyclePhaseTemplateService cyclePhaseTemplates,
            TrainingDayService trainingDays, DayExerciseService dayExercises) {
        this.cyclePhases = cyclePhases;
        this.cyclePhaseTemplates = cyclePhaseTemplates;
        this.trainingDays = trainingDays;
        this.dayExercises = dayExercises;
    }

    @GetMapping("/cycle-phases")
    @Operation(summary = "Cycle phases: a player's own, or a coach's client's")
    List<CyclePhaseView> cyclePhases(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return cyclePhases.listForCaller(principal.id(), playerProfileId);
    }

    @PutMapping("/players/{playerProfileId}/cycle-phases")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace the whole cycle phase list for one player")
    List<CyclePhaseView> replaceCyclePhases(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody ReplaceCyclePhasesRequest request) {
        return cyclePhases.replaceForPlayer(principal.id(), playerProfileId, request);
    }

    @PostMapping("/players/{playerProfileId}/cycle-phases/apply-template")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Copy a template's weeks into one player's cycle phases")
    List<CyclePhaseView> applyCyclePhaseTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody ApplyCyclePhaseTemplateRequest request) {
        return cyclePhases.applyTemplateToPlayer(principal.id(), playerProfileId, request);
    }

    @GetMapping("/cycle-phase-templates")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "The organisation's reusable cycle phase templates")
    List<CyclePhaseTemplateView> cyclePhaseTemplates(@AuthenticationPrincipal AuthenticatedUser principal) {
        return cyclePhaseTemplates.list(principal.id());
    }

    @PutMapping("/cycle-phase-templates/{name}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Create or replace a named cycle phase template")
    CyclePhaseTemplateView replaceCyclePhaseTemplate(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable String name, @Valid @RequestBody ReplaceCyclePhaseTemplateRequest request) {
        return cyclePhaseTemplates.createOrReplace(principal.id(), name, request);
    }

    @DeleteMapping("/cycle-phase-templates/{name}")
    @PreAuthorize("hasRole('COACH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a cycle phase template")
    void deleteCyclePhaseTemplate(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable String name) {
        cyclePhaseTemplates.delete(principal.id(), name);
    }

    @GetMapping("/training-days")
    @Operation(summary = "Training days: a player's own, or a coach's client's")
    List<TrainingDayView> trainingDays(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return trainingDays.listForCaller(principal.id(), playerProfileId);
    }

    @PutMapping("/players/{playerProfileId}/training-days")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace the whole training day list for one player")
    List<TrainingDayView> replaceTrainingDays(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody ReplaceTrainingDaysRequest request) {
        return trainingDays.replaceForPlayer(principal.id(), playerProfileId, request);
    }

    @GetMapping("/training-days/{trainingDayId}/exercises")
    @Operation(summary = "The exercises assigned to one training day")
    List<DayExerciseView> dayExercises(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID trainingDayId) {
        return dayExercises.listForCaller(principal.id(), trainingDayId);
    }

    @PutMapping("/training-days/{trainingDayId}/exercises")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace the whole exercise list for one training day")
    List<DayExerciseView> replaceDayExercises(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID trainingDayId, @Valid @RequestBody ReplaceDayExercisesRequest request) {
        return dayExercises.replaceForDay(principal.id(), trainingDayId, request);
    }
}
