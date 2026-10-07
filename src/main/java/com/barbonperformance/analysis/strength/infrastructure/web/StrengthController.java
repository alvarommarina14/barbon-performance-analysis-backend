package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.CyclePhaseService;
import com.barbonperformance.analysis.strength.application.DaySectionService;
import com.barbonperformance.analysis.strength.application.TrainingDayService;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDaySectionsRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/strength")
public class StrengthController {

    private final CyclePhaseService cyclePhases;
    private final TrainingDayService trainingDays;
    private final DaySectionService daySections;

    public StrengthController(CyclePhaseService cyclePhases, TrainingDayService trainingDays,
            DaySectionService daySections) {
        this.cyclePhases = cyclePhases;
        this.trainingDays = trainingDays;
        this.daySections = daySections;
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

    @GetMapping("/training-days/{trainingDayId}/sections")
    @Operation(summary = "One training day's blocks, each with its exercises")
    List<DaySectionView> daySections(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID trainingDayId) {
        return daySections.listForCaller(principal.id(), trainingDayId);
    }

    @PutMapping("/training-days/{trainingDayId}/sections")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace one training day's blocks and their exercises")
    List<DaySectionView> replaceDaySections(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID trainingDayId, @Valid @RequestBody ReplaceDaySectionsRequest request) {
        return daySections.replaceForDay(principal.id(), trainingDayId, request);
    }
}
