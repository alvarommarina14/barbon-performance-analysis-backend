package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.ExerciseLogService;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.ExerciseLogView;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.LogExerciseRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
public class ExerciseLogController {

    private final ExerciseLogService logs;

    public ExerciseLogController(ExerciseLogService logs) {
        this.logs = logs;
    }

    @GetMapping("/training-days/{trainingDayId}/logs")
    @Operation(summary = "What was logged on one training day in one plan week")
    List<ExerciseLogView> dayLogs(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID trainingDayId, @RequestParam int planWeek) {
        return logs.listForDay(principal.id(), trainingDayId, planWeek);
    }

    @PutMapping("/day-exercises/{dayExerciseId}/logs/{planWeek}")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "Log the sets of one exercise for one plan week; no sets removes the log")
    ResponseEntity<ExerciseLogView> logExercise(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID dayExerciseId, @PathVariable int planWeek,
            @Valid @RequestBody LogExerciseRequest request) {
        return logs.log(principal.id(), dayExerciseId, planWeek, request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
