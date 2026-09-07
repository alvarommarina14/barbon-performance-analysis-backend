package com.barbonperformance.analysis.video.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.video.application.PlaybackService;
import com.barbonperformance.analysis.video.application.VideoUploadService;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.CreateSubmissionRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.DownloadUrlResponse;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.PlaybackResponse;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @Operation(summary = "Register a video and get a one-time resumable upload URL")
    ResponseEntity<UploadIntentResponse> uploadIntent(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UploadIntentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploads.requestUploadIntent(principal.id(), request));
    }

    @PostMapping("/submissions")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "Create the submission once the video has finished uploading")
    ResponseEntity<SubmissionSummary> createSubmission(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateSubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploads.createSubmission(principal.id(), request));
    }

    @GetMapping("/submissions/mine")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "The signed-in player's own submissions")
    List<SubmissionSummary> mySubmissions(@AuthenticationPrincipal AuthenticatedUser principal) {
        return uploads.mySubmissions(principal.id());
    }

    @DeleteMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "Discard one of your submissions the coach has not started on")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteSubmission(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID submissionId) {
        uploads.deleteSubmission(principal.id(), submissionId);
    }

    @GetMapping("/submissions/queue")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Submissions from the coach's clients that still need an analysis")
    List<SubmissionSummary> coachQueue(@AuthenticationPrincipal AuthenticatedUser principal) {
        return uploads.coachQueue(principal.id());
    }

    @GetMapping("/videos/{videoAssetId}/download-url")
    @Operation(summary = "A download URL for the original video the caller may access")
    DownloadUrlResponse downloadUrl(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID videoAssetId) {
        return playback.downloadUrl(principal.id(), videoAssetId);
    }

    @GetMapping("/videos/{videoAssetId}/playback")
    @Operation(summary = "A short-lived signed playback token for a video the caller may watch")
    PlaybackResponse playback(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID videoAssetId) {
        return playback.playback(principal.id(), videoAssetId);
    }
}
