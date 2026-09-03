package com.barbonperformance.courtproof.video.infrastructure.web;

import com.barbonperformance.courtproof.auth.domain.AuthenticatedUser;
import com.barbonperformance.courtproof.video.application.PlaybackService;
import com.barbonperformance.courtproof.video.application.VideoUploadService;
import com.barbonperformance.courtproof.video.application.dto.VideoDtos.PlaybackResponse;
import com.barbonperformance.courtproof.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.courtproof.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.courtproof.video.application.dto.VideoDtos.UploadIntentResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VideoController {

    private final VideoUploadService uploads;
    private final PlaybackService playback;

    public VideoController(VideoUploadService uploads, PlaybackService playback) {
        this.uploads = uploads;
        this.playback = playback;
    }

    @PostMapping("/submissions/upload-intent")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "Reserve a submission and get a one-time Cloudflare upload URL")
    ResponseEntity<UploadIntentResponse> uploadIntent(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UploadIntentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploads.requestUploadIntent(principal.id(), request));
    }

    @GetMapping("/submissions/mine")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "The signed-in player's own submissions")
    List<SubmissionSummary> mySubmissions(@AuthenticationPrincipal AuthenticatedUser principal) {
        return uploads.mySubmissions(principal.id());
    }

    @GetMapping("/videos/{videoAssetId}/playback")
    @Operation(summary = "A short-lived signed playback token for a video the caller may watch")
    PlaybackResponse playback(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID videoAssetId) {
        return playback.playback(principal.id(), videoAssetId);
    }
}
