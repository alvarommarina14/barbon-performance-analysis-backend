package com.barbonperformance.analysis.analysis.infrastructure.web;

import com.barbonperformance.analysis.analysis.application.AnalysisPlaybackService;
import com.barbonperformance.analysis.analysis.application.AnalysisReadService;
import com.barbonperformance.analysis.analysis.application.AnalysisService;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisDetail;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisSummary;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.CreateAnalysisRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FeedbackUploadIntentRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FeedbackUploadIntentResponse;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.PlaybackView;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.ReplaceFindingsRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.UpdateAnalysisRequest;
import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnalysisReadService readService;
    private final AnalysisPlaybackService playbackService;

    public AnalysisController(AnalysisService analysisService, AnalysisReadService readService,
            AnalysisPlaybackService playbackService) {
        this.analysisService = analysisService;
        this.readService = readService;
        this.playbackService = playbackService;
    }

    @GetMapping
    @Operation(summary = "Analyses visible to the caller: a player's own published ones, or a coach's")
    List<AnalysisSummary> list(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return readService.list(principal.id(), playerProfileId);
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "One analysis with its findings")
    AnalysisDetail detail(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId) {
        return readService.detail(principal.id(), analysisId);
    }

    @GetMapping("/{analysisId}/playback")
    @Operation(summary = "A short-lived signed playback token for the feedback video")
    PlaybackView playback(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId) {
        return playbackService.playback(principal.id(), analysisId);
    }

    @PostMapping
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Start a draft analysis for a submission or a player")
    ResponseEntity<AnalysisDetail> create(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateAnalysisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(analysisService.create(principal.id(), request));
    }

    @PutMapping("/{analysisId}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Edit a draft analysis' title and summary")
    AnalysisDetail update(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId, @Valid @RequestBody UpdateAnalysisRequest request) {
        return analysisService.update(principal.id(), analysisId, request);
    }

    @PutMapping("/{analysisId}/findings")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace the whole finding list of a draft analysis")
    AnalysisDetail replaceFindings(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId, @Valid @RequestBody ReplaceFindingsRequest request) {
        return analysisService.replaceFindings(principal.id(), analysisId, request);
    }

    @PostMapping("/{analysisId}/feedback-upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a Cloudflare upload URL for the edited feedback video")
    ResponseEntity<FeedbackUploadIntentResponse> feedbackUploadIntent(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID analysisId,
            @Valid @RequestBody FeedbackUploadIntentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(analysisService.requestFeedbackUploadIntent(principal.id(), analysisId, request));
    }

    @PostMapping("/{analysisId}/publish")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Publish a draft analysis so the player can watch it")
    AnalysisDetail publish(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId) {
        return analysisService.publish(principal.id(), analysisId);
    }

    @DeleteMapping("/{analysisId}")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Discard a draft analysis")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID analysisId) {
        analysisService.deleteDraft(principal.id(), analysisId);
        return ResponseEntity.noContent().build();
    }
}
