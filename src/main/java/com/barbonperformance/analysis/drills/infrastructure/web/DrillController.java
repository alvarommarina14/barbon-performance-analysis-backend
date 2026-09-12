package com.barbonperformance.analysis.drills.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.drills.application.DrillAssignmentService;
import com.barbonperformance.analysis.drills.application.DrillLibraryService;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.CreateLibraryItemRequest;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.DrillAssignmentView;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.DrillPlaybackView;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.LibraryItemView;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.LibraryUploadIntentRequest;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.LibraryUploadIntentResponse;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.ReplaceAssignmentsRequest;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.UpdateLibraryItemRequest;
import com.barbonperformance.analysis.drills.domain.DrillCategory;
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
@RequestMapping("/api/v1/drills")
public class DrillController {

    private final DrillLibraryService library;
    private final DrillAssignmentService assignments;

    public DrillController(DrillLibraryService library, DrillAssignmentService assignments) {
        this.library = library;
        this.assignments = assignments;
    }

    @GetMapping("/library")
    @Operation(summary = "The organisation's whole drill library")
    List<LibraryItemView> library(@AuthenticationPrincipal AuthenticatedUser principal) {
        return library.list(principal.id());
    }

    @PostMapping("/library")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Add a drill to the library once its video has finished uploading")
    LibraryItemView createLibraryItem(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateLibraryItemRequest request) {
        return library.createLibraryItem(principal.id(), request);
    }

    @PostMapping("/library/upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for a drill video")
    LibraryUploadIntentResponse uploadIntent(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody LibraryUploadIntentRequest request) {
        return library.requestUploadIntent(principal.id(), request);
    }

    @PutMapping("/library/{itemId}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Edit a drill's title, cue or category")
    LibraryItemView updateLibraryItem(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID itemId, @Valid @RequestBody UpdateLibraryItemRequest request) {
        return library.update(principal.id(), itemId, request);
    }

    @DeleteMapping("/library/{itemId}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Remove a drill from the library (rejected while it is assigned)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteLibraryItem(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID itemId) {
        library.delete(principal.id(), itemId);
    }

    @GetMapping("/library/{itemId}/playback")
    @Operation(summary = "A short-lived signed playback token for a drill video")
    DrillPlaybackView playback(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID itemId) {
        return library.playback(principal.id(), itemId);
    }

    @GetMapping("/assignments")
    @Operation(summary = "Assigned drills: a player's own, or a coach's client's")
    List<DrillAssignmentView> assignments(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) DrillCategory category,
            @RequestParam(required = false) UUID analysisId,
            @RequestParam(required = false) UUID playerProfileId) {
        return assignments.listForCaller(principal.id(), category, analysisId, playerProfileId);
    }

    @PutMapping("/analyses/{analysisId}/assignments")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace the whole drill list attached to one analysis")
    List<DrillAssignmentView> replaceForAnalysis(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId, @Valid @RequestBody ReplaceAssignmentsRequest request) {
        return assignments.replaceForAnalysis(principal.id(), analysisId, request);
    }
}
