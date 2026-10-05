package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.WeeklyNoteService;
import com.barbonperformance.analysis.strength.application.dto.WeeklyNoteDtos.WeeklyNoteView;
import com.barbonperformance.analysis.strength.application.dto.WeeklyNoteDtos.WriteWeeklyNoteRequest;
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
public class WeeklyNoteController {

    private final WeeklyNoteService weeklyNotes;

    public WeeklyNoteController(WeeklyNoteService weeklyNotes) {
        this.weeklyNotes = weeklyNotes;
    }

    @GetMapping("/weekly-notes")
    @Operation(summary = "Weekly notes: a player's own reached weeks, or all of a coach's client's")
    List<WeeklyNoteView> weeklyNotes(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return weeklyNotes.listForCaller(principal.id(), playerProfileId);
    }

    @PutMapping("/players/{playerProfileId}/weekly-notes/{planWeek}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Write one week's note for a player; an empty body deletes it")
    List<WeeklyNoteView> writeWeeklyNote(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @PathVariable int planWeek,
            @Valid @RequestBody WriteWeeklyNoteRequest request) {
        return weeklyNotes.write(principal.id(), playerProfileId, planWeek, request);
    }
}
