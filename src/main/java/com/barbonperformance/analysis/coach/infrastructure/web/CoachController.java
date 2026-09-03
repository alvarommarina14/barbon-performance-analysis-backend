package com.barbonperformance.analysis.coach.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.coach.application.CoachService;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.ClientSummary;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coach")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @GetMapping("/clients")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "The signed-in coach's clients")
    List<ClientSummary> clients(@AuthenticationPrincipal AuthenticatedUser principal) {
        return coachService.clients(principal.id());
    }
}
