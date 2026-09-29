package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.PlanService;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.PlanView;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.SetPlanStartRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
public class PlanController {

    private final PlanService plans;

    public PlanController(PlanService plans) {
        this.plans = plans;
    }

    @GetMapping("/plan")
    @Operation(summary = "The twelve-week plan: a player's own, or a coach's client's")
    PlanView plan(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return plans.get(principal.id(), playerProfileId);
    }

    @PutMapping("/players/{playerProfileId}/plan")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Set or clear the start date of a player's twelve-week plan")
    PlanView setPlanStart(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody SetPlanStartRequest request) {
        return plans.setStart(principal.id(), playerProfileId, request);
    }
}
